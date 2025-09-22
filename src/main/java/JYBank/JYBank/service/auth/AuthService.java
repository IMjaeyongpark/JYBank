package JYBank.JYBank.service.auth;

import JYBank.JYBank.domain.user.AppUser;
import JYBank.JYBank.domain.user.KycStatus;
import JYBank.JYBank.domain.user.UserRole;
import JYBank.JYBank.dto.auth.LoginDtos.*;
import JYBank.JYBank.dto.auth.SignUpDtos.*;
import JYBank.JYBank.dto.auth.TokenRefreshDtos.*;
import JYBank.JYBank.repository.AppUserRepository;
import JYBank.JYBank.support.mail.SmtpMailService;
import JYBank.JYBank.util.JwtUtil;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;

import JYBank.JYBank.aop.annotation.Auditable;
import JYBank.JYBank.aop.annotation.RateLimited;
import JYBank.JYBank.aop.annotation.Idempotent;


import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Optional;
import java.util.Set;

@Service
public class AuthService {
    @Value("${jwt.secret}")
    private String secretKey;

    @Value("${app.jwt.access-ttl:3600000}")
    private Long accessTokenExpiredMs;

    @Value("${app.jwt.refresh-ttl:1209600000}")
    private Long refreshTokenExpiredMs;

    @Value("${app.front-url}")
    private String frontBaseUrl;
    private final SmtpMailService mailer;

    // ====== TTL 상수 ======
    private static final Duration EMAIL_VERIFY_TTL = Duration.ofMinutes(10);
    private static final Duration EMAIL_VERIFY_THROTTLE = Duration.ofSeconds(60); // 1분 쿨다운
    private static final Duration PW_RESET_TTL = Duration.ofMinutes(15);
    private static final Duration PW_RESET_THROTTLE = Duration.ofMinutes(2);

    // ====== 코드/토큰 생성 ======
    private static final SecureRandom RNG = new SecureRandom();


    private final AppUserRepository userRepo;
    private final PasswordEncoder passwordEncoder;

    private final StringRedisTemplate redis;

    public AuthService(SmtpMailService mailer, AppUserRepository userRepo, PasswordEncoder passwordEncoder, StringRedisTemplate redis) {
        this.mailer = mailer;
        this.userRepo = userRepo;
        this.passwordEncoder = passwordEncoder;
        this.redis = redis;
    }


    private String rKey(String refreshToken) { // refresh 토큰 해시 키
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(refreshToken.getBytes(StandardCharsets.UTF_8));
            return "auth:refresh:" + Base64.getUrlEncoder().withoutPadding().encodeToString(digest);
        } catch (Exception e) {
            return "auth:refresh:" + Integer.toHexString(refreshToken.hashCode());
        }
    }


    private boolean existsRefresh(String refreshToken) {
        return Boolean.TRUE.equals(redis.hasKey(rKey(refreshToken)));
    }

    private String uSetKey(String loginId) {
        return "auth:user:" + loginId + ":refresh";
    }

    //레디스에 리프레쉬 토큰 저장
    private void storeRefresh(String refreshToken, String loginId) {
        String key = rKey(refreshToken);
        Duration ttl = Duration.ofMillis(refreshTokenExpiredMs);

        // 개별 토큰 화이트리스트
        redis.opsForValue().set(key, "1", ttl);

        // 사용자별 세션 Set에 등록
        String setKey = uSetKey(loginId);
        redis.opsForSet().add(setKey, key);
        redis.expire(setKey, ttl);
    }

    //레디스에 리프레쉬 토큰 삭제 - 로그아웃
    private void deleteRefresh(String refreshToken, String loginId) {
        String key = rKey(refreshToken);
        redis.delete(key);
        if (loginId != null && !loginId.isBlank()) {
            redis.opsForSet().remove(uSetKey(loginId), key);
        }
    }


    // 회원가입
    @Auditable(action = "REGISTER")
// 동시/중복 가입 방지: 같은 이메일 5분 동안 1회만 처리
    @Idempotent(key = "'register:' + #req.email()", ttlSeconds = 300)
// 과도한 가입 시도 제어: 이메일당 분당 3회
    @RateLimited(key = "'register:' + #req.email()", permitsPerMinute = 3)
    @Transactional
    public SignUpResponse register(SignUpRequest req) {
        //소문자로 변경
        String email = req.email().trim().toLowerCase();

        //아이디 중복 확인
        if (userRepo.existsByEmailIgnoreCase(email)) {
            throw new EmailAlreadyUsedException(email);
        }

        //유저 생성
        AppUser user = AppUser.builder().email(email).passwordHash(passwordEncoder.encode(req.password())).fullName(req.fullName()).phone(req.phone()).kycStatus(KycStatus.PENDING).role(UserRole.USER).build();

        //유저 저장
        AppUser saved = userRepo.save(user);
        return new SignUpResponse(saved.getUserId(), saved.getEmail(), saved.getKycStatus().name(), saved.getCreatedAt());
    }


    // 로그인
    @Auditable(action = "LOGIN")
// 이메일 기준 분당 10회 제한 (IP까지 쓰고 싶으면 서비스 시그니처에 ip 추가 후 "'login:'+ #req.email() + ':' + #ip")
    @RateLimited(key = "'login:' + #req.email()", permitsPerMinute = 10)
    @Transactional
    public LoginResponse login(LoginRequest req) {
        // 1) 이메일 정규화
        String email = Optional.ofNullable(req.email()).orElse("").trim().toLowerCase();

        // 2) 유저 조회 & 패스워드 검증
        AppUser user = userRepo.findByEmailIgnoreCase(email).orElseThrow(() -> new IllegalArgumentException("잘못된 이메일 또는 비밀번호"));

        if (!passwordEncoder.matches(req.password(), user.getPasswordHash())) {
            throw new IllegalArgumentException("잘못된 이메일 또는 비밀번호");
        }

        // 3) 토큰 발급
        String accessToken = JwtUtil.createAccessToken(user.getEmail(), secretKey, accessTokenExpiredMs);
        String refreshToken = JwtUtil.createRefreshToken(user.getEmail(), secretKey, refreshTokenExpiredMs);

        // 4) Redis 화이트리스트 등록 (TTL=refresh 만료와 동일)
        storeRefresh(refreshToken, user.getEmail());

        return new LoginResponse(accessToken, refreshToken);
    }

    // 리프레시 토큰 재발급 (회전)
// 재사용 탐지/보안 이벤트를 감사로그로 남김
    @Auditable(action = "TOKEN_REFRESH")
//과도한 재발급 방지 — 리프레시 키 해시를 쓰고 싶으면 서비스 시그니처에 해시 계산 추가 또는 #req.refreshToken()에 해시 함수 적용
    @RateLimited(key = "'refresh:' + #req.refreshToken()", permitsPerMinute = 6)
    public TokenRefreshResponse refresh(TokenRefreshRequest req) {
        String oldRefresh = req.refreshToken();

        // 1) 만료/타입 검증
        if (JwtUtil.isExpired(oldRefresh, secretKey)) {
            throw new IllegalArgumentException("리프레시 토큰이 만료되었습니다.");
        }
        if (!JwtUtil.isRefreshToken(oldRefresh, secretKey)) {
            throw new IllegalArgumentException("Access 토큰으로는 재발급할 수 없습니다.");
        }

        // 2) 화이트리스트 확인 (로그아웃/폐기된 세션 차단)
        if (!existsRefresh(oldRefresh)) {
            throw new IllegalArgumentException("세션이 유효하지 않습니다. 다시 로그인 해주세요.");
        }

        // 3) 주체(loginId) 추출
        String loginId = JwtUtil.getLoginId(oldRefresh, secretKey);

        // 4) 새 토큰 발급 (회전)
        String newAccess = JwtUtil.createAccessToken(loginId, secretKey, accessTokenExpiredMs);
        String newRefresh = JwtUtil.createRefreshToken(loginId, secretKey, refreshTokenExpiredMs);

        // 5) 기존 refresh 폐기 → 새 refresh 저장
        deleteRefresh(oldRefresh, loginId);
        storeRefresh(newRefresh, loginId);

        return new TokenRefreshResponse(newAccess, newRefresh, Instant.now().plusMillis(accessTokenExpiredMs));
    }

    // 전체 로그아웃(로그인 아이디 기준)
    @Auditable(action = "LOGOUT_ALL")
// 오남용 방지: 분당 3회
    @RateLimited(key = "'logout:' + #loginId", permitsPerMinute = 3)
    @Transactional
    public int logout(String loginId) {
        if (loginId == null || loginId.isBlank()) return 0;

        String setKey = uSetKey(loginId);
        Set<String> tokenKeys = redis.opsForSet().members(setKey);

        int revoked = 0;
        if (tokenKeys != null && !tokenKeys.isEmpty()) {
            Long n = redis.delete(tokenKeys);      // 모든 refresh whitelist 키 일괄 삭제
            revoked = (n == null) ? 0 : n.intValue();
        }
        redis.delete(setKey);                      // 사용자 Set 자체 삭제

        // (옵션 강추) 즉시 무효화: lastLogoutAt 갱신 → Access iat 비교로 즉시 차단
        userRepo.findByEmailIgnoreCase(loginId).ifPresent(u -> {
            u.setLastLogoutAt(Instant.now());
            userRepo.save(u);
        });

        return revoked;
    }


    public static class EmailAlreadyUsedException extends RuntimeException {
        public EmailAlreadyUsedException(String email) {
            super("이미 가입된 이메일입니다: " + email);
        }
    }

    // ====== Redis 키 유틸 ======
    private String emailVerifyKey(String email) {             // 코드 저장
        return "auth:email:verify:" + email.toLowerCase();
    }

    private String emailVerifiedFlagKey(String email) {       // 영구 인증 플래그
        return "auth:email:verified:" + email.toLowerCase();
    }

    private String emailVerifyThrottleKey(String email) {     // 발송 레이트리밋
        return "auth:email:verify:throttle:" + email.toLowerCase();
    }

    private String pwResetKey(String code) {                  // 코드→email 매핑
        return "auth:pwreset:" + code;
    }

    private String pwResetThrottleKey(String email) {         // 발송 레이트리밋
        return "auth:pwreset:throttle:" + email.toLowerCase();
    }

    private String random6Digit() {
        int n = RNG.nextInt(1_000_000); // 0~999999
        return String.format("%06d", n);
    }

    private String randomToken() {
        byte[] b = new byte[24];
        RNG.nextBytes(b);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(b);
    }

    // ====== 레이트리밋 헬퍼 ======
    private void bumpThrottle(String key, Duration window, int limit) {
        Long c = redis.opsForValue().increment(key);
        if (c != null && c == 1) {
            redis.expire(key, window);
        }
        if (c != null && c > limit) {
            throw new IllegalStateException("요청이 너무 많습니다. 잠시 후 다시 시도해 주세요.");
        }
    }

    // ====== 이메일 인증 발송 ======
    public String sendEmailVerification(String email) {
        String normalized = Optional.ofNullable(email).orElse("").trim().toLowerCase();
        if (normalized.isBlank()) throw new IllegalArgumentException("이메일이 필요합니다.");

        // 존재하는 사용자만 발송(정보 노출 방지엔 보통 같은 응답, 여기선 단순 처리)
        userRepo.findByEmailIgnoreCase(normalized).orElseThrow(() -> new IllegalArgumentException("가입되지 않은 이메일입니다."));

        // 레이트리밋: 같은 이메일 1분에 1회(연속 스팸 방지)
        bumpThrottle(emailVerifyThrottleKey(normalized), EMAIL_VERIFY_THROTTLE, 1);

        // 이미 인증된 경우 조용히 성공으로 처리해도 됨
        if (Boolean.TRUE.equals(redis.hasKey(emailVerifiedFlagKey(normalized)))) {
            return "000000"; // or null; 실제 운영에선 같은 메시지 반환 권장
        }

        String code = random6Digit();
        redis.opsForValue().set(emailVerifyKey(normalized), code, EMAIL_VERIFY_TTL);

        // TODO: 실제 메일 전송 구현 (템플릿/메일러)
        // mailer.sendVerificationCode(normalized, code);

        return code; // 개발/테스트 편의상 반환(운영에선 반환 X 권장)
    }

    // ====== 이메일 인증 검증 ======
    // 이메일 인증 코드 검증
    @Auditable(action = "EMAIL_VERIFY_CONFIRM")
// 분당 6회 허용
    @RateLimited(key = "'email:verify:confirm:' + #email", permitsPerMinute = 6)
    @Transactional
    public boolean verifyEmailCode(String email, String code) {
        String normalized = Optional.ofNullable(email).orElse("").trim().toLowerCase();
        if (normalized.isBlank() || code == null) return false;

        String saved = redis.opsForValue().get(emailVerifyKey(normalized));
        if (saved == null) return false;
        if (!saved.equals(code.trim())) return false;

        // 성공: 코드 제거 + 영구 플래그 저장(무기한, Redis 영속성 환경 권장 / 추후 DB 컬럼 이동)
        redis.delete(emailVerifyKey(normalized));
        redis.opsForValue().set(emailVerifiedFlagKey(normalized), "1");

        // (선택-권장) DB에도 반영: AppUser.emailVerifiedAt 추가 시
        // AppUser u = userRepo.findByEmailIgnoreCase(normalized).orElseThrow();
        // u.setEmailVerifiedAt(Instant.now());
        // userRepo.save(u);

        return true;
    }

    // ====== 이메일 인증 상태 조회/보장 ======
    public boolean isEmailVerified(String loginId) {
        // 우선 Redis 플래그 확인 (추후 DB 컬럼이 있으면 거기 먼저 확인)
        return Boolean.TRUE.equals(redis.hasKey(emailVerifiedFlagKey(loginId)));
    }

    public void ensureEmailVerified(String loginId) {
        if (!isEmailVerified(loginId)) {
            throw new IllegalStateException("이메일 인증이 필요합니다.");
        }
    }

    // ====== 비밀번호 재설정: 시작(코드/링크 발급) ======

    // 비밀번호 재설정 시작 (코드/링크 발급)
    @Auditable(action = "PW_RESET_START")
// 분당 1회
    @RateLimited(key = "'pwreset:start:' + #email", permitsPerMinute = 1)
    public String startPasswordReset(String email) {
        String normalized = Optional.ofNullable(email).orElse("").trim().toLowerCase();
        // 존재 유무 노출 방지 정책이라면 "성공"만 반환하고 내부적으로만 처리하는 것도 가능
        userRepo.findByEmailIgnoreCase(normalized).orElseThrow(() -> new IllegalArgumentException("가입되지 않은 이메일입니다."));

        // 레이트리밋: 같은 이메일 2분에 1회
        bumpThrottle(pwResetThrottleKey(normalized), PW_RESET_THROTTLE, 1);

        String code = randomToken();
        redis.opsForValue().set(pwResetKey(code), normalized, PW_RESET_TTL);


// 실제 메일 전송 (프론트 미정이어도 베이스 URL만 바꾸면 OK)
        String link = frontBaseUrl + "/reset?code=" + code;
        mailer.sendPasswordResetLink(normalized, link);

        return code; // dev에서만 노출, 운영은 숨김 권장

    }

    // ====== 비밀번호 재설정: 완료 ======

    // 비밀번호 재설정 완료
    @Auditable(action = "PW_RESET_FINISH")
// 같은 코드로 중복 호출 방지 (코드 TTL 15분과 일치)
    @Idempotent(key = "'pwreset:finish:' + #code", ttlSeconds = 900)
// 오남용 방지
    @RateLimited(key = "'pwreset:finish:' + #code", permitsPerMinute = 6)
    @Transactional
    public void resetPassword(String code, String newPassword) {
        if (code == null || code.isBlank()) throw new IllegalArgumentException("코드가 필요합니다.");
        if (newPassword == null || newPassword.length() < 8) throw new IllegalArgumentException("비밀번호는 8자 이상이어야 합니다.");

        String email = redis.opsForValue().get(pwResetKey(code));
        if (email == null) throw new IllegalArgumentException("코드가 만료되었거나 잘못되었습니다.");

        AppUser u = userRepo.findByEmailIgnoreCase(email).orElseThrow();
        u.setPasswordHash(passwordEncoder.encode(newPassword));
        //비밀번호 변경 시각 저장 필드가 있으면 업데이트
        u.setLastPasswordChangeAt(Instant.now());
        userRepo.save(u);

        // 보안: 모든 세션 강제 로그아웃(Refresh 전부 폐기)
        logout(email);

        // 코드 사용 소진
        redis.delete(pwResetKey(code));
    }

}

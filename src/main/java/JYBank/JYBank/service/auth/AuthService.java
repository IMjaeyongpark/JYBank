package JYBank.JYBank.service.auth;

import JYBank.JYBank.domain.user.AppUser;
import JYBank.JYBank.domain.user.KycStatus;
import JYBank.JYBank.domain.user.UserRole;
import JYBank.JYBank.dto.auth.LoginDtos.*;
import JYBank.JYBank.dto.auth.SignUpDtos.*;
import JYBank.JYBank.dto.auth.TokenRefreshDtos.*;
import JYBank.JYBank.repository.AppUserRepository;
import JYBank.JYBank.util.JwtUtil;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

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


    private final AppUserRepository userRepo;
    private final PasswordEncoder passwordEncoder;

    private final StringRedisTemplate redis;

    public AuthService(AppUserRepository userRepo, PasswordEncoder passwordEncoder, StringRedisTemplate redis) {
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


    //회원가입
    @Transactional
    public SignUpResponse register(SignUpRequest req) {
        //소문자로 변경
        String email = req.email().trim().toLowerCase();

        //아이디 중복 확인
        if (userRepo.existsByEmailIgnoreCase(email)) {
            throw new EmailAlreadyUsedException(email);
        }

        //유저 생성
        AppUser user = AppUser.builder()
                .email(email)
                .passwordHash(passwordEncoder.encode(req.password()))
                .fullName(req.fullName())
                .phone(req.phone())
                .kycStatus(KycStatus.PENDING)
                .role(UserRole.USER)
                .build();

        //유저 저장
        AppUser saved = userRepo.save(user);
        return new SignUpResponse(
                saved.getUserId(),
                saved.getEmail(),
                saved.getKycStatus().name(),
                saved.getCreatedAt()
        );
    }


    //유저 로그인
    @Transactional
    public LoginResponse login(LoginRequest req) {
        // 1) 이메일 정규화
        String email = Optional.ofNullable(req.email()).orElse("").trim().toLowerCase();

        // 2) 유저 조회 & 패스워드 검증
        AppUser user = userRepo.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new IllegalArgumentException("잘못된 이메일 또는 비밀번호"));

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

    //리프레쉬 토큰 재발급
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

        return new TokenRefreshResponse(newAccess, newRefresh,
                Instant.now().plusMillis(accessTokenExpiredMs));
    }

    // 전체 디바이스에서 로그아웃: loginId(=email) 기준으로 모든 Refresh 폐기
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


}

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
            // fallback (실무에선 로깅)
            return "auth:refresh:" + Integer.toHexString(refreshToken.hashCode());
        }
    }

    private void storeRefresh(String refreshToken) {
        String key = rKey(refreshToken);
        redis.opsForValue().set(key, "1", Duration.ofMillis(refreshTokenExpiredMs));
    }

    private boolean existsRefresh(String refreshToken) {
        return Boolean.TRUE.equals(redis.hasKey(rKey(refreshToken)));
    }

    private void deleteRefresh(String refreshToken) {
        redis.delete(rKey(refreshToken));
    }

    @Transactional
    public SignUpResponse register(SignUpRequest req) {
        String email = req.email().trim().toLowerCase();

        if (userRepo.existsByEmailIgnoreCase(email)) {
            throw new EmailAlreadyUsedException(email);
        }

        AppUser user = AppUser.builder()
                .email(email)
                .passwordHash(passwordEncoder.encode(req.password()))
                .fullName(req.fullName())
                .phone(req.phone())
                .kycStatus(KycStatus.PENDING)
                .role(UserRole.USER)
                .build();

        AppUser saved = userRepo.save(user);
        return new SignUpResponse(
                saved.getUserId(),
                saved.getEmail(),
                saved.getKycStatus().name(),
                saved.getCreatedAt()
        );
    }


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
        storeRefresh(refreshToken);

        return new LoginResponse(accessToken, refreshToken);
    }

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
        deleteRefresh(oldRefresh);
        storeRefresh(newRefresh);

        return new TokenRefreshResponse(newAccess, newRefresh,
                Instant.now().plusMillis(accessTokenExpiredMs));
    }


    public static class EmailAlreadyUsedException extends RuntimeException {
        public EmailAlreadyUsedException(String email) {
            super("이미 가입된 이메일입니다: " + email);
        }
    }


}

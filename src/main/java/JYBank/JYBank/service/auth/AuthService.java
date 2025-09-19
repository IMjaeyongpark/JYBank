package JYBank.JYBank.service.auth;

import JYBank.JYBank.domain.user.AppUser;
import JYBank.JYBank.domain.user.KycStatus;
import JYBank.JYBank.domain.user.UserRole;
import JYBank.JYBank.dto.auth.SignUpDtos;
import JYBank.JYBank.repository.AppUserRepository;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {
    @Value("${jwt.secret}")
    private String secretKey;

    //30분
    private Long accessTokenExpiredMs = 1000 * 60 * 30L;

    //1일
    private Long refreshTokenExpiredMs = 1000 * 60 * 60 * 24L;


    private final AppUserRepository userRepo;
    private final PasswordEncoder passwordEncoder;

    public AuthService(AppUserRepository userRepo, PasswordEncoder passwordEncoder) {
        this.userRepo = userRepo;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public SignUpDtos.SignUpResponse register(SignUpDtos.SignUpRequest req) {
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
        return new SignUpDtos.SignUpResponse(
                saved.getUserId(),
                saved.getEmail(),
                saved.getKycStatus().name(),
                saved.getCreatedAt()
        );
    }

    public static class EmailAlreadyUsedException extends RuntimeException {
        public EmailAlreadyUsedException(String email) {
            super("이미 가입된 이메일입니다: " + email);
        }
    }
}

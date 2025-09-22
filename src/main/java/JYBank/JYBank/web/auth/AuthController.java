package JYBank.JYBank.web.auth;

import JYBank.JYBank.dto.auth.EmailVerificationDtos.*;
import JYBank.JYBank.dto.auth.LoginDtos.*;
import JYBank.JYBank.dto.auth.PasswordResetDtos.*;
import JYBank.JYBank.dto.auth.SignUpDtos.*;
import JYBank.JYBank.dto.auth.TokenRefreshDtos.*;
import JYBank.JYBank.service.auth.AuthService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.Map;


@RestController
@RequestMapping("/v1/auth")
public class AuthController {

    @Value("${app.expose-dev-codes:false}")
    private boolean exposeDevCodes;
    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }


    //회원가입
    @PostMapping("/register")
    public ResponseEntity<SignUpResponse> register(@RequestBody @Valid SignUpRequest req) {
        SignUpResponse res = authService.register(req);
        return ResponseEntity.created(URI.create("/v1/users/" + res.userId())).body(res);
    }

    //로그인
    @GetMapping("/login")
    public ResponseEntity<LoginResponse> register(@RequestBody @Valid LoginRequest req) {
        return ResponseEntity.ok(authService.login(req));
    }

    //리프레쉬 토큰 재발급
    @PostMapping("/refresh")
    public ResponseEntity<TokenRefreshResponse> refresh(@RequestBody @Valid TokenRefreshRequest req) {
        return ResponseEntity.ok(authService.refresh(req));
    }

    @PostMapping("/logout")
    public ResponseEntity<Map<String, Object>> logoutAll(@AuthenticationPrincipal(expression = "username") String loginId) {
        int n = authService.logout(loginId);
        return ResponseEntity.ok(Map.of("revokedSessions", n));
    }

    // 이메일 인증 코드 발송
    @PostMapping("/email/verify/send")
    public ResponseEntity<SendResponse> sendVerify(@RequestBody @Valid SendRequest req) {
        String code = authService.sendEmailVerification(req.email());
        return ResponseEntity.ok(new SendResponse(true, exposeDevCodes ? code : null));
    }

    // 이메일 인증 코드 검증
    @PostMapping("/email/verify/confirm")
    public ResponseEntity<ConfirmResponse> confirm(@RequestBody @Valid ConfirmRequest req) {
        boolean ok = authService.verifyEmailCode(req.email(), req.code());
        return ResponseEntity.ok(new ConfirmResponse(ok));
    }

    // 비밀번호 재설정 시작(코드/링크 발급)
    @PostMapping("/password/reset/start")
    public ResponseEntity<StartResponse> startReset(@RequestBody @Valid StartRequest req) {
        String code = authService.startPasswordReset(req.email());
        return ResponseEntity.ok(new StartResponse(true, exposeDevCodes ? code : null));
    }

    // 비밀번호 재설정 완료
    @PostMapping("/password/reset/finish")
    public ResponseEntity<Void> finishReset(@RequestBody @Valid FinishRequest req) {
        authService.resetPassword(req.code(), req.newPassword());
        return ResponseEntity.noContent().build();
    }
}

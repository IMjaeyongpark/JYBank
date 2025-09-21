package JYBank.JYBank.web.auth;

import JYBank.JYBank.dto.auth.LoginDtos.*;
import JYBank.JYBank.dto.auth.SignUpDtos.*;
import JYBank.JYBank.dto.auth.TokenRefreshDtos.*;
import JYBank.JYBank.service.auth.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;


@RestController
@RequestMapping("/v1/auth")
public class AuthController {
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

}

package JYBank.JYBank.web.auth;

import JYBank.JYBank.dto.auth.LoginDtos;
import JYBank.JYBank.dto.auth.LoginDtos.*;
import JYBank.JYBank.dto.auth.SignUpDtos.*;
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


    @PostMapping("/register")
    public ResponseEntity<SignUpResponse> register(@RequestBody @Valid SignUpRequest req) {
        SignUpResponse res = authService.register(req);
        return ResponseEntity.created(URI.create("/v1/users/" + res.userId())).body(res);
    }

    @GetMapping("/login")
    public ResponseEntity<LoginResponse> register(@RequestBody @Valid LoginRequest req) {
        return ResponseEntity.ok(authService.login(req));
    }

}

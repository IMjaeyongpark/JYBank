package JYBank.JYBank.dto.auth;

import jakarta.validation.constraints.NotBlank;

import java.time.Instant;

public class TokenRefreshDtos {
    public record TokenRefreshRequest(
            @NotBlank String refreshToken
    ) {}


    public record TokenRefreshResponse(
            String accessToken,
            String refreshToken,
            Instant accessTokenExpiresAt
    ) {}
}

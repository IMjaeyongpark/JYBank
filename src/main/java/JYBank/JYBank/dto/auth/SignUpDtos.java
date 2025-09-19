package JYBank.JYBank.dto.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

public class SignUpDtos {

    public record SignUpRequest(
            @NotBlank @Email String email,
            @NotBlank @Size(min = 8, max = 100) String password,
            @Size(max = 100) String fullName,
            @Size(max = 30) String phone
    ) {}

    public record SignUpResponse(
            Long userId,
            String email,
            String kycStatus,
            LocalDateTime createdAt
    ) {}
}

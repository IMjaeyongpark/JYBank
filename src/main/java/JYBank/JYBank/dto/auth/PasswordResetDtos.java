package JYBank.JYBank.dto.auth;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public final class PasswordResetDtos {

    // 시작(코드/링크 발급) 요청
    public record StartRequest(
            @NotBlank @Email String email
    ) {}

    // 시작 응답 (운영에서는 devCode 숨김 권장)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record StartResponse(
            boolean sent,
            String devCode
    ) {}

    // 완료 요청
    public record FinishRequest(
            @NotBlank String code,
            @NotBlank @Size(min = 8, max = 64, message = "비밀번호는 8~64자")
            String newPassword
    ) {}
}

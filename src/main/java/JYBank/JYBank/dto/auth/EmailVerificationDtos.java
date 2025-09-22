package JYBank.JYBank.dto.auth;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public final class EmailVerificationDtos {

    // 발송 요청
    public record SendRequest(
            @NotBlank @Email String email
    ) {}

    // 발송 응답 (운영에서는 devCode 숨김 권장)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record SendResponse(
            boolean sent,
            String devCode
    ) {}

    // 검증 요청
    public record ConfirmRequest(
            @NotBlank @Email String email,
            @NotBlank @Pattern(regexp = "^[0-9]{6}$", message = "6자리 숫자여야 합니다.")
            String code
    ) {}

    // 검증 응답
    public record ConfirmResponse(
            boolean verified
    ) {}
}

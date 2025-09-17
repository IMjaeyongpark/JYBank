package JYBank.JYBank.dto;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;

public class PayoutDtos {
    public record CreateReq(@NotNull Long walletId, @NotBlank String bankCode, @NotBlank String accountNo,
                            @NotNull @Positive BigDecimal amount, @NotBlank String idemKey) {}
    public record CreateRes(Long payoutId, String status) {}
}

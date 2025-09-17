package JYBank.JYBank.dto;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;

public class DepositDtos {
    public record WebhookReq(@NotBlank String pgTrxId, @NotBlank String virtualAccount,
                             @NotNull @Positive BigDecimal amount, @NotNull Long walletId) {}
    public record Res(Long depositId, String status) {}
}
package JYBank.JYBank.dto;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;

public class TransferDtos {
    public record CreateReq(@NotNull Long sourceWalletId, @NotNull Long destWalletId,
                            @NotNull @Positive BigDecimal amount, String memo, @NotBlank String idemKey) {}
    public record CreateRes(Long transferId, String status) {}
}

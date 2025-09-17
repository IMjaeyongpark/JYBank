package JYBank.JYBank.domain.payout;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "payout")
public class Payout {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private Long walletId;
    private String bankCode;
    private String accountNo;
    @Column(precision = 20, scale = 0)
    private BigDecimal amount;
    private String status; // PENDING|PROCESSING|PAID|FAILED
    private String reason;
    @Column(unique = true)
    private String idemKey;
    private Instant createdAt = Instant.now();
}

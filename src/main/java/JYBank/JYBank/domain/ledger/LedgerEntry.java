package JYBank.JYBank.domain.ledger;

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
@Table(name = "ledger_entry")
public class LedgerEntry {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private Long walletId;
    private String direction; // debit | credit
    @Column(precision = 20, scale = 0)
    private BigDecimal amount;
    @Column(precision = 20, scale = 0)
    private BigDecimal balanceAfter;
    private String refType; // TRANSFER|PAYOUT|DEPOSIT
    private String refId;
    private Instant createdAt = Instant.now();
}

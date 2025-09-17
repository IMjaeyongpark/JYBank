package JYBank.JYBank.domain.deposit;

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
@Table(name = "deposit")
public class Deposit {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private Long walletId;
    @Column(unique = true)
    private String pgTrxId; // 멱등 키
    private String virtualAccount;
    @Column(precision = 20, scale = 0)
    private BigDecimal amount;
    private String status; // PENDING|COMPLETED|FAILED
    private Instant createdAt = Instant.now();
}

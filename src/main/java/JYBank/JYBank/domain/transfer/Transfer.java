package JYBank.JYBank.domain.transfer;

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
@Table(name = "transfer")
public class Transfer {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private Long sourceWalletId;
    private Long destWalletId;
    @Column(precision = 20, scale = 0)
    private BigDecimal amount;
    private String status; // PENDING|COMPLETED|FAILED
    @Column(unique = true)
    private String idemKey;
    private Instant createdAt = Instant.now();
}

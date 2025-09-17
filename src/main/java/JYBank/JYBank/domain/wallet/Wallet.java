package JYBank.JYBank.domain.wallet;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "wallet")
public class Wallet {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private Long userId;
    private String currency; // e.g. KRW
    @Column(precision = 20, scale = 0)
    private BigDecimal balance = BigDecimal.ZERO;
    @Column(precision = 20, scale = 0)
    private BigDecimal holdAmount = BigDecimal.ZERO;

    // getters/setters omitted

    public void credit(BigDecimal amount) { this.balance = this.balance.add(amount); }
    public void debit(BigDecimal amount) { this.balance = this.balance.subtract(amount); }
}

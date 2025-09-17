package JYBank.JYBank.domain.bank;

import jakarta.persistence.*;
import lombok.*;


@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "bank_account")
public class BankAccount {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private Long userId;
    private String bankCode; // 004 등
    private String accountNo;
    private String status; // VERIFIED|ACTIVE|CLOSED
}

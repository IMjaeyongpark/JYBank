package JYBank.JYBank.repository;

import JYBank.JYBank.domain.deposit.Deposit;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DepositRepository extends JpaRepository<Deposit, Long> {
}

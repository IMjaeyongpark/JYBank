package JYBank.JYBank.repository;

import JYBank.JYBank.domain.payout.Payout;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PayoutRepository extends JpaRepository<Payout, Long> {
}

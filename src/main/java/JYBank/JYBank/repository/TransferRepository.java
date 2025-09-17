package JYBank.JYBank.repository;

import JYBank.JYBank.domain.transfer.Transfer;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TransferRepository extends JpaRepository<Transfer, Long> {
}

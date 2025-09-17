package JYBank.JYBank.repository;

import JYBank.JYBank.domain.ledger.LedgerEntry;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LedgerEntryRepository extends JpaRepository<LedgerEntry, Long> {
}

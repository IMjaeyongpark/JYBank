package JYBank.JYBank.repository;

import JYBank.JYBank.domain.wallet.Wallet;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.util.Optional;

public interface WalletRepository extends JpaRepository<Wallet, Long> {
    @Query(value = "select * from wallet where id = :id for update", nativeQuery = true)
    Optional<Wallet> findByIdForUpdate(@Param("id") Long id);
}

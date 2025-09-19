package JYBank.JYBank.repository;

import JYBank.JYBank.domain.user.AppUser;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AppUserRepository extends JpaRepository<AppUser, Long> {
    boolean existsByEmailIgnoreCase(String email);
    Optional<AppUser> findByEmailIgnoreCase(String email);
}

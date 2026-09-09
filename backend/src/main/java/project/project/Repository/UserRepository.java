package project.project.Repository;

import org.springframework.data.jpa.repository.JpaRepository;
import project.project.Entity.user.User;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);
    Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);
    boolean existsByUsername(String username);
}

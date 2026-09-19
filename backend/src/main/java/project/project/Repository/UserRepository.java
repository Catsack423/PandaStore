package project.project.Repository;

import org.springframework.data.jpa.repository.JpaRepository;
import project.project.Entity.user.User;
import java.util.Optional;
import project.project.Entity.user.UserRole;
import project.project.Entity.user.UserStatus;
import java.util.List;


public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);
    Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);

    boolean existsByUsername(String username);

    Optional<User> findByUsername(String username);

    List<User> findByRoleAndStatus(UserRole role, UserStatus status);
}
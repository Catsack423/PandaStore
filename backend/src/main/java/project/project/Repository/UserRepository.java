package project.project.Repository;

import org.springframework.data.jpa.repository.JpaRepository;
import project.project.Entity.user.User;

public interface UserRepository extends JpaRepository<User, Long> {
    boolean existsByEmail(String email);
    boolean existsByUsername(String username);
}

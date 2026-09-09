package project.project.Repository;

import java.time.Instant;
import org.springframework.data.jpa.repository.JpaRepository;
import project.project.Entity.user.AuthSession;

public interface AuthSessionRepository extends JpaRepository<AuthSession, String> {
    void deleteByExpiresAtBefore(Instant now);
    void deleteByUser_UserId(Long userId);
}

package project.project.Repository;

import java.time.Instant;
import org.springframework.data.jpa.repository.JpaRepository;
import project.project.Entity.user.AuthSession;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;

public interface AuthSessionRepository extends JpaRepository<AuthSession, String> {
    @Override
    @EntityGraph(attributePaths = "user")
    Optional<AuthSession> findById(String tokenHash);
    void deleteByExpiresAtBefore(Instant now);
    void deleteByUser_UserId(Long userId);
}

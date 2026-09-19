package project.project.Entity.user;

import java.time.Instant;
import jakarta.persistence.*;

@Entity
@Table(name = "auth_sessions")
public class AuthSession {
    @Id
    @Column(length = 64)
    private String tokenHash;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private Instant expiresAt;

    @Column(nullable = false, length = 255)
    private String credentialHash;

    protected AuthSession() {}

    public AuthSession(String tokenHash, User user, Instant expiresAt) {
        this.tokenHash = tokenHash;
        this.user = user;
        this.expiresAt = expiresAt;
        this.credentialHash = user.getPasswordHash();
    }

    public User getUser() { return user; }
    public Instant getExpiresAt() { return expiresAt; }
    public String getCredentialHash() { return credentialHash; }
}

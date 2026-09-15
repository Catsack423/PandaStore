package project.project.Security;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import project.project.Entity.user.UserStatus;
import project.project.Repository.AuthSessionRepository;
import project.project.Service.implement.JwtTokenService;

@Service
public class SessionAuthenticator {
    private final JwtTokenService jwtService;
    private final AuthSessionRepository sessionsRepository;
    public SessionAuthenticator(JwtTokenService jwt, AuthSessionRepository sessionsRepository) {
        this.jwtService = jwt;
        this.sessionsRepository = sessionsRepository;
    }

    @Transactional(readOnly = true)
    public Optional<AuthenticatedUser> authenticate(String token) {
        //verify content in jwt
        var subject = jwtService.verifiedSubject(token);
        if (subject.isEmpty()) return Optional.empty();

        return sessionsRepository.findById(tokenHash(token))
                .filter(session -> session.getUser().getUserId().toString().equals(subject.get()))
                .filter(session -> session.getExpiresAt().isAfter(Instant.now()))
                .filter(session -> session.getUser().getStatus() == UserStatus.ACTIVE)
                .filter(session -> session.getCredentialHash().equals(session.getUser().getPasswordHash()))
                .map(session -> new AuthenticatedUser(session.getUser().getUserId(), session.getUser().getRole()));
    }

    private String tokenHash(String token) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(token.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is unavailable", e);
        }
    }
}

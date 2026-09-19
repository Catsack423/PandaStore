package project.project.Service.implement;

import java.time.Instant;
import java.util.Date;
import java.util.HexFormat;
import java.util.Optional;
import java.util.UUID;
import javax.crypto.SecretKey;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class JwtTokenService {
    private final SecretKey key;
    private final JwtParser parser;

    public JwtTokenService(@Value("${JWT_SECRET:}") String hexSecret) {
        if (hexSecret == null || !hexSecret.matches("[0-9a-fA-F]{64}")) {
            throw new IllegalStateException("JWT_SECRET must contain exactly 64 hexadecimal characters (32 random bytes)");
        }
        key = Keys.hmacShaKeyFor(HexFormat.of().parseHex(hexSecret));
        parser = Jwts.parser().verifyWith(key).requireIssuer("pandastore")
                .sig().clear().add(Jwts.SIG.HS256).and().build();
    }

    public String issue(long userId, Instant issuedAt, Instant expiresAt) {
        return Jwts.builder().issuer("pandastore").subject(Long.toString(userId))
                .id(UUID.randomUUID().toString()).issuedAt(Date.from(issuedAt))
                .expiration(Date.from(expiresAt)).signWith(key, Jwts.SIG.HS256).compact();
    }

    public Optional<String> verifiedSubject(String token) {
        if (token == null || token.length() > 4096) return Optional.empty();
        try {
            Claims claims = parser.parseSignedClaims(token).getPayload();
            if (claims.getExpiration() == null || claims.getIssuedAt() == null
                    || claims.getId() == null || claims.getSubject() == null) return Optional.empty();
            return Optional.of(claims.getSubject());
        } catch (JwtException | IllegalArgumentException e) {
            return Optional.empty();
        }
    }
}

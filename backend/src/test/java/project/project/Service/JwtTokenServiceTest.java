package project.project.Service;

import static org.junit.jupiter.api.Assertions.*;
import java.time.Instant;
import java.util.Date;
import java.util.HexFormat;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.Test;
import project.project.Service.implement.JwtTokenService;

class JwtTokenServiceTest {
    private static final String KEY = "0123456789abcdef".repeat(4);
    private final JwtTokenService jwt = new JwtTokenService(KEY);

    @Test
    void validatesSignedTokensAndRejectsTamperingAndOtherKeys() {
        Instant now = Instant.now();
        String token = jwt.issue(42, now, now.plusSeconds(3600));
        assertEquals("42", jwt.verifiedSubject(token).orElseThrow());
        assertNotEquals(token, jwt.issue(42, now, now.plusSeconds(3600)));
        int signature = token.lastIndexOf('.') + 1;
        String tampered = token.substring(0, signature)
                + (token.charAt(signature) == 'A' ? 'B' : 'A') + token.substring(signature + 1);
        assertTrue(jwt.verifiedSubject(tampered).isEmpty());
        assertTrue(new JwtTokenService("ab".repeat(32)).verifiedSubject(token).isEmpty());
    }

    @Test
    void rejectsExpiredUnsignedWrongIssuerWrongAlgorithmAndMissingExpiry() {
        Instant now = Instant.now();
        assertTrue(jwt.verifiedSubject(jwt.issue(42, now.minusSeconds(120), now.minusSeconds(60))).isEmpty());
        var key = Keys.hmacShaKeyFor(HexFormat.of().parseHex(KEY));
        assertTrue(jwt.verifiedSubject(Jwts.builder().subject("42").compact()).isEmpty());
        assertTrue(jwt.verifiedSubject(Jwts.builder().issuer("other").subject("42")
                .expiration(Date.from(now.plusSeconds(60))).signWith(key, Jwts.SIG.HS256).compact()).isEmpty());
        assertTrue(jwt.verifiedSubject(Jwts.builder().issuer("pandastore").subject("42")
                .issuedAt(Date.from(now)).id("test").signWith(key, Jwts.SIG.HS256).compact()).isEmpty());
        assertTrue(jwt.verifiedSubject(Jwts.builder().issuer("pandastore").subject("42")
                .signWith(Jwts.SIG.HS384.key().build(), Jwts.SIG.HS384).compact()).isEmpty());
        assertTrue(jwt.verifiedSubject(null).isEmpty());
        assertTrue(jwt.verifiedSubject("bad.token.value").isEmpty());
    }

    @Test
    void requiresExplicitHexKeyWithoutFallback() {
        for (String invalid : new String[] { "", "short", "z".repeat(64), "ab".repeat(16) }) {
            assertThrows(IllegalStateException.class, () -> new JwtTokenService(invalid));
        }
    }
}

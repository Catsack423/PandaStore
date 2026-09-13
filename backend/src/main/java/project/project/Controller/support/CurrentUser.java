package project.project.Controller.support;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import project.project.Entity.user.User;
import project.project.Entity.user.UserRole;
import project.project.Repository.AuthSessionRepository;
import project.project.Repository.CustomerRepository;
import project.project.Service.api.AuthService;

@Component
public class CurrentUser {
    private final AuthService auth;
    private final AuthSessionRepository sessions;
    private final CustomerRepository customers;

    public CurrentUser(AuthService auth, AuthSessionRepository sessions, CustomerRepository customers) {
        this.auth = auth;
        this.sessions = sessions;
        this.customers = customers;
    }

    public String token(String authorization) {
        if (authorization == null || !authorization.regionMatches(true, 0, "Bearer ", 0, 7)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "กรุณาเข้าสู่ระบบ");
        }
        String token = authorization.substring(7).trim();
        if (!token.matches("[A-Za-z0-9_-]{43}")) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "โทเคนไม่ถูกต้อง");
        }
        return token;
    }

    @Transactional(readOnly = true)
    public User requireUser(String authorization) {
        String token = token(authorization);
        if (!auth.validateToken(token)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "โทเคนไม่ถูกต้องหรือหมดอายุ");
        }
        try {
            String hash = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(token.getBytes(StandardCharsets.UTF_8)));
            return sessions.findById(hash).orElseThrow(() ->
                    new ResponseStatusException(HttpStatus.UNAUTHORIZED, "ไม่พบโทเคน")).getUser();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is unavailable", e);
        }
    }

    @Transactional(readOnly = true)
    public Long requireCustomerId(String authorization) {
        User user = requireUser(authorization);
        if (user.getRole() != UserRole.CUSTOMER) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "บัญชีนี้ไม่ใช่ลูกค้า");
        }
        return customers.findByUser_UserId(user.getUserId()).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.FORBIDDEN, "ไม่พบข้อมูลลูกค้า")).getCustomerId();
    }
}

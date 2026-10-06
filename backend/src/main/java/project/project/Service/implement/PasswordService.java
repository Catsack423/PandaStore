package project.project.Service.implement;

import java.nio.charset.StandardCharsets;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class PasswordService {
    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder(12);

    public boolean isValid(String password) {
        return password != null && !password.isBlank() && password.length() >= 6
                && password.getBytes(StandardCharsets.UTF_8).length <= 72;
    }

    public String hash(String password) {
        if (!isValid(password)) {
            throw new IllegalArgumentException("Password must contain at least 6 characters and at most 72 UTF-8 bytes");
        }
        return encoder.encode(password);
    }

    public boolean matches(String password, String hash) {
        return isValid(password) && hash != null && encoder.matches(password, hash);
    }
}

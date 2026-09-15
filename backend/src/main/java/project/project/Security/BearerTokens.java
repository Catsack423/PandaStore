package project.project.Security;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
public final class BearerTokens {
    private BearerTokens() {}
    public static String requireToken(String authorization) {
        if (authorization == null || !authorization.regionMatches(true, 0, "Bearer ", 0, 7)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "กรุณาเข้าสู่ระบบ");
        }
        String token = authorization.substring(7).trim();
        if (token.length() > 4096 || !token.matches("[A-Za-z0-9_-]+\\.[A-Za-z0-9_-]+\\.[A-Za-z0-9_-]+")) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "โทเคนไม่ถูกต้อง");
        }
        return token;
    }

}

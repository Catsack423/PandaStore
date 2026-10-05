package project.project.Filter;

import java.io.IOException;
import java.time.Clock;
import java.util.HashMap;
import java.util.Map;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/** Bounded, atomic, per-source fixed window. Forwarded headers cannot reset a bucket. */
@Component
public final class LoginRateLimitFilter extends OncePerRequestFilter {
    private static final int MAX_CLIENTS = 10_000;
    private final Map<String, Window> clients = new HashMap<>();
    private final int maxAttempts;
    private final long windowMillis;
    private final Clock clock;

    @Autowired
    public LoginRateLimitFilter(@Value("${app.security.login.max-attempts:5}") int maxAttempts,
            @Value("${app.security.login.window-seconds:60}") long windowSeconds) {
        this(maxAttempts, windowSeconds, Clock.systemUTC());
    }

    LoginRateLimitFilter(int maxAttempts, long windowSeconds, Clock clock) {
        if (maxAttempts < 1 || windowSeconds < 1) {
            throw new IllegalArgumentException("Login limits must be positive");
        }
        this.maxAttempts = maxAttempts;
        this.windowMillis = Math.multiplyExact(windowSeconds, 1000L);
        this.clock = clock;
    }

    private record Window(long expiresAt, int attempts) {}

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI().substring(request.getContextPath().length());
        return !"POST".equals(request.getMethod()) || !"/api/auth/login".equals(path);
    }

    private synchronized long retryAfter(String address) {
        long now = clock.millis();
        Window window = clients.get(address);
        if (window == null || window.expiresAt() <= now) {
            clients.entrySet().removeIf(entry -> entry.getValue().expiresAt() <= now);
            // Fail closed under saturation; never evict an active limit to admit new identities.
            if (clients.size() >= MAX_CLIENTS) return Math.max(1L, windowMillis / 1000);
            window = new Window(now + windowMillis, 0);
        }
        if (window.attempts() >= maxAttempts) {
            return Math.max(1L, (window.expiresAt() - now + 999) / 1000);
        }
        clients.put(address, new Window(window.expiresAt(), window.attempts() + 1));
        return 0;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        long retry = retryAfter(request.getRemoteAddr());
        if (retry == 0) {
            chain.doFilter(request, response);
            return;
        }
        response.setStatus(429);
        response.setHeader("Retry-After", Long.toString(retry));
        response.setHeader("Cache-Control", "no-store");
        response.setContentType("application/json");
        response.getWriter().write("{\"success\":false,\"message\":\"Too many login attempts\"}");
    }
}

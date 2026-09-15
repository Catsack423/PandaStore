package project.project.Filter;

import java.io.IOException;
import java.util.Collections;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.databind.ObjectMapper;
import project.project.ApiResponse.ApiResponse;
import project.project.Security.BearerTokens;
import project.project.Security.SessionAuthenticator;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import project.project.Security.AuthenticatedUser;

/**
 * Per-request identity; mandatory authentication remains scoped to Auth/Cart.
 */
@Component
@Order(0)
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    private final SessionAuthenticator authenticator;
    private final ObjectMapper json;
    private static final List<String> MONITORED_PATH_PREFIXES = List.of(
            "/api/cart",
            "/api/auth"
    );

    // Endpoint เมธอด POST ที่อนุญาตให้ผ่านได้โดยไม่ต้องมี Token
    private static final Set<String> PUBLIC_POST_PATHS = Set.of(
            "/api/auth/login",
            "/api/auth/register/customer",
            "/api/auth/register/seller"
    );

    public JwtAuthenticationFilter(SessionAuthenticator authenticator, ObjectMapper json) {
        this.authenticator = authenticator;
        this.json = json;
    }

    private boolean authenticationOptional(HttpServletRequest request) {
        // 1. อนุญาต Pre-flight request ของ CORS
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }

        String path = request.getRequestURI().substring(request.getContextPath().length());
        String method = request.getMethod();

        // 2. ถ้าไม่ใช่ Path ที่ Filter นี้ดูแล ให้ข้ามการตรวจได้เลย
        boolean isMonitored = MONITORED_PATH_PREFIXES.stream()
                .anyMatch(prefix -> path.equals(prefix) || path.startsWith(prefix + "/"));
        if (!isMonitored) {
            return true;
        }

        // 3. ตรวจสอบ POST endpoints ที่เป็น Public
        if ("POST".equalsIgnoreCase(method) && PUBLIC_POST_PATHS.contains(path)) {
            return true;
        }

        // 4. ยกเว้น GET /api/auth/token เพื่อให้ Controller ตรวจสอบสถานะ Token เอง
        return "GET".equalsIgnoreCase(method) && "/api/auth/token".equals(path);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
            FilterChain chain) throws ServletException, IOException {
        SecurityContextHolder.clearContext();
        try {
            boolean optional = authenticationOptional(request);
            var identity = resolveIdentity(request);
            if (identity.isEmpty() && !optional) {
                unauthorized(response);
                return;
            }
            identity.ifPresent(user -> {
                var context = SecurityContextHolder.createEmptyContext();
                context.setAuthentication(UsernamePasswordAuthenticationToken.authenticated(user, null,
                        List.of(new SimpleGrantedAuthority("ROLE_" + user.role().name()))));
                SecurityContextHolder.setContext(context);
            });
            chain.doFilter(request, response);
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    private Optional<AuthenticatedUser> resolveIdentity(HttpServletRequest request) {
        if ("OPTIONS".equals(request.getMethod()))
            return Optional.empty();
        var headers = Collections.list(request.getHeaders(HttpHeaders.AUTHORIZATION));
        if (headers.size() != 1)
            return Optional.empty();
        String token;
        try {
            token = BearerTokens.requireToken(headers.getFirst());
        } catch (ResponseStatusException e) {
            return Optional.empty();
        }
        return authenticator.authenticate(token);
    }

    //return Error Unauthorized
    private void unauthorized(HttpServletResponse response) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setHeader(HttpHeaders.WWW_AUTHENTICATE, "Bearer");
        response.setHeader(HttpHeaders.CACHE_CONTROL, "no-store");
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write(json.writeValueAsString(
                ApiResponse.error("กรุณาเข้าสู่ระบบ โทเคนไม่ถูกต้องหรือหมดอายุ", null)));
    }
}

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
import project.project.Security.AuthenticatedUser;


/** Per-request identity; mandatory authentication remains scoped to Auth/Cart. */
@Component
@Order(0)
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    private final SessionAuthenticator authenticator;
    private final ObjectMapper json;

    public JwtAuthenticationFilter(SessionAuthenticator authenticator, ObjectMapper json) {
        this.authenticator = authenticator;
        this.json = json;
    }

    private boolean authenticationOptional(HttpServletRequest request) {
        if ("OPTIONS".equals(request.getMethod())) return true;
        String path = request.getRequestURI().substring(request.getContextPath().length());
        boolean ownedPath = path.equals("/api/cart") || path.startsWith("/api/cart/")
                || path.equals("/api/auth") || path.startsWith("/api/auth/");
        if (!ownedPath) return true;
        if ("POST".equals(request.getMethod()) && (path.equals("/api/auth/login")
                || path.equals("/api/auth/register/customer") || path.equals("/api/auth/register/seller"))) {
            return true;
        }
        // This endpoint reports validity itself, including valid=false for expired/revoked JWTs.
        return "GET".equals(request.getMethod()) && path.equals("/api/auth/token");
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
        if ("OPTIONS".equals(request.getMethod())) return Optional.empty();
        var headers = Collections.list(request.getHeaders(HttpHeaders.AUTHORIZATION));
        if (headers.size() != 1) return Optional.empty();
        String token;
        try {
            token = BearerTokens.requireToken(headers.getFirst());
        } catch (ResponseStatusException e) {
            return Optional.empty();
        }
        return authenticator.authenticate(token);
    }

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

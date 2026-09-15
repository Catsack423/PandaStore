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
import project.project.Controller.support.CurrentUser;
import project.project.Service.api.AuthService;

/** Token gate for the Auth/Cart APIs. User context will be introduced separately. */
@Component
@Order(0)
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    private final AuthService auth;
    private final CurrentUser currentUser;
    private final ObjectMapper json;

    public JwtAuthenticationFilter(AuthService auth, CurrentUser currentUser, ObjectMapper json) {
        this.auth = auth;
        this.currentUser = currentUser;
        this.json = json;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
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
        var headers = Collections.list(request.getHeaders(HttpHeaders.AUTHORIZATION));
        String token;
        try {
            if (headers.size() != 1) {
                unauthorized(response);
                return;
            }
            token = currentUser.token(headers.getFirst());
        } catch (ResponseStatusException e) {
            unauthorized(response);
            return;
        }
        // validateToken also checks persisted session, expiry and account status.
        if (!auth.validateToken(token)) {
            unauthorized(response);
            return;
        }
        // Keep downstream exceptions outside authentication handling.
        chain.doFilter(request, response);
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

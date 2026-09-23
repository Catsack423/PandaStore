package project.project.Filter;

import java.io.IOException;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import project.project.ApiResponse.ApiResponse;
import project.project.Entity.user.UserRole;
import project.project.Security.AuthenticatedUser;
import tools.jackson.databind.ObjectMapper;

/** Authorizes seller application review actions after JwtAuthenticationFilter resolves the caller. */
@Component
@Order(1)
public class SellerApplicationAdminFilter extends OncePerRequestFilter {
    private static final String BASE_PATH = "/api/seller-applications";

    private final ObjectMapper json;

    public SellerApplicationAdminFilter(ObjectMapper json) {
        this.json = json;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }

        String path = request.getRequestURI().substring(request.getContextPath().length());
        if (!path.equals(BASE_PATH) && !path.startsWith(BASE_PATH + "/")) {
            return true;
        }

        String method = request.getMethod();
        if ("POST".equalsIgnoreCase(method) && path.equals(BASE_PATH)) {
            return true;
        }
        if ("GET".equalsIgnoreCase(method) || "HEAD".equalsIgnoreCase(method)) {
            return path.equals(BASE_PATH + "/mine") || path.matches(BASE_PATH + "/[0-9]+");
        }
        return false;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
            FilterChain chain) throws ServletException, IOException {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()
                || !(authentication.getPrincipal() instanceof AuthenticatedUser identity)) {
            deny(response, HttpServletResponse.SC_UNAUTHORIZED, "กรุณาเข้าสู่ระบบ");
            return;
        }
        if (identity.role() != UserRole.ADMIN) {
            deny(response, HttpServletResponse.SC_FORBIDDEN, "ต้องเป็นผู้ดูแลระบบ");
            return;
        }
        chain.doFilter(request, response);
    }

    private void deny(HttpServletResponse response, int status, String message) throws IOException {
        response.setStatus(status);
        if (status == HttpServletResponse.SC_UNAUTHORIZED) {
            response.setHeader(HttpHeaders.WWW_AUTHENTICATE, "Bearer");
        }
        response.setHeader(HttpHeaders.CACHE_CONTROL, "no-store");
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write(json.writeValueAsString(ApiResponse.error(message, null)));
    }
}

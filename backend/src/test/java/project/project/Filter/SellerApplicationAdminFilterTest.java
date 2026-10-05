package project.project.Filter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.Optional;

import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;
import project.project.Entity.user.UserRole;
import project.project.Security.AuthenticatedUser;
import project.project.Security.SessionAuthenticator;
import tools.jackson.databind.json.JsonMapper;

class SellerApplicationAdminFilterTest {
    private final SessionAuthenticator authenticator = mock(SessionAuthenticator.class);
    private final JwtAuthenticationFilter authenticationFilter = new JwtAuthenticationFilter(authenticator, new JsonMapper());
    private final SellerApplicationAdminFilter adminFilter = new SellerApplicationAdminFilter(new JsonMapper());

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void reviewEndpointsRequireAnAdminAfterJwtAuthentication() throws Exception {
        when(authenticator.authenticate("customer.jwt.token"))
                .thenReturn(Optional.of(new AuthenticatedUser(2, UserRole.CUSTOMER)));
        when(authenticator.authenticate("seller.jwt.token"))
                .thenReturn(Optional.of(new AuthenticatedUser(3, UserRole.SELLER)));
        when(authenticator.authenticate("admin.jwt.token"))
                .thenReturn(Optional.of(new AuthenticatedUser(1, UserRole.ADMIN)));

        for (String[] route : new String[][] {
                {"GET", "/api/seller-applications/pending"},
                {"HEAD", "/api/seller-applications/pending"},
                {"PUT", "/api/seller-applications/42/approve"},
                {"PUT", "/api/seller-applications/42/reject"},
                {"PUT", "/api/seller-applications/42/request-docs"},
                {"PUT", "/api/seller-applications/42/approve/"}}) {
            var missing = run(route[0], route[1], null);
            assertEquals(401, missing.response.getStatus(), route[1]);
            assertEquals("Bearer", missing.response.getHeader("WWW-Authenticate"));
            verifyNoInteractions(missing.controller);

            for (String token : new String[] {"customer.jwt.token", "seller.jwt.token"}) {
                var forbidden = run(route[0], route[1], token);
                assertEquals(403, forbidden.response.getStatus(), route[1]);
                assertTrue(forbidden.response.getContentAsString().contains("\"success\":false"));
                verifyNoInteractions(forbidden.controller);
            }

            var allowed = run(route[0], route[1], "admin.jwt.token");
            assertEquals(200, allowed.response.getStatus(), route[1]);
            verify(allowed.controller).doFilter(allowed.request, allowed.response);
        }
    }

    @Test
    void customerApplicationRoutesRemainAvailableAndOtherPathsAreUnaffected() throws Exception {
        when(authenticator.authenticate("customer.jwt.token"))
                .thenReturn(Optional.of(new AuthenticatedUser(2, UserRole.CUSTOMER)));

        for (String[] route : new String[][] {
                {"POST", "/api/seller-applications"},
                {"GET", "/api/seller-applications/mine"},
                {"GET", "/api/seller-applications/42"},
                {"OPTIONS", "/api/seller-applications/42/approve"},
                {"GET", "/api/seller-applications-extra/pending"}}) {
            var allowed = run(route[0], route[1], "customer.jwt.token");
            assertEquals(200, allowed.response.getStatus(), route[1]);
            verify(allowed.controller).doFilter(allowed.request, allowed.response);
        }
    }

    @Test
    void rejectsMissingIdentityWhenUsedWithoutAuthenticationFilter() throws Exception {
        var request = new MockHttpServletRequest("GET", "/store/api/seller-applications/pending");
        request.setContextPath("/store");
        var response = new MockHttpServletResponse();
        FilterChain controller = mock(FilterChain.class);

        adminFilter.doFilter(request, response, controller);

        assertEquals(401, response.getStatus());
        assertEquals("Bearer", response.getHeader("WWW-Authenticate"));
        verifyNoInteractions(controller);
    }

    private Result run(String method, String path, String token) throws Exception {
        var request = new MockHttpServletRequest(method, path);
        if (token != null) {
            request.addHeader("Authorization", "Bearer " + token);
        }
        var response = new MockHttpServletResponse();
        FilterChain controller = mock(FilterChain.class);
        authenticationFilter.doFilter(request, response,
                (req, res) -> adminFilter.doFilter(req, res, controller));
        return new Result(request, response, controller);
    }

    private record Result(MockHttpServletRequest request, MockHttpServletResponse response, FilterChain controller) {}
}

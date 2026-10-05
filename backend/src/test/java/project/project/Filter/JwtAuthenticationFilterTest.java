package project.project.Filter;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import tools.jackson.databind.json.JsonMapper;
import project.project.Security.CurrentUser;
import project.project.Security.SessionAuthenticator;
import project.project.Security.AuthenticatedUser;
import project.project.Entity.user.UserRole;
import java.util.Optional;
import org.springframework.security.core.context.SecurityContextHolder;


class JwtAuthenticationFilterTest {
    private final SessionAuthenticator auth = mock(SessionAuthenticator.class);
    private final CurrentUser currentUser = new CurrentUser(null, null);
    private final JwtAuthenticationFilter filter = new JwtAuthenticationFilter(auth, new JsonMapper());
    private final String token = "abc.def.ghi";

    @Test
    void rejectsMissingMalformedDuplicateAndInvalidTokensBeforeChain() throws Exception {
        for (String header : new String[] {"", "Basic abc", "Bearer invalid", "Bearer " + token}) {
            var request = new MockHttpServletRequest("GET", "/api/cart");
            if (!header.isEmpty()) request.addHeader("Authorization", header);
            var response = new MockHttpServletResponse();
            FilterChain chain = mock(FilterChain.class);
            filter.doFilter(request, response, chain);
            assertEquals(401, response.getStatus());
            assertEquals("Bearer", response.getHeader("WWW-Authenticate"));
            assertTrue(response.getContentAsString().contains("\"success\":false"));
            verifyNoInteractions(chain);
        }
        var request = new MockHttpServletRequest("POST", "/api/auth/reset-password");
        request.addHeader("Authorization", "Bearer " + token);
        request.addHeader("Authorization", "Bearer " + token);
        var response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);
        filter.doFilter(request, response, chain);
        assertEquals(401, response.getStatus());
        verifyNoInteractions(chain);
    }

    @Test
    void allowsValidTokenWithContextPathAndDoesNotHideDownstreamFailures() throws Exception {
        when(auth.authenticate(token)).thenReturn(Optional.of(new AuthenticatedUser(42, UserRole.CUSTOMER)));
        var request = new MockHttpServletRequest("GET", "/store/api/cart/items");
        request.setContextPath("/store");
        request.addHeader("Authorization", "Bearer " + token);
        var response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);
        doThrow(new ServletException("downstream failure")).when(chain).doFilter(request, response);
        assertThrows(ServletException.class, () -> filter.doFilter(request, response, chain));
        verify(auth).authenticate(token);
        assertNull(SecurityContextHolder.getContext().getAuthentication());
        assertNotEquals(401, response.getStatus());
    }

    @Test
    void preservesPublicEndpointsPreflightAndOtherTeamsRoutes() throws Exception {
        for (String[] route : new String[][] {
                {"POST", "/api/auth/login"}, {"POST", "/api/auth/register/customer"},
                {"POST", "/api/auth/register/seller"}, {"GET", "/api/auth/token"},
                {"OPTIONS", "/api/cart"}, {"GET", "/api/customers"}, {"GET", "/api/cartography"}}) {
            var request = new MockHttpServletRequest(route[0], route[1]);
            var response = new MockHttpServletResponse();
            FilterChain chain = mock(FilterChain.class);
            filter.doFilter(request, response, chain);
            verify(chain).doFilter(request, response);
        }
        verifyNoInteractions(auth);
    }

    @Test
    void publicPathPrefixDoesNotBypassProtection() throws Exception {
        for (String path : new String[] {"/api/auth/login/extra", "/api/auth/register/customer/extra"}) {
            var request = new MockHttpServletRequest("POST", path);
            var response = new MockHttpServletResponse();
            FilterChain chain = mock(FilterChain.class);
            filter.doFilter(request, response, chain);
            assertEquals(401, response.getStatus());
            verifyNoInteractions(chain);
        }
    }

    @Test
    void helperReadsIdentityAndContextIsClearedForNextRequest() throws Exception {
        when(auth.authenticate(token)).thenReturn(Optional.of(new AuthenticatedUser(42, UserRole.CUSTOMER)));
        var request = new MockHttpServletRequest("GET", "/api/cart");
        request.addHeader("Authorization", "Bearer " + token);
        filter.doFilter(request, new MockHttpServletResponse(), (req, res) -> {
            assertEquals(42, currentUser.getCurrentUserId());
            var authentication = SecurityContextHolder.getContext().getAuthentication();
            assertEquals("42", authentication.getName());
            assertNull(authentication.getCredentials());
            assertEquals("ROLE_CUSTOMER", authentication.getAuthorities().iterator().next().getAuthority());
        });
        assertNull(SecurityContextHolder.getContext().getAuthentication());
        assertThrows(org.springframework.web.server.ResponseStatusException.class, currentUser::getCurrentUserId);
        filter.doFilter(new MockHttpServletRequest("GET", "/api/customers"), new MockHttpServletResponse(),
                (req, res) -> assertNull(SecurityContextHolder.getContext().getAuthentication()));
        // A valid token provides identity on other teams' routes without requiring login there.
        var optional = new MockHttpServletRequest("GET", "/api/customers");
        optional.addHeader("Authorization", "Bearer " + token);
        filter.doFilter(optional, new MockHttpServletResponse(),
                (req, res) -> assertEquals(42, currentUser.getCurrentUserId()));
        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    void concurrentRequestsDoNotShareIdentity() throws Exception {
        var ready = new java.util.concurrent.CountDownLatch(2);
        when(auth.authenticate(token)).thenReturn(Optional.of(new AuthenticatedUser(42, UserRole.CUSTOMER)));
        when(auth.authenticate("other.jwt.signature")).thenReturn(Optional.of(new AuthenticatedUser(84, UserRole.SELLER)));
        try (var pool = java.util.concurrent.Executors.newFixedThreadPool(2)) {
            var first = pool.submit(() -> runConcurrentRequest(token, 42, ready));
            var second = pool.submit(() -> runConcurrentRequest("other.jwt.signature", 84, ready));
            first.get(10, java.util.concurrent.TimeUnit.SECONDS);
            second.get(10, java.util.concurrent.TimeUnit.SECONDS);
        }
    }

    private Void runConcurrentRequest(String jwt, long expectedId,
            java.util.concurrent.CountDownLatch ready) throws Exception {
        var request = new MockHttpServletRequest("GET", "/api/cart");
        request.addHeader("Authorization", "Bearer " + jwt);
        filter.doFilter(request, new MockHttpServletResponse(), (req, res) -> {
            ready.countDown();
            try { assertTrue(ready.await(5, java.util.concurrent.TimeUnit.SECONDS)); }
            catch (InterruptedException e) { Thread.currentThread().interrupt(); throw new ServletException(e); }
            assertEquals(expectedId, currentUser.getCurrentUserId());
        });
        assertNull(SecurityContextHolder.getContext().getAuthentication());
        return null;
    }
}

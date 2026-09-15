package project.project.Filter;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import tools.jackson.databind.json.JsonMapper;
import project.project.Controller.support.CurrentUser;
import project.project.Service.api.AuthService;

class JwtAuthenticationFilterTest {
    private final AuthService auth = mock(AuthService.class);
    private final CurrentUser currentUser = new CurrentUser(auth, null, null);
    private final JwtAuthenticationFilter filter = new JwtAuthenticationFilter(auth, currentUser, new JsonMapper());
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
        when(auth.validateToken(token)).thenReturn(true);
        var request = new MockHttpServletRequest("GET", "/store/api/cart/items");
        request.setContextPath("/store");
        request.addHeader("Authorization", "Bearer " + token);
        var response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);
        doThrow(new ServletException("downstream failure")).when(chain).doFilter(request, response);
        assertThrows(ServletException.class, () -> filter.doFilter(request, response, chain));
        verify(auth).validateToken(token);
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
}

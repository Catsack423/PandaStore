package project.project.Filter;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.concurrent.Executors;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class LoginRateLimitFilterTest {
    @Test void expiresAndDoesNotBlockOtherSourcesOrOtherEndpoints() throws Exception {
        Clock clock = mock(Clock.class);
        when(clock.millis()).thenReturn(0L);
        var filter = new LoginRateLimitFilter(5, 60, clock);
        for (int i = 0; i < 5; i++) assertEquals(200, request(filter, "192.0.2.1", "/api/auth/login"));
        assertEquals(429, request(filter, "192.0.2.1", "/api/auth/login"));
        assertEquals(200, request(filter, "192.0.2.2", "/api/auth/login"));
        assertEquals(200, request(filter, "192.0.2.1", "/api/auth/logout"));
        when(clock.millis()).thenReturn(60_000L);
        assertEquals(200, request(filter, "192.0.2.1", "/api/auth/login"));
    }

    @Test void simultaneousRequestsCannotExceedLimit() throws Exception {
        var filter = new LoginRateLimitFilter(5, 60, Clock.fixed(Instant.EPOCH, ZoneOffset.UTC));
        try (var pool = Executors.newFixedThreadPool(10)) {
            var tasks = java.util.stream.IntStream.range(0, 30)
                    .<java.util.concurrent.Callable<Integer>>mapToObj(i -> () -> request(filter, "192.0.2.1", "/api/auth/login"))
                    .toList();
            int allowed = 0;
            for (var result : pool.invokeAll(tasks)) if (result.get() == 200) allowed++;
            assertEquals(5, allowed);
        }
    }

    private int request(LoginRateLimitFilter filter, String address, String path) throws Exception {
        var request = new MockHttpServletRequest("POST", path);
        request.setRemoteAddr(address);
        var response = new MockHttpServletResponse();
        filter.doFilter(request, response, (req, res) -> {});
        return response.getStatus();
    }
}

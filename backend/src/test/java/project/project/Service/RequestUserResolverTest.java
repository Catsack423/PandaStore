package project.project.Service;

import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import project.project.Controller.RequestUserResolver;
import project.project.Entity.user.*;
import project.project.Repository.UserRepository;
import project.project.Security.AuthenticatedUser;
import java.util.List;
import java.util.Optional;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

class RequestUserResolverTest {
    @Test
    void jwtIdentityWorksWithoutServletPrincipal() {
        var repository = mock(UserRepository.class);
        var user = new User(); user.setUserId(3L); user.setStatus(UserStatus.ACTIVE);
        when(repository.findById(3L)).thenReturn(Optional.of(user));
        SecurityContextHolder.getContext().setAuthentication(UsernamePasswordAuthenticationToken.authenticated(
                new AuthenticatedUser(3L, UserRole.CUSTOMER), null, List.of()));
        try {
            assertEquals(3L, new RequestUserResolver(repository).requireUserId(null));
            verify(repository, never()).findByUsername(anyString());
        } finally { SecurityContextHolder.clearContext(); }
    }

    @Test
    void missingIdentityRemainsUnauthorized() {
        SecurityContextHolder.clearContext();
        assertThrows(org.springframework.web.server.ResponseStatusException.class,
                () -> new RequestUserResolver(mock(UserRepository.class)).requireUserId(null));
    }
}

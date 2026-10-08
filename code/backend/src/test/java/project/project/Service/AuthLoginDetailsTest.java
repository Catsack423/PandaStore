package project.project.Service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.util.Optional;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;
import project.project.Entity.user.AuthSession;
import project.project.Entity.user.User;
import project.project.Entity.user.UserRole;
import project.project.Entity.user.UserStatus;
import project.project.Repository.AuthSessionRepository;
import project.project.Repository.CustomerRepository;
import project.project.Repository.UserRepository;
import project.project.Repository.SellerRepository;
import project.project.Security.SessionAuthenticator;
import project.project.Service.api.CartService;
import project.project.Service.api.CustomerService;
import project.project.Service.api.SellerService;
import project.project.Service.implement.AuthServiceImp;
import project.project.Service.implement.JwtTokenService;
import project.project.Service.implement.PasswordService;

class AuthLoginDetailsTest {
    @Test
    void loginDetailsComeFromAuthenticatedUser() {
        UserRepository users = mock(UserRepository.class);
        AuthSessionRepository sessions = mock(AuthSessionRepository.class);
        PasswordService passwords = mock(PasswordService.class);
        JwtTokenService jwt = mock(JwtTokenService.class);
        User user = new User("customer", "customer@example.com", "stored-hash",
                UserRole.CUSTOMER, UserStatus.ACTIVE);
        user.setUserId(7L);
        when(users.findByUsername("customer")).thenReturn(Optional.of(user));
        when(users.findByEmail("customer")).thenReturn(Optional.empty());
        when(passwords.matches("password123", "stored-hash")).thenReturn(true);
        when(jwt.issue(eq(7L), any(), any())).thenReturn("signed.jwt.token");

        AuthServiceImp auth = new AuthServiceImp(users, mock(CustomerRepository.class),
                mock(CartService.class), sessions, mock(CustomerService.class), passwords,
                mock(Validator.class), jwt, mock(SessionAuthenticator.class),
                mock(SellerRepository.class), mock(SellerService.class));
        var result = auth.loginWithUser("customer", "password123");

        assertEquals("signed.jwt.token", result.token());
        assertEquals(UserRole.CUSTOMER, result.role());
        assertEquals(7L, result.user().userId());
        assertEquals("customer@example.com", result.user().email());
        verify(sessions).save(any(AuthSession.class));
        assertThrows(IllegalArgumentException.class,
                () -> auth.loginWithUser("customer", "wrong"));
    }
}

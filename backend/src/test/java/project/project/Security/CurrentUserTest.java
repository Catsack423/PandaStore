package project.project.Security;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.server.ResponseStatusException;
import project.project.Repository.UserRepository;
import project.project.Repository.CustomerRepository;
import project.project.Entity.user.*;

class CurrentUserTest {
    private final UserRepository users = mock(UserRepository.class);
    private final CustomerRepository customers = mock(CustomerRepository.class);
    private final CurrentUser helper = new CurrentUser(users, customers);
    @AfterEach void clear() { SecurityContextHolder.clearContext(); }
    private void login(UserRole role) {
        var context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(UsernamePasswordAuthenticationToken.authenticated(
                new AuthenticatedUser(42, role), null, List.of()));
        SecurityContextHolder.setContext(context);
    }
    @Test void retrievesUserAndCustomerUsingContextIdentity() {
        login(UserRole.CUSTOMER);
        User user = mock(User.class);
        Customer customer = mock(Customer.class);
        when(customer.getCustomerId()).thenReturn(7L);
        when(users.findById(42L)).thenReturn(Optional.of(user));
        when(customers.findByUser_UserId(42L)).thenReturn(Optional.of(customer));
        assertSame(user, helper.requireUser());
        assertEquals(7L, helper.requireCustomerId());
    }
    @Test void rejectsAnonymousAndSellerCustomerLookup() {
        assertEquals(401, assertThrows(ResponseStatusException.class, helper::getCurrentUserId).getStatusCode().value());
        login(UserRole.SELLER);
        assertEquals(403, assertThrows(ResponseStatusException.class, helper::requireCustomerId).getStatusCode().value());
        verifyNoInteractions(customers);
    }
}

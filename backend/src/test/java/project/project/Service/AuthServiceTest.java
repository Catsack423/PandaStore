package project.project.Service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import jakarta.validation.ConstraintViolationException;
import project.project.DTO.customer.CreateCustomerRequest;
import project.project.Entity.user.*;
import project.project.Repository.*;
import project.project.Service.api.AuthService;
import project.project.Service.api.CartService;
import project.project.Service.api.CustomerService;

@SpringBootTest(properties = {
    "spring.datasource.url=jdbc:h2:mem:auth-tests;DB_CLOSE_DELAY=-1",
    "spring.datasource.username=sa", "spring.datasource.password=",
    "spring.jpa.hibernate.ddl-auto=create-drop"
})
class AuthServiceTest {
    @Autowired AuthService auth;
    @Autowired UserRepository users;
    @Autowired CustomerRepository customers;
    @Autowired CartRepository carts;
    @Autowired PlatformTransactionManager transactions;
    @MockitoBean CustomerService customerService;
    @MockitoSpyBean CartService cartService;

    @BeforeEach
    void setup() {
        carts.deleteAll();
        customers.deleteAll();
        users.deleteAll();
        // จำลองสัญญาของ CustomerService: สร้างลูกค้าแล้วคืน customerId
        when(customerService.createCustomer(any())).thenAnswer(call -> {
            CreateCustomerRequest request = call.getArgument(0);
            User user = users.save(new User(request.getUsername(), request.getEmail(), request.getPassword(),
                    UserRole.CUSTOMER, UserStatus.ACTIVE));
            return customers.save(new Customer(user, request.getFullName(), request.getPhoneNumber())).getCustomerId();
        });
    }

    private Customer register() {
        return auth.registerCustomer("customer", "customer@example.com", "password123", "password123",
                "Test Customer", "0812345678");
    }

    @Test
    void registersThroughCustomerInterfaceAndCreatesCart() {
        Customer customer = register();
        verify(customerService).createCustomer(any(CreateCustomerRequest.class));
        verify(cartService).createCart(customer.getCustomerId());
        assertEquals(1, users.count());
        assertEquals(1, carts.count());
        assertTrue(users.findAll().getFirst().getPasswordHash().startsWith("pbkdf2$"));
        assertNotEquals("password123", users.findAll().getFirst().getPasswordHash());
        assertTrue(auth.validateToken(auth.login("customer", "password123")));
    }

    @Test
    void invalidRegistrationDoesNotCallCustomerService() {
        assertThrows(IllegalArgumentException.class, () -> auth.registerCustomer("customer", "a@example.com",
                "password123", "different", "Name", "0812345678"));
        assertThrows(ConstraintViolationException.class, () -> auth.registerCustomer("customer", "invalid",
                "password123", "password123", "Name", "0812345678"));
        verify(customerService, never()).createCustomer(any());
        assertEquals(0, users.count());
    }

    @Test
    void cartFailureRollsBackCustomerAndUser() {
        doThrow(new DataIntegrityViolationException("Cart save failed")).when(cartService).createCart(anyLong());
        assertThrows(DataIntegrityViolationException.class, this::register);
        assertEquals(0, users.count());
        assertEquals(0, customers.count());
        assertEquals(0, carts.count());
    }

    @Test
    void sellerRegistrationExplicitlyWaitsForTeamContractWithoutWriting() {
        assertThrows(UnsupportedOperationException.class,
                () -> auth.registerSeller("seller", "seller@example.com", "password123", "password123"));
        verify(customerService, never()).createCustomer(any());
        assertEquals(0, users.count());
        assertEquals(0, carts.count());
    }

    @Test
    void acceptsUsernameAndEmailButRejectsWrongCredentialsAndMalformedTokens() {
        register();
        String first = auth.login("customer", "password123");
        String second = auth.login("customer@example.com", "password123");
        assertNotEquals(first, second);
        assertTrue(auth.validateToken(first));
        assertTrue(auth.validateToken(second));
        assertThrows(IllegalArgumentException.class, () -> auth.login("customer", "wrong123"));
        assertThrows(IllegalArgumentException.class, () -> auth.login("missing", "password123"));
        assertThrows(IllegalArgumentException.class, () -> auth.login(null, "password123"));
        assertFalse(auth.validateToken(null));
        assertFalse(auth.validateToken("invalid"));
        assertFalse(auth.validateToken("A".repeat(43)));
    }

    @Test
    void inactiveUserCannotLoginOrUseExistingToken() {
        register();
        String token = auth.login("customer", "password123");
        User user = users.findAll().getFirst();
        user.setStatus(UserStatus.SUSPENDED);
        users.save(user);
        assertFalse(auth.validateToken(token));
        assertThrows(IllegalArgumentException.class, () -> auth.login("customer", "password123"));
    }

    @Test
    void originalResetSignatureChangesPasswordAndInvalidatesTokens() {
        register();
        long id = users.findAll().getFirst().getUserId();
        String first = auth.login("customer", "password123");
        String second = auth.login("customer", "password123");
        assertFalse(auth.resetPassword(id, "newPassword123", "different"));
        assertFalse(auth.resetPassword(Long.MAX_VALUE, "newPassword123", "newPassword123"));
        assertTrue(auth.validateToken(first));
        assertTrue(auth.resetPassword(id, "newPassword123", "newPassword123"));
        assertFalse(auth.validateToken(first));
        assertFalse(auth.validateToken(second));
        assertThrows(IllegalArgumentException.class, () -> auth.login("customer", "password123"));
        assertTrue(auth.validateToken(auth.login("customer", "newPassword123")));
    }

    @Test
    void rolledBackResetDoesNotInvalidateCommittedPasswordOrToken() {
        register();
        long id = users.findAll().getFirst().getUserId();
        String token = auth.login("customer", "password123");
        new TransactionTemplate(transactions).executeWithoutResult(status -> {
            assertTrue(auth.resetPassword(id, "newPassword123", "newPassword123"));
            status.setRollbackOnly();
        });
        assertTrue(auth.validateToken(token));
        assertTrue(auth.validateToken(auth.login("customer", "password123")));
    }

    @Test
    void doesNotTreatPlaintextAsPasswordHash() {
        users.save(new User("legacy", "legacy@example.com", "password123", UserRole.CUSTOMER, UserStatus.ACTIVE));
        assertThrows(IllegalArgumentException.class, () -> auth.login("legacy", "password123"));
    }

    @Test
    void validatesPasswordsAndRejectsAmbiguousLogin() {
        assertFalse(auth.verifyPassword(null, null));
        assertFalse(auth.verifyPassword("      ", "      "));
        assertFalse(auth.verifyPassword("short", "short"));
        assertTrue(auth.verifyPassword("password123", "password123"));
        register();
        auth.registerCustomer("customer@example.com", "another@example.com", "password123", "password123",
                "Another", "0812345678");
        assertThrows(IllegalArgumentException.class, () -> auth.login("customer@example.com", "password123"));
    }
}

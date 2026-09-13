package project.project.Service;

import static org.junit.jupiter.api.Assertions.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.HexFormat;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import jakarta.validation.ConstraintViolationException;
import project.project.Entity.user.*;
import project.project.Exception.DuplicateUserException;
import project.project.Repository.*;
import project.project.Service.api.AuthService;
import project.project.Service.implement.PasswordService;

@SpringBootTest(properties = {
    "spring.datasource.url=jdbc:h2:mem:auth-tests;DB_CLOSE_DELAY=-1",
    "spring.datasource.username=sa",
    "spring.datasource.password=",
    "spring.jpa.hibernate.ddl-auto=create-drop"
})
class AuthServiceTest {
    @Autowired AuthService auth;
    @Autowired PasswordService passwords;
    @Autowired UserRepository users;
    @Autowired CustomerRepository customers;
    @org.springframework.test.context.bean.override.mockito.MockitoSpyBean
    CartRepository carts;
    @Autowired SellerRepository sellers;
    @Autowired SellerApplicationRepository applications;
    @Autowired AuthSessionRepository sessions;

    @BeforeEach
    void cleanDatabase() {
        sessions.deleteAll();
        carts.deleteAll();
        customers.deleteAll();
        applications.deleteAll();
        sellers.deleteAll();
        users.deleteAll();
    }

    private Customer registerCustomer() {
        return auth.registerCustomer("customer", "customer@example.com", "password123", "password123",
                "Test Customer", "0812345678");
    }

    @Test
    void customerRegistrationCreatesCartAndHashesPassword() {
        Customer customer = registerCustomer();
        assertNotNull(customer.getCustomerId());
        assertEquals(1, customers.count());
        assertEquals(1, carts.count());
        User user = users.findByUsername("customer").orElseThrow();
        assertNotEquals("password123", user.getPasswordHash());
        assertTrue(passwords.matches("password123", user.getPasswordHash()));
        assertEquals(UserRole.CUSTOMER, user.getRole());
    }

    @Test
    void duplicateRegistrationDoesNotCreateExtraCustomerOrCart() {
        registerCustomer();
        assertThrows(DuplicateUserException.class, this::registerCustomer);
        assertThrows(DuplicateUserException.class, () -> auth.registerCustomer("another",
                "customer@example.com", "password123", "password123", "Name", "0812345678"));
        assertEquals(1, users.count());
        assertEquals(1, customers.count());
        assertEquals(1, carts.count());
    }

    @Test
    void invalidRegistrationDoesNotWriteAnything() {
        assertThrows(IllegalArgumentException.class, () -> auth.registerCustomer("customer", "a@example.com",
                "password123", "different", "Name", "0812345678"));
        assertThrows(ConstraintViolationException.class, () -> auth.registerCustomer("customer", "invalid",
                "password123", "password123", "Name", "0812345678"));
        assertEquals(0, users.count());
        assertEquals(0, carts.count());
    }

    @Test
    void cartFailureRollsBackCustomerAndUser() {
        org.mockito.Mockito.doThrow(new DataIntegrityViolationException("Cart save failed"))
                .when(carts).save(org.mockito.ArgumentMatchers.any(project.project.Entity.order.Cart.class));
        assertThrows(DataIntegrityViolationException.class, this::registerCustomer);
        assertEquals(0, users.count());
        assertEquals(0, customers.count());
        assertEquals(0, carts.count());
    }

    @Test
    void sellerRegistrationWaitsForInterfaceWithoutWritingData() {
        assertThrows(UnsupportedOperationException.class,
                () -> auth.registerSeller("seller", "seller@example.com", "password123", "password123"));
        assertThrows(IllegalArgumentException.class,
                () -> auth.registerSeller("seller", "seller@example.com", "password123", "different"));
        assertEquals(0, users.count());
        assertEquals(0, sellers.count());
        assertEquals(0, applications.count());
    }

    @Test
    void loginAcceptsUsernameAndEmailAndStoresOnlyTokenDigest() throws Exception {
        registerCustomer();
        String token = auth.login("customer", "password123");
        assertTrue(auth.validateToken(token));
        String otherToken = auth.login("customer@example.com", "password123");
        assertTrue(auth.validateToken(otherToken));
        assertNotEquals(token, otherToken);
        assertFalse(sessions.existsById(token));
        String digest = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                .digest(token.getBytes(StandardCharsets.UTF_8)));
        assertTrue(sessions.existsById(digest));
        assertFalse(auth.validateToken("A".repeat(43)));
        assertFalse(auth.validateToken(null));
        assertFalse(auth.validateToken("invalid"));
    }

    @Test
    void loginRejectsWrongPasswordAndSuspendedAccount() {
        registerCustomer();
        assertThrows(IllegalArgumentException.class, () -> auth.login("customer", "wrong123"));
        assertThrows(IllegalArgumentException.class, () -> auth.login("missing", "password123"));
        assertThrows(IllegalArgumentException.class, () -> auth.login(null, "password123"));
        String token = auth.login("customer", "password123");
        User user = users.findByUsername("customer").orElseThrow();
        user.setStatus(UserStatus.SUSPENDED);
        users.save(user);
        assertFalse(auth.validateToken(token));
        assertThrows(IllegalArgumentException.class, () -> auth.login("customer", "password123"));
    }

    @Test
    void expiredTokensAreRejected() throws Exception {
        registerCustomer();
        String token = auth.login("customer", "password123");
        String digest = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                .digest(token.getBytes(StandardCharsets.UTF_8)));
        sessions.save(new AuthSession(digest, users.findByUsername("customer").orElseThrow(),
                Instant.now().minusSeconds(1)));
        assertFalse(auth.validateToken(token));
    }

    @Test
    void passwordResetUsesOriginalInterfaceAndRevokesEverySession() {
        registerCustomer();
        long userId = users.findByUsername("customer").orElseThrow().getUserId();
        String token = auth.login("customer", "password123");
        String secondToken = auth.login("customer", "password123");
        assertFalse(auth.resetPassword(0, "newPassword123", "newPassword123"));
        assertFalse(auth.resetPassword(Long.MAX_VALUE, "newPassword123", "newPassword123"));
        assertFalse(auth.resetPassword(userId, "newPassword123", "different"));
        assertTrue(auth.validateToken(token));
        assertTrue(auth.resetPassword(userId, "newPassword123", "newPassword123"));
        assertFalse(auth.validateToken(token));
        assertFalse(auth.validateToken(secondToken));
        assertEquals(0, sessions.count());
        assertThrows(IllegalArgumentException.class, () -> auth.login("customer", "password123"));
        assertTrue(auth.validateToken(auth.login("customer", "newPassword123")));
    }

    @Test
    void rejectsAmbiguousUsernameAndEmail() {
        registerCustomer();
        auth.registerCustomer("customer@example.com", "another@example.com", "password123", "password123",
                "Another Customer", "0812345678");
        assertThrows(IllegalArgumentException.class, () -> auth.login("customer@example.com", "password123"));
        assertEquals(0, sessions.count());
    }

    @Test
    void passwordValidationEnforcesBcryptByteLimit() {
        assertFalse(auth.verifyPassword(null, null));
        assertFalse(auth.verifyPassword("      ", "      "));
        assertFalse(auth.verifyPassword("short", "short"));
        assertFalse(auth.verifyPassword("ก".repeat(25), "ก".repeat(25)));
        assertTrue(auth.verifyPassword("ก".repeat(24), "ก".repeat(24)));
        String first = passwords.hash("password123");
        String second = passwords.hash("password123");
        assertNotEquals(first, second);
        assertTrue(passwords.matches("password123", first));
        assertFalse(passwords.matches("wrong123", first));
    }
}

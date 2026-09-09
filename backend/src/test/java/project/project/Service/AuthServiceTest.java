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
import project.project.DTO.auth.RegisterSellerRequest;
import project.project.Entity.user.*;
import project.project.Entity.seller.*;
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

    private RegisterSellerRequest sellerRequest(String username, String shopName) {
        return new RegisterSellerRequest(username, username + "@example.com", "password123", "password123",
                shopName, "Shop description", "0812345678", "shop@example.com", "Bangkok",
                "Test", "Seller", "1234567890123", "/id-card.png", "Test Seller", "Test Bank",
                "1234567890", "/bank-book.png");
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
    void sellerRegistrationCreatesPendingSellerAndApplication() {
        Seller seller = auth.registerSeller(sellerRequest("seller", "My Shop"));
        assertNotNull(seller.getSellerId());
        assertEquals(SellerStatus.PENDING, seller.getStatus());
        assertEquals(1, applications.count());
        SellerApplication application = applications.findAll().getFirst();
        assertEquals(SellerApplicationStatus.PENDING, application.getStatus());
        assertEquals("1234567890123", application.getIdCardNumber());
        assertEquals("My Shop", application.getShopName());
        assertTrue(passwords.matches("password123", users.findByUsername("seller").orElseThrow().getPasswordHash()));
        assertEquals(0, carts.count());
    }

    @Test
    void sellerDatabaseFailureRollsBackNewUser() {
        auth.registerSeller(sellerRequest("seller", "Same Shop"));
        assertThrows(DataIntegrityViolationException.class,
                () -> auth.registerSeller(sellerRequest("another", "Same Shop")));
        assertEquals(1, users.count());
        assertEquals(1, sellers.count());
        assertEquals(1, applications.count());
        assertFalse(users.existsByUsername("another"));
    }

    @Test
    void sellerRejectsMissingDetails() {
        assertThrows(IllegalArgumentException.class, () -> auth.registerSeller(null));
        assertThrows(ConstraintViolationException.class, () -> auth.registerSeller(sellerRequest("seller", "")));
        assertEquals(0, users.count());
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
        String token = "B".repeat(43);
        String digest = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                .digest(token.getBytes(StandardCharsets.UTF_8)));
        sessions.save(new AuthSession(digest, users.findByUsername("customer").orElseThrow(),
                Instant.now().minusSeconds(1)));
        assertFalse(auth.validateToken(token));
        assertFalse(auth.resetPassword(token, "password123", "newPassword123", "newPassword123"));
    }

    @Test
    void passwordChangeRequiresValidCredentialsAndRevokesEverySession() {
        registerCustomer();
        String token = auth.login("customer", "password123");
        String secondToken = auth.login("customer", "password123");
        assertFalse(auth.resetPassword(null, "password123", "newPassword123", "newPassword123"));
        assertFalse(auth.resetPassword(token, "wrong123", "newPassword123", "newPassword123"));
        assertFalse(auth.resetPassword(token, "password123", "newPassword123", "different"));
        assertTrue(auth.validateToken(token));
        assertTrue(auth.resetPassword(token, "password123", "newPassword123", "newPassword123"));
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

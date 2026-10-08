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
import project.project.Entity.seller.*;
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
    @Autowired SellerBankAccountRepository bankAccounts;
    @Autowired AuthSessionRepository sessions;

    @BeforeEach
    void cleanDatabase() {
        sessions.deleteAll();
        carts.deleteAll();
        customers.deleteAll();
        bankAccounts.deleteAll();
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
    void sellerRegistrationRejectsPasswordMismatch() {
        var request = new project.project.DTO.auth.AuthRequests.RegisterSeller(
                "seller", "seller@example.com", "password123", "different",
                "Shop", "Desc", "0812345678", "shop@example.com", "Bangkok",
                "Somchai", "Jaidee", "1234567890123",
                "Kasikorn", "Somchai Jaidee", "1234567890",
                null, null, null
        );
        assertThrows(IllegalArgumentException.class, () -> auth.registerSeller(request));
        assertEquals(0, users.count());
        assertEquals(0, sellers.count());
        assertEquals(0, applications.count());
        assertEquals(0, bankAccounts.count());
    }

    @Test
    void sellerRegistrationWithFullDetailsCreatesUserSellerApplicationAndBankAccount() {
        var request = new project.project.DTO.auth.AuthRequests.RegisterSeller(
                "newSeller", "newseller@example.com", "password123", "password123",
                "My Shop", "Shop description", "0812345678", "shop@example.com",
                "Bangkok", "Somchai", "Jaidee", "1234567890123",
                "Kasikorn", "Somchai Jaidee", "1234567890",
                null, null, null
        );
        Seller seller = auth.registerSeller(request);
        assertNotNull(seller.getSellerId());
        assertEquals("My Shop", seller.getShopName());
        assertEquals(project.project.Entity.seller.SellerStatus.PENDING, seller.getStatus());

        assertEquals(1, users.count());
        assertEquals(1, sellers.count());
        assertEquals(1, applications.count());
        assertEquals(1, bankAccounts.count());

        User user = users.findByUsername("newSeller").orElseThrow();
        assertEquals(UserRole.SELLER, user.getRole());
        assertTrue(passwords.matches("password123", user.getPasswordHash()));

        project.project.Entity.seller.SellerApplication app = applications.findByUser_UserId(user.getUserId()).get(0);
        assertEquals("My Shop", app.getShopName());
        assertEquals("Somchai", app.getSellerFirstName());
        assertEquals("1234567890123", app.getIdCardNumber());
        assertEquals(project.project.Entity.seller.SellerApplicationStatus.PENDING, app.getStatus());

        project.project.Entity.seller.SellerBankAccount bank = bankAccounts.findBySeller_SellerId(seller.getSellerId()).orElseThrow();
        assertEquals("Kasikorn", bank.getBankName());
        assertEquals("1234567890", bank.getAccountNumber());
        assertEquals("Somchai Jaidee", bank.getAccountName());
    }

    @Test
    void sellerRegistrationRejectsDuplicateShopName() {
        var request1 = new project.project.DTO.auth.AuthRequests.RegisterSeller(
                "seller1", "seller1@example.com", "password123", "password123",
                "Same Shop", "Shop description", "0812345678", "shop1@example.com",
                "Bangkok", "Somchai", "Jaidee", "1234567890123",
                "Kasikorn", "Somchai Jaidee", "1234567890",
                null, null, null
        );
        auth.registerSeller(request1);

        var request2 = new project.project.DTO.auth.AuthRequests.RegisterSeller(
                "seller2", "seller2@example.com", "password123", "password123",
                "Same Shop", "Shop description", "0812345679", "shop2@example.com",
                "Bangkok", "Somsak", "Jaidee", "1234567890124",
                "SCB", "Somsak Jaidee", "0987654321",
                null, null, null
        );
        assertThrows(DuplicateUserException.class, () -> auth.registerSeller(request2));
        assertEquals(1, sellers.count());
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
    void logoutOnlyDeletesMatchingSessionAndRejectsInvalidTokens() throws Exception {
        registerCustomer();
        String token = auth.login("customer", "password123");
        String second = auth.login("customer", "password123");
        assertFalse(auth.logout(null));
        assertFalse(auth.logout("invalid"));
        assertEquals(2, sessions.count());
        assertTrue(auth.logout(token));
        String digest = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                .digest(token.getBytes(StandardCharsets.UTF_8)));
        assertFalse(sessions.existsById(digest));
        assertFalse(auth.validateToken(token));
        assertFalse(auth.logout(token));
        assertTrue(auth.validateToken(second));
        assertEquals(1, sessions.count());
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

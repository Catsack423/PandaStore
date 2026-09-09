package project.project.Service.implement;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

import jakarta.persistence.EntityManager;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validator;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import project.project.DTO.customer.CreateCustomerRequest;
import project.project.Entity.seller.Seller;
import project.project.Entity.user.Customer;
import project.project.Entity.user.User;
import project.project.Entity.user.UserStatus;
import project.project.Service.api.AuthService;
import project.project.Service.api.CartService;
import project.project.Service.api.CustomerService;

@Service
@Transactional
public class AuthServiceImp implements AuthService {
    private static final int PASSWORD_ITERATIONS = 600_000;
    private final EntityManager entities;
    private final CustomerService customerService;
    private final CartService cartService;
    private final Validator validator;
    private final SecureRandom random = new SecureRandom();

    // เก็บเฉพาะใน instance นี้: restart แล้วต้อง login ใหม่ ยังไม่รองรับหลาย server
    private final Map<String, Session> sessions = new ConcurrentHashMap<>();
    private record Session(Long userId, String passwordHash, Instant expiresAt) {}

    public AuthServiceImp(EntityManager entities, CustomerService customerService,
            CartService cartService, Validator validator) {
        this.entities = entities;
        this.customerService = customerService;
        this.cartService = cartService;
        this.validator = validator;
    }

    @Override
    public Customer registerCustomer(String username, String email, String password, String confirmPassword,
            String fullName, String phoneNumber) {
        if (!verifyPassword(password, confirmPassword)) {
            throw new IllegalArgumentException("Passwords are invalid or do not match");
        }
        var request = new CreateCustomerRequest(username, email, password, fullName, phoneNumber);
        var violations = validator.validate(request);
        if (!violations.isEmpty()) throw new ConstraintViolationException(violations);
        long id = customerService.createCustomer(request);
        Customer customer = entities.find(Customer.class, id);
        if (customer == null || customer.getUser() == null) {
            throw new IllegalStateException("CustomerService did not create a customer with a user");
        }
        // Auth รับผิดชอบ hash ภายใน transaction เดียวกัน ไม่แก้ implementation ของ CustomerService
        customer.getUser().setPasswordHash(hashPassword(password));
        cartService.createCart(id);
        return customer;
    }

    @Override
    public Seller registerSeller(String username, String email, String password, String confirmPassword) {
        if (!verifyPassword(password, confirmPassword)) {
            throw new IllegalArgumentException("Passwords are invalid or do not match");
        }
        // TODO: ต่อ SellerService และ SellerApplicationService เมื่อทีมตกลงพารามิเตอร์แล้ว
        // createSeller() ยังรับข้อมูลบัญชีไม่ได้ และ Auth interface ยังไม่รับข้อมูลใบสมัคร
        // หยุดก่อนเขียนข้อมูล แทนการสร้างบัญชี/ร้านที่ไม่ครบหรือเขียน service ของเพื่อนแทน
        throw new UnsupportedOperationException(
                "Seller registration is waiting for SellerService parameters and application details");
    }

    @Override
    public String login(String usernameOrEmail, String password) {
        if (usernameOrEmail == null || usernameOrEmail.isBlank()) {
            throw new IllegalArgumentException("Invalid credentials");
        }
        var matches = entities.createQuery(
                "select u from User u where u.username = :identifier or u.email = :identifier", User.class)
                .setParameter("identifier", usernameOrEmail).getResultList();
        if (matches.size() != 1) throw new IllegalArgumentException("Invalid credentials");
        User user = matches.getFirst();
        if (user.getStatus() != UserStatus.ACTIVE || !matchesPassword(password, user.getPasswordHash())) {
            throw new IllegalArgumentException("Invalid credentials");
        }
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        Instant now = Instant.now();
        sessions.entrySet().removeIf(entry -> !entry.getValue().expiresAt().isAfter(now));
        sessions.put(tokenKey(token), new Session(user.getUserId(), user.getPasswordHash(), now.plus(24, ChronoUnit.HOURS)));
        return token;
    }

    @Override
    @Transactional(readOnly = true)
    public boolean validateToken(String token) {
        if (token == null || !token.matches("[A-Za-z0-9_-]{43}")) return false;
        Session session = sessions.get(tokenKey(token));
        if (session == null || !session.expiresAt().isAfter(Instant.now())) return false;
        User user = entities.find(User.class, session.userId());
        return user != null && user.getStatus() == UserStatus.ACTIVE
                && session.passwordHash().equals(user.getPasswordHash());
    }

    @Override
    public boolean verifyPassword(String password, String confirmPassword) {
        return validPassword(password) && password.equals(confirmPassword);
    }

    /** ผู้เรียกต้องยืนยันสิทธิ์ reset ของ userId นี้ก่อน ห้ามเปิดเป็น endpoint รับ id ตรง ๆ */
    @Override
    public boolean resetPassword(long id, String password, String confirmPassword) {
        if (id <= 0 || !verifyPassword(password, confirmPassword)) return false;
        User user = entities.find(User.class, id);
        if (user == null) return false;
        user.setPasswordHash(hashPassword(password));
        // validateToken จะปฏิเสธ session ที่ใช้ hash เก่า หลัง transaction commit แล้ว
        return true;
    }

    private boolean validPassword(String password) {
        return password != null && !password.isBlank() && password.length() >= 6 && password.length() <= 1024;
    }

    private String hashPassword(String password) {
        byte[] salt = new byte[16];
        random.nextBytes(salt);
        var encoder = Base64.getEncoder();
        return "pbkdf2$" + PASSWORD_ITERATIONS + "$" + encoder.encodeToString(salt)
                + "$" + encoder.encodeToString(deriveKey(password, salt));
    }

    private boolean matchesPassword(String password, String encoded) {
        if (!validPassword(password) || encoded == null) return false;
        String[] parts = encoded.split("\\$", -1);
        if (parts.length != 4 || !parts[0].equals("pbkdf2")
                || !parts[1].equals(Integer.toString(PASSWORD_ITERATIONS))) return false;
        try {
            byte[] salt = Base64.getDecoder().decode(parts[2]);
            byte[] expected = Base64.getDecoder().decode(parts[3]);
            return salt.length == 16 && expected.length == 32
                    && MessageDigest.isEqual(expected, deriveKey(password, salt));
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    private byte[] deriveKey(String password, byte[] salt) {
        var spec = new PBEKeySpec(password.toCharArray(), salt, PASSWORD_ITERATIONS, 256);
        try {
            return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).getEncoded();
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("Password hashing is unavailable", e);
        } finally {
            spec.clearPassword();
        }
    }

    private String tokenKey(String token) {
        try {
            return Base64.getEncoder().encodeToString(MessageDigest.getInstance("SHA-256")
                    .digest(token.getBytes(StandardCharsets.UTF_8)));
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("Token hashing is unavailable", e);
        }
    }
}

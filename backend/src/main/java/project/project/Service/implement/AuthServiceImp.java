package project.project.Service.implement;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validator;
import project.project.DTO.auth.RegisterSellerRequest;
import project.project.DTO.customer.CreateCustomerRequest;
import project.project.Entity.order.Cart;
import project.project.Entity.seller.Seller;
import project.project.Entity.user.*;
import project.project.Repository.*;
import project.project.Service.api.AuthService;
import project.project.Service.api.CustomerService;

@Service
@Transactional
public class AuthServiceImp implements AuthService {
    private final UserRepository users;
    private final CustomerRepository customers;
    private final CartRepository carts;
    private final AuthSessionRepository sessions;
    private final CustomerService customerService;
    private final SellerRegistrationService sellerRegistration;
    private final PasswordService passwords;
    private final Validator validator;
    private final SecureRandom random = new SecureRandom();

    public AuthServiceImp(UserRepository users, CustomerRepository customers, CartRepository carts,
            AuthSessionRepository sessions, CustomerService customerService,
            SellerRegistrationService sellerRegistration, PasswordService passwords, Validator validator) {
        this.users = users;
        this.customers = customers;
        this.carts = carts;
        this.sessions = sessions;
        this.customerService = customerService;
        this.sellerRegistration = sellerRegistration;
        this.passwords = passwords;
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
        Customer customer = customers.findById(id)
                .orElseThrow(() -> new IllegalStateException("Created customer was not found"));
        carts.save(new Cart(null, customer));
        return customer;
    }

    @Override
    public Seller registerSeller(RegisterSellerRequest request) {
        return sellerRegistration.register(request);
    }

    @Override
    public String login(String usernameOrEmail, String password) {
        if (usernameOrEmail == null || usernameOrEmail.isBlank()) {
            throw new IllegalArgumentException("Invalid credentials");
        }
        // Refuse ambiguous identifiers rather than authenticating the wrong account.
        Optional<User> byUsername = users.findByUsername(usernameOrEmail);
        Optional<User> byEmail = users.findByEmail(usernameOrEmail);
        if (byUsername.isPresent() && byEmail.isPresent()
                && !byUsername.get().getUserId().equals(byEmail.get().getUserId())) {
            throw new IllegalArgumentException("Invalid credentials");
        }
        User user = byUsername.or(() -> byEmail)
                .orElseThrow(() -> new IllegalArgumentException("Invalid credentials"));
        if (user.getStatus() != UserStatus.ACTIVE || !passwords.matches(password, user.getPasswordHash())) {
            throw new IllegalArgumentException("Invalid credentials");
        }
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        sessions.deleteByExpiresAtBefore(Instant.now());
        sessions.save(new AuthSession(tokenHash(token), user, Instant.now().plus(24, ChronoUnit.HOURS)));
        return token;
    }

    @Override
    @Transactional(readOnly = true)
    public boolean validateToken(String token) {
        return activeSession(token).isPresent();
    }

    @Override
    public boolean verifyPassword(String password, String confirmPassword) {
        return passwords.isValid(password) && password.equals(confirmPassword);
    }

    @Override
    public boolean resetPassword(String token, String currentPassword, String password, String confirmPassword) {
        if (!verifyPassword(password, confirmPassword)) return false;
        var session = activeSession(token);
        if (session.isEmpty()) return false;
        User user = session.get().getUser();
        if (!passwords.matches(currentPassword, user.getPasswordHash())) return false;
        user.setPasswordHash(passwords.hash(password));
        users.save(user);
        sessions.deleteByUser_UserId(user.getUserId());
        return true;
    }

    private Optional<AuthSession> activeSession(String token) {
        if (token == null || !token.matches("[A-Za-z0-9_-]{43}")) return Optional.empty();
        return sessions.findById(tokenHash(token))
                .filter(session -> session.getExpiresAt().isAfter(Instant.now()))
                .filter(session -> session.getUser().getStatus() == UserStatus.ACTIVE)
                .filter(session -> session.getCredentialHash().equals(session.getUser().getPasswordHash()));
    }

    private String tokenHash(String token) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(token.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is unavailable", e);
        }
    }
}

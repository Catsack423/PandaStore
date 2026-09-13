package project.project.Service.implement;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HexFormat;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validator;
import project.project.DTO.customer.CreateCustomerRequest;
import project.project.Entity.seller.Seller;
import project.project.Entity.user.*;
import project.project.Repository.*;
import project.project.Service.api.AuthService;
import project.project.Service.api.CustomerService;
import project.project.Service.api.CartService;

@Service
@Transactional
public class AuthServiceImp implements AuthService {
    private final UserRepository users;
    private final CustomerRepository customers;
    private final CartService cartService;
    private final AuthSessionRepository sessions;
    private final CustomerService customerService;
    private final PasswordService passwords;
    private final Validator validator;
    private final JwtTokenService jwt;

    public AuthServiceImp(UserRepository users, CustomerRepository customers, CartService cartService,
            AuthSessionRepository sessions, CustomerService customerService,
            PasswordService passwords, Validator validator, JwtTokenService jwt) {
        this.users = users;
        this.customers = customers;
        this.cartService = cartService;
        this.sessions = sessions;
        this.customerService = customerService;
        this.passwords = passwords;
        this.validator = validator;
        this.jwt = jwt;
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
        // Auth owns password hashing; leave CustomerService's implementation unchanged.
        customer.getUser().setPasswordHash(passwords.hash(password));
        users.save(customer.getUser());
        cartService.createCart(id);
        return customer;
    }

    @Override
    public Seller registerSeller(String username, String email, String password, String confirmPassword) {
        if (!verifyPassword(password, confirmPassword)) {
            throw new IllegalArgumentException("Passwords are invalid or do not match");
        }
        // TODO: connect SellerService and SellerApplicationService after the team agrees on inputs.
        // createSeller() currently accepts no account details; this interface has no application details.
        throw new UnsupportedOperationException(
                "Seller registration is waiting for SellerService parameters and application details");
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
        Instant now = Instant.now().truncatedTo(ChronoUnit.SECONDS);
        Instant expiresAt = now.plus(24, ChronoUnit.HOURS);
        String token = jwt.issue(user.getUserId(), now, expiresAt);
        sessions.deleteByExpiresAtBefore(now);
        sessions.save(new AuthSession(tokenHash(token), user, expiresAt));
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

    /** The caller must authorize the reset for this user ID before calling this method. */
    @Override
    public boolean resetPassword(long id, String password, String confirmPassword) {
        if (id <= 0 || !verifyPassword(password, confirmPassword)) return false;
        var existing = users.findById(id);
        if (existing.isEmpty()) return false;
        User user = existing.get();
        user.setPasswordHash(passwords.hash(password));
        users.save(user);
        sessions.deleteByUser_UserId(user.getUserId());
        return true;
    }

    private Optional<AuthSession> activeSession(String token) {
        var subject = jwt.verifiedSubject(token);
        if (subject.isEmpty()) return Optional.empty();
        return sessions.findById(tokenHash(token))
                .filter(session -> session.getUser().getUserId().toString().equals(subject.get()))
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

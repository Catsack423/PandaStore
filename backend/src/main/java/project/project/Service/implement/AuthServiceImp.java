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
import project.project.DTO.auth.AuthRequests;
import project.project.DTO.customer.CreateCustomerRequest;
import project.project.DTO.seller.CreateSellerRequest;
import project.project.Entity.seller.Seller;
import project.project.Entity.user.*;
import project.project.Repository.*;
import project.project.Service.api.AuthService;
import project.project.Service.api.CartService;
import project.project.Service.api.CustomerService;
import project.project.Service.api.SellerService;

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
    private final project.project.Security.SessionAuthenticator authenticator;
    private final SellerRepository sellers;
    private final SellerService sellerService;

    public AuthServiceImp(UserRepository users, CustomerRepository customers, CartService cartService,
            AuthSessionRepository sessions, CustomerService customerService,
            PasswordService passwords, Validator validator, JwtTokenService jwt,
            project.project.Security.SessionAuthenticator authenticator,
            SellerRepository sellers, SellerService sellerService) {
        this.users = users;
        this.customers = customers;
        this.cartService = cartService;
        this.sessions = sessions;
        this.customerService = customerService;
        this.passwords = passwords;
        this.validator = validator;
        this.jwt = jwt;
        this.authenticator = authenticator;
        this.sellers = sellers;
        this.sellerService = sellerService;
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
    public Seller registerSeller(AuthRequests.RegisterSeller request) {
        if (request == null) {
            throw new IllegalArgumentException("Request body cannot be null");
        }
        var violations = validator.validate(request);
        if (!violations.isEmpty()) {
            throw new ConstraintViolationException(violations);
        }
        if (request.confirmPassword() != null && !request.confirmPassword().isBlank()) {
            if (!verifyPassword(request.password(), request.confirmPassword())) {
                throw new IllegalArgumentException("Passwords are invalid or do not match");
            }
        } else {
            if (!passwords.isValid(request.password())) {
                throw new IllegalArgumentException("Password does not meet requirements");
            }
        }

        var createSellerRequest = CreateSellerRequest.from(request);
        long sellerId = sellerService.createSeller(createSellerRequest);
        Seller seller = sellers.findById(sellerId)
                .orElseThrow(() -> new IllegalStateException("Created seller was not found"));
        seller.getUser().setPasswordHash(passwords.hash(request.password()));
        users.save(seller.getUser());
        return seller;
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
        return authenticator.authenticate(token).isPresent();
    }

    @Override
    public boolean logout(String token) {
        if (authenticator.authenticate(token).isEmpty()) return false;
        sessions.deleteById(tokenHash(token));
        return true;
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

    private String tokenHash(String token) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(token.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is unavailable", e);
        }
    }
}

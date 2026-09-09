package project.project.Service.implement;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import project.project.DTO.auth.RegisterSellerRequest;
import project.project.Entity.seller.*;
import project.project.Entity.user.*;
import project.project.Exception.DuplicateUserException;
import project.project.Repository.*;

@Service
public class SellerRegistrationService {
    private final UserRepository users;
    private final SellerRepository sellers;
    private final SellerApplicationRepository applications;
    private final PasswordService passwords;
    private final jakarta.validation.Validator validator;

    public SellerRegistrationService(UserRepository users, SellerRepository sellers,
            SellerApplicationRepository applications, PasswordService passwords,
            jakarta.validation.Validator validator) {
        this.users = users;
        this.sellers = sellers;
        this.applications = applications;
        this.passwords = passwords;
        this.validator = validator;
    }

    @Transactional
    public Seller register(RegisterSellerRequest request) {
        if (request == null) throw new IllegalArgumentException("Seller details are required");
        var violations = validator.validate(request);
        if (!violations.isEmpty()) throw new jakarta.validation.ConstraintViolationException(violations);
        if (!passwords.isValid(request.password()) || !request.password().equals(request.confirmPassword())) {
            throw new IllegalArgumentException("Passwords are invalid or do not match");
        }
        if (users.existsByUsername(request.username()) || users.existsByEmail(request.email())) {
            throw new DuplicateUserException("Username or email is already registered");
        }
        User user = users.save(new User(request.username(), request.email(), passwords.hash(request.password()),
                UserRole.SELLER, UserStatus.ACTIVE));
        Seller seller = new Seller();
        seller.setUser(user);
        seller.setShopName(request.shopName());
        seller.setShopDescription(request.shopDescription());
        seller.setShopPhone(request.shopPhone());
        seller.setShopEmail(request.shopEmail());
        seller.setShopAddress(request.shopAddress());
        seller.setStatus(SellerStatus.PENDING);
        seller = sellers.save(seller);

        SellerApplication application = new SellerApplication();
        application.setUser(user);
        application.setShopName(request.shopName());
        application.setShopDescription(request.shopDescription());
        application.setShopPhone(request.shopPhone());
        application.setShopEmail(request.shopEmail());
        application.setShopAddress(request.shopAddress());
        application.setSellerFirstName(request.sellerFirstName());
        application.setSellerLastName(request.sellerLastName());
        application.setIdCardNumber(request.idCardNumber());
        application.setIdCardImageUrl(request.idCardImageUrl());
        application.setBankAccountName(request.bankAccountName());
        application.setBankName(request.bankName());
        application.setBankAccountNumber(request.bankAccountNumber());
        application.setBankBookImageUrl(request.bankBookImageUrl());
        application.setStatus(SellerApplicationStatus.PENDING);
        applications.save(application);
        return seller;
    }
}

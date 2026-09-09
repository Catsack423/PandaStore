package project.project.Service.api;

import project.project.DTO.auth.RegisterSellerRequest;
import project.project.Entity.seller.Seller;
import project.project.Entity.user.Customer;

public interface AuthService {
    // Creates the customer and an empty cart in one transaction.
    Customer registerCustomer(String username, String email, String password, String confirmPassword,String fullName, String phoneNumber);
    // Creates a pending seller and a pending application in one transaction.
    Seller registerSeller(RegisterSellerRequest request);

    
    String login(String usernameOrEmail, String password);

    boolean validateToken(String token);

    boolean verifyPassword(String password, String confirmPassword);
    
    // Authenticated password change; invalidates all previously issued tokens.
    boolean resetPassword(String token, String currentPassword, String password, String confirmPassword);
}

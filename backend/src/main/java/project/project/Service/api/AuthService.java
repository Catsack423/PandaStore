package project.project.Service.api;

import project.project.DTO.customer.CreateCustomerRequest;
import project.project.Entity.seller.Seller;
import project.project.Entity.user.Customer;
import project.project.Entity.user.User;

public interface AuthService {
    //Edit Parameter as You want for this function
    //TODO: Create Customer by Using CustumerService and create Cart For User
    Customer registerCustomer(String username, String email, String password, String confirmPassword,String fullName, String phoneNumber);
    //TODO: Create Seller Using SellerService and create Shop Application for seller
    Seller registerSeller(String username, String email, String password, String confirmPassword);

    
    String login(String usernameOrEmail, String password);

    boolean validateToken(String token);

    boolean verifyPassword(String password, String confirmPassword);
    
    boolean resetPassword(long id, String password, String confirmPassword);
}

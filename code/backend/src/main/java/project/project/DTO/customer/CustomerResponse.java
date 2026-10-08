package project.project.DTO.customer;

import lombok.Getter;
import lombok.Setter;
import project.project.Entity.user.Customer;
@Getter 
@Setter 
public class CustomerResponse {

    private Long customerId;
    private Long userId;
    private String username;
    private String email;
    private String fullName;
    private String phoneNumber;

    public CustomerResponse() {
    }

    public CustomerResponse(Long customerId, Long userId, String username, String email, String fullName, String phoneNumber) {
        this.customerId = customerId;
        this.userId = userId;
        this.username = username;
        this.email = email;
        this.fullName = fullName;
        this.phoneNumber = phoneNumber;
    }

    public static CustomerResponse fromEntity(Customer customer) {
        if (customer == null) {
            return null;
        }
        Long userId = (customer.getUser() != null) ? customer.getUser().getUserId() : null;
        String username = (customer.getUser() != null) ? customer.getUser().getUsername() : null;
        String email = (customer.getUser() != null) ? customer.getUser().getEmail() : null;

        return new CustomerResponse(
            customer.getCustomerId(),
            userId,
            username,
            email,
            customer.getFullName(),
            customer.getPhoneNumber()
        );
    }

    public Long getCustomerId() {
        return customerId;
    }

    public void setCustomerId(Long customerId) {
        this.customerId = customerId;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }
}

package project.project.DTO.seller;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import project.project.DTO.auth.AuthRequests;

public class CreateSellerRequest {

    @NotBlank(message = "Username is required")
    @Size(min = 3, max = 50, message = "Username must be between 3 and 50 characters")
    private String username;

    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    @Size(max = 100, message = "Email must not exceed 100 characters")
    private String email;

    @NotBlank(message = "Password is required")
    @Size(min = 6, message = "Password must contain at least 6 characters")
    private String password;

    @NotBlank(message = "Shop name is required")
    @Size(max = 100, message = "Shop name must not exceed 100 characters")
    private String shopName;

    @NotBlank(message = "Shop description is required")
    private String shopDescription;

    @NotBlank(message = "Shop phone number is required")
    @Pattern(regexp = "^[0-9]{9,15}$", message = "Shop phone number must contain 9-15 digits")
    private String shopPhone;

    @NotBlank(message = "Shop email is required")
    @Email(message = "Invalid shop email format")
    @Size(max = 100, message = "Shop email must not exceed 100 characters")
    private String shopEmail;

    @NotBlank(message = "Shop address is required")
    @Size(max = 255, message = "Shop address must not exceed 255 characters")
    private String shopAddress;

    @NotBlank(message = "Seller first name is required")
    @Size(max = 100, message = "Seller first name must not exceed 100 characters")
    private String sellerFirstName;

    @NotBlank(message = "Seller last name is required")
    @Size(max = 100, message = "Seller last name must not exceed 100 characters")
    private String sellerLastName;

    @NotBlank(message = "National ID number is required")
    @Pattern(regexp = "^[0-9]{13}$", message = "National ID number must contain 13 digits")
    private String idCardNumber;

    @NotBlank(message = "Bank name is required")
    @Size(max = 100, message = "Bank name must not exceed 100 characters")
    private String bankName;

    @NotBlank(message = "Bank account name is required")
    @Size(max = 100, message = "Bank account name must not exceed 100 characters")
    private String bankAccountName;

    @NotBlank(message = "Bank account number is required")
    @Pattern(regexp = "^[0-9]{10,15}$", message = "Bank account number must contain 10-15 digits")
    private String bankAccountNumber;

    private String idCardImageUrl;
    private String bankBookImageUrl;
    private String proofImageUrl;

    public CreateSellerRequest() {
    }

    public CreateSellerRequest(String username, String email, String password, String shopName,
                               String shopDescription, String shopPhone, String shopEmail,
                               String shopAddress, String sellerFirstName, String sellerLastName,
                               String idCardNumber, String bankName, String bankAccountName,
                               String bankAccountNumber, String idCardImageUrl, String bankBookImageUrl,
                               String proofImageUrl) {
        this.username = username;
        this.email = email;
        this.password = password;
        this.shopName = shopName;
        this.shopDescription = shopDescription;
        this.shopPhone = shopPhone;
        this.shopEmail = shopEmail;
        this.shopAddress = shopAddress;
        this.sellerFirstName = sellerFirstName;
        this.sellerLastName = sellerLastName;
        this.idCardNumber = idCardNumber;
        this.bankName = bankName;
        this.bankAccountName = bankAccountName;
        this.bankAccountNumber = bankAccountNumber;
        this.idCardImageUrl = idCardImageUrl;
        this.bankBookImageUrl = bankBookImageUrl;
        this.proofImageUrl = proofImageUrl;
    }

    public static CreateSellerRequest from(AuthRequests.RegisterSeller r) {
        return new CreateSellerRequest(
                r.username(),
                r.email(),
                r.password(),
                r.shopName(),
                r.shopDescription(),
                r.shopPhone(),
                r.shopEmail(),
                r.shopAddress(),
                r.sellerFirstName(),
                r.sellerLastName(),
                r.idCardNumber(),
                r.bankName(),
                r.bankAccountName(),
                r.bankAccountNumber(),
                r.idCardImageUrl(),
                r.bankBookImageUrl(),
                r.proofImageUrl()
        );
    }

    public CreateSellerApplicationRequest toSellerApplicationRequest() {
        return new CreateSellerApplicationRequest(
                shopName,
                shopDescription,
                shopPhone,
                shopEmail,
                shopAddress,
                sellerFirstName,
                sellerLastName,
                idCardNumber,
                idCardImageUrl != null ? idCardImageUrl : "",
                bankAccountName,
                bankName,
                bankAccountNumber,
                bankBookImageUrl != null ? bankBookImageUrl : ""
        );
    }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getShopName() { return shopName; }
    public void setShopName(String shopName) { this.shopName = shopName; }

    public String getShopDescription() { return shopDescription; }
    public void setShopDescription(String shopDescription) { this.shopDescription = shopDescription; }

    public String getShopPhone() { return shopPhone; }
    public void setShopPhone(String shopPhone) { this.shopPhone = shopPhone; }

    public String getShopEmail() { return shopEmail; }
    public void setShopEmail(String shopEmail) { this.shopEmail = shopEmail; }

    public String getShopAddress() { return shopAddress; }
    public void setShopAddress(String shopAddress) { this.shopAddress = shopAddress; }

    public String getSellerFirstName() { return sellerFirstName; }
    public void setSellerFirstName(String sellerFirstName) { this.sellerFirstName = sellerFirstName; }

    public String getSellerLastName() { return sellerLastName; }
    public void setSellerLastName(String sellerLastName) { this.sellerLastName = sellerLastName; }

    public String getIdCardNumber() { return idCardNumber; }
    public void setIdCardNumber(String idCardNumber) { this.idCardNumber = idCardNumber; }

    public String getBankName() { return bankName; }
    public void setBankName(String bankName) { this.bankName = bankName; }

    public String getBankAccountName() { return bankAccountName; }
    public void setBankAccountName(String bankAccountName) { this.bankAccountName = bankAccountName; }

    public String getBankAccountNumber() { return bankAccountNumber; }
    public void setBankAccountNumber(String bankAccountNumber) { this.bankAccountNumber = bankAccountNumber; }

    public String getIdCardImageUrl() { return idCardImageUrl; }
    public void setIdCardImageUrl(String idCardImageUrl) { this.idCardImageUrl = idCardImageUrl; }

    public String getBankBookImageUrl() { return bankBookImageUrl; }
    public void setBankBookImageUrl(String bankBookImageUrl) { this.bankBookImageUrl = bankBookImageUrl; }

    public String getProofImageUrl() { return proofImageUrl; }
    public void setProofImageUrl(String proofImageUrl) { this.proofImageUrl = proofImageUrl; }
}


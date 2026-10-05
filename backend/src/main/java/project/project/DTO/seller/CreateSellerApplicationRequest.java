package project.project.DTO.seller;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class CreateSellerApplicationRequest {

    @NotBlank(message = "Shop name is required")
    @Size(max = 100, message = "Shop name must not exceed 100 characters")
    private String shopName;

    @NotBlank(message = "Shop description is required")
    private String shopDescription;

    @NotBlank(message = "Shop phone number is required")
    @Pattern(regexp = "^[0-9]{9,15}$", message = "Phone number must contain 9-15 digits")
    private String shopPhone;

    @NotBlank(message = "Shop email is required")
    @Email(message = "Invalid email format")
    @Size(max = 100, message = "Email must not exceed 100 characters")
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

    @NotBlank(message = "National ID number must contain 13 digits")
    @Pattern(regexp = "^[0-9]{13}$", message = "National ID number must contain 13 digits")
    private String idCardNumber;

    @NotBlank(message = "National ID card image is required")
    @Size(max = 255, message = "National ID card image URL must not exceed 255 characters")
    private String idCardImageUrl;

    @NotBlank(message = "Bank account name is required")
    @Size(max = 100, message = "Bank account name must not exceed 100 characters")
    private String bankAccountName;

    @NotBlank(message = "Bank name is required")
    @Size(max = 100, message = "Bank name must not exceed 100 characters")
    private String bankName;

    @NotBlank(message = "Bank account number is required")
    @Size(max = 30, message = "Bank account number must not exceed 30 characters")
    private String bankAccountNumber;

    @NotBlank(message = "Bankbook image is required")
    @Size(max = 255, message = "Bankbook image URL must not exceed 255 characters")
    private String bankBookImageUrl;

    public CreateSellerApplicationRequest() {
    }

    public CreateSellerApplicationRequest(String shopName, String shopDescription, String shopPhone, String shopEmail,
                                         String shopAddress, String sellerFirstName, String sellerLastName,
                                         String idCardNumber, String idCardImageUrl, String bankAccountName,
                                         String bankName, String bankAccountNumber, String bankBookImageUrl) {
        this.shopName = shopName;
        this.shopDescription = shopDescription;
        this.shopPhone = shopPhone;
        this.shopEmail = shopEmail;
        this.shopAddress = shopAddress;
        this.sellerFirstName = sellerFirstName;
        this.sellerLastName = sellerLastName;
        this.idCardNumber = idCardNumber;
        this.idCardImageUrl = idCardImageUrl;
        this.bankAccountName = bankAccountName;
        this.bankName = bankName;
        this.bankAccountNumber = bankAccountNumber;
        this.bankBookImageUrl = bankBookImageUrl;
    }

    public String getShopName() {
        return shopName;
    }

    public void setShopName(String shopName) {
        this.shopName = shopName;
    }

    public String getShopDescription() {
        return shopDescription;
    }

    public void setShopDescription(String shopDescription) {
        this.shopDescription = shopDescription;
    }

    public String getShopPhone() {
        return shopPhone;
    }

    public void setShopPhone(String shopPhone) {
        this.shopPhone = shopPhone;
    }

    public String getShopEmail() {
        return shopEmail;
    }

    public void setShopEmail(String shopEmail) {
        this.shopEmail = shopEmail;
    }

    public String getShopAddress() {
        return shopAddress;
    }

    public void setShopAddress(String shopAddress) {
        this.shopAddress = shopAddress;
    }

    public String getSellerFirstName() {
        return sellerFirstName;
    }

    public void setSellerFirstName(String sellerFirstName) {
        this.sellerFirstName = sellerFirstName;
    }

    public String getSellerLastName() {
        return sellerLastName;
    }

    public void setSellerLastName(String sellerLastName) {
        this.sellerLastName = sellerLastName;
    }

    public String getIdCardNumber() {
        return idCardNumber;
    }

    public void setIdCardNumber(String idCardNumber) {
        this.idCardNumber = idCardNumber;
    }

    public String getIdCardImageUrl() {
        return idCardImageUrl;
    }

    public void setIdCardImageUrl(String idCardImageUrl) {
        this.idCardImageUrl = idCardImageUrl;
    }

    public String getBankAccountName() {
        return bankAccountName;
    }

    public void setBankAccountName(String bankAccountName) {
        this.bankAccountName = bankAccountName;
    }

    public String getBankName() {
        return bankName;
    }

    public void setBankName(String bankName) {
        this.bankName = bankName;
    }

    public String getBankAccountNumber() {
        return bankAccountNumber;
    }

    public void setBankAccountNumber(String bankAccountNumber) {
        this.bankAccountNumber = bankAccountNumber;
    }

    public String getBankBookImageUrl() {
        return bankBookImageUrl;
    }

    public void setBankBookImageUrl(String bankBookImageUrl) {
        this.bankBookImageUrl = bankBookImageUrl;
    }
}

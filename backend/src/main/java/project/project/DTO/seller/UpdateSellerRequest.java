package project.project.DTO.seller;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class UpdateSellerRequest {

    @Size(max = 100, message = "Shop name must not exceed 100 characters")
    private String shopName;

    private String shopDescription;

    @Pattern(regexp = "^[0-9]{9,15}$", message = "Shop phone number must contain 9-15 digits")
    private String shopPhone;

    @Email(message = "Invalid shop email format")
    @Size(max = 100, message = "Shop email must not exceed 100 characters")
    private String shopEmail;

    @Size(max = 255, message = "Shop address must not exceed 255 characters")
    private String shopAddress;

    public UpdateSellerRequest() {
    }

    public UpdateSellerRequest(String shopName, String shopDescription, String shopPhone, String shopEmail, String shopAddress) {
        this.shopName = shopName;
        this.shopDescription = shopDescription;
        this.shopPhone = shopPhone;
        this.shopEmail = shopEmail;
        this.shopAddress = shopAddress;
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
}


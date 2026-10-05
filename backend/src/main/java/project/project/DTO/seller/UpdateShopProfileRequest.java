package project.project.DTO.seller;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateShopProfileRequest {

    @NotBlank(message = "Shop name is required")
    @Size(max = 100, message = "Shop name must not exceed 100 characters")
    private String shopName;

    private String shopDescription;

    @NotBlank(message = "Shop phone number is required")
    @Size(max = 20, message = "Phone number must not exceed 20 characters")
    private String shopPhone;

    @NotBlank(message = "Shop email is required")
    @Email(message = "Invalid email format")
    @Size(max = 100, message = "Email must not exceed 100 characters")
    private String shopEmail;

    @NotBlank(message = "Shop address is required")
    @Size(max = 255, message = "Shop address must not exceed 255 characters")
    private String shopAddress;

    public UpdateShopProfileRequest() {
    }

    public UpdateShopProfileRequest(String shopName, String shopDescription, String shopPhone, String shopEmail, String shopAddress) {
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

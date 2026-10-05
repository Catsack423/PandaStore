package project.project.DTO.seller;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateShopProfileRequest {

    @NotBlank(message = "ชื่อร้านค้าห้ามว่าง")
    @Size(max = 100, message = "ชื่อร้านค้าต้องไม่เกิน 100 ตัวอักษร")
    private String shopName;

    private String shopDescription;

    @NotBlank(message = "เบอร์โทรศัพท์ร้านค้าห้ามว่าง")
    @Size(max = 20, message = "เบอร์โทรศัพท์ต้องไม่เกิน 20 ตัวอักษร")
    private String shopPhone;

    @NotBlank(message = "อีเมลร้านค้าห้ามว่าง")
    @Email(message = "รูปแบบอีเมลไม่ถูกต้อง")
    @Size(max = 100, message = "อีเมลต้องไม่เกิน 100 ตัวอักษร")
    private String shopEmail;

    @NotBlank(message = "ที่อยู่ร้านค้าห้ามว่าง")
    @Size(max = 255, message = "ที่อยู่ร้านค้าต้องไม่เกิน 255 ตัวอักษร")
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

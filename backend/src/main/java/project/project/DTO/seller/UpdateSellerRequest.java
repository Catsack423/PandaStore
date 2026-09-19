package project.project.DTO.seller;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class UpdateSellerRequest {

    @Size(max = 100, message = "ชื่อร้านค้าต้องมีความยาวไม่เกิน 100 ตัวอักษร")
    private String shopName;

    private String shopDescription;

    @Pattern(regexp = "^[0-9]{9,15}$", message = "เบอร์โทรศัพท์ร้านค้าต้องเป็นตัวเลขความยาว 9-15 หลัก")
    private String shopPhone;

    @Email(message = "รูปแบบ Email ร้านค้าไม่ถูกต้อง")
    @Size(max = 100, message = "Email ร้านค้าต้องมีความยาวไม่เกิน 100 ตัวอักษร")
    private String shopEmail;

    @Size(max = 255, message = "ที่อยู่ร้านค้าต้องมีความยาวไม่เกิน 255 ตัวอักษร")
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


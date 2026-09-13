package project.project.DTO.seller;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class CreateSellerApplicationRequest {

    @NotBlank(message = "ชื่อร้านค้าห้ามว่าง")
    @Size(max = 100, message = "ชื่อร้านค้าต้องมีความยาวไม่เกิน 100 ตัวอักษร")
    private String shopName;

    @NotBlank(message = "รายละเอียดร้านค้าห้ามว่าง")
    private String shopDescription;

    @NotBlank(message = "เบอร์โทรศัพท์ร้านค้าห้ามว่าง")
    @Pattern(regexp = "^[0-9]{9,15}$", message = "เบอร์โทรศัพท์ต้องเป็นตัวเลขความยาว 9-15 หลัก")
    private String shopPhone;

    @NotBlank(message = "อีเมลร้านค้าห้ามว่าง")
    @Email(message = "รูปแบบอีเมลไม่ถูกต้อง")
    @Size(max = 100, message = "อีเมลต้องมีความยาวไม่เกิน 100 ตัวอักษร")
    private String shopEmail;

    @NotBlank(message = "ที่อยู่ร้านค้าห้ามว่าง")
    @Size(max = 255, message = "ที่อยู่ร้านค้าต้องมีความยาวไม่เกิน 255 ตัวอักษร")
    private String shopAddress;

    @NotBlank(message = "ชื่อผู้ขายห้ามว่าง")
    @Size(max = 100, message = "ชื่อผู้ขายต้องมีความยาวไม่เกิน 100 ตัวอักษร")
    private String sellerFirstName;

    @NotBlank(message = "นามสกุลผู้ขายห้ามว่าง")
    @Size(max = 100, message = "นามสกุลผู้ขายต้องมีความยาวไม่เกิน 100 ตัวอักษร")
    private String sellerLastName;

    @NotBlank(message = "เลขประจำตัวประชาชนต้องเป็นตัวเลข 13 หลัก")
    @Pattern(regexp = "^[0-9]{13}$", message = "เลขประจำตัวประชาชนต้องเป็นตัวเลข 13 หลัก")
    private String idCardNumber;

    @NotBlank(message = "รูปภาพบัตรประชาชนห้ามว่าง")
    @Size(max = 255, message = "URL รูปภาพบัตรประชาชนต้องมีความยาวไม่เกิน 255 ตัวอักษร")
    private String idCardImageUrl;

    @NotBlank(message = "ชื่อบัญชีธนาคารห้ามว่าง")
    @Size(max = 100, message = "ชื่อบัญชีธนาคารต้องมีความยาวไม่เกิน 100 ตัวอักษร")
    private String bankAccountName;

    @NotBlank(message = "ชื่อธนาคารห้ามว่าง")
    @Size(max = 100, message = "ชื่อธนาคารต้องมีความยาวไม่เกิน 100 ตัวอักษร")
    private String bankName;

    @NotBlank(message = "เลขที่บัญชีธนาคารห้ามว่าง")
    @Size(max = 30, message = "เลขที่บัญชีธนาคารต้องมีความยาวไม่เกิน 30 ตัวอักษร")
    private String bankAccountNumber;

    @NotBlank(message = "รูปภาพสมุดบัญชีห้ามว่าง")
    @Size(max = 255, message = "URL รูปภาพสมุดบัญชีต้องมีความยาวไม่เกิน 255 ตัวอักษร")
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

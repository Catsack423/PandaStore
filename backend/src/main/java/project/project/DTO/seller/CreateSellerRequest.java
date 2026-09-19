package project.project.DTO.seller;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import project.project.DTO.auth.AuthRequests;

public class CreateSellerRequest {

    @NotBlank(message = "Username ห้ามว่าง")
    @Size(min = 3, max = 50, message = "Username ต้องมีความยาวระหว่าง 3 ถึง 50 ตัวอักษร")
    private String username;

    @NotBlank(message = "Email ห้ามว่าง")
    @Email(message = "รูปแบบ Email ไม่ถูกต้อง")
    @Size(max = 100, message = "Email ต้องมีความยาวไม่เกิน 100 ตัวอักษร")
    private String email;

    @NotBlank(message = "Password ห้ามว่าง")
    @Size(min = 6, message = "Password ต้องมีความยาวอย่างน้อย 6 ตัวอักษร")
    private String password;

    @NotBlank(message = "ชื่อร้านค้าห้ามว่าง")
    @Size(max = 100, message = "ชื่อร้านค้าต้องมีความยาวไม่เกิน 100 ตัวอักษร")
    private String shopName;

    @NotBlank(message = "รายละเอียดร้านค้าห้ามว่าง")
    private String shopDescription;

    @NotBlank(message = "เบอร์โทรศัพท์ร้านค้าห้ามว่าง")
    @Pattern(regexp = "^[0-9]{9,15}$", message = "เบอร์โทรศัพท์ร้านค้าต้องเป็นตัวเลขความยาว 9-15 หลัก")
    private String shopPhone;

    @NotBlank(message = "Email ร้านค้าห้ามว่าง")
    @Email(message = "รูปแบบ Email ร้านค้าไม่ถูกต้อง")
    @Size(max = 100, message = "Email ร้านค้าต้องมีความยาวไม่เกิน 100 ตัวอักษร")
    private String shopEmail;

    @NotBlank(message = "ที่อยู่ร้านค้าห้ามว่าง")
    @Size(max = 255, message = "ที่อยู่ร้านค้าต้องมีความยาวไม่เกิน 255 ตัวอักษร")
    private String shopAddress;

    @NotBlank(message = "ชื่อจริงผู้ขายห้ามว่าง")
    @Size(max = 100, message = "ชื่อจริงผู้ขายต้องมีความยาวไม่เกิน 100 ตัวอักษร")
    private String sellerFirstName;

    @NotBlank(message = "นามสกุลจริงผู้ขายห้ามว่าง")
    @Size(max = 100, message = "นามสกุลจริงผู้ขายต้องมีความยาวไม่เกิน 100 ตัวอักษร")
    private String sellerLastName;

    @NotBlank(message = "เลขบัตรประชาชนห้ามว่าง")
    @Pattern(regexp = "^[0-9]{13}$", message = "เลขประจำตัวประชาชนต้องเป็นตัวเลข 13 หลัก")
    private String idCardNumber;

    @NotBlank(message = "ชื่อธนาคารห้ามว่าง")
    @Size(max = 100, message = "ชื่อธนาคารต้องมีความยาวไม่เกิน 100 ตัวอักษร")
    private String bankName;

    @NotBlank(message = "ชื่อบัญชีธนาคารห้ามว่าง")
    @Size(max = 100, message = "ชื่อบัญชีธนาคารต้องมีความยาวไม่เกิน 100 ตัวอักษร")
    private String bankAccountName;

    @NotBlank(message = "เลขที่บัญชีธนาคารห้ามว่าง")
    @Pattern(regexp = "^[0-9]{10,15}$", message = "เลขที่บัญชีต้องเป็นตัวเลข 10-15 หลัก")
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


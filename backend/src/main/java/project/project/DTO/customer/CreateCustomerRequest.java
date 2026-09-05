package project.project.DTO.customer;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;


public class CreateCustomerRequest {

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

    @NotBlank(message = "Full Name ห้ามว่าง")
    @Size(max = 100, message = "Full Name ต้องมีความยาวไม่เกิน 100 ตัวอักษร")
    private String fullName;

    @NotBlank(message = "Phone Number ห้ามว่าง")
    @Pattern(regexp = "^[0-9]{9,15}$", message = "เบอร์โทรศัพท์ต้องเป็นตัวเลขความยาว 9-15 หลัก")
    private String phoneNumber;

    public CreateCustomerRequest() {
    }

    public CreateCustomerRequest(String username, String email, String password, String fullName, String phoneNumber) {
        this.username = username;
        this.email = email;
        this.password = password;
        this.fullName = fullName;
        this.phoneNumber = phoneNumber;
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

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
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

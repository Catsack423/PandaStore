package project.project.DTO.customer;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
@Getter 
@Setter 
public class UpdateCustomerRequest {

    @Size(max = 100, message = "Full Name ต้องมีความยาวไม่เกิน 100 ตัวอักษร")
    private String fullName;

    @Pattern(regexp = "^[0-9]{9,15}$", message = "เบอร์โทรศัพท์ต้องเป็นตัวเลขความยาว 9-15 หลัก")
    private String phoneNumber;

    public UpdateCustomerRequest() {
    }

    public UpdateCustomerRequest(String fullName, String phoneNumber) {
        this.fullName = fullName;
        this.phoneNumber = phoneNumber;
    }


}

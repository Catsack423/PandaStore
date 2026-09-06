package project.project.DTO.address;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record CreateAddressRequest(

        @NotNull(message = "Customer ID ห้ามว่าง")
        @Positive(message = "Customer ID ต้องมากกว่า 0")
        Long customerId,

        @NotBlank(message = "ชื่อผู้รับห้ามว่าง")
        @Size(max = 100, message = "ชื่อผู้รับต้องมีความยาวไม่เกิน 100 ตัวอักษร")
        String receiverName,

        @NotBlank(message = "เบอร์โทรศัพท์ห้ามว่าง")
        @Pattern(regexp = "^[0-9]{9,15}$", message = "เบอร์โทรศัพท์ต้องเป็นตัวเลขความยาว 9-15 หลัก")
        String phoneNumber,

        @NotBlank(message = "ที่อยู่ห้ามว่าง")
        @Size(max = 255, message = "ที่อยู่ต้องมีความยาวไม่เกิน 255 ตัวอักษร")
        String addressLine,

        @NotBlank(message = "อำเภอ/เขต ห้ามว่าง")
        @Size(max = 100, message = "อำเภอ/เขต ต้องมีความยาวไม่เกิน 100 ตัวอักษร")
        String district,

        @NotBlank(message = "จังหวัดห้ามว่าง")
        @Size(max = 100, message = "จังหวัดต้องมีความยาวไม่เกิน 100 ตัวอักษร")
        String province,

        @NotBlank(message = "รหัสไปรษณีย์ห้ามว่าง")
        @Pattern(regexp = "^[0-9]{5}$", message = "รหัสไปรษณีย์ต้องเป็นตัวเลข 5 หลัก")
        String postalCode,

        Boolean isDefault
) {
    public CreateAddressRequest {
        if (isDefault == null) {
            isDefault = false;
        }
    }
}


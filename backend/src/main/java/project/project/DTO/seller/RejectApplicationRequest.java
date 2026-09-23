package project.project.DTO.seller;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class RejectApplicationRequest {

    @NotBlank(message = "เหตุผลในการปฏิเสธห้ามว่าง")
    @Size(max = 1000, message = "เหตุผลในการปฏิเสธต้องมีความยาวไม่เกิน 1000 ตัวอักษร")
    private String reason;

    public RejectApplicationRequest() {
    }

    public RejectApplicationRequest(String reason) {
        this.reason = reason;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }
}

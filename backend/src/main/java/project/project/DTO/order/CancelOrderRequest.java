package project.project.DTO.order;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CancelOrderRequest {

    @NotBlank(message = "กรุณาระบุเหตุผลในการยกเลิกคำสั่งซื้อ")
    @Size(max = 255, message = "เหตุผลต้องไม่เกิน 255 ตัวอักษร")
    private String reason;

    public CancelOrderRequest() {
    }

    public CancelOrderRequest(String reason) {
        this.reason = reason;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }
}

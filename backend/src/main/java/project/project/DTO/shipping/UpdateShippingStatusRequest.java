package project.project.DTO.shipping;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateShippingStatusRequest {

    @NotBlank(message = "กรุณาระบุสถานะการจัดส่ง (status เช่น PENDING, SHIPPED, DELIVERED)")
    private String status;

    public UpdateShippingStatusRequest() {
    }

    public UpdateShippingStatusRequest(String status) {
        this.status = status;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}

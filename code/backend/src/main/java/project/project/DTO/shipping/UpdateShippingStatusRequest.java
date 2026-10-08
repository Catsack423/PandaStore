package project.project.DTO.shipping;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateShippingStatusRequest {

    @NotBlank(message = "Please specify the shipping status (status, e.g. PENDING, SHIPPED, DELIVERED)")
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

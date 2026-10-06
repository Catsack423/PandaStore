package project.project.DTO.order;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RejectOrderRequest {

    @NotBlank(message = "Please provide a reason for rejecting the order")
    @Size(max = 255, message = "Reason must not exceed 255 characters")
    private String reason;

    public RejectOrderRequest() {
    }

    public RejectOrderRequest(String reason) {
        this.reason = reason;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }
}

package project.project.DTO.seller;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class RejectApplicationRequest {

    @NotBlank(message = "Rejection reason is required")
    @Size(max = 1000, message = "Rejection reason must not exceed 1000 characters")
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

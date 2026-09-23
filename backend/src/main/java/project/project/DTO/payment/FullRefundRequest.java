package project.project.DTO.payment;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class FullRefundRequest {

    @NotNull(message = "Order group ID is required")
    private Long orderGroupId;

    @NotBlank(message = "Refund reason is required")
    private String reason;

    public FullRefundRequest() {
    }

    public FullRefundRequest(Long orderGroupId, String reason) {
        this.orderGroupId = orderGroupId;
        this.reason = reason;
    }

    public Long getOrderGroupId() {
        return orderGroupId;
    }

    public void setOrderGroupId(Long orderGroupId) {
        this.orderGroupId = orderGroupId;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }
}

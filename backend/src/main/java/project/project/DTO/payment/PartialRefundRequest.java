package project.project.DTO.payment;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public class PartialRefundRequest {

    @NotNull(message = "Order ID is required")
    private Long orderId;

    @NotNull(message = "Refund amount is required")
    @DecimalMin(value = "0.01", message = "Refund amount must be greater than 0")
    private BigDecimal refundAmount;

    @NotBlank(message = "Refund reason is required")
    private String reason;

    public PartialRefundRequest() {
    }

    public PartialRefundRequest(Long orderId, BigDecimal refundAmount, String reason) {
        this.orderId = orderId;
        this.refundAmount = refundAmount;
        this.reason = reason;
    }

    public Long getOrderId() {
        return orderId;
    }

    public void setOrderId(Long orderId) {
        this.orderId = orderId;
    }

    public BigDecimal getRefundAmount() {
        return refundAmount;
    }

    public void setRefundAmount(BigDecimal refundAmount) {
        this.refundAmount = refundAmount;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }
}

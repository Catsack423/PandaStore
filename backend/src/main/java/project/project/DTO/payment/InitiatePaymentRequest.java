package project.project.DTO.payment;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import project.project.Entity.order.PaymentMethod;

import java.math.BigDecimal;

public class InitiatePaymentRequest {

    @NotNull(message = "Order group ID is required")
    private Long orderGroupId;

    @NotNull(message = "Payment method is required")
    private PaymentMethod paymentMethod;

    @DecimalMin(value = "0.01", message = "Amount must be greater than 0")
    private BigDecimal amount;

    public InitiatePaymentRequest() {
    }

    public InitiatePaymentRequest(Long orderGroupId, PaymentMethod paymentMethod, BigDecimal amount) {
        this.orderGroupId = orderGroupId;
        this.paymentMethod = paymentMethod;
        this.amount = amount;
    }

    public Long getOrderGroupId() {
        return orderGroupId;
    }

    public void setOrderGroupId(Long orderGroupId) {
        this.orderGroupId = orderGroupId;
    }

    public PaymentMethod getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(PaymentMethod paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }
}

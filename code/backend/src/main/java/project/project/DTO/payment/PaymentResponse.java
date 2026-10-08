package project.project.DTO.payment;

import project.project.Entity.order.Payment;
import project.project.Entity.order.PaymentMethod;
import project.project.Entity.order.PaymentStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class PaymentResponse {

    private Long paymentId;
    private Long orderGroupId;
    private PaymentMethod paymentMethod;
    private BigDecimal amount;
    private BigDecimal refundedAmount;
    private PaymentStatus status;
    private String gatewayTransactionId;
    private LocalDateTime paidAt;

    public PaymentResponse() {
    }

    public PaymentResponse(Long paymentId, Long orderGroupId, PaymentMethod paymentMethod, BigDecimal amount, BigDecimal refundedAmount, PaymentStatus status, String gatewayTransactionId, LocalDateTime paidAt) {
        this.paymentId = paymentId;
        this.orderGroupId = orderGroupId;
        this.paymentMethod = paymentMethod;
        this.amount = amount;
        this.refundedAmount = refundedAmount;
        this.status = status;
        this.gatewayTransactionId = gatewayTransactionId;
        this.paidAt = paidAt;
    }

    public static PaymentResponse fromEntity(Payment payment) {
        if (payment == null) {
            return null;
        }
        return new PaymentResponse(
                payment.getPaymentId(),
                payment.getOrderGroup() != null ? payment.getOrderGroup().getOrderGroupId() : null,
                payment.getPaymentMethod(),
                payment.getAmount(),
                payment.getRefundedAmount(),
                payment.getStatus(),
                payment.getGatewayTransactionId(),
                payment.getPaidAt()
        );
    }

    public Long getPaymentId() {
        return paymentId;
    }

    public void setPaymentId(Long paymentId) {
        this.paymentId = paymentId;
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

    public BigDecimal getRefundedAmount() {
        return refundedAmount;
    }

    public void setRefundedAmount(BigDecimal refundedAmount) {
        this.refundedAmount = refundedAmount;
    }

    public PaymentStatus getStatus() {
        return status;
    }

    public void setStatus(PaymentStatus status) {
        this.status = status;
    }

    public String getGatewayTransactionId() {
        return gatewayTransactionId;
    }

    public void setGatewayTransactionId(String gatewayTransactionId) {
        this.gatewayTransactionId = gatewayTransactionId;
    }

    public LocalDateTime getPaidAt() {
        return paidAt;
    }

    public void setPaidAt(LocalDateTime paidAt) {
        this.paidAt = paidAt;
    }
}

package project.project.DTO.payment;

import jakarta.validation.constraints.NotNull;

public class PaymentCallbackRequest {

    private String gatewayTransactionId;
    private Long orderGroupId;

    @NotNull(message = "Success status is required")
    private Boolean isSuccess = true;

    public PaymentCallbackRequest() {
    }

    public PaymentCallbackRequest(String gatewayTransactionId, Boolean isSuccess) {
        this.gatewayTransactionId = gatewayTransactionId;
        this.isSuccess = isSuccess;
    }

    public PaymentCallbackRequest(Long orderGroupId, Boolean isSuccess) {
        this.orderGroupId = orderGroupId;
        this.isSuccess = isSuccess;
    }

    public String getGatewayTransactionId() {
        return gatewayTransactionId;
    }

    public void setGatewayTransactionId(String gatewayTransactionId) {
        this.gatewayTransactionId = gatewayTransactionId;
    }

    public Long getOrderGroupId() {
        return orderGroupId;
    }

    public void setOrderGroupId(Long orderGroupId) {
        this.orderGroupId = orderGroupId;
    }

    public Boolean getIsSuccess() {
        return isSuccess;
    }

    public void setIsSuccess(Boolean isSuccess) {
        this.isSuccess = isSuccess;
    }
}

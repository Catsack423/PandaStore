package project.project.Service.api;

import project.project.Entity.order.Payment;
import project.project.Entity.order.PaymentMethod;
import java.math.BigDecimal;


//mock
public interface PaymentService {
    Payment initiatePayment(Long orderGroupId, PaymentMethod method, BigDecimal amount);
    void handleGatewayCallback(String gatewayTransactionId, boolean isSuccess);
    void processPartialRefund(Long orderId, BigDecimal refundAmount, String reason);
    void processFullRefund(Long orderGroupId, String reason);
    Payment getPaymentByOrderGroupId(Long orderGroupId);
}

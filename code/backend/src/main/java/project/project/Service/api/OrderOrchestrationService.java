package project.project.Service.api;

import project.project.Entity.order.OrderGroup;
import project.project.Entity.order.PaymentMethod;
import java.util.Map;

public interface OrderOrchestrationService {
    OrderGroup createOrderGroupFromCart(Long customerId, Long shippingAddressId, Map<Long, String> sellerShippingMethods, PaymentMethod paymentMethod);
    void handlePaymentSuccess(Long orderGroupId, String gatewayTransactionId);
    void handlePaymentFailure(Long orderGroupId, String failureReason);
    OrderGroup getOrderGroupDetails(Long orderGroupId);
}

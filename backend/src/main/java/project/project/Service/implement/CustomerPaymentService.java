package project.project.Service.implement;

import jakarta.persistence.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import project.project.DTO.order.OrderGroupResponse;
import project.project.Entity.order.*;
import project.project.Repository.OrderGroupRepository;
import project.project.Service.api.OrderOrchestrationService;

/** Customer payment transitions share the existing checkout lock and inventory transaction. */
@Service
@Transactional
public class CustomerPaymentService {
    private final OrderGroupRepository groups;
    private final OrderStateLock lock;
    private final OrderOrchestrationService orders;
    private final boolean mockEnabled;

    public CustomerPaymentService(OrderGroupRepository groups, OrderStateLock lock,
            OrderOrchestrationService orders, @Value("${MOCK_PAYMENT_ENABLED:false}") boolean mockEnabled) {
        this.groups = groups;
        this.lock = lock;
        this.orders = orders;
        this.mockEnabled = mockEnabled;
    }

    public OrderGroupResponse confirmPayment(Long customerId, Long orderGroupId) {
        var group = pendingOwnedGroup(customerId, orderGroupId);
        if (!mockEnabled) throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Payment is not available");
        // TODO: Replace this demo transition with a verified gateway payment/webhook.
        // Never mark an order paid based on a browser's claim in a real payment integration.
        orders.handlePaymentSuccess(orderGroupId, "DEMO-" + orderGroupId);
        return OrderGroupResponse.fromEntity(group);
    }

    public OrderGroupResponse cancelOrder(Long customerId, Long orderGroupId) {
        var group = pendingOwnedGroup(customerId, orderGroupId);
        // Existing unpaid-payment failure cancels every sub-order and releases reserved stock.
        // FAILED is the existing payment status for a cancelled unpaid checkout; no refund occurred.
        orders.handlePaymentFailure(orderGroupId, "Order cancelled by customer before payment");
        return OrderGroupResponse.fromEntity(group);
    }

    private OrderGroup pendingOwnedGroup(Long customerId, Long id) {
        var group = groups.findById(id).orElseThrow(() -> new EntityNotFoundException("Order not found"));
        if (!group.getCustomer().getCustomerId().equals(customerId)) {
            throw new EntityNotFoundException("Order not found");
        }
        lock.lockGroup(group);
        if (group.getPaymentStatus() != OrderGroupPaymentStatus.PENDING || group.getPayment() == null
                || group.getPayment().getStatus() != PaymentStatus.PENDING || group.getSubOrders().isEmpty()
                || group.getSubOrders().stream().anyMatch(o -> o.getOrderStatus() != OrderStatus.PENDING_PAYMENT)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "This order is no longer pending payment. Refresh to see its current status.");
        }
        return group;
    }
}

package project.project.Service.api;

import project.project.Entity.order.Order;
import java.util.List;

public interface SubOrderService {
    Order getSubOrderById(Long orderId);
    List<Order> getSubOrdersByOrderGroup(Long orderGroupId);
    List<Order> getSubOrdersBySeller(Long sellerId);
    void sellerAcceptOrder(Long sellerId, Long orderId);
    void sellerRejectOrder(Long sellerId, Long orderId, String reason);
    void confirmOrderDelivered(Long customerId, Long orderId);
    void autoConfirmDelivered(Long orderId);
}

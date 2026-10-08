package project.project.Event;

import java.util.List;

public record OrderPaidEvent(
        Long orderGroupId,
        Long customerId,
        List<SellerOrder> sellerOrders) {
    public OrderPaidEvent {
        sellerOrders = List.copyOf(sellerOrders);
    }

    public record SellerOrder(
            Long sellerId,
            Long orderId) {
    }
}
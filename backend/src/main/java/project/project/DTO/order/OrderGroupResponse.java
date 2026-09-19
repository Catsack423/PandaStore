package project.project.DTO.order;

import project.project.Entity.order.Order;
import project.project.Entity.order.OrderGroup;
import project.project.Entity.order.OrderGroupPaymentStatus;
import project.project.Entity.order.OrderItem;
import project.project.Entity.order.OrderStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record OrderGroupResponse(
        Long orderGroupId,
        String groupNumber,
        BigDecimal totalProductsAmount,
        BigDecimal totalShippingFee,
        BigDecimal totalDiscount,
        BigDecimal grandTotal,
        OrderGroupPaymentStatus paymentStatus,
        LocalDateTime createdAt,
        List<SubOrderResponse> subOrders) {
    public static OrderGroupResponse fromEntity(OrderGroup group) {
        return new OrderGroupResponse(
                group.getOrderGroupId(),
                group.getGroupNumber(),
                group.getTotalProductsAmount(),
                group.getTotalShippingFee(),
                group.getTotalDiscount(),
                group.getGrandTotal(),
                group.getPaymentStatus(),
                group.getCreatedAt(),
                group.getSubOrders().stream()
                        .map(SubOrderResponse::fromEntity)
                        .toList());
    }

    public record SubOrderResponse(
            Long orderId,
            String subOrderNumber,
            BigDecimal subtotal,
            BigDecimal shippingFee,
            BigDecimal sellerDiscount,
            BigDecimal totalAmount,
            OrderStatus orderStatus,
            List<OrderItemResponse> items) {
        public static SubOrderResponse fromEntity(Order order) {
            return new SubOrderResponse(
                    order.getOrderId(),
                    order.getSubOrderNumber(),
                    order.getSubtotal(),
                    order.getShippingFee(),
                    order.getSellerDiscount(),
                    order.getTotalAmount(),
                    order.getOrderStatus(),
                    order.getOrderItems().stream()
                            .map(OrderItemResponse::fromEntity)
                            .toList());
        }
    }

    public record OrderItemResponse(
            Long orderItemId,
            String productName,
            BigDecimal unitPrice,
            Integer quantity,
            BigDecimal totalPrice) {
        public static OrderItemResponse fromEntity(OrderItem item) {
            return new OrderItemResponse(
                    item.getOrderItemId(),
                    item.getProductName(),
                    item.getUnitPrice(),
                    item.getQuantity(),
                    item.getTotalPrice());
        }
    }
}
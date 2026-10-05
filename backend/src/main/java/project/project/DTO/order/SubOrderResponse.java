package project.project.DTO.order;

import lombok.Getter;
import lombok.Setter;
import project.project.DTO.shipping.ShipmentResponse;
import project.project.Entity.order.Order;
import project.project.Entity.order.OrderStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Getter
@Setter
public class SubOrderResponse {

    private Long orderId;
    private String subOrderNumber;
    private Long orderGroupId;
    private Long sellerId;
    private String shopName;
    private BigDecimal subtotal;
    private BigDecimal shippingFee;
    private BigDecimal sellerDiscount;
    private BigDecimal totalAmount;
    private OrderStatus orderStatus;
    private String rejectionReason;
    private LocalDateTime shippedAt;
    private LocalDateTime completedAt;
    private LocalDateTime createdAt;
    private List<OrderItemResponse> items = new ArrayList<>();
    private ShipmentResponse shipment;

    public SubOrderResponse() {
    }

    public SubOrderResponse(Long orderId, String subOrderNumber, Long orderGroupId, Long sellerId,
                            String shopName, BigDecimal subtotal, BigDecimal shippingFee,
                            BigDecimal sellerDiscount, BigDecimal totalAmount, OrderStatus orderStatus,
                            String rejectionReason, LocalDateTime shippedAt, LocalDateTime completedAt,
                            LocalDateTime createdAt, List<OrderItemResponse> items, ShipmentResponse shipment) {
        this.orderId = orderId;
        this.subOrderNumber = subOrderNumber;
        this.orderGroupId = orderGroupId;
        this.sellerId = sellerId;
        this.shopName = shopName;
        this.subtotal = subtotal;
        this.shippingFee = shippingFee;
        this.sellerDiscount = sellerDiscount;
        this.totalAmount = totalAmount;
        this.orderStatus = orderStatus;
        this.rejectionReason = rejectionReason;
        this.shippedAt = shippedAt;
        this.completedAt = completedAt;
        this.createdAt = createdAt;
        this.items = items != null ? items : new ArrayList<>();
        this.shipment = shipment;
    }

    public static SubOrderResponse fromEntity(Order order) {
        if (order == null) {
            return null;
        }

        Long orderGroupId = (order.getOrderGroup() != null) ? order.getOrderGroup().getOrderGroupId() : null;
        Long sellerId = (order.getSeller() != null) ? order.getSeller().getSellerId() : null;
        String shopName = (order.getSeller() != null) ? order.getSeller().getShopName() : null;

        List<OrderItemResponse> itemResponses = new ArrayList<>();
        if (order.getOrderItems() != null) {
            itemResponses = order.getOrderItems().stream()
                    .map(OrderItemResponse::fromEntity)
                    .collect(Collectors.toList());
        }

        ShipmentResponse shipmentResponse = (order.getShipment() != null)
                ? ShipmentResponse.fromEntity(order.getShipment())
                : null;

        return new SubOrderResponse(
                order.getOrderId(),
                order.getSubOrderNumber(),
                orderGroupId,
                sellerId,
                shopName,
                order.getSubtotal(),
                order.getShippingFee(),
                order.getSellerDiscount(),
                order.getTotalAmount(),
                order.getOrderStatus(),
                order.getRejectionReason(),
                order.getShippedAt(),
                order.getCompletedAt(),
                order.getCreatedAt(),
                itemResponses,
                shipmentResponse
        );
    }

    public Long getOrderId() {
        return orderId;
    }

    public void setOrderId(Long orderId) {
        this.orderId = orderId;
    }

    public String getSubOrderNumber() {
        return subOrderNumber;
    }

    public void setSubOrderNumber(String subOrderNumber) {
        this.subOrderNumber = subOrderNumber;
    }

    public Long getOrderGroupId() {
        return orderGroupId;
    }

    public void setOrderGroupId(Long orderGroupId) {
        this.orderGroupId = orderGroupId;
    }

    public Long getSellerId() {
        return sellerId;
    }

    public void setSellerId(Long sellerId) {
        this.sellerId = sellerId;
    }

    public String getShopName() {
        return shopName;
    }

    public void setShopName(String shopName) {
        this.shopName = shopName;
    }

    public BigDecimal getSubtotal() {
        return subtotal;
    }

    public void setSubtotal(BigDecimal subtotal) {
        this.subtotal = subtotal;
    }

    public BigDecimal getShippingFee() {
        return shippingFee;
    }

    public void setShippingFee(BigDecimal shippingFee) {
        this.shippingFee = shippingFee;
    }

    public BigDecimal getSellerDiscount() {
        return sellerDiscount;
    }

    public void setSellerDiscount(BigDecimal sellerDiscount) {
        this.sellerDiscount = sellerDiscount;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(BigDecimal totalAmount) {
        this.totalAmount = totalAmount;
    }

    public OrderStatus getOrderStatus() {
        return orderStatus;
    }

    public void setOrderStatus(OrderStatus orderStatus) {
        this.orderStatus = orderStatus;
    }

    public String getRejectionReason() {
        return rejectionReason;
    }

    public void setRejectionReason(String rejectionReason) {
        this.rejectionReason = rejectionReason;
    }

    public LocalDateTime getShippedAt() {
        return shippedAt;
    }

    public void setShippedAt(LocalDateTime shippedAt) {
        this.shippedAt = shippedAt;
    }

    public LocalDateTime getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(LocalDateTime completedAt) {
        this.completedAt = completedAt;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public List<OrderItemResponse> getItems() {
        return items;
    }

    public void setItems(List<OrderItemResponse> items) {
        this.items = items;
    }

    public ShipmentResponse getShipment() {
        return shipment;
    }

    public void setShipment(ShipmentResponse shipment) {
        this.shipment = shipment;
    }
}

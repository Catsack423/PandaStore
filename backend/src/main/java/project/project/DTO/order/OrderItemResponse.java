package project.project.DTO.order;

import lombok.Getter;
import lombok.Setter;
import project.project.Entity.order.OrderItem;

import java.math.BigDecimal;

@Getter
@Setter
public class OrderItemResponse {

    private Long orderItemId;
    private Long productId;
    private String productName;
    private BigDecimal unitPrice;
    private Integer quantity;
    private BigDecimal totalPrice;
    private Boolean isReviewed;

    public OrderItemResponse() {
    }

    public OrderItemResponse(Long orderItemId, Long productId, String productName,
                             BigDecimal unitPrice, Integer quantity, BigDecimal totalPrice, Boolean isReviewed) {
        this.orderItemId = orderItemId;
        this.productId = productId;
        this.productName = productName;
        this.unitPrice = unitPrice;
        this.quantity = quantity;
        this.totalPrice = totalPrice;
        this.isReviewed = isReviewed;
    }

    public static OrderItemResponse fromEntity(OrderItem item) {
        if (item == null) {
            return null;
        }

        Long productId = (item.getProduct() != null) ? item.getProduct().getProductId() : null;

        return new OrderItemResponse(
                item.getOrderItemId(),
                productId,
                item.getProductName(),
                item.getUnitPrice(),
                item.getQuantity(),
                item.getTotalPrice(),
                item.getIsReviewed()
        );
    }

    public Long getOrderItemId() {
        return orderItemId;
    }

    public void setOrderItemId(Long orderItemId) {
        this.orderItemId = orderItemId;
    }

    public Long getProductId() {
        return productId;
    }

    public void setProductId(Long productId) {
        this.productId = productId;
    }

    public String getProductName() {
        return productName;
    }

    public void setProductName(String productName) {
        this.productName = productName;
    }

    public BigDecimal getUnitPrice() {
        return unitPrice;
    }

    public void setUnitPrice(BigDecimal unitPrice) {
        this.unitPrice = unitPrice;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }

    public BigDecimal getTotalPrice() {
        return totalPrice;
    }

    public void setTotalPrice(BigDecimal totalPrice) {
        this.totalPrice = totalPrice;
    }

    public Boolean getIsReviewed() {
        return isReviewed;
    }

    public void setIsReviewed(Boolean isReviewed) {
        this.isReviewed = isReviewed;
    }
}

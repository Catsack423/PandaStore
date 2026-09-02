package project.project.Entity.order;

import jakarta.persistence.*;
import project.project.Entity.seller.Seller;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "orders")
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "order_id")
    private Long orderId;

    @Column(name = "sub_order_number", nullable = false, unique = true, length = 36)
    private String subOrderNumber;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_group_id", nullable = false)
    private OrderGroup orderGroup;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "seller_id", nullable = false)
    private Seller seller;

    @Column(name = "subtotal", nullable = false, precision = 12, scale = 2)
    private BigDecimal subtotal;

    @Column(name = "shipping_fee", nullable = false, precision = 12, scale = 2)
    private BigDecimal shippingFee;

    @Column(name = "seller_discount", precision = 12, scale = 2)
    private BigDecimal sellerDiscount = BigDecimal.ZERO;

    @Column(name = "total_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal totalAmount;

    @Enumerated(EnumType.STRING)
    @Column(name = "order_status", nullable = false, length = 30)
    private OrderStatus orderStatus = OrderStatus.WAITING_SELLER_CONFIRM;

    @Column(name = "rejection_reason", length = 255)
    private String rejectionReason;

    @Column(name = "shipped_at")
    private LocalDateTime shippedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL)
    private List<OrderItem> orderItems = new ArrayList<>();

    @OneToOne(mappedBy = "order", cascade = CascadeType.ALL)
    private Shipment shipment;

    public Order() {
    }

    public Order(Long orderId, String subOrderNumber, OrderGroup orderGroup, Seller seller, BigDecimal subtotal, BigDecimal shippingFee, BigDecimal sellerDiscount, BigDecimal totalAmount, OrderStatus orderStatus, String rejectionReason, LocalDateTime shippedAt, LocalDateTime completedAt, LocalDateTime createdAt) {
        this.orderId = orderId;
        this.subOrderNumber = subOrderNumber;
        this.orderGroup = orderGroup;
        this.seller = seller;
        this.subtotal = subtotal;
        this.shippingFee = shippingFee;
        this.sellerDiscount = sellerDiscount != null ? sellerDiscount : BigDecimal.ZERO;
        this.totalAmount = totalAmount;
        this.orderStatus = orderStatus != null ? orderStatus : OrderStatus.WAITING_SELLER_CONFIRM;
        this.rejectionReason = rejectionReason;
        this.shippedAt = shippedAt;
        this.completedAt = completedAt;
        this.createdAt = createdAt;
    }

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        if (this.sellerDiscount == null) this.sellerDiscount = BigDecimal.ZERO;
        if (this.orderStatus == null) this.orderStatus = OrderStatus.WAITING_SELLER_CONFIRM;
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

    public OrderGroup getOrderGroup() {
        return orderGroup;
    }

    public void setOrderGroup(OrderGroup orderGroup) {
        this.orderGroup = orderGroup;
    }

    public Seller getSeller() {
        return seller;
    }

    public void setSeller(Seller seller) {
        this.seller = seller;
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

    public List<OrderItem> getOrderItems() {
        return orderItems;
    }

    public void setOrderItems(List<OrderItem> orderItems) {
        this.orderItems = orderItems;
    }

    public Shipment getShipment() {
        return shipment;
    }

    public void setShipment(Shipment shipment) {
        this.shipment = shipment;
    }
}

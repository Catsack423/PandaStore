package project.project.Entity.order;

import jakarta.persistence.*;
import project.project.Entity.user.Address;
import project.project.Entity.user.Customer;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "order_groups")
public class OrderGroup {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "order_group_id")
    private Long orderGroupId;

    @Column(name = "group_number", nullable = false, unique = true, length = 36)
    private String groupNumber;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "shipping_address_id", nullable = false)
    private Address shippingAddress;

    @Column(name = "total_products_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal totalProductsAmount;

    @Column(name = "total_shipping_fee", nullable = false, precision = 12, scale = 2)
    private BigDecimal totalShippingFee;

    @Column(name = "total_discount", precision = 12, scale = 2)
    private BigDecimal totalDiscount = BigDecimal.ZERO;

    @Column(name = "grand_total", nullable = false, precision = 12, scale = 2)
    private BigDecimal grandTotal;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_status", nullable = false, length = 25)
    private OrderGroupPaymentStatus paymentStatus = OrderGroupPaymentStatus.PENDING;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @OneToMany(mappedBy = "orderGroup", cascade = CascadeType.ALL)
    private List<Order> subOrders = new ArrayList<>();

    @OneToOne(mappedBy = "orderGroup", cascade = CascadeType.ALL)
    private Payment payment;

    public OrderGroup() {
    }

    public OrderGroup(Long orderGroupId, String groupNumber, Customer customer, Address shippingAddress, BigDecimal totalProductsAmount, BigDecimal totalShippingFee, BigDecimal totalDiscount, BigDecimal grandTotal, OrderGroupPaymentStatus paymentStatus, LocalDateTime createdAt) {
        this.orderGroupId = orderGroupId;
        this.groupNumber = groupNumber;
        this.customer = customer;
        this.shippingAddress = shippingAddress;
        this.totalProductsAmount = totalProductsAmount;
        this.totalShippingFee = totalShippingFee;
        this.totalDiscount = totalDiscount != null ? totalDiscount : BigDecimal.ZERO;
        this.grandTotal = grandTotal;
        this.paymentStatus = paymentStatus != null ? paymentStatus : OrderGroupPaymentStatus.PENDING;
        this.createdAt = createdAt;
    }

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        if (this.totalDiscount == null) this.totalDiscount = BigDecimal.ZERO;
        if (this.paymentStatus == null) this.paymentStatus = OrderGroupPaymentStatus.PENDING;
    }

    public Long getOrderGroupId() {
        return orderGroupId;
    }

    public void setOrderGroupId(Long orderGroupId) {
        this.orderGroupId = orderGroupId;
    }

    public String getGroupNumber() {
        return groupNumber;
    }

    public void setGroupNumber(String groupNumber) {
        this.groupNumber = groupNumber;
    }

    public Customer getCustomer() {
        return customer;
    }

    public void setCustomer(Customer customer) {
        this.customer = customer;
    }

    public Address getShippingAddress() {
        return shippingAddress;
    }

    public void setShippingAddress(Address shippingAddress) {
        this.shippingAddress = shippingAddress;
    }

    public BigDecimal getTotalProductsAmount() {
        return totalProductsAmount;
    }

    public void setTotalProductsAmount(BigDecimal totalProductsAmount) {
        this.totalProductsAmount = totalProductsAmount;
    }

    public BigDecimal getTotalShippingFee() {
        return totalShippingFee;
    }

    public void setTotalShippingFee(BigDecimal totalShippingFee) {
        this.totalShippingFee = totalShippingFee;
    }

    public BigDecimal getTotalDiscount() {
        return totalDiscount;
    }

    public void setTotalDiscount(BigDecimal totalDiscount) {
        this.totalDiscount = totalDiscount;
    }

    public BigDecimal getGrandTotal() {
        return grandTotal;
    }

    public void setGrandTotal(BigDecimal grandTotal) {
        this.grandTotal = grandTotal;
    }

    public OrderGroupPaymentStatus getPaymentStatus() {
        return paymentStatus;
    }

    public void setPaymentStatus(OrderGroupPaymentStatus paymentStatus) {
        this.paymentStatus = paymentStatus;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public List<Order> getSubOrders() {
        return subOrders;
    }

    public void setSubOrders(List<Order> subOrders) {
        this.subOrders = subOrders;
    }

    public Payment getPayment() {
        return payment;
    }

    public void setPayment(Payment payment) {
        this.payment = payment;
    }
}

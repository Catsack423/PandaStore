package project.project.Service.implement;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityNotFoundException;
import jakarta.persistence.LockModeType;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import project.project.Entity.order.Cart;
import project.project.Entity.order.CartItem;
import project.project.Entity.order.Order;
import project.project.Entity.order.OrderGroup;
import project.project.Entity.order.OrderGroupPaymentStatus;
import project.project.Entity.order.OrderItem;
import project.project.Entity.order.OrderStatus;
import project.project.Entity.order.Payment;
import project.project.Entity.order.PaymentMethod;
import project.project.Entity.order.PaymentStatus;
import project.project.Entity.product.Product;
import project.project.Entity.product.ProductStatus;
import project.project.Entity.seller.SellerStatus;
import project.project.Entity.user.Address;
import project.project.Entity.user.Customer;
import project.project.Service.api.NotificationService;
import project.project.Service.api.OrderOrchestrationService;
import project.project.Service.api.ShippingService;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import java.util.UUID;

@Service
@Transactional
public class OrderOrchestrationServiceImp
        implements OrderOrchestrationService {

    private final EntityManager entityManager;
    private final ShippingService shippingService;
    private final NotificationService notificationService;

    public OrderOrchestrationServiceImp(
            EntityManager entityManager,
            ShippingService shippingService,
            NotificationService notificationService) {

        this.entityManager = entityManager;
        this.shippingService = shippingService;
        this.notificationService = notificationService;
    }

    @Override
    public OrderGroup createOrderGroupFromCart(
            Long customerId,
            Long shippingAddressId,
            Map<Long, String> sellerShippingMethods,
            PaymentMethod paymentMethod) {

        Customer customer = find(
                Customer.class, customerId, "customerId");

        Address address = find(
                Address.class, shippingAddressId, "shippingAddressId");

        if (!Objects.equals(
                address.getCustomer().getCustomerId(), customerId)) {
            throw new IllegalArgumentException(
                    "Shipping address does not belong to customer");
        }

        if (paymentMethod == null) {
            throw new IllegalArgumentException(
                    "Payment method is required");
        }

        if (sellerShippingMethods == null
                || sellerShippingMethods.isEmpty()) {
            throw new IllegalArgumentException(
                    "Shipping methods are required");
        }

        // ล็อกตะกร้าเพื่อไม่ให้ checkout ตะกร้าเดียวกันพร้อมกัน
        Cart cart = entityManager.createQuery(
                """
                        select c from Cart c
                        where c.customer.customerId = :customerId
                        """,
                Cart.class)
                .setParameter("customerId", customerId)
                .setLockMode(LockModeType.PESSIMISTIC_WRITE)
                .getResultStream()
                .findFirst()
                .orElseThrow(() -> new EntityNotFoundException(
                        "Cart not found for customer: " + customerId));

        List<CartItem> selected = cart.getItems().stream()
                .filter(item -> Boolean.TRUE.equals(item.getIsSelected()))
                .toList();

        if (selected.isEmpty()) {
            throw new IllegalArgumentException(
                    "Cart has no selected items");
        }

        // รวมจำนวนต่อสินค้า และเรียง ID ให้ล็อกในลำดับเดียวกัน
        Map<Long, Integer> quantities = new TreeMap<>();

        for (CartItem item : selected) {
            if (item.getProduct() == null
                    || item.getProduct().getProductId() == null
                    || item.getQuantity() == null
                    || item.getQuantity() <= 0) {
                throw new IllegalArgumentException(
                        "Cart contains an invalid item");
            }

            quantities.merge(
                    item.getProduct().getProductId(),
                    item.getQuantity(),
                    (a, b) -> Math.addExact(a, b));
        }

        OrderGroup group = new OrderGroup();
        group.setGroupNumber(UUID.randomUUID().toString());
        group.setCustomer(customer);
        group.setShippingAddress(address);
        group.setPaymentStatus(OrderGroupPaymentStatus.PENDING);
        group.setTotalDiscount(BigDecimal.ZERO);

        Map<Long, Order> ordersBySeller = new TreeMap<>();

        for (Map.Entry<Long, Integer> entry : quantities.entrySet()) {
            Product product = lockProduct(entry.getKey());
            int quantity = entry.getValue();

            if (product.getStatus() != ProductStatus.ACTIVE
                    || product.getSeller().getStatus() != SellerStatus.ACTIVE) {
                throw new IllegalStateException(
                        "Product or seller is not active: "
                                + product.getProductId());
            }

            BigDecimal unitPrice = money(
                    product.getPrice(), "Product price");

            if (unitPrice.signum() <= 0) {
                throw new IllegalArgumentException(
                        "Product price must be greater than zero");
            }

            if (product.getStock() == null
                    || product.getStock() < quantity) {
                throw new IllegalStateException(
                        "Insufficient stock for product: "
                                + product.getProductId());
            }

            Long sellerId = product.getSeller().getSellerId();
            Order order = ordersBySeller.get(sellerId);

            if (order == null) {
                String method = sellerShippingMethods.get(sellerId);
                requireText(method, "Shipping method for seller " + sellerId);

                BigDecimal shippingFee = money(
                        shippingService.calculateShippingFee(
                                sellerId,
                                method.trim(),
                                shippingAddressId),
                        "Shipping fee");

                order = new Order();
                order.setSubOrderNumber(UUID.randomUUID().toString());
                order.setOrderGroup(group);
                order.setSeller(product.getSeller());
                order.setOrderStatus(OrderStatus.PENDING_PAYMENT);
                order.setSubtotal(BigDecimal.ZERO);
                order.setShippingFee(shippingFee);
                order.setSellerDiscount(BigDecimal.ZERO);

                ordersBySeller.put(sellerId, order);
                group.getSubOrders().add(order);
            }

            BigDecimal lineTotal = money(
                    unitPrice.multiply(BigDecimal.valueOf(quantity)),
                    "Line total");

            OrderItem orderItem = new OrderItem();
            orderItem.setOrder(order);
            orderItem.setProduct(product);
            orderItem.setProductName(product.getName());
            orderItem.setUnitPrice(unitPrice);
            orderItem.setQuantity(quantity);
            orderItem.setTotalPrice(lineTotal);
            orderItem.setIsReviewed(false);

            order.getOrderItems().add(orderItem);
            order.setSubtotal(order.getSubtotal().add(lineTotal));

            // จองสต็อกตอน checkout ภายใน transaction เดียวกัน
            product.setStock(product.getStock() - quantity);
        }

        BigDecimal productsTotal = BigDecimal.ZERO;
        BigDecimal shippingTotal = BigDecimal.ZERO;

        for (Order order : group.getSubOrders()) {
            order.setTotalAmount(money(
                    order.getSubtotal().add(order.getShippingFee()),
                    "Order total"));

            productsTotal = productsTotal.add(order.getSubtotal());
            shippingTotal = shippingTotal.add(order.getShippingFee());
        }

        group.setTotalProductsAmount(
                money(productsTotal, "Products total"));
        group.setTotalShippingFee(
                money(shippingTotal, "Shipping total"));
        group.setGrandTotal(money(
                productsTotal.add(shippingTotal), "Grand total"));

        Payment payment = new Payment();
        payment.setOrderGroup(group);
        payment.setPaymentMethod(paymentMethod);
        payment.setAmount(group.getGrandTotal());
        payment.setRefundedAmount(BigDecimal.ZERO);
        payment.setStatus(PaymentStatus.PENDING);

        group.setPayment(payment);

        // CascadeType.ALL บันทึก sub-orders, items และ payment
        entityManager.persist(group);

        // ลบเฉพาะรายการที่ใช้ checkout ผ่าน orphanRemoval ของ Cart
        cart.getItems().removeAll(selected);

        entityManager.flush();
        return group;
    }

    /**
     * ผู้เรียกต้องตรวจสอบ callback กับ payment gateway ก่อน
     * รวมถึง transaction, order และยอดเงิน
     */
    @Override
    public void handlePaymentSuccess(
            Long orderGroupId,
            String gatewayTransactionId) {

        requireText(gatewayTransactionId, "Gateway transaction ID");

        String transactionId = gatewayTransactionId.trim();
        if (transactionId.length() > 100) {
            throw new IllegalArgumentException(
                    "Gateway transaction ID must not exceed 100 characters");
        }

        OrderGroup group = lockGroup(orderGroupId);
        Payment payment = lockPayment(group);

        if (payment.getStatus() == PaymentStatus.SUCCESS) {
            if (!Objects.equals(
                    payment.getGatewayTransactionId(), transactionId)) {
                throw new IllegalStateException(
                        "Payment succeeded with another transaction ID");
            }

            // callback เดิมซ้ำ: ไม่เปลี่ยนข้อมูลหรือแจ้งเตือนซ้ำ
            return;
        }

        requirePending(group, payment);

        for (Order order : group.getSubOrders()) {
            if (order.getOrderStatus() != OrderStatus.PENDING_PAYMENT) {
                throw new IllegalStateException(
                        "Sub-order is not pending payment: "
                                + order.getOrderId());
            }
        }

        payment.setStatus(PaymentStatus.SUCCESS);
        payment.setGatewayTransactionId(transactionId);
        payment.setPaidAt(LocalDateTime.now());
        group.setPaymentStatus(OrderGroupPaymentStatus.PAID);

        for (Order order : group.getSubOrders()) {
            order.setOrderStatus(OrderStatus.WAITING_SELLER_CONFIRM);
        }

        // ไม่ตัดสต็อกซ้ำ เพราะจองไว้ตอน checkout แล้ว
        notificationService.notifyCustomerOrderPaid(
                group.getCustomer().getCustomerId(),
                group.getOrderGroupId());

        for (Order order : group.getSubOrders()) {
            notificationService.notifySellerNewOrder(
                    order.getSeller().getSellerId(),
                    order.getOrderId());
        }
    }

    @Override
    public void handlePaymentFailure(
            Long orderGroupId,
            String failureReason) {

        requireText(failureReason, "Failure reason");

        String reason = failureReason.trim();
        if (reason.length() > 255) {
            throw new IllegalArgumentException(
                    "Failure reason must not exceed 255 characters");
        }

        OrderGroup group = lockGroup(orderGroupId);
        Payment payment = lockPayment(group);

        if (payment.getStatus() == PaymentStatus.FAILED) {
            // callback ซ้ำ: ห้ามคืนสต็อกอีกครั้ง
            return;
        }

        // ห้ามเปลี่ยน SUCCESS / REFUNDED กลับเป็น FAILED
        requirePending(group, payment);

        Map<Long, Integer> quantities = new TreeMap<>();

        for (Order order : group.getSubOrders()) {
            if (order.getOrderStatus() != OrderStatus.PENDING_PAYMENT) {
                throw new IllegalStateException(
                        "Sub-order is not pending payment: "
                                + order.getOrderId());
            }

            for (OrderItem item : order.getOrderItems()) {
                quantities.merge(
                        item.getProduct().getProductId(),
                        item.getQuantity(),
                        (a, b) -> Math.addExact(a, b));
            }
        }

        // คืนสต็อกที่จองไว้ โดยใช้ลำดับล็อกเดียวกับ checkout
        for (Map.Entry<Long, Integer> entry : quantities.entrySet()) {
            Product product = lockProduct(entry.getKey());

            if (product.getStock() == null) {
                throw new IllegalStateException(
                        "Product stock is missing: " + entry.getKey());
            }

            product.setStock(Math.addExact(
                    product.getStock(), entry.getValue()));
        }

        for (Order order : group.getSubOrders()) {
            order.setOrderStatus(OrderStatus.CANCELLED);

            // Entity ปัจจุบันยังไม่มี Payment.failureReason
            order.setRejectionReason(reason);
        }

        payment.setStatus(PaymentStatus.FAILED);
        group.setPaymentStatus(OrderGroupPaymentStatus.FAILED);
    }

    @Override
    @Transactional(readOnly = true)
    public OrderGroup getOrderGroupDetails(Long orderGroupId) {
        OrderGroup group = find(
                OrderGroup.class, orderGroupId, "orderGroupId");

        // โหลดรายละเอียดหลักภายใน transaction
        group.getCustomer().getFullName();
        group.getShippingAddress().getAddressLine();

        if (group.getPayment() != null) {
            group.getPayment().getStatus();
        }

        for (Order order : group.getSubOrders()) {
            order.getSeller().getShopName();
            order.getOrderItems().size();
        }

        return group;
    }

    private OrderGroup lockGroup(Long orderGroupId) {
        requireId(orderGroupId, "orderGroupId");

        OrderGroup group = entityManager.find(
                OrderGroup.class,
                orderGroupId,
                LockModeType.PESSIMISTIC_WRITE);

        if (group == null) {
            throw new EntityNotFoundException(
                    "Order group not found: " + orderGroupId);
        }

        entityManager.refresh(group, LockModeType.PESSIMISTIC_WRITE);
        return group;
    }

    private Payment lockPayment(OrderGroup group) {
        Payment payment = group.getPayment();

        if (payment == null) {
            throw new IllegalStateException(
                    "Order group has no payment");
        }

        entityManager.refresh(payment, LockModeType.PESSIMISTIC_WRITE);
        return payment;
    }

    private Product lockProduct(Long productId) {
        requireId(productId, "productId");

        Product product = entityManager.find(
                Product.class,
                productId,
                LockModeType.PESSIMISTIC_WRITE);

        if (product == null) {
            throw new EntityNotFoundException(
                    "Product not found: " + productId);
        }

        // Product อาจถูกโหลดผ่าน Cart มาก่อน ต้องอ่านสต็อกล่าสุด
        entityManager.refresh(product, LockModeType.PESSIMISTIC_WRITE);
        return product;
    }

    private void requirePending(OrderGroup group, Payment payment) {
        if (group.getPaymentStatus() != OrderGroupPaymentStatus.PENDING
                || payment.getStatus() != PaymentStatus.PENDING) {
            throw new IllegalStateException(
                    "Payment is no longer pending");
        }
    }

    private <T> T find(Class<T> type, Long id, String name) {
        requireId(id, name);

        T entity = entityManager.find(type, id);
        if (entity == null) {
            throw new EntityNotFoundException(
                    type.getSimpleName() + " not found: " + id);
        }

        return entity;
    }

    private BigDecimal money(BigDecimal value, String name) {
        if (value == null || value.signum() < 0) {
            throw new IllegalArgumentException(
                    name + " must not be null or negative");
        }

        BigDecimal result;
        try {
            result = value.setScale(2, RoundingMode.UNNECESSARY);
        } catch (ArithmeticException exception) {
            throw new IllegalArgumentException(
                    name + " must have at most two decimal places",
                    exception);
        }

        if (result.precision() > 12) {
            throw new IllegalArgumentException(
                    name + " exceeds the supported amount");
        }

        return result;
    }

    private void requireId(Long id, String name) {
        if (id == null || id <= 0) {
            throw new IllegalArgumentException(
                    name + " must be a positive value");
        }
    }

    private void requireText(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(
                    name + " is required");
        }
    }
}
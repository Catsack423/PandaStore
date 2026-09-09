package project.project.Service.implement;

import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;
import project.project.Entity.order.*;
import project.project.Entity.product.Product;
import project.project.Entity.user.Address;
import project.project.Repository.AddressRepository;
import project.project.Repository.CartRepository;
import project.project.Repository.OrderGroupRepository;
import project.project.Service.api.InventoryService;
import project.project.Service.api.NotificationService;
import project.project.Service.api.OrderDraftFactory;
import project.project.Service.api.OrderOrchestrationService;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
@Transactional(readOnly = true)
public class OrderOrchestrationServiceImp
        implements OrderOrchestrationService {

    private final OrderGroupRepository orderGroupRepository;
    private final CartRepository cartRepository;
    private final AddressRepository addressRepository;

    private final InventoryService inventoryService;
    private final OrderDraftFactory orderDraftFactory;
    private final NotificationService notificationService;

    public OrderOrchestrationServiceImp(
            OrderGroupRepository orderGroupRepository,
            CartRepository cartRepository,
            AddressRepository addressRepository,
            InventoryService inventoryService,
            OrderDraftFactory orderDraftFactory,
            NotificationService notificationService) {
        this.orderGroupRepository = orderGroupRepository;
        this.cartRepository = cartRepository;
        this.addressRepository = addressRepository;
        this.inventoryService = inventoryService;
        this.orderDraftFactory = orderDraftFactory;
        this.notificationService = notificationService;
    }

    @Override
    @Transactional
    public OrderGroup createOrderGroupFromCart(
            Long customerId,
            Long shippingAddressId,
            Map<Long, String> sellerShippingMethods,
            PaymentMethod paymentMethod) {
        Assert.notNull(customerId, "Customer ID is required");
        Assert.notNull(shippingAddressId, "Shipping address ID is required");
        Assert.notNull(sellerShippingMethods, "Shipping methods are required");
        Assert.notNull(paymentMethod, "Payment method is required");

        Cart cart = cartRepository.findByCustomerIdForUpdate(customerId)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Cart not found for customer: " + customerId));

        Address address = addressRepository.findById(shippingAddressId)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Address not found: " + shippingAddressId));

        if (!Objects.equals(
                address.getCustomer().getCustomerId(),
                customerId)) {
            throw new IllegalArgumentException(
                    "Shipping address does not belong to customer");
        }

        List<CartItem> selectedItems = cart.getItems().stream()
                .filter(item -> Boolean.TRUE.equals(item.getIsSelected()))
                .toList();

        if (selectedItems.isEmpty()) {
            throw new IllegalStateException("No selected cart items");
        }

        Map<Long, Integer> quantities = new HashMap<>();

        for (CartItem item : selectedItems) {
            if (item.getQuantity() == null || item.getQuantity() <= 0) {
                throw new IllegalArgumentException(
                        "Invalid quantity for cart item: " + item.getCartItemId());
            }

            quantities.merge(
                    item.getProduct().getProductId(),
                    item.getQuantity(),
                    Math::addExact);
        }

        Map<Long, Product> products = inventoryService.reserve(quantities);

        OrderGroup group = orderDraftFactory.create(
                cart.getCustomer(),
                address,
                products,
                quantities,
                sellerShippingMethods,
                paymentMethod);

        // Cascade บันทึก subOrders, orderItems และ payment
        OrderGroup savedGroup = orderGroupRepository.save(group);

        // Cart.items มี orphanRemoval = true
        cart.getItems().removeAll(selectedItems);

        return savedGroup;
    }

    @Override
    @Transactional
    public void handlePaymentSuccess(
            Long orderGroupId,
            String gatewayTransactionId) {
        Assert.hasText(gatewayTransactionId, "Gateway transaction ID is required");
        Assert.isTrue(
                gatewayTransactionId.length() <= 100,
                "Gateway transaction ID exceeds 100 characters");

        OrderGroup group = lockOrderGroup(orderGroupId);
        Payment payment = requirePayment(group);

        if (hasSuccessfulPayment(group)) {
            // Callback ซ้ำต้องอ้างถึงธุรกรรมเดิม
            if (!Objects.equals(
                    payment.getGatewayTransactionId(),
                    gatewayTransactionId)) {
                throw new IllegalStateException(
                        "Order group already has another successful payment");
            }
            return;
        }

        if (group.getPaymentStatus() != OrderGroupPaymentStatus.PENDING) {
            throw new IllegalStateException(
                    "Cannot confirm payment from status: "
                            + group.getPaymentStatus());
        }

        requirePendingPayment(payment);
        requirePendingSubOrders(group);

        payment.setStatus(PaymentStatus.SUCCESS);
        payment.setGatewayTransactionId(gatewayTransactionId);
        payment.setPaidAt(LocalDateTime.now());

        group.setPaymentStatus(OrderGroupPaymentStatus.PAID);

        for (Order order : group.getSubOrders()) {
            order.setOrderStatus(OrderStatus.WAITING_SELLER_CONFIRM);

            notificationService.notifySellerNewOrder(
                    order.getSeller().getSellerId(),
                    order.getOrderId());
        }

        notificationService.notifyCustomerOrderPaid(
                group.getCustomer().getCustomerId(),
                group.getOrderGroupId());
    }

    @Override
    @Transactional
    public void handlePaymentFailure(
            Long orderGroupId,
            String failureReason) {
        OrderGroup group = lockOrderGroup(orderGroupId);

        // ไม่คืนสต็อกซ้ำ และไม่ให้ failure ที่มาช้าเปลี่ยนสถานะที่จ่ายแล้ว
        if (group.getPaymentStatus() == OrderGroupPaymentStatus.FAILED
                || hasSuccessfulPayment(group)) {
            return;
        }

        if (group.getPaymentStatus() != OrderGroupPaymentStatus.PENDING) {
            throw new IllegalStateException(
                    "Cannot fail payment from status: "
                            + group.getPaymentStatus());
        }

        Assert.hasText(failureReason, "Failure reason is required");
        Assert.isTrue(
                failureReason.length() <= 255,
                "Failure reason exceeds 255 characters");

        Payment payment = requirePayment(group);
        requirePendingPayment(payment);
        requirePendingSubOrders(group);

        Map<Long, Integer> quantities = new HashMap<>();

        for (Order order : group.getSubOrders()) {
            for (OrderItem item : order.getOrderItems()) {
                quantities.merge(
                        item.getProduct().getProductId(),
                        item.getQuantity(),
                        Math::addExact);
            }
        }

        inventoryService.release(quantities);

        payment.setStatus(PaymentStatus.FAILED);
        group.setPaymentStatus(OrderGroupPaymentStatus.FAILED);

        for (Order order : group.getSubOrders()) {
            order.setOrderStatus(OrderStatus.CANCELLED);
            order.setRejectionReason(failureReason);
        }
    }

    @Override
    public OrderGroup getOrderGroupDetails(Long orderGroupId) {
        Assert.notNull(orderGroupId, "Order group ID is required");

        OrderGroup group = orderGroupRepository
                .findDetailsById(orderGroupId)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Order group not found: " + orderGroupId));

        // โหลด collection อีกระดับใน transaction
        // ไม่ fetch สอง List พร้อมกันใน EntityGraph เดียว
        for (Order order : group.getSubOrders()) {
            order.getOrderItems().size();
        }

        return group;
    }

    private OrderGroup lockOrderGroup(Long orderGroupId) {
        Assert.notNull(orderGroupId, "Order group ID is required");

        return orderGroupRepository.findByIdForUpdate(orderGroupId)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Order group not found: " + orderGroupId));
    }

    private Payment requirePayment(OrderGroup group) {
        if (group.getPayment() == null) {
            throw new IllegalStateException(
                    "Payment not found for order group: "
                            + group.getOrderGroupId());
        }

        return group.getPayment();
    }

    private void requirePendingPayment(Payment payment) {
        if (payment.getStatus() != PaymentStatus.PENDING) {
            throw new IllegalStateException(
                    "Payment is not pending: " + payment.getStatus());
        }
    }

    private void requirePendingSubOrders(OrderGroup group) {
        if (group.getSubOrders().isEmpty()) {
            throw new IllegalStateException("Order group has no sub-orders");
        }

        for (Order order : group.getSubOrders()) {
            if (order.getOrderStatus() != OrderStatus.PENDING_PAYMENT) {
                throw new IllegalStateException(
                        "Sub-order is not pending payment: " + order.getOrderId());
            }
        }
    }

    private boolean hasSuccessfulPayment(OrderGroup group) {
        return switch (group.getPaymentStatus()) {
            case PAID, PARTIALLY_REFUNDED, REFUNDED -> true;
            default -> false;
        };
    }
}
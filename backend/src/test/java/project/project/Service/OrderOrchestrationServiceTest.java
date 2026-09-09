package project.project.Service;

import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import jakarta.persistence.TypedQuery;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

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
import project.project.Entity.seller.Seller;
import project.project.Entity.seller.SellerStatus;
import project.project.Entity.user.Address;
import project.project.Entity.user.Customer;
import project.project.Service.api.NotificationService;
import project.project.Service.api.ShippingService;
import project.project.Service.implement.OrderOrchestrationServiceImp;

import java.math.BigDecimal;
import java.util.Map;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderOrchestrationServiceTest {

    @Mock
    private EntityManager entityManager;

    @Mock
    private ShippingService shippingService;

    @Mock
    private NotificationService notificationService;

    @Mock
    private TypedQuery<Cart> cartQuery;

    private OrderOrchestrationServiceImp service;

    private Customer customer;
    private Address address;
    private Seller seller;
    private Product product;
    private OrderGroup group;
    private Order order;
    private Payment payment;

    @BeforeEach
    void setUp() {
        service = new OrderOrchestrationServiceImp(
                entityManager,
                shippingService,
                notificationService);

        customer = new Customer();
        customer.setCustomerId(1L);

        address = new Address();
        address.setAddressId(2L);
        address.setCustomer(customer);

        seller = new Seller();
        seller.setSellerId(3L);
        seller.setStatus(SellerStatus.ACTIVE);

        product = new Product();
        product.setProductId(4L);
        product.setSeller(seller);
        product.setName("Panda Shirt");
        product.setPrice(new BigDecimal("100.00"));
        product.setStock(5);
        product.setStatus(ProductStatus.ACTIVE);

        group = new OrderGroup();
        group.setOrderGroupId(10L);
        group.setCustomer(customer);
        group.setPaymentStatus(OrderGroupPaymentStatus.PENDING);

        order = new Order();
        order.setOrderId(20L);
        order.setOrderGroup(group);
        order.setSeller(seller);
        order.setOrderStatus(OrderStatus.PENDING_PAYMENT);

        OrderItem orderItem = new OrderItem();
        orderItem.setOrder(order);
        orderItem.setProduct(product);
        orderItem.setQuantity(2);

        order.getOrderItems().add(orderItem);
        group.getSubOrders().add(order);

        payment = new Payment();
        payment.setOrderGroup(group);
        payment.setStatus(PaymentStatus.PENDING);
        group.setPayment(payment);
    }

    @Test
    void checkoutShouldCalculateTotalReserveStockAndRemoveSelectedItems() {
        Cart cart = new Cart();
        cart.setCustomer(customer);

        CartItem selected = cartItem(cart, 2, true);
        CartItem unselected = cartItem(cart, 1, false);

        cart.getItems().add(selected);
        cart.getItems().add(unselected);

        mockCheckout(cart);
        mockProductLock();

        when(shippingService.calculateShippingFee(
                3L, "STANDARD", 2L))
                .thenReturn(new BigDecimal("40.00"));

        OrderGroup result = service.createOrderGroupFromCart(
                1L,
                2L,
                Map.of(3L, "STANDARD"),
                PaymentMethod.values()[0]);

        assertMoney("200.00", result.getTotalProductsAmount());
        assertMoney("40.00", result.getTotalShippingFee());
        assertMoney("240.00", result.getGrandTotal());
        assertMoney("240.00", result.getPayment().getAmount());

        assertEquals(
                OrderGroupPaymentStatus.PENDING,
                result.getPaymentStatus());

        assertEquals(
                PaymentStatus.PENDING,
                result.getPayment().getStatus());

        assertEquals(1, result.getSubOrders().size());

        Order createdOrder = result.getSubOrders().get(0);

        assertEquals(
                OrderStatus.PENDING_PAYMENT,
                createdOrder.getOrderStatus());

        assertMoney("240.00", createdOrder.getTotalAmount());
        assertEquals(2, createdOrder.getOrderItems().get(0).getQuantity());

        assertEquals(3, product.getStock());

        assertEquals(1, cart.getItems().size());
        assertSame(unselected, cart.getItems().get(0));

        verify(entityManager).persist(result);
        verify(entityManager).flush();
        verifyNoInteractions(notificationService);
    }

    @Test
    void checkoutShouldRejectInsufficientStock() {
        Cart cart = new Cart();
        cart.setCustomer(customer);
        cart.getItems().add(cartItem(cart, 6, true));

        mockCheckout(cart);
        mockProductLock();

        assertThrows(
                IllegalStateException.class,
                () -> service.createOrderGroupFromCart(
                        1L,
                        2L,
                        Map.of(3L, "STANDARD"),
                        PaymentMethod.values()[0]));

        assertEquals(5, product.getStock());
        assertEquals(1, cart.getItems().size());

        verify(entityManager, never()).persist(any());
        verifyNoInteractions(shippingService, notificationService);
    }

    @Test
    void checkoutShouldRejectAnotherCustomersAddress() {
        Customer anotherCustomer = new Customer();
        anotherCustomer.setCustomerId(99L);
        address.setCustomer(anotherCustomer);

        when(entityManager.find(Customer.class, 1L))
                .thenReturn(customer);

        when(entityManager.find(Address.class, 2L))
                .thenReturn(address);

        assertThrows(
                IllegalArgumentException.class,
                () -> service.createOrderGroupFromCart(
                        1L,
                        2L,
                        Map.of(3L, "STANDARD"),
                        PaymentMethod.values()[0]));

        verify(entityManager, never()).persist(any());
        verifyNoInteractions(shippingService, notificationService);
    }

    @Test
    void paymentSuccessShouldUpdateStatusWithoutDeductingStockAgain() {
        // Checkout จองสินค้าไปแล้ว 2 ชิ้น
        product.setStock(3);
        mockGroupLock();

        service.handlePaymentSuccess(10L, "TX-001");

        assertEquals(PaymentStatus.SUCCESS, payment.getStatus());
        assertEquals("TX-001", payment.getGatewayTransactionId());
        assertNotNull(payment.getPaidAt());

        assertEquals(
                OrderGroupPaymentStatus.PAID,
                group.getPaymentStatus());

        assertEquals(
                OrderStatus.WAITING_SELLER_CONFIRM,
                order.getOrderStatus());

        assertEquals(3, product.getStock());

        verify(notificationService)
                .notifyCustomerOrderPaid(1L, 10L);

        verify(notificationService)
                .notifySellerNewOrder(3L, 20L);
    }

    @Test
    void duplicateSuccessShouldNotNotifyTwice() {
        product.setStock(3);
        mockGroupLock();

        service.handlePaymentSuccess(10L, "TX-001");
        service.handlePaymentSuccess(10L, "TX-001");

        assertEquals(3, product.getStock());
        assertEquals(PaymentStatus.SUCCESS, payment.getStatus());

        verify(notificationService, times(1))
                .notifyCustomerOrderPaid(1L, 10L);

        verify(notificationService, times(1))
                .notifySellerNewOrder(3L, 20L);
    }

    @Test
    void successWithDifferentTransactionShouldBeRejected() {
        mockGroupLock();

        service.handlePaymentSuccess(10L, "TX-001");

        assertThrows(
                IllegalStateException.class,
                () -> service.handlePaymentSuccess(10L, "TX-002"));

        assertEquals("TX-001", payment.getGatewayTransactionId());
        assertEquals(PaymentStatus.SUCCESS, payment.getStatus());

        verify(notificationService, times(1))
                .notifyCustomerOrderPaid(1L, 10L);
    }

    @Test
    void paymentFailureShouldRestoreStockAndCancelOrder() {
        product.setStock(3);

        mockGroupLock();
        mockProductLock();

        service.handlePaymentFailure(10L, "Payment declined");

        assertEquals(5, product.getStock());
        assertEquals(PaymentStatus.FAILED, payment.getStatus());

        assertEquals(
                OrderGroupPaymentStatus.FAILED,
                group.getPaymentStatus());

        assertEquals(OrderStatus.CANCELLED, order.getOrderStatus());
        assertEquals("Payment declined", order.getRejectionReason());

        verifyNoInteractions(notificationService);
    }

    @Test
    void duplicateFailureShouldNotRestoreStockTwice() {
        product.setStock(3);

        mockGroupLock();
        mockProductLock();

        service.handlePaymentFailure(10L, "Payment declined");
        service.handlePaymentFailure(10L, "Payment declined");

        assertEquals(5, product.getStock());
        assertEquals(PaymentStatus.FAILED, payment.getStatus());
        assertEquals(OrderStatus.CANCELLED, order.getOrderStatus());
    }

    @Test
    void failureAfterSuccessShouldNotChangePaidOrder() {
        product.setStock(3);
        mockGroupLock();

        service.handlePaymentSuccess(10L, "TX-001");

        assertThrows(
                IllegalStateException.class,
                () -> service.handlePaymentFailure(
                        10L, "Delayed failure callback"));

        assertEquals(PaymentStatus.SUCCESS, payment.getStatus());
        assertEquals(
                OrderGroupPaymentStatus.PAID,
                group.getPaymentStatus());

        assertEquals(
                OrderStatus.WAITING_SELLER_CONFIRM,
                order.getOrderStatus());

        assertEquals(3, product.getStock());
    }

    @Test
    void successAfterFailureShouldNotReopenCancelledOrder() {
        product.setStock(3);

        mockGroupLock();
        mockProductLock();

        service.handlePaymentFailure(10L, "Payment declined");

        assertThrows(
                IllegalStateException.class,
                () -> service.handlePaymentSuccess(10L, "TX-001"));

        assertEquals(PaymentStatus.FAILED, payment.getStatus());
        assertEquals(OrderStatus.CANCELLED, order.getOrderStatus());
        assertEquals(5, product.getStock());

        verifyNoInteractions(notificationService);
    }

    private CartItem cartItem(
            Cart cart,
            int quantity,
            boolean selected) {

        CartItem item = new CartItem();
        item.setCart(cart);
        item.setProduct(product);
        item.setQuantity(quantity);
        item.setIsSelected(selected);
        return item;
    }

    private void mockCheckout(Cart cart) {
        when(entityManager.find(Customer.class, 1L))
                .thenReturn(customer);

        when(entityManager.find(Address.class, 2L))
                .thenReturn(address);

        when(entityManager.createQuery(anyString(), eq(Cart.class)))
                .thenReturn(cartQuery);

        when(cartQuery.setParameter("customerId", 1L))
                .thenReturn(cartQuery);

        when(cartQuery.setLockMode(LockModeType.PESSIMISTIC_WRITE))
                .thenReturn(cartQuery);

        when(cartQuery.getResultStream())
                .thenAnswer(invocation -> Stream.of(cart));
    }

    private void mockGroupLock() {
        when(entityManager.find(
                OrderGroup.class,
                10L,
                LockModeType.PESSIMISTIC_WRITE))
                .thenReturn(group);
    }

    private void mockProductLock() {
        when(entityManager.find(
                Product.class,
                4L,
                LockModeType.PESSIMISTIC_WRITE))
                .thenReturn(product);
    }

    private void assertMoney(String expected, BigDecimal actual) {
        assertNotNull(actual);
        assertEquals(0, new BigDecimal(expected).compareTo(actual));
    }
}
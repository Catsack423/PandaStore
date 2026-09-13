package project.project.Service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import project.project.Entity.order.*;
import project.project.Entity.product.Product;
import project.project.Entity.seller.Seller;
import project.project.Entity.user.Customer;
import project.project.Entity.user.User;
import project.project.Repository.OrderRepository;
import project.project.Repository.ProductRepository;
import project.project.Service.api.NotificationService;
import project.project.Service.api.PaymentService;
import project.project.Service.implement.SubOrderServiceImp;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class SubOrderServiceTest {

    @InjectMocks
    private SubOrderServiceImp subOrderService;

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private PaymentService paymentService;

    @Mock
    private NotificationService notificationService;

    @Test
    @DisplayName("getSubOrderById - สำเร็จเมื่อพบคำสั่งซื้อ")
    void getSubOrderById_Success() {
        Long orderId = 1L;
        Order order = new Order();
        order.setOrderId(orderId);
        order.setSubOrderNumber("ORD-12345");

        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));

        Order result = subOrderService.getSubOrderById(orderId);

        assertNotNull(result);
        assertEquals("ORD-12345", result.getSubOrderNumber());
        verify(orderRepository, times(1)).findById(orderId);
    }

    @Test
    @DisplayName("getSubOrderById - โยน Exception เมื่อไม่พบ")
    void getSubOrderById_NotFound_ThrowsException() {
        Long orderId = 999L;
        when(orderRepository.findById(orderId)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> {
            subOrderService.getSubOrderById(orderId);
        });
    }

    @Test
    @DisplayName("getSubOrdersByOrderGroup - คืนค่ารายการคำสั่งซื้อย่อยใน Group")
    void getSubOrdersByOrderGroup_Success() {
        Long groupId = 10L;
        Order order1 = new Order();
        order1.setOrderId(1L);
        Order order2 = new Order();
        order2.setOrderId(2L);

        when(orderRepository.findByOrderGroup_OrderGroupId(groupId)).thenReturn(List.of(order1, order2));

        List<Order> result = subOrderService.getSubOrdersByOrderGroup(groupId);

        assertEquals(2, result.size());
        verify(orderRepository, times(1)).findByOrderGroup_OrderGroupId(groupId);
    }

    @Test
    @DisplayName("getSubOrdersBySeller - คืนค่ารายการคำสั่งซื้อย่อยของร้านค้า")
    void getSubOrdersBySeller_Success() {
        Long sellerId = 5L;
        Order order1 = new Order();
        order1.setOrderId(1L);

        when(orderRepository.findBySeller_SellerId(sellerId)).thenReturn(List.of(order1));

        List<Order> result = subOrderService.getSubOrdersBySeller(sellerId);

        assertEquals(1, result.size());
        verify(orderRepository, times(1)).findBySeller_SellerId(sellerId);
    }

    @Test
    @DisplayName("sellerAcceptOrder - สำเร็จ: เปลี่ยนสถานะเป็น PREPARING และแจ้งเตือนลูกค้า")
    void sellerAcceptOrder_Success() {
        Long sellerId = 1L;
        Long orderId = 10L;

        Seller seller = new Seller();
        seller.setSellerId(sellerId);

        Customer customer = new Customer();
        customer.setCustomerId(50L);
        User user = new User();
        user.setUserId(200L);
        customer.setUser(user);

        OrderGroup group = new OrderGroup();
        group.setCustomer(customer);

        Order order = new Order();
        order.setOrderId(orderId);
        order.setSeller(seller);
        order.setOrderStatus(OrderStatus.WAITING_SELLER_CONFIRM);
        order.setOrderGroup(group);
        order.setSubOrderNumber("ORD-001");

        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));

        subOrderService.sellerAcceptOrder(sellerId, orderId);

        assertEquals(OrderStatus.PREPARING, order.getOrderStatus());
        verify(orderRepository, times(1)).save(order);
        verify(notificationService, times(1)).sendNotification(
                eq(200L),
                eq("ร้านค้ายืนยันคำสั่งซื้อ"),
                contains("ORD-001"),
                any()
        );
    }

    @Test
    @DisplayName("sellerAcceptOrder - โยน Exception เมื่อคำสั่งซื้อไม่ใช่ของร้านค้านี้")
    void sellerAcceptOrder_WrongSeller_ThrowsException() {
        Long sellerId = 1L;
        Long otherSellerId = 2L;
        Long orderId = 10L;

        Seller otherSeller = new Seller();
        otherSeller.setSellerId(otherSellerId);

        Order order = new Order();
        order.setOrderId(orderId);
        order.setSeller(otherSeller);

        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));

        assertThrows(RuntimeException.class, () -> {
            subOrderService.sellerAcceptOrder(sellerId, orderId);
        });
        verify(orderRepository, never()).save(any());
    }

    @Test
    @DisplayName("sellerAcceptOrder - โยน Exception เมื่อสถานะไม่ใช่ WAITING_SELLER_CONFIRM")
    void sellerAcceptOrder_InvalidStatus_ThrowsException() {
        Long sellerId = 1L;
        Long orderId = 10L;

        Seller seller = new Seller();
        seller.setSellerId(sellerId);

        Order order = new Order();
        order.setOrderId(orderId);
        order.setSeller(seller);
        order.setOrderStatus(OrderStatus.PREPARING);

        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));

        RuntimeException ex = assertThrows(RuntimeException.class, () -> {
            subOrderService.sellerAcceptOrder(sellerId, orderId);
        });
        assertTrue(ex.getMessage().contains("ต้องเป็น WAITING_SELLER_CONFIRM"));
    }

    @Test
    @DisplayName("sellerRejectOrder - สำเร็จ: คืน Stock, เรียก Partial Refund, อัปเดต Group Payment, แจ้งเตือน")
    void sellerRejectOrder_Success() {
        Long sellerId = 1L;
        Long orderId = 10L;

        Seller seller = new Seller();
        seller.setSellerId(sellerId);

        Customer customer = new Customer();
        customer.setCustomerId(50L);
        User user = new User();
        user.setUserId(200L);
        customer.setUser(user);

        OrderGroup group = new OrderGroup();
        group.setOrderGroupId(100L);
        group.setCustomer(customer);

        Product product = new Product();
        product.setProductId(1L);
        product.setStock(10);

        OrderItem item = new OrderItem();
        item.setProduct(product);
        item.setQuantity(2);

        Order order = new Order();
        order.setOrderId(orderId);
        order.setSeller(seller);
        order.setOrderStatus(OrderStatus.WAITING_SELLER_CONFIRM);
        order.setTotalAmount(new BigDecimal("500.00"));
        order.setOrderGroup(group);
        order.setSubOrderNumber("ORD-001");
        order.setOrderItems(List.of(item));

        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));
        when(orderRepository.findByOrderGroup_OrderGroupId(100L)).thenReturn(List.of(order));

        subOrderService.sellerRejectOrder(sellerId, orderId, "สินค้าหมดสต็อก");

        assertEquals(OrderStatus.CANCELLED, order.getOrderStatus());
        assertEquals("สินค้าหมดสต็อก", order.getRejectionReason());

        // ตรวจสอบการคืน Stock: 10 + 2 = 12
        assertEquals(12, product.getStock());
        verify(productRepository, times(1)).save(product);

        // ตรวจสอบการเรียก Partial Refund
        verify(paymentService, times(1)).processPartialRefund(
                eq(orderId),
                eq(new BigDecimal("500.00")),
                contains("สินค้าหมดสต็อก")
        );

        // ตรวจสอบสถานะกลุ่ม OrderGroup: เมื่อทุก Order ถูก Cancel -> REFUNDED
        assertEquals(OrderGroupPaymentStatus.REFUNDED, group.getPaymentStatus());

        // ตรวจสอบ Notification
        verify(notificationService, times(1)).sendNotification(
                eq(200L),
                eq("ร้านค้าปฏิเสธคำสั่งซื้อ"),
                contains("สินค้าหมดสต็อก"),
                any()
        );
    }

    @Test
    @DisplayName("confirmOrderDelivered - สำเร็จ: เปลี่ยนสถานะเป็น COMPLETED และบันทึก completedAt")
    void confirmOrderDelivered_Success() {
        Long customerId = 50L;
        Long orderId = 10L;

        Customer customer = new Customer();
        customer.setCustomerId(customerId);

        OrderGroup group = new OrderGroup();
        group.setCustomer(customer);

        Order order = new Order();
        order.setOrderId(orderId);
        order.setOrderStatus(OrderStatus.SHIPPED);
        order.setOrderGroup(group);

        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));

        subOrderService.confirmOrderDelivered(customerId, orderId);

        assertEquals(OrderStatus.COMPLETED, order.getOrderStatus());
        assertNotNull(order.getCompletedAt());
        verify(orderRepository, times(1)).save(order);
    }

    @Test
    @DisplayName("confirmOrderDelivered - โยน Exception เมื่อไม่ใช่ Customer เจ้าของ Order")
    void confirmOrderDelivered_WrongCustomer_ThrowsException() {
        Long customerId = 50L;
        Long otherCustomerId = 99L;
        Long orderId = 10L;

        Customer otherCustomer = new Customer();
        otherCustomer.setCustomerId(otherCustomerId);

        OrderGroup group = new OrderGroup();
        group.setCustomer(otherCustomer);

        Order order = new Order();
        order.setOrderId(orderId);
        order.setOrderGroup(group);

        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));

        assertThrows(RuntimeException.class, () -> {
            subOrderService.confirmOrderDelivered(customerId, orderId);
        });
        verify(orderRepository, never()).save(any());
    }

    @Test
    @DisplayName("confirmOrderDelivered - โยน Exception เมื่อสถานะไม่ใช่ SHIPPED")
    void confirmOrderDelivered_InvalidStatus_ThrowsException() {
        Long customerId = 50L;
        Long orderId = 10L;

        Customer customer = new Customer();
        customer.setCustomerId(customerId);

        OrderGroup group = new OrderGroup();
        group.setCustomer(customer);

        Order order = new Order();
        order.setOrderId(orderId);
        order.setOrderStatus(OrderStatus.PREPARING);
        order.setOrderGroup(group);

        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));

        RuntimeException ex = assertThrows(RuntimeException.class, () -> {
            subOrderService.confirmOrderDelivered(customerId, orderId);
        });
        assertTrue(ex.getMessage().contains("ต้องเป็น SHIPPED"));
    }

    @Test
    @DisplayName("autoConfirmDelivered - เปลี่ยนสถานะเป็น COMPLETED เมื่อสถานะคือ SHIPPED")
    void autoConfirmDelivered_Success() {
        Long orderId = 10L;
        Order order = new Order();
        order.setOrderId(orderId);
        order.setOrderStatus(OrderStatus.SHIPPED);

        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));

        subOrderService.autoConfirmDelivered(orderId);

        assertEquals(OrderStatus.COMPLETED, order.getOrderStatus());
        assertNotNull(order.getCompletedAt());
        verify(orderRepository, times(1)).save(order);
    }

    @Test
    @DisplayName("autoConfirmDelivered - ข้ามไปเมื่อสถานะไม่ใช่ SHIPPED")
    void autoConfirmDelivered_NotShipped_Skipped() {
        Long orderId = 10L;
        Order order = new Order();
        order.setOrderId(orderId);
        order.setOrderStatus(OrderStatus.COMPLETED); // Already completed

        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));

        subOrderService.autoConfirmDelivered(orderId);

        verify(orderRepository, never()).save(order);
    }
}

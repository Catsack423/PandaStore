package project.project.Service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import project.project.Entity.order.*;
import project.project.Entity.seller.Seller;
import project.project.Entity.user.Customer;
import project.project.Exception.ResourceNotFoundException;
import project.project.Repository.OrderGroupRepository;
import project.project.Repository.OrderRepository;
import project.project.Repository.PaymentRepository;
import project.project.Service.api.NotificationService;
import project.project.Service.implement.PaymentServiceImp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class PaymentServiceTest {

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private OrderGroupRepository orderGroupRepository;

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private PaymentServiceImp paymentService;

    private Customer sampleCustomer;
    private Seller sampleSeller;
    private OrderGroup sampleOrderGroup;
    private Order sampleSubOrder;
    private Payment samplePayment;

    @BeforeEach
    void setUp() {
        sampleCustomer = new Customer();
        sampleCustomer.setCustomerId(10L);
        sampleCustomer.setFullName("นายทดสอบ ระบบซื้อขาย");

        sampleSeller = new Seller();
        sampleSeller.setSellerId(20L);
        sampleSeller.setShopName("Panda Tech Shop");

        sampleOrderGroup = new OrderGroup();
        sampleOrderGroup.setOrderGroupId(100L);
        sampleOrderGroup.setGroupNumber("GRP-20260916-0001");
        sampleOrderGroup.setCustomer(sampleCustomer);
        sampleOrderGroup.setTotalProductsAmount(new BigDecimal("1000.00"));
        sampleOrderGroup.setTotalShippingFee(new BigDecimal("50.00"));
        sampleOrderGroup.setGrandTotal(new BigDecimal("1050.00"));
        sampleOrderGroup.setPaymentStatus(OrderGroupPaymentStatus.PENDING);

        sampleSubOrder = new Order();
        sampleSubOrder.setOrderId(200L);
        sampleSubOrder.setSubOrderNumber("ORD-20260916-0001-S1");
        sampleSubOrder.setOrderGroup(sampleOrderGroup);
        sampleSubOrder.setSeller(sampleSeller);
        sampleSubOrder.setTotalAmount(new BigDecimal("1050.00"));
        sampleSubOrder.setOrderStatus(OrderStatus.WAITING_SELLER_CONFIRM);

        List<Order> subOrders = new ArrayList<>();
        subOrders.add(sampleSubOrder);
        sampleOrderGroup.setSubOrders(subOrders);

        samplePayment = new Payment();
        samplePayment.setPaymentId(500L);
        samplePayment.setOrderGroup(sampleOrderGroup);
        samplePayment.setPaymentMethod(PaymentMethod.PROMPTPAY);
        samplePayment.setAmount(new BigDecimal("1050.00"));
        samplePayment.setRefundedAmount(BigDecimal.ZERO);
        samplePayment.setStatus(PaymentStatus.PENDING);
        samplePayment.setGatewayTransactionId("GW-TXN-12345");
    }

    // ==========================================
    // 1. initiatePayment Tests
    // ==========================================

    @Test
    @DisplayName("UC1: เริ่มทำรายการชำระเงินสำเร็จ ระบุยอดเงินตรงกับ Grand Total")
    void testInitiatePayment_Success_WithSpecifiedAmount() {
        when(orderGroupRepository.findById(100L)).thenReturn(Optional.of(sampleOrderGroup));
        when(paymentRepository.findByOrderGroup_OrderGroupId(100L)).thenReturn(Optional.empty());
        when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(orderGroupRepository.save(any(OrderGroup.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Payment result = paymentService.initiatePayment(100L, PaymentMethod.PROMPTPAY, new BigDecimal("1050.00"));

        assertNotNull(result);
        assertEquals(PaymentMethod.PROMPTPAY, result.getPaymentMethod());
        assertEquals(new BigDecimal("1050.00"), result.getAmount());
        assertEquals(PaymentStatus.PENDING, result.getStatus());
        assertNotNull(result.getGatewayTransactionId());
        verify(paymentRepository, times(1)).save(any(Payment.class));
        verify(orderGroupRepository, times(1)).save(sampleOrderGroup);
    }

    @Test
    @DisplayName("UC1: เริ่มทำรายการชำระเงินสำเร็จ โดยดึงยอดเงินอัตโนมัติจาก Grand Total เมื่อไม่ส่ง Amount")
    void testInitiatePayment_Success_DefaultAmountFromOrderGroup() {
        when(orderGroupRepository.findById(100L)).thenReturn(Optional.of(sampleOrderGroup));
        when(paymentRepository.findByOrderGroup_OrderGroupId(100L)).thenReturn(Optional.empty());
        when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Payment result = paymentService.initiatePayment(100L, PaymentMethod.CREDIT_CARD);

        assertNotNull(result);
        assertEquals(PaymentMethod.CREDIT_CARD, result.getPaymentMethod());
        assertEquals(new BigDecimal("1050.00"), result.getAmount());
        assertEquals(PaymentStatus.PENDING, result.getStatus());
    }

    @Test
    @DisplayName("เริ่มทำรายการชำระเงินล้มเหลว: OrderGroup ID เป็น null")
    void testInitiatePayment_ThrowsException_WhenOrderGroupIdIsNull() {
        assertThrows(IllegalArgumentException.class, () ->
                paymentService.initiatePayment(null, PaymentMethod.PROMPTPAY, new BigDecimal("100.00"))
        );
    }

    @Test
    @DisplayName("เริ่มทำรายการชำระเงินล้มเหลว: PaymentMethod เป็น null")
    void testInitiatePayment_ThrowsException_WhenPaymentMethodIsNull() {
        assertThrows(IllegalArgumentException.class, () ->
                paymentService.initiatePayment(100L, null, new BigDecimal("100.00"))
        );
    }

    @Test
    @DisplayName("เริ่มทำรายการชำระเงินล้มเหลว: ไม่พบ OrderGroup")
    void testInitiatePayment_ThrowsException_WhenOrderGroupNotFound() {
        when(orderGroupRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () ->
                paymentService.initiatePayment(999L, PaymentMethod.PROMPTPAY, new BigDecimal("100.00"))
        );
    }

    @Test
    @DisplayName("เริ่มทำรายการชำระเงินล้มเหลว: ยอดเงิน <= 0")
    void testInitiatePayment_ThrowsException_WhenAmountIsZeroOrNegative() {
        when(orderGroupRepository.findById(100L)).thenReturn(Optional.of(sampleOrderGroup));

        assertThrows(IllegalArgumentException.class, () ->
                paymentService.initiatePayment(100L, PaymentMethod.PROMPTPAY, BigDecimal.ZERO)
        );
        assertThrows(IllegalArgumentException.class, () ->
                paymentService.initiatePayment(100L, PaymentMethod.PROMPTPAY, new BigDecimal("-50.00"))
        );
    }

    @Test
    @DisplayName("เริ่มทำรายการชำระเงินล้มเหลว: ยอดเงินไม่ตรงกับ Grand Total ของ OrderGroup")
    void testInitiatePayment_ThrowsException_WhenAmountDoesNotMatchGrandTotal() {
        when(orderGroupRepository.findById(100L)).thenReturn(Optional.of(sampleOrderGroup));

        assertThrows(IllegalArgumentException.class, () ->
                paymentService.initiatePayment(100L, PaymentMethod.PROMPTPAY, new BigDecimal("999.00"))
        );
    }

    @Test
    @DisplayName("เริ่มทำรายการชำระเงินล้มเหลว: มี Payment ที่ชำระเงินสำเร็จแล้ว (SUCCESS)")
    void testInitiatePayment_ThrowsException_WhenAlreadyPaidSuccess() {
        samplePayment.setStatus(PaymentStatus.SUCCESS);
        when(orderGroupRepository.findById(100L)).thenReturn(Optional.of(sampleOrderGroup));
        when(paymentRepository.findByOrderGroup_OrderGroupId(100L)).thenReturn(Optional.of(samplePayment));

        assertThrows(IllegalStateException.class, () ->
                paymentService.initiatePayment(100L, PaymentMethod.PROMPTPAY, new BigDecimal("1050.00"))
        );
    }

    @Test
    @DisplayName("เริ่มทำรายการชำระเงินล้มเหลว: Payment เคยถูก Refund ไปแล้ว")
    void testInitiatePayment_ThrowsException_WhenAlreadyRefunded() {
        samplePayment.setStatus(PaymentStatus.REFUNDED);
        when(orderGroupRepository.findById(100L)).thenReturn(Optional.of(sampleOrderGroup));
        when(paymentRepository.findByOrderGroup_OrderGroupId(100L)).thenReturn(Optional.of(samplePayment));

        assertThrows(IllegalStateException.class, () ->
                paymentService.initiatePayment(100L, PaymentMethod.PROMPTPAY, new BigDecimal("1050.00"))
        );
    }

    @Test
    @DisplayName("UC1: เริ่มทำรายการใหม่อีกครั้งเมื่อครั้งก่อนหน้า FAILED (Retry Payment)")
    void testInitiatePayment_RetryFailedPayment_Success() {
        samplePayment.setStatus(PaymentStatus.FAILED);
        when(orderGroupRepository.findById(100L)).thenReturn(Optional.of(sampleOrderGroup));
        when(paymentRepository.findByOrderGroup_OrderGroupId(100L)).thenReturn(Optional.of(samplePayment));
        when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Payment result = paymentService.initiatePayment(100L, PaymentMethod.WALLET, new BigDecimal("1050.00"));

        assertNotNull(result);
        assertEquals(PaymentMethod.WALLET, result.getPaymentMethod());
        assertEquals(PaymentStatus.PENDING, result.getStatus());
        assertNotNull(result.getGatewayTransactionId());
    }

    // ==========================================
    // 2. handleGatewayCallback Tests
    // ==========================================

    @Test
    @DisplayName("UC1: Gateway Callback สำเร็จ อัปเดตสถานะเป็น SUCCESS, OrderGroup เป็น PAID, และแจ้งเตือน")
    void testHandleGatewayCallback_Success_UpdatesPaymentAndOrderGroup() {
        when(paymentRepository.findByGatewayTransactionId("GW-TXN-12345")).thenReturn(Optional.of(samplePayment));
        when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        paymentService.handleGatewayCallback("GW-TXN-12345", true);

        assertEquals(PaymentStatus.SUCCESS, samplePayment.getStatus());
        assertNotNull(samplePayment.getPaidAt());
        assertEquals(OrderGroupPaymentStatus.PAID, sampleOrderGroup.getPaymentStatus());
        assertEquals(OrderStatus.WAITING_SELLER_CONFIRM, sampleSubOrder.getOrderStatus());

        verify(paymentRepository, times(1)).save(samplePayment);
        verify(orderGroupRepository, times(1)).save(sampleOrderGroup);
        verify(notificationService, times(1)).notifyCustomerOrderPaid(10L, 100L);
        verify(notificationService, times(1)).notifySellerNewOrder(20L, 200L);
    }

    @Test
    @DisplayName("Gateway Callback สำเร็จแบบ Idempotent: หากสถานะเป็น SUCCESS อยู่แล้ว ไม่ทำซ้ำ")
    void testHandleGatewayCallback_Success_IdempotentCall() {
        samplePayment.setStatus(PaymentStatus.SUCCESS);
        samplePayment.setPaidAt(LocalDateTime.now().minusMinutes(5));
        when(paymentRepository.findByGatewayTransactionId("GW-TXN-12345")).thenReturn(Optional.of(samplePayment));

        paymentService.handleGatewayCallback("GW-TXN-12345", true);

        verify(paymentRepository, never()).save(any());
        verify(orderGroupRepository, never()).save(any());
    }

    @Test
    @DisplayName("UC1 (27A): Gateway Callback ชำระเงินไม่ผ่าน (FAILED) อัปเดตสถานะ Payment และ OrderGroup เป็น FAILED")
    void testHandleGatewayCallback_Failure_UpdatesPaymentAndOrderGroupStatusToFailed() {
        when(paymentRepository.findByGatewayTransactionId("GW-TXN-12345")).thenReturn(Optional.of(samplePayment));
        when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        paymentService.handleGatewayCallback("GW-TXN-12345", false);

        assertEquals(PaymentStatus.FAILED, samplePayment.getStatus());
        assertEquals(OrderGroupPaymentStatus.FAILED, sampleOrderGroup.getPaymentStatus());
        verify(paymentRepository, times(1)).save(samplePayment);
        verify(orderGroupRepository, times(1)).save(sampleOrderGroup);
    }

    @Test
    @DisplayName("Gateway Callback ล้มเหลว: GatewayTransactionId ว่างหรือ null")
    void testHandleGatewayCallback_ThrowsException_WhenGatewayTransactionIdIsBlank() {
        assertThrows(IllegalArgumentException.class, () ->
                paymentService.handleGatewayCallback(null, true)
        );
        assertThrows(IllegalArgumentException.class, () ->
                paymentService.handleGatewayCallback("   ", true)
        );
    }

    @Test
    @DisplayName("Gateway Callback ล้มเหลว: ไม่พบ Payment จาก GatewayTransactionId")
    void testHandleGatewayCallback_ThrowsException_WhenPaymentNotFound() {
        when(paymentRepository.findByGatewayTransactionId("INVALID-TXN")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () ->
                paymentService.handleGatewayCallback("INVALID-TXN", true)
        );
    }

    @Test
    @DisplayName("Gateway Callback ล้มเหลว: พยายามส่งผล FAILED กับรายการที่เคย SUCCESS ไปแล้ว")
    void testHandleGatewayCallback_ThrowsException_WhenAttemptingToFailAlreadySuccessfulPayment() {
        samplePayment.setStatus(PaymentStatus.SUCCESS);
        when(paymentRepository.findByGatewayTransactionId("GW-TXN-12345")).thenReturn(Optional.of(samplePayment));

        assertThrows(IllegalStateException.class, () ->
                paymentService.handleGatewayCallback("GW-TXN-12345", false)
        );
    }

    // ==========================================
    // 3. processPartialRefund Tests
    // ==========================================

    @Test
    @DisplayName("UC1 (37A): ร้านค้าปฏิเสธ คืนเงินบางส่วน (Partial Refund) สำเร็จ")
    void testProcessPartialRefund_Success_PartiallyRefunded() {
        samplePayment.setStatus(PaymentStatus.SUCCESS);
        samplePayment.setPaidAt(LocalDateTime.now());
        sampleOrderGroup.setPaymentStatus(OrderGroupPaymentStatus.PAID);

        when(orderRepository.findById(200L)).thenReturn(Optional.of(sampleSubOrder));
        when(paymentRepository.findByOrderGroup_OrderGroupId(100L)).thenReturn(Optional.of(samplePayment));
        when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        paymentService.processPartialRefund(200L, new BigDecimal("500.00"), "สินค้าบางรายการหมดสต็อก");

        assertEquals(new BigDecimal("500.00"), samplePayment.getRefundedAmount());
        assertEquals(PaymentStatus.PARTIALLY_REFUNDED, samplePayment.getStatus());
        assertEquals(OrderGroupPaymentStatus.PARTIALLY_REFUNDED, sampleOrderGroup.getPaymentStatus());
        assertEquals(OrderStatus.CANCELLED, sampleSubOrder.getOrderStatus());
        assertEquals("สินค้าบางรายการหมดสต็อก", sampleSubOrder.getRejectionReason());

        verify(orderRepository, times(1)).save(sampleSubOrder);
        verify(orderGroupRepository, times(1)).save(sampleOrderGroup);
        verify(paymentRepository, times(1)).save(samplePayment);
    }

    @Test
    @DisplayName("UC1 (37A): คืนเงินครบยอดเต็มจำนวนพอดี เปลี่ยนสถานะเป็น REFUNDED")
    void testProcessPartialRefund_Success_StatusBecomesRefundedWhenAccumulatedEqualsTotal() {
        samplePayment.setStatus(PaymentStatus.SUCCESS);
        sampleOrderGroup.setPaymentStatus(OrderGroupPaymentStatus.PAID);

        when(orderRepository.findById(200L)).thenReturn(Optional.of(sampleSubOrder));
        when(paymentRepository.findByOrderGroup_OrderGroupId(100L)).thenReturn(Optional.of(samplePayment));

        paymentService.processPartialRefund(200L, new BigDecimal("1050.00"), "ยกเลิกทั้งร้าน");

        assertEquals(new BigDecimal("1050.00"), samplePayment.getRefundedAmount());
        assertEquals(PaymentStatus.REFUNDED, samplePayment.getStatus());
        assertEquals(OrderGroupPaymentStatus.REFUNDED, sampleOrderGroup.getPaymentStatus());
    }

    @Test
    @DisplayName("Partial Refund ล้มเหลว: ยอดเงินคืนเกิน Sub-Order total amount")
    void testProcessPartialRefund_ThrowsException_WhenRefundAmountExceedsSubOrderTotal() {
        sampleSubOrder.setTotalAmount(new BigDecimal("300.00"));
        when(orderRepository.findById(200L)).thenReturn(Optional.of(sampleSubOrder));

        assertThrows(IllegalArgumentException.class, () ->
                paymentService.processPartialRefund(200L, new BigDecimal("500.00"), "เหตุผล")
        );
    }

    @Test
    @DisplayName("Partial Refund ล้มเหลว: Payment ยังไม่สำเร็จ (PENDING) ไม่สามารถ Refund ได้")
    void testProcessPartialRefund_ThrowsException_WhenPaymentNotSuccessfulYet() {
        samplePayment.setStatus(PaymentStatus.PENDING);
        when(orderRepository.findById(200L)).thenReturn(Optional.of(sampleSubOrder));
        when(paymentRepository.findByOrderGroup_OrderGroupId(100L)).thenReturn(Optional.of(samplePayment));

        assertThrows(IllegalStateException.class, () ->
                paymentService.processPartialRefund(200L, new BigDecimal("200.00"), "เหตุผล")
        );
    }

    @Test
    @DisplayName("Partial Refund ล้มเหลว: ยอดคืนสะสมเกินยอดเงินจริงที่ชำระไว้")
    void testProcessPartialRefund_ThrowsException_WhenRefundAmountExceedsRemainingBalance() {
        samplePayment.setStatus(PaymentStatus.PARTIALLY_REFUNDED);
        samplePayment.setRefundedAmount(new BigDecimal("900.00")); // เหลือ refund ได้ 150
        when(orderRepository.findById(200L)).thenReturn(Optional.of(sampleSubOrder));
        when(paymentRepository.findByOrderGroup_OrderGroupId(100L)).thenReturn(Optional.of(samplePayment));

        assertThrows(IllegalArgumentException.class, () ->
                paymentService.processPartialRefund(200L, new BigDecimal("200.00"), "เหตุผล")
        );
    }

    // ==========================================
    // 4. processFullRefund Tests
    // ==========================================

    @Test
    @DisplayName("UC1: ดำเนินการคืนเงินเต็มจำนวน (Full Refund) สำเร็จ")
    void testProcessFullRefund_Success() {
        samplePayment.setStatus(PaymentStatus.SUCCESS);
        when(orderGroupRepository.findById(100L)).thenReturn(Optional.of(sampleOrderGroup));
        when(paymentRepository.findByOrderGroup_OrderGroupId(100L)).thenReturn(Optional.of(samplePayment));

        paymentService.processFullRefund(100L, "ลูกค้าขอยกเลิกคำสั่งซื้อทั้งบิล");

        assertEquals(new BigDecimal("1050.00"), samplePayment.getRefundedAmount());
        assertEquals(PaymentStatus.REFUNDED, samplePayment.getStatus());
        assertEquals(OrderGroupPaymentStatus.REFUNDED, sampleOrderGroup.getPaymentStatus());
        assertEquals(OrderStatus.CANCELLED, sampleSubOrder.getOrderStatus());
        assertEquals("ลูกค้าขอยกเลิกคำสั่งซื้อทั้งบิล", sampleSubOrder.getRejectionReason());

        verify(orderRepository, times(1)).save(sampleSubOrder);
        verify(orderGroupRepository, times(1)).save(sampleOrderGroup);
        verify(paymentRepository, times(1)).save(samplePayment);
    }

    @Test
    @DisplayName("Full Refund ล้มเหลว: เคยถูกคืนเงินเต็มจำนวนไปแล้ว")
    void testProcessFullRefund_ThrowsException_WhenAlreadyFullyRefunded() {
        samplePayment.setStatus(PaymentStatus.REFUNDED);
        samplePayment.setRefundedAmount(new BigDecimal("1050.00"));
        when(orderGroupRepository.findById(100L)).thenReturn(Optional.of(sampleOrderGroup));
        when(paymentRepository.findByOrderGroup_OrderGroupId(100L)).thenReturn(Optional.of(samplePayment));

        assertThrows(IllegalStateException.class, () ->
                paymentService.processFullRefund(100L, "ขอคืนซ้ำ")
        );
    }

    // ==========================================
    // 5. Query / Get Tests
    // ==========================================

    @Test
    @DisplayName("ดึงข้อมูลการชำระเงินตาม OrderGroupId สำเร็จ")
    void testGetPaymentByOrderGroupId_Success() {
        when(paymentRepository.findByOrderGroup_OrderGroupId(100L)).thenReturn(Optional.of(samplePayment));

        Payment payment = paymentService.getPaymentByOrderGroupId(100L);

        assertNotNull(payment);
        assertEquals(500L, payment.getPaymentId());
    }

    @Test
    @DisplayName("ดึงข้อมูลการชำระเงินตาม OrderGroupId ไม่พบ โยน ResourceNotFoundException")
    void testGetPaymentByOrderGroupId_ThrowsException_WhenNotFound() {
        when(paymentRepository.findByOrderGroup_OrderGroupId(999L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () ->
                paymentService.getPaymentByOrderGroupId(999L)
        );
    }

    @Test
    @DisplayName("ดึงข้อมูลการชำระเงินตาม PaymentId สำเร็จ")
    void testGetPaymentById_Success() {
        when(paymentRepository.findById(500L)).thenReturn(Optional.of(samplePayment));

        Payment payment = paymentService.getPaymentById(500L);

        assertNotNull(payment);
        assertEquals(500L, payment.getPaymentId());
    }

    @Test
    @DisplayName("ดึงข้อมูลการชำระเงินตาม PaymentId ไม่พบ โยน ResourceNotFoundException")
    void testGetPaymentById_ThrowsException_WhenNotFound() {
        when(paymentRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () ->
                paymentService.getPaymentById(999L)
        );
    }

    @Test
    @DisplayName("ดึงข้อมูลการชำระเงินตาม GatewayTransactionId สำเร็จ")
    void testGetPaymentByGatewayTransactionId_Success() {
        when(paymentRepository.findByGatewayTransactionId("GW-TXN-12345")).thenReturn(Optional.of(samplePayment));

        Payment payment = paymentService.getPaymentByGatewayTransactionId("GW-TXN-12345");

        assertNotNull(payment);
        assertEquals(500L, payment.getPaymentId());
        assertEquals("GW-TXN-12345", payment.getGatewayTransactionId());
    }

    @Test
    @DisplayName("ดึงข้อมูลการชำระเงินตาม GatewayTransactionId ล้มเหลว: รหัสว่าง โยน IllegalArgumentException")
    void testGetPaymentByGatewayTransactionId_ThrowsException_WhenBlank() {
        assertThrows(IllegalArgumentException.class, () ->
                paymentService.getPaymentByGatewayTransactionId("   ")
        );
    }

    @Test
    @DisplayName("ดึงข้อมูลการชำระเงินตาม GatewayTransactionId ไม่พบ โยน ResourceNotFoundException")
    void testGetPaymentByGatewayTransactionId_ThrowsException_WhenNotFound() {
        when(paymentRepository.findByGatewayTransactionId("UNKNOWN")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () ->
                paymentService.getPaymentByGatewayTransactionId("UNKNOWN")
        );
    }
}

package project.project.Controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import project.project.Entity.order.OrderGroup;
import project.project.Entity.order.Payment;
import project.project.Entity.order.PaymentMethod;
import project.project.Entity.order.PaymentStatus;
import project.project.Exception.ResourceNotFoundException;
import project.project.Service.api.PaymentService;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
public class PaymentControllerTest {

    private MockMvc mockMvc;

    @Mock
    private PaymentService paymentService;

    @InjectMocks
    private PaymentController paymentController;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(paymentController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("POST /api/payments/initiate - เริ่มต้นทำรายการชำระเงินสำเร็จ")
    void initiatePayment_Success() throws Exception {
        Long orderGroupId = 100L;
        String requestJson = """
                {
                    "orderGroupId": 100,
                    "paymentMethod": "PROMPTPAY",
                    "amount": 750.00
                }
                """;

        OrderGroup orderGroup = new OrderGroup();
        orderGroup.setOrderGroupId(orderGroupId);

        Payment payment = new Payment();
        payment.setPaymentId(1L);
        payment.setOrderGroup(orderGroup);
        payment.setPaymentMethod(PaymentMethod.PROMPTPAY);
        payment.setAmount(new BigDecimal("750.00"));
        payment.setRefundedAmount(BigDecimal.ZERO);
        payment.setStatus(PaymentStatus.PENDING);
        payment.setGatewayTransactionId("TXN_100_12345678");

        when(paymentService.initiatePayment(eq(orderGroupId), eq(PaymentMethod.PROMPTPAY), any(BigDecimal.class)))
                .thenReturn(payment);

        mockMvc.perform(post("/api/payments/initiate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.paymentId").value(1L))
                .andExpect(jsonPath("$.data.orderGroupId").value(orderGroupId))
                .andExpect(jsonPath("$.data.paymentMethod").value("PROMPTPAY"))
                .andExpect(jsonPath("$.data.amount").value(750.00))
                .andExpect(jsonPath("$.data.status").value("PENDING"))
                .andExpect(jsonPath("$.data.gatewayTransactionId").value("TXN_100_12345678"));
    }

    @Test
    @DisplayName("POST /api/payments/initiate - Validation Error เมื่อยอดชำระ <= 0 หรือไม่มี orderGroupId")
    void initiatePayment_ValidationError() throws Exception {
        String invalidJson = """
                {
                    "orderGroupId": null,
                    "paymentMethod": "PROMPTPAY",
                    "amount": 0.00
                }
                """;

        mockMvc.perform(post("/api/payments/initiate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.orderGroupId").exists())
                .andExpect(jsonPath("$.error.amount").exists());
    }

    @Test
    @DisplayName("POST /api/payments/callback - Gateway แจ้งผลชำระเงินสำเร็จ (PAID)")
    void handleGatewayCallback_Success() throws Exception {
        String callbackJson = """
                {
                    "gatewayTransactionId": "TXN_100_12345678",
                    "orderGroupId": 100,
                    "isSuccess": true
                }
                """;

        OrderGroup orderGroup = new OrderGroup();
        orderGroup.setOrderGroupId(100L);

        Payment updated = new Payment();
        updated.setPaymentId(1L);
        updated.setOrderGroup(orderGroup);
        updated.setPaymentMethod(PaymentMethod.PROMPTPAY);
        updated.setAmount(new BigDecimal("750.00"));
        updated.setStatus(PaymentStatus.SUCCESS);
        updated.setGatewayTransactionId("TXN_100_12345678");
        updated.setPaidAt(LocalDateTime.now());

        doNothing().when(paymentService).handleGatewayCallback("TXN_100_12345678", true);
        when(paymentService.getPaymentByOrderGroupId(100L)).thenReturn(updated);

        mockMvc.perform(post("/api/payments/callback")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(callbackJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("SUCCESS"))
                .andExpect(jsonPath("$.data.paymentId").value(1L));
    }

    @Test
    @DisplayName("POST /api/payments/simulate - จำลองผลการชำระเงินสำเร็จ")
    void simulatePayment_Success() throws Exception {
        String simulateJson = """
                {
                    "orderGroupId": 100,
                    "gatewayTransactionId": "TXN_100_SIMULATED",
                    "isSuccess": true
                }
                """;

        OrderGroup orderGroup = new OrderGroup();
        orderGroup.setOrderGroupId(100L);

        Payment updated = new Payment();
        updated.setPaymentId(1L);
        updated.setOrderGroup(orderGroup);
        updated.setStatus(PaymentStatus.SUCCESS);
        updated.setGatewayTransactionId("TXN_100_SIMULATED");

        doNothing().when(paymentService).handleGatewayCallback("TXN_100_SIMULATED", true);
        when(paymentService.getPaymentByOrderGroupId(100L)).thenReturn(updated);

        mockMvc.perform(post("/api/payments/simulate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(simulateJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("จำลองการชำระเงินสำเร็จ (PAID)"));
    }

    @Test
    @DisplayName("POST /api/payments/simulate - จำลองผลการชำระเงินล้มเหลว")
    void simulatePayment_Failure() throws Exception {
        String simulateJson = """
                {
                    "orderGroupId": 100,
                    "gatewayTransactionId": "TXN_100_FAILED",
                    "isSuccess": false
                }
                """;

        OrderGroup orderGroup = new OrderGroup();
        orderGroup.setOrderGroupId(100L);

        Payment updated = new Payment();
        updated.setPaymentId(1L);
        updated.setOrderGroup(orderGroup);
        updated.setStatus(PaymentStatus.FAILED);
        updated.setGatewayTransactionId("TXN_100_FAILED");

        doNothing().when(paymentService).handleGatewayCallback("TXN_100_FAILED", false);
        when(paymentService.getPaymentByOrderGroupId(100L)).thenReturn(updated);

        mockMvc.perform(post("/api/payments/simulate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(simulateJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("จำลองการชำระเงินล้มเหลว (FAILED)"));
    }

    @Test
    @DisplayName("POST /api/payments/refund/partial - ขอคืนเงินบางส่วนสำเร็จ (ร้านค้าปฏิเสธรับคำสั่งซื้อ)")
    void processPartialRefund_Success() throws Exception {
        String refundJson = """
                {
                    "orderId": 501,
                    "refundAmount": 250.00,
                    "reason": "ร้านค้าไม่สามารถจัดเตรียมสินค้าได้ทันเวลา"
                }
                """;

        doNothing().when(paymentService)
                .processPartialRefund(eq(501L), any(BigDecimal.class), eq("ร้านค้าไม่สามารถจัดเตรียมสินค้าได้ทันเวลา"));

        mockMvc.perform(post("/api/payments/refund/partial")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(refundJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("ดำเนินการคืนเงินบางส่วนสำเร็จ"));
    }

    @Test
    @DisplayName("POST /api/payments/refund/partial - Validation Error เมื่อยอดเงินคืน <= 0 หรือไม่มีเหตุผล")
    void processPartialRefund_ValidationError() throws Exception {
        String invalidJson = """
                {
                    "orderId": null,
                    "refundAmount": 0.00,
                    "reason": ""
                }
                """;

        mockMvc.perform(post("/api/payments/refund/partial")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.orderId").exists())
                .andExpect(jsonPath("$.error.refundAmount").exists())
                .andExpect(jsonPath("$.error.reason").exists());
    }

    @Test
    @DisplayName("POST /api/payments/refund/full - ขอคืนเงินเต็มจำนวนสำเร็จ (ยกเลิกคำสั่งซื้อทั้งตะกร้า)")
    void processFullRefund_Success() throws Exception {
        String refundJson = """
                {
                    "orderGroupId": 100,
                    "reason": "ลูกค้ายกเลิกคำสั่งซื้อทั้งหมดก่อนการจัดส่ง"
                }
                """;

        doNothing().when(paymentService)
                .processFullRefund(eq(100L), eq("ลูกค้ายกเลิกคำสั่งซื้อทั้งหมดก่อนการจัดส่ง"));

        mockMvc.perform(post("/api/payments/refund/full")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(refundJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("ดำเนินการคืนเงินเต็มจำนวนสำเร็จ"));
    }

    @Test
    @DisplayName("POST /api/payments/refund/full - Validation Error เมื่อไม่มี orderGroupId หรือเหตุผล")
    void processFullRefund_ValidationError() throws Exception {
        String invalidJson = """
                {
                    "orderGroupId": null,
                    "reason": ""
                }
                """;

        mockMvc.perform(post("/api/payments/refund/full")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.orderGroupId").exists())
                .andExpect(jsonPath("$.error.reason").exists());
    }

    @Test
    @DisplayName("GET /api/payments/order-group/{orderGroupId} - ค้นหาการชำระเงินตาม Order Group ID สำเร็จ")
    void getPaymentByOrderGroupId_Success() throws Exception {
        Long orderGroupId = 100L;
        OrderGroup orderGroup = new OrderGroup();
        orderGroup.setOrderGroupId(orderGroupId);

        Payment payment = new Payment();
        payment.setPaymentId(1L);
        payment.setOrderGroup(orderGroup);
        payment.setPaymentMethod(PaymentMethod.PROMPTPAY);
        payment.setAmount(new BigDecimal("750.00"));
        payment.setStatus(PaymentStatus.SUCCESS);
        payment.setGatewayTransactionId("TXN_100_12345678");

        when(paymentService.getPaymentByOrderGroupId(orderGroupId)).thenReturn(payment);

        mockMvc.perform(get("/api/payments/order-group/{orderGroupId}", orderGroupId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.orderGroupId").value(orderGroupId))
                .andExpect(jsonPath("$.data.paymentId").value(1L))
                .andExpect(jsonPath("$.data.status").value("SUCCESS"));
    }

    @Test
    @DisplayName("GET /api/payments/{id} - ค้นหาการชำระเงินตาม Payment ID สำเร็จ")
    void getPaymentById_Success() throws Exception {
        Long paymentId = 1L;
        Payment payment = new Payment();
        payment.setPaymentId(paymentId);
        payment.setPaymentMethod(PaymentMethod.CREDIT_CARD);
        payment.setAmount(new BigDecimal("1200.00"));
        payment.setStatus(PaymentStatus.SUCCESS);

        when(paymentService.getPaymentById(paymentId)).thenReturn(payment);

        mockMvc.perform(get("/api/payments/{id}", paymentId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.paymentId").value(paymentId))
                .andExpect(jsonPath("$.data.paymentMethod").value("CREDIT_CARD"));
    }

    @Test
    @DisplayName("GET /api/payments/{id} - ไม่พบข้อมูลการชำระเงิน (404 Not Found)")
    void getPaymentById_NotFound() throws Exception {
        Long paymentId = 999L;

        when(paymentService.getPaymentById(paymentId))
                .thenThrow(new ResourceNotFoundException("ไม่พบข้อมูลการชำระเงิน ID: " + paymentId));

        mockMvc.perform(get("/api/payments/{id}", paymentId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false));
    }
}

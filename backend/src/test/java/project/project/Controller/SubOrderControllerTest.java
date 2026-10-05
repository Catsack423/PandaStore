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
import project.project.Entity.order.Order;
import project.project.Entity.order.OrderStatus;
import project.project.Service.api.SubOrderService;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
public class SubOrderControllerTest {

    private MockMvc mockMvc;

    @Mock
    private SubOrderService subOrderService;

    @InjectMocks
    private SubOrderController subOrderController;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(subOrderController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("GET /api/sub-orders/{orderId} - ดึงรายละเอียด Sub-Order สำเร็จ")
    void getSubOrderById_Success() throws Exception {
        Long orderId = 1L;
        Order order = new Order();
        order.setOrderId(orderId);
        order.setSubOrderNumber("ORD-001");
        order.setOrderStatus(OrderStatus.WAITING_SELLER_CONFIRM);
        order.setTotalAmount(new BigDecimal("350.00"));

        when(subOrderService.getSubOrderById(orderId)).thenReturn(order);

        mockMvc.perform(get("/api/sub-orders/{orderId}", orderId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("ดึงข้อมูลคำสั่งซื้อย่อยสำเร็จ"))
                .andExpect(jsonPath("$.data.orderId").value(orderId))
                .andExpect(jsonPath("$.data.subOrderNumber").value("ORD-001"));
    }

    @Test
    @DisplayName("GET /api/sub-orders/order-group/{orderGroupId} - ดึงคำสั่งซื้อย่อยใน Group สำเร็จ")
    void getSubOrdersByOrderGroup_Success() throws Exception {
        Long groupId = 10L;
        Order order1 = new Order();
        order1.setOrderId(1L);
        order1.setSubOrderNumber("ORD-001");

        when(subOrderService.getSubOrdersByOrderGroup(groupId)).thenReturn(List.of(order1));

        mockMvc.perform(get("/api/sub-orders/order-group/{orderGroupId}", groupId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("ดึงรายการคำสั่งซื้อย่อยในกลุ่มสำเร็จ"))
                .andExpect(jsonPath("$.data[0].orderId").value(1L));
    }

    @Test
    @DisplayName("GET /api/sub-orders/seller/{sellerId} - ดึงคำสั่งซื้อย่อยของร้านค้าสำเร็จ")
    void getSubOrdersBySeller_Success() throws Exception {
        Long sellerId = 5L;
        Order order1 = new Order();
        order1.setOrderId(1L);

        when(subOrderService.getSubOrdersBySeller(sellerId)).thenReturn(List.of(order1));

        mockMvc.perform(get("/api/sub-orders/seller/{sellerId}", sellerId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("ดึงรายการคำสั่งซื้อย่อยของร้านค้าสำเร็จ"));
    }

    @Test
    @DisplayName("POST /api/sub-orders/{orderId}/accept - ยืนยันรับคำสั่งซื้อสำเร็จ")
    void sellerAcceptOrder_Success() throws Exception {
        Long orderId = 1L;
        Long sellerId = 5L;

        doNothing().when(subOrderService).sellerAcceptOrder(sellerId, orderId);

        mockMvc.perform(post("/api/sub-orders/{orderId}/accept", orderId)
                        .param("sellerId", String.valueOf(sellerId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("ยืนยันรับคำสั่งซื้อสำเร็จ"));
    }

    @Test
    @DisplayName("POST /api/sub-orders/{orderId}/reject - ปฏิเสธคำสั่งซื้อสำเร็จ")
    void sellerRejectOrder_Success() throws Exception {
        Long orderId = 1L;
        Long sellerId = 5L;
        String requestJson = "{" +
                "\"reason\":\"สินค้าชำรุด\"" +
                "}";

        doNothing().when(subOrderService).sellerRejectOrder(sellerId, orderId, "สินค้าชำรุด");

        mockMvc.perform(post("/api/sub-orders/{orderId}/reject", orderId)
                        .param("sellerId", String.valueOf(sellerId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("ปฏิเสธคำสั่งซื้อสำเร็จ"));
    }

    @Test
    @DisplayName("POST /api/sub-orders/{orderId}/reject - Validation Error เมื่อไม่ระบุเหตุผล")
    void sellerRejectOrder_ValidationError() throws Exception {
        Long orderId = 1L;
        Long sellerId = 5L;
        String invalidJson = "{" +
                "\"reason\":\"\"" +
                "}";

        mockMvc.perform(post("/api/sub-orders/{orderId}/reject", orderId)
                        .param("sellerId", String.valueOf(sellerId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.reason").exists());
    }

    @Test
    @DisplayName("POST /api/sub-orders/{orderId}/confirm-delivered - ยืนยันการรับสินค้าสำเร็จ")
    void confirmOrderDelivered_Success() throws Exception {
        Long orderId = 1L;
        Long customerId = 10L;

        doNothing().when(subOrderService).confirmOrderDelivered(customerId, orderId);

        mockMvc.perform(post("/api/sub-orders/{orderId}/confirm-delivered", orderId)
                        .param("customerId", String.valueOf(customerId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("ยืนยันการรับสินค้าสำเร็จ"));
    }

    @Test
    @DisplayName("POST /api/sub-orders/{orderId}/cancel - ลูกค้ายกเลิกคำสั่งซื้อสำเร็จ")
    void customerCancelOrder_Success() throws Exception {
        Long orderId = 1L;
        Long customerId = 10L;

        String json = "{\"reason\":\"เปลี่ยนใจไม่ต้องการสินค้า\"}";

        doNothing().when(subOrderService).customerCancelOrder(customerId, orderId, "เปลี่ยนใจไม่ต้องการสินค้า");

        mockMvc.perform(post("/api/sub-orders/{orderId}/cancel", orderId)
                        .param("customerId", String.valueOf(customerId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("ยกเลิกคำสั่งซื้อสำเร็จ"));
    }

    @Test
    @DisplayName("POST /api/sub-orders/{orderId}/cancel - Validation Error เมื่อ reason ว่าง")
    void customerCancelOrder_ValidationError_BlankReason() throws Exception {
        Long orderId = 1L;
        Long customerId = 10L;

        String invalidJson = "{\"reason\":\"\"}";

        mockMvc.perform(post("/api/sub-orders/{orderId}/cancel", orderId)
                        .param("customerId", String.valueOf(customerId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.reason").exists());
    }
}

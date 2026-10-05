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
import project.project.Entity.order.Shipment;
import project.project.Entity.order.ShippingStatus;
import project.project.Service.api.ShippingService;

import java.math.BigDecimal;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
public class ShippingControllerTest {

    private MockMvc mockMvc;

    @Mock
    private ShippingService shippingService;

    @InjectMocks
    private ShippingController shippingController;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(shippingController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("POST /api/shipping/calculate-fee - คำนวณค่าจัดส่งสำเร็จ")
    void calculateShippingFee_Success() throws Exception {
        String requestJson = "{" +
                "\"sellerId\":1," +
                "\"shippingMethod\":\"KERRY\"," +
                "\"addressId\":10" +
                "}";

        when(shippingService.calculateShippingFee(eq(1L), eq("KERRY"), eq(10L)))
                .thenReturn(new BigDecimal("50.00"));

        mockMvc.perform(post("/api/shipping/calculate-fee")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("คำนวณค่าจัดส่งสำเร็จ"))
                .andExpect(jsonPath("$.data.shippingMethod").value("KERRY"))
                .andExpect(jsonPath("$.data.shippingFee").value(50.00));
    }

    @Test
    @DisplayName("POST /api/shipping/calculate-fee - Validation Error เมื่อไม่ระบุ shippingMethod")
    void calculateShippingFee_ValidationError() throws Exception {
        String invalidJson = "{" +
                "\"sellerId\":1," +
                "\"shippingMethod\":\"\"," +
                "\"addressId\":10" +
                "}";

        mockMvc.perform(post("/api/shipping/calculate-fee")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.shippingMethod").exists());
    }

    @Test
    @DisplayName("POST /api/shipping/orders/{orderId}/assign-tracking - บันทึก Tracking Number สำเร็จ")
    void assignTrackingNumber_Success() throws Exception {
        Long orderId = 100L;
        Long sellerId = 1L;
        String requestJson = "{" +
                "\"courierName\":\"Kerry Express\"," +
                "\"trackingNumber\":\"KRY12345\"" +
                "}";

        Shipment shipment = new Shipment();
        shipment.setShipmentId(10L);
        shipment.setCourierName("Kerry Express");
        shipment.setTrackingNumber("KRY12345");
        shipment.setShippingStatus(ShippingStatus.SHIPPED);

        when(shippingService.assignTrackingNumber(sellerId, orderId, "Kerry Express", "KRY12345"))
                .thenReturn(shipment);

        mockMvc.perform(post("/api/shipping/orders/{orderId}/assign-tracking", orderId)
                        .param("sellerId", String.valueOf(sellerId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("บันทึกหมายเลข Tracking สำเร็จ"))
                .andExpect(jsonPath("$.data.trackingNumber").value("KRY12345"));
    }

    @Test
    @DisplayName("GET /api/shipping/orders/{orderId} - ดึงข้อมูลการจัดส่งสำเร็จ")
    void getShipmentByOrderId_Success() throws Exception {
        Long orderId = 100L;
        Shipment shipment = new Shipment();
        shipment.setShipmentId(10L);
        shipment.setCourierName("Flash Express");
        shipment.setTrackingNumber("FL12345");
        shipment.setShippingStatus(ShippingStatus.SHIPPED);

        when(shippingService.getShipmentByOrderId(orderId)).thenReturn(shipment);

        mockMvc.perform(get("/api/shipping/orders/{orderId}", orderId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.courierName").value("Flash Express"));
    }

    @Test
    @DisplayName("PATCH /api/shipping/shipments/{shipmentId}/status - อัปเดตสถานะสำเร็จ")
    void updateShippingStatus_Success() throws Exception {
        Long shipmentId = 10L;
        String requestJson = "{" +
                "\"status\":\"DELIVERED\"" +
                "}";

        doNothing().when(shippingService).updateShippingStatus(shipmentId, "DELIVERED");

        mockMvc.perform(patch("/api/shipping/shipments/{shipmentId}/status", shipmentId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("อัปเดตสถานะการจัดส่งสำเร็จ"));
    }
}

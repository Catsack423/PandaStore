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
import project.project.DTO.seller.CreateSellerApplicationRequest;
import project.project.DTO.seller.SellerApplicationResponse;
import project.project.Entity.seller.SellerApplication;
import project.project.Entity.seller.SellerApplicationStatus;
import project.project.Entity.user.User;
import project.project.Entity.user.UserRole;
import project.project.Entity.user.UserStatus;
import project.project.Exception.ResourceNotFoundException;
import project.project.Service.api.SellerApplicationService;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
public class SellerApplicationControllerTest {

    private MockMvc mockMvc;

    @Mock
    private SellerApplicationService sellerApplicationService;

    @InjectMocks
    private SellerApplicationController sellerApplicationController;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(sellerApplicationController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("POST /api/seller-applications - ยื่นคำขอเปิดร้านค้าสำเร็จ")
    void submitApplication_Success() throws Exception {
        Long userId = 2L;
        String requestJson = """
                {
                    "shopName": "Panda Craft Shop",
                    "shopDescription": "ร้านขายงานฝีมือคุณภาพดี ทำด้วยใจ",
                    "shopPhone": "0812345678",
                    "shopEmail": "pandacraft@gmail.com",
                    "shopAddress": "123 ถ.มิตรภาพ ต.ในเมือง อ.เมือง จ.ขอนแก่น 40000",
                    "sellerFirstName": "สมศักดิ์",
                    "sellerLastName": "รักดี",
                    "idCardNumber": "1234567890123",
                    "idCardImageUrl": "https://pandastore.com/docs/idcard.jpg",
                    "bankAccountName": "สมศักดิ์ รักดี",
                    "bankName": "Kasikornbank",
                    "bankAccountNumber": "0123456789",
                    "bankBookImageUrl": "https://pandastore.com/docs/bankbook.jpg"
                }
                """;

        User user = new User();
        user.setUserId(userId);
        user.setUsername("customer1");
        user.setRole(UserRole.CUSTOMER);
        user.setStatus(UserStatus.ACTIVE);

        SellerApplication app = new SellerApplication();
        app.setApplicationId(10L);
        app.setUser(user);
        app.setShopName("Panda Craft Shop");
        app.setShopDescription("ร้านขายงานฝีมือคุณภาพดี ทำด้วยใจ");
        app.setShopPhone("0812345678");
        app.setShopEmail("pandacraft@gmail.com");
        app.setShopAddress("123 ถ.มิตรภาพ ต.ในเมือง อ.เมือง จ.ขอนแก่น 40000");
        app.setSellerFirstName("สมศักดิ์");
        app.setSellerLastName("รักดี");
        app.setIdCardNumber("1234567890123");
        app.setIdCardImageUrl("https://pandastore.com/docs/idcard.jpg");
        app.setBankAccountName("สมศักดิ์ รักดี");
        app.setBankName("Kasikornbank");
        app.setBankAccountNumber("0123456789");
        app.setBankBookImageUrl("https://pandastore.com/docs/bankbook.jpg");
        app.setStatus(SellerApplicationStatus.PENDING);
        app.setCreatedAt(LocalDateTime.now());

        when(sellerApplicationService.submitApplication(eq(userId), any(CreateSellerApplicationRequest.class)))
                .thenReturn(app);

        mockMvc.perform(post("/api/seller-applications")
                        .param("userId", String.valueOf(userId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.applicationId").value(10L))
                .andExpect(jsonPath("$.data.shopName").value("Panda Craft Shop"))
                .andExpect(jsonPath("$.data.status").value("PENDING"));
    }

    @Test
    @DisplayName("POST /api/seller-applications - Validation Error เมื่อเลขประจำตัวประชาชนไม่ครบ 13 หลัก")
    void submitApplication_InvalidIdCardNumber() throws Exception {
        Long userId = 2L;
        String invalidJson = """
                {
                    "shopName": "Panda Shop",
                    "shopDescription": "รายละเอียดร้านค้า",
                    "shopPhone": "0812345678",
                    "shopEmail": "shop@gmail.com",
                    "shopAddress": "ขอนแก่น",
                    "sellerFirstName": "สมศักดิ์",
                    "sellerLastName": "รักดี",
                    "idCardNumber": "123",
                    "idCardImageUrl": "https://pandastore.com/docs/idcard.jpg",
                    "bankAccountName": "สมศักดิ์ รักดี",
                    "bankName": "SCB",
                    "bankAccountNumber": "123456789",
                    "bankBookImageUrl": "https://pandastore.com/docs/bankbook.jpg"
                }
                """;

        mockMvc.perform(post("/api/seller-applications")
                        .param("userId", String.valueOf(userId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.idCardNumber").exists());
    }

    @Test
    @DisplayName("GET /api/seller-applications/{id} - ดึงรายละเอียดใบสมัครตาม ID สำเร็จ")
    void getApplicationById_Success() throws Exception {
        Long applicationId = 10L;
        SellerApplicationResponse response = new SellerApplicationResponse();
        response.setApplicationId(applicationId);
        response.setUserId(2L);
        response.setShopName("Panda Craft Shop");
        response.setStatus(SellerApplicationStatus.PENDING);
        response.setCreatedAt(LocalDateTime.now());

        when(sellerApplicationService.getApplicationResponseById(applicationId)).thenReturn(response);

        mockMvc.perform(get("/api/seller-applications/{id}", applicationId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.applicationId").value(applicationId))
                .andExpect(jsonPath("$.data.shopName").value("Panda Craft Shop"));
    }

    @Test
    @DisplayName("GET /api/seller-applications/{id} - ไม่พบใบสมัคร (404 Not Found)")
    void getApplicationById_NotFound() throws Exception {
        Long applicationId = 999L;

        when(sellerApplicationService.getApplicationResponseById(applicationId))
                .thenThrow(new ResourceNotFoundException("ไม่พบข้อมูลใบสมัคร ID: " + applicationId));

        mockMvc.perform(get("/api/seller-applications/{id}", applicationId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @DisplayName("GET /api/seller-applications/pending - Admin ดึงรายการใบสมัครที่รอตรวจสอบทั้งหมด")
    void getPendingApplications_Success() throws Exception {
        SellerApplicationResponse r1 = new SellerApplicationResponse();
        r1.setApplicationId(1L);
        r1.setShopName("Shop 1");
        r1.setStatus(SellerApplicationStatus.PENDING);

        SellerApplicationResponse r2 = new SellerApplicationResponse();
        r2.setApplicationId(2L);
        r2.setShopName("Shop 2");
        r2.setStatus(SellerApplicationStatus.PENDING);

        when(sellerApplicationService.getPendingApplicationResponses()).thenReturn(List.of(r1, r2));

        mockMvc.perform(get("/api/seller-applications/pending"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.length()").value(2))
                .andExpect(jsonPath("$.data[0].applicationId").value(1L))
                .andExpect(jsonPath("$.data[1].applicationId").value(2L));
    }

    @Test
    @DisplayName("PUT /api/seller-applications/{id}/approve - Admin อนุมัติใบสมัครเปิดร้านค้าสำเร็จ")
    void approveApplication_Success() throws Exception {
        Long applicationId = 10L;
        Long adminId = 1L;

        doNothing().when(sellerApplicationService).approveApplication(applicationId, adminId);

        mockMvc.perform(put("/api/seller-applications/{id}/approve", applicationId)
                        .param("adminId", String.valueOf(adminId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("อนุมัติคำขอเปิดร้านค้าสำเร็จ"));
    }

    @Test
    @DisplayName("PUT /api/seller-applications/{id}/reject - Admin ปฏิเสธคำขอเปิดร้านค้าสำเร็จ")
    void rejectApplication_Success() throws Exception {
        Long applicationId = 10L;
        Long adminId = 1L;
        String requestJson = """
                {
                    "reason": "ภาพถ่ายบัตรประชาชนไม่ชัดเจน ไม่สามารถอ่านข้อมูลได้"
                }
                """;

        doNothing().when(sellerApplicationService)
                .rejectApplication(eq(applicationId), eq(adminId), eq("ภาพถ่ายบัตรประชาชนไม่ชัดเจน ไม่สามารถอ่านข้อมูลได้"));

        mockMvc.perform(put("/api/seller-applications/{id}/reject", applicationId)
                        .param("adminId", String.valueOf(adminId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("ปฏิเสธคำขอเปิดร้านค้าสำเร็จ"));
    }

    @Test
    @DisplayName("PUT /api/seller-applications/{id}/reject - Validation Error เมื่อไม่ได้ระบุเหตุผลการปฏิเสธ")
    void rejectApplication_MissingReason_ValidationError() throws Exception {
        Long applicationId = 10L;
        Long adminId = 1L;
        String invalidJson = """
                {
                    "reason": ""
                }
                """;

        mockMvc.perform(put("/api/seller-applications/{id}/reject", applicationId)
                        .param("adminId", String.valueOf(adminId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.reason").exists());
    }

    @Test
    @DisplayName("PUT /api/seller-applications/{id}/request-docs - Admin ส่งคำขอเอกสารเพิ่มเติมสำเร็จ")
    void requestMoreDocuments_Success() throws Exception {
        Long applicationId = 10L;
        Long adminId = 1L;
        String requestJson = """
                {
                    "message": "กรุณาแนบรูปภาพหน้าแรกสมุดบัญชีธนาคารที่มีชื่อบัญชีชัดเจนเพิ่มเติม"
                }
                """;

        doNothing().when(sellerApplicationService)
                .requestMoreDocuments(eq(applicationId), eq(adminId), eq("กรุณาแนบรูปภาพหน้าแรกสมุดบัญชีธนาคารที่มีชื่อบัญชีชัดเจนเพิ่มเติม"));

        mockMvc.perform(put("/api/seller-applications/{id}/request-docs", applicationId)
                        .param("adminId", String.valueOf(adminId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("ส่งคำขอเอกสารเพิ่มเติมสำเร็จ"));
    }

    @Test
    @DisplayName("PUT /api/seller-applications/{id}/request-docs - Validation Error เมื่อไม่ได้ระบุข้อความขอเอกสาร")
    void requestMoreDocuments_MissingMessage_ValidationError() throws Exception {
        Long applicationId = 10L;
        Long adminId = 1L;
        String invalidJson = """
                {
                    "message": ""
                }
                """;

        mockMvc.perform(put("/api/seller-applications/{id}/request-docs", applicationId)
                        .param("adminId", String.valueOf(adminId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.message").exists());
    }
}

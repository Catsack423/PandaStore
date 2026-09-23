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
import project.project.Entity.seller.Seller;
import project.project.Entity.seller.SellerBankAccount;
import project.project.Entity.seller.SellerStatus;
import project.project.Service.api.SellerShopService;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
public class SellerShopControllerTest {

    private MockMvc mockMvc;

    @Mock
    private SellerShopService sellerShopService;

    @InjectMocks
    private SellerShopController sellerShopController;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(sellerShopController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("GET /api/seller/shops/{sellerId} - คืนค่าข้อมูลร้านค้าสำเร็จ")
    void getShopBySellerId_Success() throws Exception {
        Long sellerId = 1L;
        Seller mockSeller = new Seller();
        mockSeller.setSellerId(sellerId);
        mockSeller.setShopName("Panda Official Shop");
        mockSeller.setStatus(SellerStatus.ACTIVE);

        when(sellerShopService.getShopBySellerId(sellerId)).thenReturn(mockSeller);

        mockMvc.perform(get("/api/seller/shops/{sellerId}", sellerId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("ดึงข้อมูลร้านค้าสำเร็จ"))
                .andExpect(jsonPath("$.data.sellerId").value(sellerId))
                .andExpect(jsonPath("$.data.shopName").value("Panda Official Shop"));
    }

    @Test
    @DisplayName("GET /api/seller/shops/{sellerId} - เมื่อไม่พบร้านค้า คืนค่า Error จาก GlobalExceptionHandler")
    void getShopBySellerId_NotFound() throws Exception {
        Long sellerId = 999L;
        when(sellerShopService.getShopBySellerId(sellerId))
                .thenThrow(new RuntimeException("ไม่พบร้านค้า sellerId: 999"));

        mockMvc.perform(get("/api/seller/shops/{sellerId}", sellerId))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("ไม่พบร้านค้า sellerId: 999"));
    }

    @Test
    @DisplayName("PUT /api/seller/shops/{sellerId} - อัปเดตข้อมูลร้านค้าสำเร็จ")
    void updateShopProfile_Success() throws Exception {
        Long sellerId = 1L;
        String requestJson = "{" +
                "\"shopName\":\"Updated Shop\"," +
                "\"shopDescription\":\"Shop desc\"," +
                "\"shopPhone\":\"0812345678\"," +
                "\"shopEmail\":\"shop@test.com\"," +
                "\"shopAddress\":\"123 Bangkok\"" +
                "}";

        Seller updatedSeller = new Seller();
        updatedSeller.setSellerId(sellerId);
        updatedSeller.setShopName("Updated Shop");
        updatedSeller.setShopPhone("0812345678");

        when(sellerShopService.updateShopProfile(eq(sellerId), any(Seller.class)))
                .thenReturn(updatedSeller);

        mockMvc.perform(put("/api/seller/shops/{sellerId}", sellerId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("อัปเดตข้อมูลร้านค้าสำเร็จ"))
                .andExpect(jsonPath("$.data.shopName").value("Updated Shop"));
    }

    @Test
    @DisplayName("PUT /api/seller/shops/{sellerId} - Validation Error เมื่อส่งข้อมูลไม่ถูกต้อง")
    void updateShopProfile_ValidationError() throws Exception {
        Long sellerId = 1L;
        String invalidJson = "{" +
                "\"shopName\":\"\"," +
                "\"shopDescription\":\"\"," +
                "\"shopPhone\":\"\"," +
                "\"shopEmail\":\"invalid-email\"," +
                "\"shopAddress\":\"\"" +
                "}";

        mockMvc.perform(put("/api/seller/shops/{sellerId}", sellerId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("ข้อมูลที่ส่งมาไม่ถูกต้อง"))
                .andExpect(jsonPath("$.error.shopName").exists())
                .andExpect(jsonPath("$.error.shopEmail").exists());
    }

    @Test
    @DisplayName("PUT /api/seller/shops/{sellerId}/bank-account - บันทึกข้อมูลบัญชีธนาคารสำเร็จ")
    void updateBankAccount_Success() throws Exception {
        Long sellerId = 1L;
        String requestJson = "{" +
                "\"bankName\":\"Kasikornbank\"," +
                "\"accountNumber\":\"1234567890\"," +
                "\"accountName\":\"Panda Shop\"," +
                "\"proofImageUrl\":\"https://img.com/proof.jpg\"" +
                "}";

        SellerBankAccount savedAccount = new SellerBankAccount();
        savedAccount.setBankAccountId(10L);
        savedAccount.setBankName("Kasikornbank");
        savedAccount.setAccountNumber("1234567890");

        when(sellerShopService.addOrUpdateBankAccount(eq(sellerId), any(SellerBankAccount.class)))
                .thenReturn(savedAccount);

        mockMvc.perform(put("/api/seller/shops/{sellerId}/bank-account", sellerId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("บันทึกข้อมูลบัญชีธนาคารสำเร็จ"))
                .andExpect(jsonPath("$.data.bankName").value("Kasikornbank"));
    }
}

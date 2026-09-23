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
import project.project.Entity.product.Product;
import project.project.Entity.product.ProductStatus;
import project.project.Service.api.ProductService;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
public class ProductControllerTest {

    private MockMvc mockMvc;

    @Mock
    private ProductService productService;

    @InjectMocks
    private ProductController productController;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(productController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("POST /api/products - สร้างสินค้าใหม่สำเร็จ (Random Test Data)")
    void createProduct_Success() throws Exception {
        Long sellerId = 1L;
        String requestJson = """
                {
                    "name": "Panda Gaming T-Shirt Limited Edition",
                    "description": "เสื้อยืดสกรีนลายแพนด้า ผ้า Cotton 100% ระบายอากาศดีเยี่ยม",
                    "price": 350.00,
                    "stock": 50,
                    "shippingInfo": "Standard Express จัดส่งภายใน 1-3 วัน",
                    "imageUrls": ["https://images.unsplash.com/photo-1521572267360-ee0c2909d518"]
                }
                """;

        Product created = new Product();
        created.setProductId(101L);
        created.setName("Panda Gaming T-Shirt Limited Edition");
        created.setPrice(new BigDecimal("350.00"));
        created.setStock(50);
        created.setStatus(ProductStatus.ACTIVE);

        when(productService.createProduct(eq(sellerId), any(Product.class), any())).thenReturn(created);

        mockMvc.perform(post("/api/products")
                        .param("sellerId", String.valueOf(sellerId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.productId").value(101L))
                .andExpect(jsonPath("$.data.name").value("Panda Gaming T-Shirt Limited Edition"));
    }

    @Test
    @DisplayName("GET /api/products/{id} - ดึงรายละเอียดสินค้าสำเร็จ")
    void getProductById_Success() throws Exception {
        Long productId = 101L;
        Product product = new Product();
        product.setProductId(productId);
        product.setName("Panda Hoodie Premium");
        product.setPrice(new BigDecimal("890.00"));
        product.setStock(20);
        product.setStatus(ProductStatus.ACTIVE);

        when(productService.getProductById(productId)).thenReturn(product);

        mockMvc.perform(get("/api/products/{id}", productId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.productId").value(productId))
                .andExpect(jsonPath("$.data.name").value("Panda Hoodie Premium"));
    }

    @Test
    @DisplayName("GET /api/products - ดึงสินค้าที่เปิดขายทั้งหมด")
    void getAllActiveProducts_Success() throws Exception {
        Product p1 = new Product();
        p1.setProductId(1L);
        p1.setName("Product A");

        when(productService.getAllActiveProducts()).thenReturn(List.of(p1));

        mockMvc.perform(get("/api/products"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].productId").value(1L));
    }

    @Test
    @DisplayName("GET /api/products/search - ค้นหาสินค้าตาม Keyword")
    void searchProducts_Success() throws Exception {
        Product p1 = new Product();
        p1.setProductId(1L);
        p1.setName("Panda Cap");

        when(productService.searchProducts(eq("panda"), any())).thenReturn(List.of(p1));

        mockMvc.perform(get("/api/products/search")
                        .param("keyword", "panda"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].name").value("Panda Cap"));
    }

    @Test
    @DisplayName("PUT /api/products/{id} - อัปเดตข้อมูลสินค้าสำเร็จ")
    void updateProduct_Success() throws Exception {
        Long productId = 101L;
        Long sellerId = 1L;
        String updateJson = """
                {
                    "name": "Panda Gaming T-Shirt V2 (Updated)",
                    "price": 320.00,
                    "stock": 45,
                    "status": "ACTIVE"
                }
                """;

        Product updated = new Product();
        updated.setProductId(productId);
        updated.setName("Panda Gaming T-Shirt V2 (Updated)");
        updated.setPrice(new BigDecimal("320.00"));
        updated.setStock(45);
        updated.setStatus(ProductStatus.ACTIVE);

        when(productService.updateProduct(eq(sellerId), eq(productId), any(Product.class))).thenReturn(updated);

        mockMvc.perform(put("/api/products/{id}", productId)
                        .param("sellerId", String.valueOf(sellerId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.name").value("Panda Gaming T-Shirt V2 (Updated)"))
                .andExpect(jsonPath("$.data.price").value(320.00));
    }

    @Test
    @DisplayName("POST /api/products/{id}/deduct-stock - ตัดสต็อกสินค้า")
    void deductStock_Success() throws Exception {
        Long productId = 101L;
        int qty = 2;

        doNothing().when(productService).validateAndDeductStock(productId, qty);

        mockMvc.perform(post("/api/products/{id}/deduct-stock", productId)
                        .param("quantity", String.valueOf(qty)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @DisplayName("POST /api/products/{id}/restore-stock - คืนสต็อกสินค้า")
    void restoreStock_Success() throws Exception {
        Long productId = 101L;
        int qty = 2;

        doNothing().when(productService).restoreStock(productId, qty);

        mockMvc.perform(post("/api/products/{id}/restore-stock", productId)
                        .param("quantity", String.valueOf(qty)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }
}

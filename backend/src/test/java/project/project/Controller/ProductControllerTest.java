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
import project.project.Entity.product.Category;
import project.project.Entity.product.Product;
import project.project.Entity.product.ProductStatus;
import project.project.Service.api.ProductService;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import project.project.DTO.product.ProductResponse;
import project.project.Entity.seller.Seller;
import project.project.Entity.seller.SellerStatus;
import project.project.Entity.user.UserRole;
import project.project.Repository.SellerRepository;
import project.project.Security.AuthenticatedUser;
import project.project.Security.CurrentUser;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import java.util.Optional;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
public class ProductControllerTest {

    private MockMvc mockMvc;

    @Mock
    private ProductService productService;

    @Mock
    private CurrentUser currentUser;

    @Mock
    private SellerRepository sellers;

    @InjectMocks
    private ProductController productController;

    @BeforeEach
    void setUp() {
        Seller shop = new Seller();
        shop.setSellerId(1L);
        shop.setStatus(SellerStatus.ACTIVE);
        lenient().when(currentUser.requireIdentity()).thenReturn(new AuthenticatedUser(10L, UserRole.SELLER));
        lenient().when(sellers.findByUser_UserId(10L)).thenReturn(Optional.of(shop));
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

        when(productService.createProduct(eq(sellerId), any(Product.class), any(), any())).thenReturn(created);

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
    @DisplayName("DEF-007: POST /api/products - ส่ง categoryIds แล้วได้รับ category_id / categoryIds ในผลลัพธ์")
    void createProduct_WithCategories_Success() throws Exception {
        Long sellerId = 1L;
        String requestJson = """
                {
                    "name": "Panda Gaming T-Shirt Limited Edition",
                    "description": "เสื้อยืดสกรีนลายแพนด้า",
                    "price": 350.00,
                    "stock": 50,
                    "categoryIds": [1],
                    "imageUrls": ["https://images.unsplash.com/photo-1521572267360-ee0c2909d518"]
                }
                """;

        Product created = new Product();
        created.setProductId(101L);
        created.setName("Panda Gaming T-Shirt Limited Edition");
        created.setPrice(new BigDecimal("350.00"));
        created.setStock(50);
        created.setStatus(ProductStatus.ACTIVE);
        Category category = new Category(1L, "เสื้อผ้า", "หมวดหมู่เสื้อผ้า");
        created.setCategories(Set.of(category));

        when(productService.createProduct(eq(sellerId), any(Product.class), any(), any())).thenReturn(created);

        mockMvc.perform(post("/api/products")
                        .param("sellerId", String.valueOf(sellerId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.categoryIds[0]").value(1L))
                .andExpect(jsonPath("$.data.category_id[0]").value(1L));
    }

    @Test
    @DisplayName("DEF-008: POST /api/products - ส่ง Category ที่ไม่มีในระบบ คืนค่า 400 Bad Request 'Categoryนี้ไม่มีในระบบ'")
    void createProduct_NonExistentCategory_BadRequest() throws Exception {
        Long sellerId = 1L;
        String requestJson = """
                {
                    "name": "Panda Gaming T-Shirt",
                    "description": "เสื้อยืดสกรีนลายแพนด้า",
                    "price": 350.00,
                    "stock": 50,
                    "categoryIds": [999],
                    "imageUrls": ["https://images.unsplash.com/photo-1521572267360-ee0c2909d518"]
                }
                """;

        when(productService.createProduct(eq(sellerId), any(Product.class), any(), any()))
                .thenThrow(new IllegalArgumentException("Category not found: 999999"));

        mockMvc.perform(post("/api/products")
                        .param("sellerId", String.valueOf(sellerId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Category not found: 999999"));
    }

    @Test
    @DisplayName("DEF-009: POST /api/products - ส่ง imageUrls เป็น empty string [\"\"] คืนค่า 400 Bad Request")
    void createProduct_EmptyImage_BadRequest() throws Exception {
        Long sellerId = 1L;
        String requestJson = """
                {
                    "name": "Panda Gaming T-Shirt",
                    "description": "เสื้อยืดสกรีนลายแพนด้า",
                    "price": 350.00,
                    "stock": 50,
                    "imageUrls": [""]
                }
                """;

        when(productService.createProduct(eq(sellerId), any(Product.class), any(), any()))
                .thenThrow(new IllegalArgumentException("At least one product image is required"));

        mockMvc.perform(post("/api/products")
                        .param("sellerId", String.valueOf(sellerId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("At least one product image is required"));
    }

    @Test
    @DisplayName("DEF-010: POST /api/products - ส่ง imageUrls เป็น whitespace [\" \"] คืนค่า 400 Bad Request")
    void createProduct_WhitespaceImage_BadRequest() throws Exception {
        Long sellerId = 1L;
        String requestJson = """
                {
                    "name": "Panda Gaming T-Shirt",
                    "description": "เสื้อยืดสกรีนลายแพนด้า",
                    "price": 350.00,
                    "stock": 50,
                    "imageUrls": ["   "]
                }
                """;

        when(productService.createProduct(eq(sellerId), any(Product.class), any(), any()))
                .thenThrow(new IllegalArgumentException("At least one product image is required"));

        mockMvc.perform(post("/api/products")
                        .param("sellerId", String.valueOf(sellerId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("At least one product image is required"));
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

        Page<ProductResponse> page = new PageImpl<>(List.of(ProductResponse.fromEntity(p1)));
        when(productService.searchProductsPage(eq("panda"), any(), any(), any(), any(), anyInt(), anyInt()))
                .thenReturn(page);

        mockMvc.perform(get("/api/products/search")
                        .param("keyword", "panda"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.items[0].name").value("Panda Cap"));
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

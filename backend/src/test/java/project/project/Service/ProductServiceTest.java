package project.project.Service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import project.project.Entity.product.Product;
import project.project.Entity.product.ProductImage;
import project.project.Entity.product.ProductStatus;
import project.project.Entity.seller.Seller;
import project.project.Exception.InsufficientStockException;
import project.project.Exception.ResourceNotFoundException;
import project.project.Repository.CategoryRepository;
import project.project.Repository.ProductImageRepository;
import project.project.Repository.ProductRepository;
import project.project.Repository.SellerRepository;
import project.project.Service.implement.ProductServiceImp;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ProductServiceTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private ProductImageRepository productImageRepository;

    @Mock
    private SellerRepository sellerRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @InjectMocks
    private ProductServiceImp productService;

    private Seller sampleSeller;
    private Product sampleProduct;

    @BeforeEach
    void setUp() {
        sampleSeller = new Seller();
        sampleSeller.setSellerId(1L);
        sampleSeller.setShopName("สมชาย Shop");

        sampleProduct = new Product();
        sampleProduct.setProductId(100L);
        sampleProduct.setSeller(sampleSeller);
        sampleProduct.setName("คีย์บอร์ดกลไก");
        sampleProduct.setDescription("Mechanical Keyboard RGB");
        sampleProduct.setPrice(new BigDecimal("1500.00"));
        sampleProduct.setStock(10);
        sampleProduct.setStatus(ProductStatus.ACTIVE);
    }

    @Test
    @DisplayName("UC4: ผู้ขายสร้างสินค้าสำเร็จ (ราคา > 0, สต็อก >= 0, มีรูปภาพอย่างน้อย 1 รูป)")
    void testCreateProduct_Success() {
        List<String> images = List.of("https://cdn.example.com/img1.jpg", "https://cdn.example.com/img2.jpg");

        when(sellerRepository.findById(1L)).thenReturn(Optional.of(sampleSeller));
        when(productRepository.save(any(Product.class))).thenAnswer(invocation -> {
            Product p = invocation.getArgument(0);
            p.setProductId(100L);
            return p;
        });
        when(productImageRepository.save(any(ProductImage.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Product result = productService.createProduct(1L, sampleProduct, images);

        assertNotNull(result);
        assertEquals(100L, result.getProductId());
        assertEquals(ProductStatus.ACTIVE, result.getStatus());
        assertEquals(sampleSeller, result.getSeller());
        assertEquals(2, result.getImages().size());
        assertTrue(result.getImages().get(0).getIsPrimary());
        assertFalse(result.getImages().get(1).getIsPrimary());

        verify(productRepository, times(1)).save(sampleProduct);
        verify(productImageRepository, times(2)).save(any(ProductImage.class));
    }

    @Test
    @DisplayName("UC4-12A: สร้างสินค้าด้วยราคา <= 0 ต้องโยน IllegalArgumentException")
    void testCreateProduct_ZeroOrNegativePrice_ThrowsException() {
        sampleProduct.setPrice(BigDecimal.ZERO);
        when(sellerRepository.findById(1L)).thenReturn(Optional.of(sampleSeller));

        assertThrows(IllegalArgumentException.class, () -> {
            productService.createProduct(1L, sampleProduct, List.of("https://cdn.example.com/img1.jpg"));
        });

        verify(productRepository, never()).save(any());
    }

    @Test
    @DisplayName("UC4-13A: สร้างสินค้าด้วยสต็อกติดลบ ต้องโยน IllegalArgumentException")
    void testCreateProduct_NegativeStock_ThrowsException() {
        sampleProduct.setStock(-1);
        when(sellerRepository.findById(1L)).thenReturn(Optional.of(sampleSeller));

        assertThrows(IllegalArgumentException.class, () -> {
            productService.createProduct(1L, sampleProduct, List.of("https://cdn.example.com/img1.jpg"));
        });

        verify(productRepository, never()).save(any());
    }

    @Test
    @DisplayName("UC4-2: สร้างสินค้าโดยไม่มีรูปภาพ ต้องโยน IllegalArgumentException")
    void testCreateProduct_NoImages_ThrowsException() {
        when(sellerRepository.findById(1L)).thenReturn(Optional.of(sampleSeller));

        assertThrows(IllegalArgumentException.class, () -> {
            productService.createProduct(1L, sampleProduct, List.of());
        });

        verify(productRepository, never()).save(any());
    }

    @Test
    @DisplayName("ตัดสต็อกสินค้าสำเร็จ และเปลี่ยนเป็น OUT_OF_STOCK เมื่อสต็อกเหลือ 0")
    void testValidateAndDeductStock_Success_SetsOutOfStockWhenZero() {
        sampleProduct.setStock(2);
        when(productRepository.findById(100L)).thenReturn(Optional.of(sampleProduct));
        when(productRepository.save(any(Product.class))).thenAnswer(invocation -> invocation.getArgument(0));

        productService.validateAndDeductStock(100L, 2);

        assertEquals(0, sampleProduct.getStock());
        assertEquals(ProductStatus.OUT_OF_STOCK, sampleProduct.getStatus());
        verify(productRepository, times(1)).save(sampleProduct);
    }

    @Test
    @DisplayName("ตัดสต็อกมากกว่าที่มี ต้องโยน InsufficientStockException")
    void testValidateAndDeductStock_InsufficientStock_ThrowsException() {
        sampleProduct.setStock(5);
        when(productRepository.findById(100L)).thenReturn(Optional.of(sampleProduct));

        assertThrows(InsufficientStockException.class, () -> {
            productService.validateAndDeductStock(100L, 10);
        });

        verify(productRepository, never()).save(any());
    }

    @Test
    @DisplayName("คืนสต็อกสินค้าสำเร็จ และเปลี่ยนจาก OUT_OF_STOCK กลับเป็น ACTIVE")
    void testRestoreStock_Success_RestoresToActive() {
        sampleProduct.setStock(0);
        sampleProduct.setStatus(ProductStatus.OUT_OF_STOCK);

        when(productRepository.findById(100L)).thenReturn(Optional.of(sampleProduct));
        when(productRepository.save(any(Product.class))).thenAnswer(invocation -> invocation.getArgument(0));

        productService.restoreStock(100L, 5);

        assertEquals(5, sampleProduct.getStock());
        assertEquals(ProductStatus.ACTIVE, sampleProduct.getStatus());
        verify(productRepository, times(1)).save(sampleProduct);
    }

    @Test
    @DisplayName("อัปเดตคะแนนเฉลี่ย (averageRating) คำนวณถูกต้องตามสัดส่วน")
    void testUpdateAverageRating_CalculatesCorrectly() {
        sampleProduct.setAverageRating(new BigDecimal("4.00"));
        sampleProduct.setReviewCount(2); // sum = 8

        when(productRepository.findById(100L)).thenReturn(Optional.of(sampleProduct));
        when(productRepository.save(any(Product.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Adding rating 5: new average = (8 + 5) / 3 = 4.33
        productService.updateAverageRating(100L, 5);

        assertEquals(3, sampleProduct.getReviewCount());
        assertEquals(new BigDecimal("4.33"), sampleProduct.getAverageRating());
        verify(productRepository, times(1)).save(sampleProduct);
    }

    @Test
    @DisplayName("แก้ไขข้อมูลสินค้า: ผู้ขายไม่ใช่เจ้าของสินค้า ต้องโยน IllegalArgumentException")
    void testUpdateProduct_UnauthorizedSeller_ThrowsException() {
        when(productRepository.findById(100L)).thenReturn(Optional.of(sampleProduct));

        Product updatedInfo = new Product();
        updatedInfo.setName("ชื่อใหม่");

        // sellerId 2L is not sampleSeller (1L)
        assertThrows(IllegalArgumentException.class, () -> {
            productService.updateProduct(2L, 100L, updatedInfo);
        });

        verify(productRepository, never()).save(any());
    }
}

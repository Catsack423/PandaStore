package project.project.Service.api;

import project.project.Entity.product.Product;
import java.util.List;
import java.util.Set;
import java.math.BigDecimal;
import org.springframework.data.domain.Page;
import project.project.DTO.product.ProductResponse;
import project.project.DTO.product.CatalogSummary;

public interface ProductService {
    CatalogSummary getCatalogSummary();
    Product createProduct(Long sellerId, Product product, List<String> imageUrls, Set<Long> categoryIds);

    default Product createProduct(Long sellerId, Product product, List<String> imageUrls) {
        return createProduct(sellerId, product, imageUrls, null);
    }
    Product updateProduct(Long sellerId, Long productId, Product updatedProduct);
    Product getProductById(Long productId);
    List<Product> getAllActiveProducts();
    List<Product> getProductsBySeller(Long sellerId);
    List<Product> searchProducts(String keyword, Long categoryId);
    Page<ProductResponse> searchProductsPage(String keyword, Long categoryId, int page, int size);
    Page<ProductResponse> searchProductsPage(String keyword, List<Long> categoryIds,
            BigDecimal minPrice, BigDecimal maxPrice, String sort, int page, int size);
    void validateAndDeductStock(Long productId, Integer quantity);
    void restoreStock(Long productId, Integer quantity);
    void updateAverageRating(Long productId, Integer newRating);
}

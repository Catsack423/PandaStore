package project.project.Service.api;

import project.project.Entity.product.Product;
import java.util.List;

public interface ProductService {
    Product createProduct(Long sellerId, Product product, List<String> imageUrls);
    Product updateProduct(Long sellerId, Long productId, Product updatedProduct);
    Product getProductById(Long productId);
    List<Product> getAllActiveProducts();
    List<Product> getProductsBySeller(Long sellerId);
    List<Product> searchProducts(String keyword, Long categoryId);
    void validateAndDeductStock(Long productId, Integer quantity);
    void restoreStock(Long productId, Integer quantity);
    void updateAverageRating(Long productId, Integer newRating);
}

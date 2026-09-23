package project.project.Repository;

import org.springframework.data.jpa.repository.JpaRepository;
import project.project.Entity.product.ProductImage;

import java.util.List;

public interface ProductImageRepository extends JpaRepository<ProductImage, Long> {
    List<ProductImage> findByProduct_ProductIdOrderByDisplayOrderAsc(Long productId);
    void deleteByProduct_ProductId(Long productId);
}

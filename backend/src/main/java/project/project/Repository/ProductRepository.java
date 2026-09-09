package project.project.Repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import project.project.Entity.product.Product;
import project.project.Entity.product.ProductStatus;

import java.util.List;

public interface ProductRepository extends JpaRepository<Product, Long> {

    List<Product> findByCategories_CategoryId(Long categoryId);

    List<Product> findByStatus(ProductStatus status);

    List<Product> findBySeller_SellerId(Long sellerId);

    List<Product> findByCategories_CategoryIdAndStatus(Long categoryId, ProductStatus status);

    @Query("SELECT p FROM Product p WHERE p.status = :status " +
           "AND (:keyword IS NULL OR LOWER(p.name) LIKE LOWER(CONCAT('%', :keyword, '%')) OR LOWER(p.description) LIKE LOWER(CONCAT('%', :keyword, '%'))) " +
           "AND (:categoryId IS NULL OR EXISTS (SELECT c FROM p.categories c WHERE c.categoryId = :categoryId))")
    List<Product> searchProducts(@Param("keyword") String keyword, @Param("categoryId") Long categoryId, @Param("status") ProductStatus status);
}

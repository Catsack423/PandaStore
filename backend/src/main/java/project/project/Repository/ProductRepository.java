package project.project.Repository;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import project.project.Entity.product.Product;
import project.project.Entity.product.ProductStatus;

import java.util.List;
import java.util.Optional;

public interface ProductRepository extends JpaRepository<Product, Long> {

    List<Product> findByCategories_CategoryId(Long categoryId);

    List<Product> findByStatus(ProductStatus status);

    List<Product> findBySeller_SellerId(Long sellerId);

    List<Product> findByCategories_CategoryIdAndStatus(
            Long categoryId, ProductStatus status);

    @Query("""
            SELECT p FROM Product p
            WHERE p.status = :status
              AND (
                  :keyword IS NULL
                  OR LOWER(p.name) LIKE LOWER(CONCAT('%', :keyword, '%'))
                  OR LOWER(p.description) LIKE LOWER(CONCAT('%', :keyword, '%'))
              )
              AND (
                  :categoryId IS NULL
                  OR EXISTS (
                      SELECT c FROM p.categories c
                      WHERE c.categoryId = :categoryId
                  )
              )
            """)
    List<Product> searchProducts(
            @Param("keyword") String keyword,
            @Param("categoryId") Long categoryId,
            @Param("status") ProductStatus status);

    @Query(value = """
            SELECT p FROM Product p
            WHERE p.status = :status
              AND (:keyword IS NULL
                   OR LOWER(p.name) LIKE LOWER(CONCAT('%', :keyword, '%'))
                   OR LOWER(p.description) LIKE LOWER(CONCAT('%', :keyword, '%')))
              AND (:categoryId IS NULL
                   OR EXISTS (SELECT c FROM p.categories c WHERE c.categoryId = :categoryId))
            """, countQuery = """
            SELECT COUNT(p) FROM Product p
            WHERE p.status = :status
              AND (:keyword IS NULL
                   OR LOWER(p.name) LIKE LOWER(CONCAT('%', :keyword, '%'))
                   OR LOWER(p.description) LIKE LOWER(CONCAT('%', :keyword, '%')))
              AND (:categoryId IS NULL
                   OR EXISTS (SELECT c FROM p.categories c WHERE c.categoryId = :categoryId))
            """)
    Page<Product> searchProductsPage(
            @Param("keyword") String keyword,
            @Param("categoryId") Long categoryId,
            @Param("status") ProductStatus status,
            Pageable pageable);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Product p where p.productId = :productId")
    Optional<Product> findByIdForUpdate(
            @Param("productId") Long productId);
}

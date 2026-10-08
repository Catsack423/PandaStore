package project.project.Repository;

import org.springframework.data.jpa.repository.JpaRepository;
import project.project.Entity.review.Review;

import java.util.List;
import java.util.Optional;

public interface ReviewRepository extends JpaRepository<Review, Long> {
    List<Review> findByProduct_ProductId(Long productId);
    List<Review> findByProduct_ProductIdOrderByCreatedAtDesc(Long productId);
    Optional<Review> findByOrderItem_OrderItemId(Long orderItemId);
    boolean existsByOrderItem_OrderItemId(Long orderItemId);
}

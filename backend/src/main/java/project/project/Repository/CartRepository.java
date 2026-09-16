package project.project.Repository;

import org.springframework.data.jpa.repository.JpaRepository;
import project.project.Entity.order.Cart;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;

public interface CartRepository extends JpaRepository<Cart, Long> {
    @EntityGraph(attributePaths = {"items", "items.product", "items.product.seller", "items.product.seller.user"})
    Optional<Cart> findByCustomer_CustomerId(Long customerId);
}

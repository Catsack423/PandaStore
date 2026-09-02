package project.project.Repository;

import org.springframework.data.jpa.repository.JpaRepository;
import project.project.Entity.order.CartItem;

public interface CartItemRepository extends JpaRepository<CartItem, Long> {

}

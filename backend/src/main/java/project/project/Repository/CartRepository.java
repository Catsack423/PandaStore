package project.project.Repository;

import org.springframework.data.jpa.repository.JpaRepository;
import project.project.Entity.order.Cart;

public interface CartRepository extends JpaRepository<Cart, Long> {

}

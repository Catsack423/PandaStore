package project.project.Repository;

import org.springframework.data.jpa.repository.JpaRepository;
import project.project.Entity.order.OrderItem;

public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {

}

package project.project.Repository;

import org.springframework.data.jpa.repository.JpaRepository;
import project.project.Entity.order.Order;

public interface OrderRepository extends JpaRepository<Order, Long> {

}

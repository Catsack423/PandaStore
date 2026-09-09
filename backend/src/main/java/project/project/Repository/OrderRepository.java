package project.project.Repository;

import org.springframework.data.jpa.repository.JpaRepository;
import project.project.Entity.order.Order;
import project.project.Entity.order.OrderStatus;

import java.time.LocalDateTime;
import java.util.List;

public interface OrderRepository extends JpaRepository<Order, Long> {
    List<Order> findByOrderGroup_OrderGroupId(Long orderGroupId);
    List<Order> findBySeller_SellerId(Long sellerId);
    List<Order> findByOrderStatusAndShippedAtBefore(OrderStatus status, LocalDateTime before);
}

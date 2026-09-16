package project.project.Repository;

import org.springframework.data.jpa.repository.JpaRepository;
import project.project.Entity.order.OrderGroup;

import java.util.List;
import java.util.Optional;

public interface OrderGroupRepository extends JpaRepository<OrderGroup, Long> {
    Optional<OrderGroup> findByGroupNumber(String groupNumber);
    List<OrderGroup> findByCustomer_CustomerId(Long customerId);
}

package project.project.Repository;

import org.springframework.data.jpa.repository.JpaRepository;
import project.project.Entity.order.OrderGroup;

public interface OrderGroupRepository extends JpaRepository<OrderGroup, Long> {

}

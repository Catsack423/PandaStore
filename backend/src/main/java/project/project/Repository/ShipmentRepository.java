package project.project.Repository;

import org.springframework.data.jpa.repository.JpaRepository;
import project.project.Entity.order.Shipment;

public interface ShipmentRepository extends JpaRepository<Shipment, Long> {

}

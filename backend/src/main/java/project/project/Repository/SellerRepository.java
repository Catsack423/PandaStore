package project.project.Repository;

import org.springframework.data.jpa.repository.JpaRepository;
import project.project.Entity.seller.Seller;

public interface SellerRepository extends JpaRepository<Seller, Long> {

}

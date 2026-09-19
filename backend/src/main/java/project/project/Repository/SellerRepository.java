package project.project.Repository;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import project.project.Entity.seller.Seller;

public interface SellerRepository extends JpaRepository<Seller, Long> {

    boolean existsByShopName(String shopName);
    Optional<Seller> findByUser_UserId(Long userId);
}

package project.project.Repository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import project.project.Entity.seller.SellerBankAccount;

public interface SellerBankAccountRepository extends JpaRepository<SellerBankAccount, Long> {

    List<SellerBankAccount> findBySeller_SellerId(Long sellerId);
    Optional<SellerBankAccount> findFirstBySeller_SellerId(Long sellerId);
}

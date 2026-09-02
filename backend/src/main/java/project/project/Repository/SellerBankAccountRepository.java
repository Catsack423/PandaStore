package project.project.Repository;

import org.springframework.data.jpa.repository.JpaRepository;
import project.project.Entity.seller.SellerBankAccount;

public interface SellerBankAccountRepository extends JpaRepository<SellerBankAccount, Long> {

}

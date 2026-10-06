package project.project.Repository;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import project.project.Entity.user.Address;

import java.util.List;
import java.util.Optional;

public interface AddressRepository extends JpaRepository<Address, Long> {

    List<Address> findByCustomer_CustomerIdOrderByIsDefaultDescAddressIdAsc(
            Long customerId);

    @Query("""
            select a from Address a
            where a.addressId = :addressId
              and a.customer.customerId = :customerId
            """)
    Optional<Address> findOwnedAddressForUpdate(
            @Param("customerId") Long customerId,
            @Param("addressId") Long addressId);
}
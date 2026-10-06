package project.project.Repository;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import project.project.Entity.order.OrderGroup;

import java.util.List;
import java.util.Optional;

public interface OrderGroupRepository extends JpaRepository<OrderGroup, Long> {

    Optional<OrderGroup> findByGroupNumber(String groupNumber);

    List<OrderGroup> findByCustomer_CustomerId(Long customerId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select g from OrderGroup g
            where g.orderGroupId = :id
            """)
    Optional<OrderGroup> findByIdForUpdate(@Param("id") Long id);

    @EntityGraph(attributePaths = {
            "customer.user",
            "shippingAddress",
            "payment",
            "subOrders.seller.user"
    })
    @Query("""
            select g from OrderGroup g
            where g.orderGroupId = :id
            """)
    Optional<OrderGroup> findDetailsById(@Param("id") Long id);

    boolean existsByShippingAddress_AddressId(Long addressId);
}
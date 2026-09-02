package project.project.Repository;

import org.springframework.data.jpa.repository.JpaRepository;
import project.project.Entity.user.Address;

public interface AddressRepository extends JpaRepository<Address, Long> {

}

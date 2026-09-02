package project.project.Repository;

import org.springframework.data.jpa.repository.JpaRepository;
import project.project.Entity.user.Customer;

public interface CustomerRepository extends JpaRepository<Customer, Long> {

}

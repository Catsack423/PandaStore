package project.project.Repository;

import org.springframework.data.jpa.repository.JpaRepository;
import project.project.Entity.product.Product;

public interface ProductRepository extends JpaRepository<Product, Long> {

}

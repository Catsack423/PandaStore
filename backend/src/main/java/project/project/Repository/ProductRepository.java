package project.project.Repository;

import org.springframework.data.jpa.repository.JpaRepository;
import project.project.Entity.product.Product;
import java.util.List;

public interface ProductRepository extends JpaRepository<Product, Long> {

    List<Product> findByCategories_CategoryId(Long categoryId);

}

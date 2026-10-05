package project.project.Repository;

import org.springframework.data.jpa.repository.JpaRepository;
import project.project.Entity.product.Category;

public interface CategoryRepository extends JpaRepository<Category, Long> {

}

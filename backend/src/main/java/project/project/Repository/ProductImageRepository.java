package project.project.Repository;

import org.springframework.data.jpa.repository.JpaRepository;
import project.project.Entity.product.ProductImage;

public interface ProductImageRepository extends JpaRepository<ProductImage, Long> {

}

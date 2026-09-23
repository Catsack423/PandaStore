package project.project.Service.api;

import java.util.List;
import org.springframework.data.domain.Page;
import project.project.DTO.product.ProductResponse;
import project.project.Entity.product.Category;

public interface CategoryService {
    Category createCategory(String categoryName, String description);
    List<Category> getAllCategories();
    Page<ProductResponse> getProductsByCategory(Long categoryId, int page, int size);
}

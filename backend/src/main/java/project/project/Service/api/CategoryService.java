package project.project.Service.api;

import java.util.List;
import project.project.Entity.product.Category;

public interface CategoryService {
    Category createCategory(String categoryName, String description);
    List<Category> getAllCategories();
}

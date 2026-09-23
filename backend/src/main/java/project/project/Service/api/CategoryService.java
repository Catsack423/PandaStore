package project.project.Service.api;

import project.project.Entity.product.Category;

public interface CategoryService {
    Category createCategory(String categoryName, String description);
}

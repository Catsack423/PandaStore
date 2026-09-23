package project.project.DTO.product;

import project.project.Entity.product.Category;

public record CategoryResponse(Long categoryId, String categoryName, String description) {
    public static CategoryResponse fromEntity(Category category) {
        return new CategoryResponse(category.getCategoryId(), category.getCategoryName(),
                category.getDescription());
    }
}

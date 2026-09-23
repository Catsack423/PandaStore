package project.project.Service.implement;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import project.project.Entity.product.Category;
import project.project.Repository.CategoryRepository;
import project.project.Service.api.CategoryService;

@Service
public class CategoryServiceImp implements CategoryService {
    private final CategoryRepository categories;

    public CategoryServiceImp(CategoryRepository categories) {
        this.categories = categories;
    }

    @Override
    @Transactional
    public Category createCategory(String categoryName, String description) {
        if (categoryName == null || categoryName.isBlank()) {
            throw new IllegalArgumentException("ชื่อหมวดหมู่ห้ามว่าง");
        }

        String name = categoryName.trim();
        if (name.length() > 100) {
            throw new IllegalArgumentException("ชื่อหมวดหมู่ต้องไม่เกิน 100 ตัวอักษร");
        }
        if (categories.existsByCategoryNameIgnoreCase(name)) {
            throw new IllegalStateException("มีหมวดหมู่นี้อยู่แล้ว");
        }

        String detail = description == null ? null : description.trim();
        if (detail != null && detail.length() > 255) {
            throw new IllegalArgumentException("คำอธิบายต้องไม่เกิน 255 ตัวอักษร");
        }
        return categories.save(new Category(null, name, detail));
    }
}

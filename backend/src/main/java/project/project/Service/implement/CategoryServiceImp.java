package project.project.Service.implement;

import jakarta.persistence.EntityNotFoundException;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import project.project.DTO.product.ProductResponse;
import project.project.Entity.product.Category;
import project.project.Repository.CategoryRepository;
import project.project.Repository.ProductRepository;
import project.project.Service.api.CategoryService;
import project.project.Service.api.ProductService;

@Service
public class CategoryServiceImp implements CategoryService {
    private final CategoryRepository categories;
    private final ProductService products;
    private final ProductRepository productRepository;

    public CategoryServiceImp(CategoryRepository categories, ProductService products,
                              ProductRepository productRepository) {
        this.categories = categories;
        this.products = products;
        this.productRepository = productRepository;
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

    @Override
    @Transactional(readOnly = true)
    public List<Category> getAllCategories() {
        Sort order = Sort.by("categoryName").ascending()
                .and(Sort.by("categoryId").ascending());
        return categories.findAll(order);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ProductResponse> getProductsByCategory(Long categoryId, int page, int size) {
        if (categoryId == null || categoryId <= 0) {
            throw new IllegalArgumentException("categoryId ต้องมากกว่า 0");
        }
        if (!categories.existsById(categoryId)) {
            throw new EntityNotFoundException("ไม่พบหมวดหมู่ที่ต้องการ");
        }
        return products.searchProductsPage(null, categoryId, page, size);
    }

    @Override
    @Transactional
    public void deleteCategory(Long categoryId) {
        if (categoryId == null || categoryId <= 0) {
            throw new IllegalArgumentException("categoryId ต้องมากกว่า 0");
        }

        Category category = categories.findById(categoryId)
                .orElseThrow(() -> new EntityNotFoundException("ไม่พบหมวดหมู่ที่ต้องการ"));
        if (productRepository.existsByCategories_CategoryId(categoryId)) {
            throw new IllegalStateException("ไม่สามารถลบหมวดหมู่ที่มีสินค้าอยู่");
        }
        categories.delete(category);
    }
}

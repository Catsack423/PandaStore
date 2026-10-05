package project.project.Service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import project.project.DTO.product.ProductResponse;
import project.project.Entity.product.Category;
import project.project.Repository.CategoryRepository;
import project.project.Repository.ProductRepository;
import project.project.Service.api.ProductService;
import project.project.Service.implement.CategoryServiceImp;
import java.util.List;
import java.util.Optional;

class CategoryServiceTest {
    private final CategoryRepository repository = mock(CategoryRepository.class);
    private final ProductService products = mock(ProductService.class);
    private final ProductRepository productRepository = mock(ProductRepository.class);
    private final CategoryServiceImp service = new CategoryServiceImp(repository, products, productRepository);

    @Test
    void trimsNameBeforeCheckingAndSaving() {
        when(repository.save(any(Category.class))).thenAnswer(call -> call.getArgument(0));

        Category result = service.createCategory("  Keyboard  ", "  Devices  ");

        verify(repository).existsByCategoryNameIgnoreCase("Keyboard");
        assertEquals("Keyboard", result.getCategoryName());
        assertEquals("Devices", result.getDescription());
    }

    @Test
    void duplicateNameIsRejected() {
        when(repository.existsByCategoryNameIgnoreCase("Keyboard")).thenReturn(true);

        assertThrows(IllegalStateException.class,
                () -> service.createCategory(" Keyboard ", null));
        verify(repository, never()).save(any());
    }

    @Test
    void categoryProductsUseExistingProductSearch() {
        when(repository.existsById(3L)).thenReturn(true);
        var result = new PageImpl<ProductResponse>(List.of(), PageRequest.of(0, 20), 0);
        when(products.searchProductsPage(null, 3L, 0, 20)).thenReturn(result);

        assertSame(result, service.getProductsByCategory(3L, 0, 20));
        verify(products).searchProductsPage(null, 3L, 0, 20);
    }

    @Test
    void missingCategoryReturnsNotFound() {
        assertThrows(EntityNotFoundException.class,
                () -> service.getProductsByCategory(99L, 0, 20));
        verifyNoInteractions(products);
    }

    @Test
    void deletesCategoryWithoutProducts() {
        Category category = new Category(3L, "Keyboard", null);
        when(repository.findById(3L)).thenReturn(Optional.of(category));

        service.deleteCategory(3L);

        verify(repository).delete(category);
    }

    @Test
    void categoryWithProductsCannotBeDeleted() {
        Category category = new Category(3L, "Keyboard", null);
        when(repository.findById(3L)).thenReturn(Optional.of(category));
        when(productRepository.existsByCategories_CategoryId(3L)).thenReturn(true);

        assertThrows(IllegalStateException.class, () -> service.deleteCategory(3L));
        verify(repository, never()).delete(any());
    }

    @Test
    void missingCategoryCannotBeDeleted() {
        assertThrows(EntityNotFoundException.class, () -> service.deleteCategory(99L));
        verify(repository, never()).delete(any());
    }
}

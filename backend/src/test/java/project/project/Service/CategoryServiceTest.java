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
import project.project.Service.api.ProductService;
import project.project.Service.implement.CategoryServiceImp;
import java.util.List;

class CategoryServiceTest {
    private final CategoryRepository repository = mock(CategoryRepository.class);
    private final ProductService products = mock(ProductService.class);
    private final CategoryServiceImp service = new CategoryServiceImp(repository, products);

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
}

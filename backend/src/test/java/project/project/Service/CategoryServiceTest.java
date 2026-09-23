package project.project.Service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.Test;
import project.project.Entity.product.Category;
import project.project.Repository.CategoryRepository;
import project.project.Service.implement.CategoryServiceImp;

class CategoryServiceTest {
    private final CategoryRepository repository = mock(CategoryRepository.class);
    private final CategoryServiceImp service = new CategoryServiceImp(repository);

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
}

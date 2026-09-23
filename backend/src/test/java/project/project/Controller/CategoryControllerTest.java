package project.project.Controller;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import jakarta.persistence.EntityNotFoundException;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import project.project.Entity.product.Category;
import project.project.DTO.product.ProductResponse;
import project.project.Entity.user.UserRole;
import project.project.Security.AuthenticatedUser;
import project.project.Security.CurrentUser;
import project.project.Service.api.CategoryService;

class CategoryControllerTest {
    private final CategoryService categories = mock(CategoryService.class);
    private final CurrentUser currentUser = mock(CurrentUser.class);
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.standaloneSetup(new CategoryController(categories, currentUser))
                .setControllerAdvice(new GlobalExceptionHandler()).build();
    }

    @Test
    void adminCanCreateCategory() throws Exception {
        when(currentUser.requireIdentity()).thenReturn(new AuthenticatedUser(1, UserRole.ADMIN));
        when(categories.createCategory("Keyboard", "Devices"))
                .thenReturn(new Category(3L, "Keyboard", "Devices"));

        mvc.perform(post("/api/categories").contentType(MediaType.APPLICATION_JSON)
                .content("{\"categoryName\":\"Keyboard\",\"description\":\"Devices\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.categoryId").value(3))
                .andExpect(jsonPath("$.data.categoryName").value("Keyboard"));
    }

    @Test
    void otherUsersCannotCreateCategory() throws Exception {
        when(currentUser.requireIdentity()).thenReturn(new AuthenticatedUser(2, UserRole.CUSTOMER));

        mvc.perform(post("/api/categories").contentType(MediaType.APPLICATION_JSON)
                .content("{\"categoryName\":\"Keyboard\"}"))
                .andExpect(status().isForbidden());
        verifyNoInteractions(categories);
    }

    @Test
    void anonymousUsersCannotCreateCategory() throws Exception {
        when(currentUser.requireIdentity()).thenThrow(
                new ResponseStatusException(HttpStatus.UNAUTHORIZED, "กรุณาเข้าสู่ระบบ"));

        mvc.perform(post("/api/categories").contentType(MediaType.APPLICATION_JSON)
                .content("{\"categoryName\":\"Keyboard\"}"))
                .andExpect(status().isUnauthorized());
        verifyNoInteractions(categories);
    }

    @Test
    void anyoneCanGetAllCategories() throws Exception {
        when(categories.getAllCategories()).thenReturn(List.of(
                new Category(1L, "Keyboard", "Devices"),
                new Category(2L, "Mouse", null)));

        mvc.perform(get("/api/categories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].categoryName").value("Keyboard"))
                .andExpect(jsonPath("$.data[1].categoryId").value(2));

        verifyNoInteractions(currentUser);
    }

    @Test
    void categoryProductsReturnPaginationDetails() throws Exception {
        ProductResponse product = new ProductResponse();
        product.setProductId(42L);
        var result = new PageImpl<>(List.of(product), PageRequest.of(0, 1), 2);
        when(categories.getProductsByCategory(3L, 0, 1)).thenReturn(result);

        mvc.perform(get("/api/categories/3/products").param("page", "0").param("size", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items[0].productId").value(42))
                .andExpect(jsonPath("$.data.totalItems").value(2))
                .andExpect(jsonPath("$.data.hasNext").value(true));
        verifyNoInteractions(currentUser);
    }

    @Test
    void missingCategoryReturnsNotFound() throws Exception {
        when(categories.getProductsByCategory(99L, 0, 20))
                .thenThrow(new EntityNotFoundException("ไม่พบหมวดหมู่ที่ต้องการ"));

        mvc.perform(get("/api/categories/99/products"))
                .andExpect(status().isNotFound());
    }
}

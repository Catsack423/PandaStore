package project.project.Controller;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import project.project.Entity.product.Category;
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
}

package project.project.Controller;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import project.project.DTO.product.ProductResponse;
import project.project.Service.api.ProductService;

class ProductSearchPageControllerTest {
    @Test
    void searchReturnsItemsAndPaginationDetails() throws Exception {
        ProductService service = mock(ProductService.class);
        ProductResponse product = new ProductResponse();
        product.setProductId(42L);
        var page = new PageImpl<>(List.of(product), PageRequest.of(0, 1), 2);
        when(service.searchProductsPage("keyboard", 3L, 0, 1)).thenReturn(page);

        var mvc = MockMvcBuilders.standaloneSetup(new ProductController(service)).build();
        mvc.perform(get("/api/products/search")
                .param("keyword", "keyboard")
                .param("categoryId", "3")
                .param("page", "0")
                .param("size", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items[0].productId").value(42))
                .andExpect(jsonPath("$.data.page").value(0))
                .andExpect(jsonPath("$.data.size").value(1))
                .andExpect(jsonPath("$.data.totalItems").value(2))
                .andExpect(jsonPath("$.data.totalPages").value(2))
                .andExpect(jsonPath("$.data.hasNext").value(true));
    }

    @Test
    void categoryEndpointUsesCategoryFilterAndPagination() throws Exception {
        ProductService service = mock(ProductService.class);
        ProductResponse product = new ProductResponse();
        product.setProductId(42L);
        var page = new PageImpl<>(List.of(product), PageRequest.of(1, 1), 2);
        when(service.searchProductsPage(null, 3L, 1, 1)).thenReturn(page);

        var mvc = MockMvcBuilders.standaloneSetup(new ProductController(service)).build();
        mvc.perform(get("/api/products/category/3")
                .param("page", "1")
                .param("size", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items[0].productId").value(42))
                .andExpect(jsonPath("$.data.page").value(1))
                .andExpect(jsonPath("$.data.totalItems").value(2))
                .andExpect(jsonPath("$.data.hasNext").value(false));

        verify(service).searchProductsPage(null, 3L, 1, 1);
    }
}

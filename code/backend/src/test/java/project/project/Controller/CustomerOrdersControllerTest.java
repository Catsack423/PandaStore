package project.project.Controller;

import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import project.project.Security.CurrentUser;
import project.project.Service.implement.CustomerCheckoutService;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class CustomerOrdersControllerTest {
    @Mock CurrentUser currentUser;
    @Mock CustomerCheckoutService checkout;
    MockMvc mvc;

    @BeforeEach void setUp() {
        mvc = MockMvcBuilders.standaloneSetup(new CustomerOrdersController(currentUser, checkout))
                .setControllerAdvice(new GlobalExceptionHandler()).build();
    }

    @Test void versionedOrdersRouteUsesCustomerOwnership() throws Exception {
        when(currentUser.requireCustomerId()).thenReturn(42L);
        when(checkout.orderHistory(42L)).thenReturn(List.of());
        mvc.perform(get("/api/v1/customers/42/orders"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray());
        mvc.perform(get("/api/v1/customers/43/orders"))
                .andExpect(status().isForbidden());
        verify(checkout).orderHistory(42L);
        verifyNoMoreInteractions(checkout);
    }
}

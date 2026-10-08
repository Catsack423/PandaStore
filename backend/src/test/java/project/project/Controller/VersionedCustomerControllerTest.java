package project.project.Controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import project.project.Entity.user.UserRole;
import project.project.Security.AuthenticatedUser;
import project.project.Security.CurrentUser;
import project.project.Service.api.CustomerService;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class VersionedCustomerControllerTest {
    @Mock CustomerService customers;
    @Mock CurrentUser currentUser;
    MockMvc mvc;

    @BeforeEach void setUp() {
        mvc = MockMvcBuilders.standaloneSetup(new CustomerController(customers, currentUser))
                .setControllerAdvice(new GlobalExceptionHandler()).build();
    }

    @Test void deleteReturns204AndMissingCustomerReturnsStandard404() throws Exception {
        when(currentUser.requireIdentity()).thenReturn(new AuthenticatedUser(1, UserRole.ADMIN));
        when(customers.deleteCustomer(42)).thenReturn(true);
        mvc.perform(delete("/api/v1/customers/42"))
                .andExpect(status().isNoContent())
                .andExpect(content().string(""));

        mvc.perform(delete("/api/v1/customers/43"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false));
    }
}

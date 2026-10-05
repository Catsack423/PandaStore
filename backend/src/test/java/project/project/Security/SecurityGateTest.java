package project.project.Security;

import java.util.UUID;
import jakarta.servlet.Filter;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import project.project.Entity.user.User;
import project.project.Entity.user.UserRole;
import project.project.Entity.user.UserStatus;
import project.project.Repository.UserRepository;
import project.project.Service.api.AuthService;
import project.project.Service.implement.PasswordService;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:security-gate;DB_CLOSE_DELAY=-1",
        "spring.datasource.username=sa", "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop", "spring.jpa.show-sql=false", "app.seed-data=false"
})
class SecurityGateTest {
    @Autowired WebApplicationContext context;
    @Autowired @Qualifier("springSecurityFilterChain") Filter security;
    @Autowired UserRepository users;
    @Autowired project.project.Repository.CustomerRepository customers;
    @Autowired AuthService auth;
    @Autowired PasswordService passwords;
    MockMvc mvc;

    @BeforeEach void setUp() {
        mvc = MockMvcBuilders.webAppContextSetup(context).addFilters(security).build();
    }

    private String customerToken() {
        String name = "gate-" + UUID.randomUUID();
        User user = users.save(new User(name, name + "@example.test", passwords.hash("sample123"),
                UserRole.CUSTOMER, UserStatus.ACTIVE));
        customers.save(new project.project.Entity.user.Customer(user, "Gate Customer", "0812345678"));
        return auth.login(name, "sample123");
    }

    @Test void protectedEndpointsRequireAuthenticationIncludingUnlistedRoutes() throws Exception {
        for (String path : new String[] {"/api/auth/me", "/api/customers", "/api/payments/1",
                "/api/seller/shops/1/bank-account", "/api/new-protected-endpoint"}) {
            mvc.perform(get(path)).andExpect(status().isUnauthorized());
            mvc.perform(get(path).header("Authorization", "Bearer invalid.token.value"))
                    .andExpect(status().isUnauthorized());
        }
    }

    @Test void publicCatalogueAndAuthenticatedIdentityRemainAvailable() throws Exception {
        mvc.perform(get("/api/categories")).andExpect(status().isOk());
        mvc.perform(get("/api/auth/me").header("Authorization", "Bearer " + customerToken()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.role").value("CUSTOMER"));
    }

    @Test void validJwtInCookieCannotAuthenticateBackend() throws Exception {
        String token = customerToken();
        mvc.perform(get("/api/auth/me").cookie(new Cookie("auth_token", token), new Cookie("token", token)))
                .andExpect(status().isUnauthorized());
    }

    @Test void customerCannotAccessAdminOrPaymentCallbackEndpoints() throws Exception {
        String token = customerToken();
        mvc.perform(get("/api/customers").header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
        for (String path : new String[] {"/api/payments/callback", "/api/payments/refund/full", "/api/payments/refund/partial", "/api/test/reset-data"}) {
            mvc.perform(post(path).header("Authorization", "Bearer " + token)
                    .contentType(MediaType.APPLICATION_JSON).content("{}"))
                    .andExpect(status().isForbidden());
        }
    }

    @Test void loginIsRateLimitedAndForwardedHeadersCannotBypassIt() throws Exception {
        for (int i = 0; i < 5; i++) {
            mvc.perform(post("/api/auth/login").with(request -> { request.setRemoteAddr("192.0.2.10"); return request; })
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"usernameOrEmail\":\"nonexistent\",\"password\":\"wrong123\"}"))
                    .andExpect(status().isUnauthorized());
        }
        mvc.perform(post("/api/auth/login").with(request -> { request.setRemoteAddr("192.0.2.10"); return request; })
                .header("X-Forwarded-For", "203.0.113.2").contentType(MediaType.APPLICATION_JSON)
                .content("{\"usernameOrEmail\":\"different\",\"password\":\"wrong123\"}"))
                .andExpect(status().isTooManyRequests()).andExpect(header().exists("Retry-After"));
    }

    @Test void headersArePresentOnPublicAndDeniedResponses() throws Exception {
        for (String path : new String[] {"/api/categories", "/api/auth/me"}) {
            mvc.perform(get(path)).andExpect(header().exists("Content-Security-Policy"))
                    .andExpect(header().string("X-Content-Type-Options", "nosniff"))
                    .andExpect(header().string("X-Frame-Options", "DENY"))
                    .andExpect(header().string("Referrer-Policy", "strict-origin-when-cross-origin"));
        }
    }

    @Test void corsAllowsOnlyConfiguredOrigin() throws Exception {
        mvc.perform(options("/api/auth/login").header("Origin", "http://localhost:3000")
                .header("Access-Control-Request-Method", "POST"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:3000"));
        mvc.perform(options("/api/auth/login").header("Origin", "https://evil.example")
                .header("Access-Control-Request-Method", "POST"))
                .andExpect(status().isForbidden()).andExpect(header().doesNotExist("Access-Control-Allow-Origin"));
    }

    @Test void invalidProfileIsRejectedByBeanValidation() throws Exception {
        mvc.perform(put("/api/auth/me").header("Authorization", "Bearer " + customerToken())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"x\",\"email\":\"not-email\",\"phone\":\"bad\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test void customerCannotReadOrChangeAnotherCustomerOrShopBankAccount() throws Exception {
        String token = customerToken();
        mvc.perform(get("/api/customers/999999").header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
        mvc.perform(delete("/api/customers/999999").header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/seller/shops/999999/bank-account").header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/payments/order-group/999999").header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound());
    }
}

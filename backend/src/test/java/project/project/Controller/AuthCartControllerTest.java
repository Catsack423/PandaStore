package project.project.Controller;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.anyLong;
import java.math.BigDecimal;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import project.project.Entity.product.Product;
import project.project.Entity.seller.*;
import project.project.Entity.user.*;
import project.project.Repository.*;
import project.project.Service.api.AuthService;
import project.project.Service.api.ProductService;
import project.project.Service.implement.PasswordService;

@SpringBootTest(properties = {
                "spring.datasource.url=jdbc:h2:mem:controller-tests;DB_CLOSE_DELAY=-1",
                "spring.datasource.username=sa", "spring.datasource.password=",
                "spring.jpa.hibernate.ddl-auto=create-drop", "spring.jpa.open-in-view=false"
})
class AuthCartControllerTest {
        @Autowired
        WebApplicationContext context;
        @Autowired
        AuthSessionRepository sessions;
        @Autowired
        CartRepository carts;
        @Autowired
        CustomerRepository customers;
        @Autowired
        ProductRepository products;
        @Autowired
        SellerRepository sellers;
        @Autowired
        SellerApplicationRepository applications;
        @Autowired
        SellerBankAccountRepository bankAccounts;
        @Autowired
        UserRepository users;
        @Autowired
        AuthService auth;
        @Autowired
        project.project.Filter.JwtAuthenticationFilter authenticationFilter;
        @Autowired
        PasswordService passwords;
        @MockitoBean
        ProductService productService;
        MockMvc mvc;
        Long productId;

        @BeforeEach
        void setup() {
                mvc = MockMvcBuilders.webAppContextSetup(context).addFilters(authenticationFilter)
                                .alwaysExpect(result -> {
                                        String body = result.getResponse().getContentAsString();
                                        boolean expectedSuccess = result.getResponse().getStatus() < 400;
                                        assertEquals(expectedSuccess, JsonPath.<Boolean>read(body, "$.success"));
                                        assertNotNull(JsonPath.read(body, "$.message"));
                                        if (!expectedSuccess)
                                                assertNull(JsonPath.read(body, "$.data"));
                                }).build();
                sessions.deleteAll();
                carts.deleteAll();
                customers.deleteAll();
                products.deleteAll();
                bankAccounts.deleteAll();
                applications.deleteAll();
                sellers.deleteAll();
                users.deleteAll();
                User user = users.save(new User("seller", "seller@example.com", passwords.hash("password123"),
                                UserRole.SELLER, UserStatus.ACTIVE));
                Seller seller = new Seller();
                seller.setUser(user);
                seller.setShopName("Test Shop");
                seller.setShopPhone("0812345678");
                seller.setShopEmail("seller@example.com");
                seller.setShopAddress("Bangkok");
                seller.setStatus(SellerStatus.ACTIVE);
                sellers.save(seller);
                Product product = new Product();
                product.setSeller(seller);
                product.setName("Test Product");
                product.setPrice(new BigDecimal("100.00"));
                product.setStock(10);
                productId = products.save(product).getProductId();
                when(productService.getProductById(anyLong()))
                                .thenAnswer(call -> products.findById(call.getArgument(0)).orElse(null));
        }

        private String register(String name) throws Exception {
                mvc.perform(post("/api/auth/register/customer").contentType(MediaType.APPLICATION_JSON).content("""
                                {"username":"%s","email":"%s@example.com","password":"password123",
                                 "confirmPassword":"password123","fullName":"Test Customer","phoneNumber":"0812345678"}
                                """.formatted(name, name)))
                                .andExpect(status().isCreated()).andExpect(jsonPath("$.data.customerId").isNumber())
                                .andExpect(jsonPath("$.data.passwordHash").doesNotExist())
                                .andExpect(jsonPath("$.data.user").doesNotExist());
                return login(name);
        }

        private String login(String name) throws Exception {
                String body = mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                                .content("{\"usernameOrEmail\":\"" + name + "\",\"password\":\"password123\"}"))
                                .andExpect(status().isOk()).andExpect(jsonPath("$.data.tokenType").value("Bearer"))
                                .andReturn().getResponse().getContentAsString();
                return "Bearer " + JsonPath.read(body, "$.data.token");
        }

        private long addItem(String authorization, int quantity) throws Exception {
                String body = mvc.perform(post("/api/cart/items").header("Authorization", authorization)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"productId\":" + productId + ",\"quantity\":" + quantity + "}"))
                                .andExpect(status().isOk()).andExpect(jsonPath("$.data.quantity").value(quantity))
                                .andExpect(jsonPath("$.data.productName").value("Test Product"))
                                .andReturn().getResponse().getContentAsString();
                return ((Number) JsonPath.read(body, "$.data.cartItemId")).longValue();
        }

        @Test
        void meReturnsOnlyCurrentAccountFieldsEvenWhenAnotherUserIdIsSupplied() throws Exception {
                String token = register("customer");
                String otherToken = register("other");
                User customer = users.findByUsername("customer").orElseThrow();
                User other = users.findByUsername("other").orElseThrow();
                // Read current account data, not a stale profile copied into JWT claims.
                customer.setEmail("updated@example.com");
                users.save(customer);
                String body = mvc.perform(get("/api/auth/me").header("Authorization", token)
                                .param("userId", other.getUserId().toString()))
                                .andExpect(status().isOk()).andExpect(header().string("Cache-Control", "no-store"))
                                .andExpect(jsonPath("$.data.userId").value(customer.getUserId()))
                                .andExpect(jsonPath("$.data.username").value("customer"))
                                .andExpect(jsonPath("$.data.email").value("updated@example.com"))
                                .andExpect(jsonPath("$.data.role").value("CUSTOMER"))
                                .andExpect(jsonPath("$.data.status").value("ACTIVE"))
                                .andReturn().getResponse().getContentAsString();
                java.util.Map<String, Object> data = JsonPath.read(body, "$.data");
                assertEquals(java.util.Set.of("userId", "username", "email", "role", "status"), data.keySet());
                mvc.perform(get("/api/auth/me").header("Authorization", otherToken))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.data.userId").value(other.getUserId()));
        }

        @Test
        void meSupportsSellerAndAdminWithoutCustomerProfiles() throws Exception {
                mvc.perform(get("/api/auth/me").header("Authorization", login("seller")))
                                .andExpect(status().isOk()).andExpect(jsonPath("$.data.role").value("SELLER"));
                users.save(new User("admin", "admin@example.com", passwords.hash("password123"),
                                UserRole.ADMIN, UserStatus.ACTIVE));
                mvc.perform(get("/api/auth/me").header("Authorization", login("admin")))
                                .andExpect(status().isOk()).andExpect(jsonPath("$.data.role").value("ADMIN"));
        }

        @Test
        void meRequiresActiveSessionAndRejectsLoggedOutAndSuspendedUsers() throws Exception {
                mvc.perform(get("/api/auth/me")).andExpect(status().isUnauthorized());
                mvc.perform(get("/api/auth/me").header("Authorization", "Bearer invalid"))
                                .andExpect(status().isUnauthorized());
                String token = register("customer");
                mvc.perform(post("/api/auth/logout").header("Authorization", token)).andExpect(status().isOk());
                mvc.perform(get("/api/auth/me").header("Authorization", token)).andExpect(status().isUnauthorized());
                String activeToken = login("customer");
                User user = users.findByUsername("customer").orElseThrow();
                user.setStatus(UserStatus.SUSPENDED);
                users.save(user);
                mvc.perform(get("/api/auth/me").header("Authorization", activeToken))
                                .andExpect(status().isUnauthorized());
        }

        @Test
        void customerCanDeselectAndReselectItemThroughApi() throws Exception {
                String token = register("customer");
                long itemId = addItem(token, 2);
                String path = "/api/cart/items/" + itemId + "/selection";

                mvc.perform(patch(path).header("Authorization", token)
                                .contentType(MediaType.APPLICATION_JSON).content("{\"selected\":false}"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.data.selected").value(false))
                                .andExpect(jsonPath("$.data.quantity").value(2));
                mvc.perform(get("/api/cart").header("Authorization", token))
                                .andExpect(jsonPath("$.data.items[0].selected").value(false));
                mvc.perform(get("/api/cart/stock").header("Authorization", token))
                                .andExpect(jsonPath("$.data.valid").value(false));
                mvc.perform(get("/api/cart/sellers").header("Authorization", token))
                                .andExpect(status().isBadRequest());

                mvc.perform(patch(path).header("Authorization", token)
                                .contentType(MediaType.APPLICATION_JSON).content("{\"selected\":true}"))
                                .andExpect(status().isOk()).andExpect(jsonPath("$.data.selected").value(true));
                mvc.perform(get("/api/cart/stock").header("Authorization", token))
                                .andExpect(jsonPath("$.data.valid").value(true));
        }

        @Test
        void selectionApiRejectsMissingLoginAndOtherCustomers() throws Exception {
                String ownerToken = register("owner");
                String otherToken = register("other");
                long itemId = addItem(ownerToken, 1);
                String path = "/api/cart/items/" + itemId + "/selection";

                mvc.perform(patch(path).contentType(MediaType.APPLICATION_JSON).content("{\"selected\":false}"))
                                .andExpect(status().isUnauthorized());
                mvc.perform(patch(path).header("Authorization", otherToken)
                                .contentType(MediaType.APPLICATION_JSON).content("{\"selected\":false}"))
                                .andExpect(status().isNotFound());
                mvc.perform(patch(path).header("Authorization", login("seller"))
                                .contentType(MediaType.APPLICATION_JSON).content("{\"selected\":false}"))
                                .andExpect(status().isForbidden());
                mvc.perform(get("/api/cart").header("Authorization", ownerToken))
                                .andExpect(jsonPath("$.data.items[0].selected").value(true));
        }

        @Test
        void selectionApiRequiresBooleanValue() throws Exception {
                String token = register("customer");
                long itemId = addItem(token, 1);
                String path = "/api/cart/items/" + itemId + "/selection";

                for (String body : new String[] { "{}", "{\"selected\":null}", "{\"selected\":{}}" }) {
                        mvc.perform(patch(path).header("Authorization", token)
                                        .contentType(MediaType.APPLICATION_JSON).content(body))
                                        .andExpect(status().isBadRequest());
                }
        }

        @Test
        void registrationLoginAndCartOperationsWorkWithoutExposingEntities() throws Exception {
                String token = register("customer");
                mvc.perform(get("/api/auth/token").header("Authorization", token))
                                .andExpect(jsonPath("$.data.valid").value(true));
                mvc.perform(post("/api/cart").header("Authorization", token)).andExpect(status().isOk());
                assertEquals(1, carts.count());
                long itemId = addItem(token, 2);
                mvc.perform(get("/api/cart").header("Authorization", token))
                                .andExpect(status().isOk()).andExpect(jsonPath("$.data.items[0].quantity").value(2))
                                .andExpect(jsonPath("$.data.items[0].product").doesNotExist());
                mvc.perform(patch("/api/cart/items/" + itemId).header("Authorization", token)
                                .contentType(MediaType.APPLICATION_JSON).content("{\"quantity\":3}"))
                                .andExpect(status().isOk()).andExpect(jsonPath("$.data.quantity").value(3));
                mvc.perform(get("/api/cart/stock").header("Authorization", token))
                                .andExpect(jsonPath("$.data.valid").value(true));
                mvc.perform(get("/api/cart/sellers").header("Authorization", token)).andExpect(status().isOk());
                mvc.perform(delete("/api/cart/items/" + itemId).header("Authorization", token))
                                .andExpect(status().isOk());
                addItem(token, 1);
                mvc.perform(delete("/api/cart/items").header("Authorization", token)).andExpect(status().isOk());
                mvc.perform(get("/api/cart").header("Authorization", token))
                                .andExpect(jsonPath("$.data.items").isEmpty());
                verify(productService, times(2)).getProductById(productId);
        }

        @Test
        void logoutRevokesOnlyPresentedSessionAndKeepsCartAndOtherLogins() throws Exception {
                String token = register("customer");
                String second = login("customer");
                String other = register("other");
                addItem(token, 2);
                mvc.perform(post("/api/auth/logout").header("Authorization", token))
                                .andExpect(status().isOk()).andExpect(jsonPath("$.success").value(true));
                assertFalse(auth.validateToken(token.substring(7)));
                assertTrue(auth.validateToken(second.substring(7)));
                assertTrue(auth.validateToken(other.substring(7)));
                mvc.perform(get("/api/cart").header("Authorization", token)).andExpect(status().isUnauthorized());
                mvc.perform(post("/api/auth/logout").header("Authorization", token))
                                .andExpect(status().isUnauthorized());
                mvc.perform(get("/api/cart").header("Authorization", second))
                                .andExpect(status().isOk()).andExpect(jsonPath("$.data.items[0].quantity").value(2));
                assertNull(org.springframework.security.core.context.SecurityContextHolder.getContext()
                                .getAuthentication());
        }

        @Test
        void logoutRejectsMissingInvalidAndDuplicateHeadersWithoutRevokingValidSession() throws Exception {
                String token = register("customer");
                mvc.perform(post("/api/auth/logout")).andExpect(status().isUnauthorized());
                mvc.perform(post("/api/auth/logout").header("Authorization", "Bearer invalid"))
                                .andExpect(status().isUnauthorized());
                mvc.perform(post("/api/auth/logout").header("Authorization", token, token))
                                .andExpect(status().isUnauthorized());
                assertTrue(auth.validateToken(token.substring(7)));
        }

        @Test
        void sellerCanLogoutWithoutCustomerProfile() throws Exception {
                String token = login("seller");
                mvc.perform(post("/api/auth/logout").header("Authorization", token)).andExpect(status().isOk());
                assertFalse(auth.validateToken(token.substring(7)));
        }

        @Test
        void missingInvalidAndSuspendedTokensCannotAccessCart() throws Exception {
                mvc.perform(get("/api/cart")).andExpect(status().isUnauthorized());
                mvc.perform(get("/api/cart").header("Authorization", "Bearer " + "A".repeat(43)))
                                .andExpect(status().isUnauthorized());
                String token = register("customer");
                User user = users.findByUsername("customer").orElseThrow();
                user.setStatus(UserStatus.SUSPENDED);
                users.save(user);
                mvc.perform(get("/api/cart").header("Authorization", token)).andExpect(status().isUnauthorized());
        }

        @Test
        void anotherCustomerCannotChangeOrDeleteItem() throws Exception {
                String owner = register("owner");
                String other = register("other");
                long itemId = addItem(owner, 2);
                mvc.perform(patch("/api/cart/items/" + itemId).header("Authorization", other)
                                .contentType(MediaType.APPLICATION_JSON).content("{\"quantity\":5}"))
                                .andExpect(status().isNotFound());
                mvc.perform(delete("/api/cart/items/" + itemId).header("Authorization", other))
                                .andExpect(status().isNotFound());
                mvc.perform(get("/api/cart").header("Authorization", owner))
                                .andExpect(jsonPath("$.data.items[0].quantity").value(2));
        }

        @Test
        void sellerCannotUseCustomerCart() throws Exception {
                mvc.perform(get("/api/cart").header("Authorization", login("seller")))
                                .andExpect(status().isForbidden());
        }

        @Test
        void resetRequiresTokenAndOldPasswordAndRevokesSessions() throws Exception {
                String token = register("customer");
                String second = login("customer");
                String request = "{\"currentPassword\":\"password123\",\"password\":\"newPassword123\",\"confirmPassword\":\"newPassword123\"}";
                mvc.perform(post("/api/auth/reset-password").contentType(MediaType.APPLICATION_JSON).content(request))
                                .andExpect(status().isUnauthorized());
                mvc.perform(post("/api/auth/reset-password").header("Authorization", token)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(request.replace("password123", "wrong123")))
                                .andExpect(status().isUnauthorized());
                mvc.perform(post("/api/auth/reset-password").header("Authorization", token)
                                .contentType(MediaType.APPLICATION_JSON).content(request))
                                .andExpect(status().isOk()).andExpect(jsonPath("$.data.success").value(true));
                mvc.perform(get("/api/cart").header("Authorization", second)).andExpect(status().isUnauthorized());
                mvc.perform(get("/api/auth/token").header("Authorization", token))
                                .andExpect(jsonPath("$.data.valid").value(false));
                assertTrue(auth.validateToken(auth.login("customer", "newPassword123")));
        }

        @Test
        void invalidInputAndInsufficientStockReturn400() throws Exception {
                String token = register("customer");
                for (int quantity : new int[] { 0, -1, 11 }) {
                        mvc.perform(post("/api/cart/items").header("Authorization", token)
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content("{\"productId\":" + productId + ",\"quantity\":" + quantity + "}"))
                                        .andExpect(status().isBadRequest());
                }
                mvc.perform(post("/api/cart/items").header("Authorization", token)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{}")).andExpect(status().isBadRequest());
                mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                                .content("{\"usernameOrEmail\":\"customer\",\"password\":\"wrong123\"}"))
                                .andExpect(status().isUnauthorized());
        }

        @Test
        void malformedJsonAndInvalidPathUseApiResponse() throws Exception {
                String token = register("customer");
                mvc.perform(post("/api/cart/items").header("Authorization", token)
                                .contentType(MediaType.APPLICATION_JSON).content("{"))
                                .andExpect(status().isBadRequest());
                mvc.perform(delete("/api/cart/items/not-a-number").header("Authorization", token))
                                .andExpect(status().isBadRequest());
        }

        @Test
        void duplicateRegistrationReturns409AndSellerRegistrationCreatesSeller() throws Exception {
                register("customer");
                mvc.perform(post("/api/auth/register/customer").contentType(MediaType.APPLICATION_JSON).content("""
                                {"username":"customer","email":"customer@example.com","password":"password123",
                                "confirmPassword":"password123","fullName":"Test","phoneNumber":"0812345678"}
                                """)).andExpect(status().isConflict());

                mvc.perform(post("/api/auth/register/seller").contentType(MediaType.APPLICATION_JSON).content("""
                                {
                                    "username":"newSeller",
                                    "email":"new@example.com",
                                    "password":"password123",
                                    "confirmPassword":"password123",
                                    "shopName":"New Seller Shop",
                                    "shopDescription":"Shop description",
                                    "shopPhone":"0812345678",
                                    "shopEmail":"shop@example.com",
                                    "shopAddress":"Bangkok",
                                    "sellerFirstName":"Somchai",
                                    "sellerLastName":"Jaidee",
                                    "idCardNumber":"1234567890123",
                                    "bankName":"Kasikorn",
                                    "bankAccountName":"Somchai Jaidee",
                                    "bankAccountNumber":"1234567890"
                                }
                                """)).andExpect(status().isCreated())
                                .andExpect(jsonPath("$.data.sellerId").isNumber());
                assertTrue(users.existsByUsername("newSeller"));
        }
}

package project.project.Controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.AfterEach;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import project.project.Security.CurrentUser;
import project.project.Security.AuthenticatedUser;
import java.util.List;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import project.project.Entity.seller.Seller;
import project.project.Entity.seller.SellerStatus;
import project.project.Entity.user.User;
import project.project.Entity.user.UserRole;
import project.project.Entity.user.UserStatus;
import project.project.Repository.ProductImageRepository;
import project.project.Repository.ProductRepository;
import project.project.Repository.SellerRepository;
import project.project.Repository.UserRepository;
import project.project.Service.api.CategoryService;
import project.project.Service.api.ProductService;
import tools.jackson.databind.json.JsonMapper;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:product-creation-defects;DB_CLOSE_DELAY=-1",
        "spring.datasource.username=sa", "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect",
        "spring.jpa.show-sql=false",
        "JWT_SECRET=0000000000000000000000000000000000000000000000000000000000000001"
})
@Transactional
class ProductCreationDefectTest {
    private static final String IMAGE = "https://images.unsplash.com/photo-1521572267360-ee0c2909d518?w=800";
    @Autowired ProductService productService;
    @Autowired CategoryService categoryService;
    @Autowired ProductRepository products;
    @Autowired ProductImageRepository images;
    @Autowired SellerRepository sellers;
    @Autowired UserRepository users;
    @Autowired EntityManager entityManager;
    @Autowired CurrentUser currentUser;
    private MockMvc mvc;
    private Long sellerId;

    @BeforeEach
    void setUp() {
        User user = users.save(new User("product-defect-seller", "product-defect@test.com", "hash",
                UserRole.SELLER, UserStatus.ACTIVE));
        Seller seller = new Seller();
        seller.setUser(user);
        seller.setShopName("Approved shop");
        seller.setShopPhone("0123456789");
        seller.setShopEmail("approved@test.com");
        seller.setShopAddress("Bangkok");
        seller.setStatus(SellerStatus.ACTIVE);
        sellerId = sellers.save(seller).getSellerId();
        SecurityContextHolder.getContext().setAuthentication(UsernamePasswordAuthenticationToken.authenticated(
                new AuthenticatedUser(user.getUserId(), UserRole.SELLER), null, List.of()));
        mvc = MockMvcBuilders.standaloneSetup(new ProductController(productService, currentUser, sellers))
                .setControllerAdvice(new GlobalExceptionHandler()).build();
    }

    @AfterEach void clearIdentity() { SecurityContextHolder.clearContext(); }

    @Test void anonymousCannotCreateProduct() throws Exception {
        SecurityContextHolder.clearContext();
        mvc.perform(post("/api/products").param("sellerId", sellerId.toString()).contentType(MediaType.APPLICATION_JSON)
                .content(body("[\"" + IMAGE + "\"]", ""))).andExpect(status().isUnauthorized());
    }

    @Test void customersAndAdminsCannotCreateProduct() throws Exception {
        long before = products.count();
        for (var role : List.of(UserRole.CUSTOMER, UserRole.ADMIN)) {
            SecurityContextHolder.getContext().setAuthentication(UsernamePasswordAuthenticationToken.authenticated(
                    new AuthenticatedUser(123, role), null, List.of()));
            mvc.perform(post("/api/products").param("sellerId", sellerId.toString()).contentType(MediaType.APPLICATION_JSON)
                    .content(body("[\"" + IMAGE + "\"]", ""))).andExpect(status().isForbidden());
        }
        assertEquals(before, products.count());
    }

    @Test void sellerCannotCreateForAnotherShopOrInactiveShop() throws Exception {
        long before = products.count();
        mvc.perform(post("/api/products").param("sellerId", "999999").contentType(MediaType.APPLICATION_JSON)
                .content(body("[\"" + IMAGE + "\"]", ""))).andExpect(status().isForbidden());
        for (var state : List.of(SellerStatus.PENDING, SellerStatus.REJECTED, SellerStatus.SUSPENDED)) {
            sellers.findById(sellerId).orElseThrow().setStatus(state);
            mvc.perform(post("/api/products").param("sellerId", sellerId.toString()).contentType(MediaType.APPLICATION_JSON)
                    .content(body("[\"" + IMAGE + "\"]", ""))).andExpect(status().isForbidden());
        }
        assertEquals(before, products.count());
    }

    @Test void multipleCategoriesAndPrimaryImageOrderSurviveReload() throws Exception {
        long first = categoryService.createCategory("Clothing", null).getCategoryId();
        long second = categoryService.createCategory("Gifts", null).getCategoryId();
        var result = mvc.perform(post("/api/products").param("sellerId", sellerId.toString()).contentType(MediaType.APPLICATION_JSON)
                .content(body("[\"https://example.com/main.png\",\"https://example.com/other.png\"]", "\"categoryIds\":[" + first + "," + second + "]")))
                .andExpect(status().isCreated()).andReturn();
        long id = new JsonMapper().readTree(result.getResponse().getContentAsString()).path("data").path("productId").asLong();
        entityManager.flush();
        // Reverse database order independently of insertion order to verify the read contract.
        var stored = products.findById(id).orElseThrow();
        stored.getImages().getFirst().setDisplayOrder(1);
        stored.getImages().getLast().setDisplayOrder(0);
        entityManager.flush(); entityManager.clear();
        stored = products.findById(id).orElseThrow();
        assertEquals(2, stored.getCategories().size());
        assertEquals("https://example.com/other.png", stored.getImages().getFirst().getImageUrl());
        assertEquals("https://example.com/main.png", stored.getImages().getLast().getImageUrl());
        assertEquals("ACTIVE", stored.getStatus().name());
    }

    @ParameterizedTest
    @ValueSource(strings = {"0", "-1", "1.234", "10000000000"})
    void rejectsPricesOutsideDatabasePrecision(String price) throws Exception {
        long before = products.count();
        mvc.perform(post("/api/products").param("sellerId", sellerId.toString()).contentType(MediaType.APPLICATION_JSON)
                .content(body("[\"" + IMAGE + "\"]", "").replace("\"price\":390", "\"price\":" + price)))
                .andExpect(status().isBadRequest());
        assertEquals(before, products.count());
    }

    @ParameterizedTest
    @ValueSource(ints = {0, -1})
    void rejectsNonPositiveCreationStock(int stock) throws Exception {
        mvc.perform(post("/api/products").param("sellerId", sellerId.toString()).contentType(MediaType.APPLICATION_JSON)
                .content(body("[\"" + IMAGE + "\"]", "").replace("\"stock\":10", "\"stock\":" + stock)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void def007PersistsCategoryCreatedByCategoryServiceAndReturnsItsId() throws Exception {
        Long categoryId = categoryService.createCategory("Clothing", "Shirts").getCategoryId();
        var result = mvc.perform(post("/api/products").param("sellerId", sellerId.toString())
                .contentType(MediaType.APPLICATION_JSON)
                .content(body("[\"" + IMAGE + "\"]", "\"categoryIds\":[" + categoryId + "]")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.categoryIds[0]").value(categoryId.intValue()))
                .andExpect(jsonPath("$.data.imageUrls[0]").value(IMAGE)).andReturn();
        long productId = new JsonMapper().readTree(result.getResponse().getContentAsString())
                .path("data").path("productId").asLong();

        entityManager.flush();
        entityManager.clear();
        var stored = products.findById(productId).orElseThrow();
        assertTrue(stored.getCategories().stream().anyMatch(c -> c.getCategoryId().equals(categoryId)));
        assertEquals(IMAGE, stored.getImages().getFirst().getImageUrl());
        assertEquals(productId, categoryService.getProductsByCategory(categoryId, 0, 20)
                .getContent().getFirst().getProductId());
    }

    @Test
    void def008MissingCategoryRejectsCreationWithoutSavingProductOrImages() throws Exception {
        Long validId = categoryService.createCategory("Clothing", null).getCategoryId();
        long productsBefore = products.count();
        long imagesBefore = images.count();
        mvc.perform(post("/api/products").param("sellerId", sellerId.toString())
                .contentType(MediaType.APPLICATION_JSON)
                .content(body("[\"" + IMAGE + "\"]", "\"categoryIds\":[" + validId + ",999999]")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Category นี้ไม่มีในระบบ: 999999"));
        assertEquals(productsBefore, products.count());
        assertEquals(imagesBefore, images.count());
    }

    @ParameterizedTest
    @ValueSource(strings = {"null", "[]", "[\"\"]", "[\" \"]", "[null]", "[\"valid\",\"\"]"})
    void def009And010RejectMissingOrBlankImageEntries(String imageJson) throws Exception {
        long productsBefore = products.count();
        long imagesBefore = images.count();
        mvc.perform(post("/api/products").param("sellerId", sellerId.toString())
                .contentType(MediaType.APPLICATION_JSON).content(body(imageJson, "")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("At least one product image is required"));
        assertEquals(productsBefore, products.count());
        assertEquals(imagesBefore, images.count());
    }

    @Test
    void validImagesStillAllowCreationWithoutOptionalCategory() throws Exception {
        mvc.perform(post("/api/products").param("sellerId", sellerId.toString())
                .contentType(MediaType.APPLICATION_JSON).content(body("[\"" + IMAGE + "\"]", "")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.categoryIds").isEmpty())
                .andExpect(jsonPath("$.data.imageUrls[0]").value(IMAGE));
    }

    private String body(String imageJson, String categoryJson) {
        return "{\"name\":\"Panda shirt\",\"description\":\"Cotton\",\"price\":390,\"stock\":10,\"imageUrls\":"
                + imageJson + (categoryJson.isEmpty() ? "" : "," + categoryJson) + "}";
    }
}

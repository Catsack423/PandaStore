package project.project.Service;

import static org.junit.jupiter.api.Assertions.*;

import java.math.BigDecimal;
import java.util.List;
import jakarta.persistence.EntityManager;
import org.hibernate.SessionFactory;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import project.project.DTO.product.ProductResponse;
import project.project.Entity.product.Category;
import project.project.Entity.product.Product;
import project.project.Entity.product.ProductImage;
import project.project.Entity.product.ProductStatus;
import project.project.Entity.seller.Seller;
import project.project.Entity.seller.SellerStatus;
import project.project.Entity.user.User;
import project.project.Entity.user.UserRole;
import project.project.Entity.user.UserStatus;
import project.project.Repository.CategoryRepository;
import project.project.Repository.ProductRepository;
import project.project.Repository.SellerRepository;
import project.project.Repository.UserRepository;
import project.project.Service.api.ProductService;
import org.springframework.data.domain.Page;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:product-search-pages;DB_CLOSE_DELAY=-1",
        "spring.datasource.username=sa", "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.show-sql=false",
        "spring.jpa.properties.hibernate.generate_statistics=true"
})
@Transactional
class ProductSearchPageTest {
    @Autowired ProductService productService;
    @Autowired ProductRepository products;
    @Autowired CategoryRepository categories;
    @Autowired SellerRepository sellers;
    @Autowired UserRepository users;
    @Autowired EntityManager entityManager;

    @Test
    void searchFiltersProductsAndReturnsPageDetails() {
        User user = users.save(new User("searchseller", "searchseller@test.com", "hash",
                UserRole.SELLER, UserStatus.ACTIVE));
        Seller seller = new Seller();
        seller.setUser(user);
        seller.setShopName("Search shop");
        seller.setShopPhone("0123456789");
        seller.setShopEmail("shop@test.com");
        seller.setShopAddress("Bangkok");
        seller.setStatus(SellerStatus.ACTIVE);
        seller = sellers.save(seller);

        Category keyboard = categories.save(new Category(null, "Keyboard", null));
        Category mouse = categories.save(new Category(null, "Mouse", null));
        Product first = saveProduct(seller, keyboard, "Gaming keyboard", ProductStatus.ACTIVE);
        Product second = saveProduct(seller, keyboard, "Office keyboard", ProductStatus.ACTIVE);
        saveProduct(seller, keyboard, "Hidden keyboard", ProductStatus.INACTIVE);
        Product soldOut = saveProduct(seller, keyboard, "Sold out keyboard", ProductStatus.ACTIVE);
        soldOut.setStock(0);
        products.save(soldOut);
        Product mouseProduct = saveProduct(seller, mouse, "Gaming mouse", ProductStatus.ACTIVE);
        first.setPrice(new BigDecimal("80.00"));
        second.setPrice(new BigDecimal("120.00"));
        mouseProduct.setPrice(new BigDecimal("200.00"));
        products.save(first);
        products.save(second);
        products.save(mouseProduct);

        Page<ProductResponse> page0 = productService.searchProductsPage(" keyboard ",
                keyboard.getCategoryId(), 0, 1);
        Page<ProductResponse> page1 = productService.searchProductsPage("keyboard",
                keyboard.getCategoryId(), 1, 1);

        assertEquals(2, page0.getTotalElements());
        assertEquals(2, page0.getTotalPages());
        assertTrue(page0.hasNext());
        assertEquals(first.getProductId(), page0.getContent().getFirst().getProductId());
        assertEquals(second.getProductId(), page1.getContent().getFirst().getProductId());
        assertFalse(page1.hasNext());
        assertEquals("Search shop", page0.getContent().getFirst().getSellerShopName());
        assertEquals(keyboard.getCategoryId(), page0.getContent().getFirst().getCategoryIds().getFirst());
        assertFalse(productService.getAllActiveProducts().stream()
                .anyMatch(product -> product.getProductId().equals(soldOut.getProductId())));
        assertEquals(2, productService.searchProducts("keyboard", keyboard.getCategoryId()).size());

        var filtered = productService.searchProductsPage("gaming", List.of(keyboard.getCategoryId(), mouse.getCategoryId()),
                new BigDecimal("100"), new BigDecimal("200"), "price-desc", 0, 1);
        assertEquals(1, filtered.getTotalElements());
        assertEquals(mouseProduct.getProductId(), filtered.getContent().getFirst().getProductId());
        var sorted = productService.searchProductsPage(null, List.of(keyboard.getCategoryId(), mouse.getCategoryId()),
                null, null, "price-desc", 0, 2);
        assertEquals(3, sorted.getTotalElements());
        assertEquals(2, sorted.getTotalPages());
        assertEquals(mouseProduct.getProductId(), sorted.getContent().getFirst().getProductId());
        var summary = productService.getCatalogSummary();
        assertEquals(0, new BigDecimal("200.00").compareTo(summary.maxPrice()));
        assertEquals(2L, summary.categoryCounts().get(keyboard.getCategoryId()));
        assertEquals(1L, summary.categoryCounts().get(mouse.getCategoryId()));

        seller.setStatus(SellerStatus.PENDING);
        sellers.save(seller);
        assertTrue(productService.getAllActiveProducts().isEmpty());
        assertTrue(productService.searchProducts("keyboard", keyboard.getCategoryId()).isEmpty());
        assertEquals(0, productService.searchProductsPage("keyboard", keyboard.getCategoryId(), 0, 20).getTotalElements());
        assertEquals(BigDecimal.ZERO, productService.getCatalogSummary().maxPrice());
    }

    @Test
    void invalidPageArgumentsAreRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> productService.searchProductsPage(null, null, -1, 20));
        assertThrows(IllegalArgumentException.class,
                () -> productService.searchProductsPage(null, null, 0, 0));
        assertThrows(IllegalArgumentException.class,
                () -> productService.searchProductsPage(null, null, 0, 101));
        assertThrows(IllegalArgumentException.class,
                () -> productService.searchProductsPage(null, List.of(), new BigDecimal("200"),
                        new BigDecimal("100"), "latest", 0, 9));
    }

    @Test
    void searchLoadsPageImagesAndCategoriesInBatches() {
        User user = users.save(new User("batchseller", "batchseller@test.com", "hash",
                UserRole.SELLER, UserStatus.ACTIVE));
        Seller seller = new Seller();
        seller.setUser(user);
        seller.setShopName("Batch shop");
        seller.setShopPhone("0123456789");
        seller.setShopEmail("batch@test.com");
        seller.setShopAddress("Bangkok");
        seller.setStatus(SellerStatus.ACTIVE);
        seller = sellers.save(seller);
        Category category = categories.save(new Category(null, "Batch category", null));
        for (int i = 0; i < 12; i++) {
            Product product = saveProduct(seller, category, "Batch product " + i, ProductStatus.ACTIVE);
            product.getImages().add(new ProductImage(null, product, "/demo.png", true, 0));
            products.save(product);
        }
        entityManager.flush();
        entityManager.clear();

        var statistics = entityManager.getEntityManagerFactory()
                .unwrap(SessionFactory.class).getStatistics();
        statistics.clear();
        var result = productService.searchProductsPage(null, List.of(), null, null, "latest", 0, 9);

        assertEquals(9, result.getContent().size());
        assertEquals(12, result.getTotalElements());
        assertTrue(result.getContent().stream().allMatch(product ->
                product.getImageUrls().size() == 1 && product.getCategoryIds().size() == 1));
        assertTrue(statistics.getPrepareStatementCount() <= 6,
                "A page of nine products should not issue one query per image or category: "
                        + statistics.getPrepareStatementCount());

        statistics.clear();
        var summary = productService.getCatalogSummary();
        assertEquals(12L, summary.categoryCounts().get(category.getCategoryId()));
        assertEquals(2, statistics.getPrepareStatementCount(),
                "Catalog summary should use aggregate queries instead of loading every product");
    }

    private Product saveProduct(Seller seller, Category category, String name, ProductStatus status) {
        Product product = new Product();
        product.setSeller(seller);
        product.setName(name);
        product.setPrice(new BigDecimal("100.00"));
        product.setStock(5);
        product.setStatus(status);
        product.addCategory(category);
        return products.save(product);
    }
}

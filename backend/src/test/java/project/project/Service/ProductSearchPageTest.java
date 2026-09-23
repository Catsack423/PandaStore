package project.project.Service;

import static org.junit.jupiter.api.Assertions.*;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import project.project.DTO.product.ProductResponse;
import project.project.Entity.product.Category;
import project.project.Entity.product.Product;
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
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
@Transactional
class ProductSearchPageTest {
    @Autowired ProductService productService;
    @Autowired ProductRepository products;
    @Autowired CategoryRepository categories;
    @Autowired SellerRepository sellers;
    @Autowired UserRepository users;

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
        saveProduct(seller, mouse, "Gaming mouse", ProductStatus.ACTIVE);

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
    }

    @Test
    void invalidPageArgumentsAreRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> productService.searchProductsPage(null, null, -1, 20));
        assertThrows(IllegalArgumentException.class,
                () -> productService.searchProductsPage(null, null, 0, 0));
        assertThrows(IllegalArgumentException.class,
                () -> productService.searchProductsPage(null, null, 0, 101));
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

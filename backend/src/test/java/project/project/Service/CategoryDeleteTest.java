package project.project.Service;

import static org.junit.jupiter.api.Assertions.*;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import project.project.Entity.product.Category;
import project.project.Entity.product.Product;
import project.project.Entity.seller.Seller;
import project.project.Entity.seller.SellerStatus;
import project.project.Entity.user.User;
import project.project.Entity.user.UserRole;
import project.project.Entity.user.UserStatus;
import project.project.Repository.CategoryRepository;
import project.project.Repository.ProductRepository;
import project.project.Repository.SellerRepository;
import project.project.Repository.UserRepository;
import project.project.Service.api.CategoryService;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:category-delete;DB_CLOSE_DELAY=-1",
        "spring.datasource.username=sa", "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop", "spring.jpa.show-sql=false"
})
@Transactional
class CategoryDeleteTest {
    @Autowired CategoryService service;
    @Autowired CategoryRepository categories;
    @Autowired ProductRepository products;
    @Autowired SellerRepository sellers;
    @Autowired UserRepository users;

    @Test
    void deletesEmptyCategoryButKeepsCategoryUsedByProduct() {
        Category empty = categories.save(new Category(null, "Empty", null));
        Category used = categories.save(new Category(null, "Used", null));

        User user = users.save(new User("deleteSeller", "deleteSeller@test.com", "hash",
                UserRole.SELLER, UserStatus.ACTIVE));
        Seller seller = new Seller();
        seller.setUser(user);
        seller.setShopName("Delete test shop");
        seller.setShopPhone("0123456789");
        seller.setShopEmail("deleteShop@test.com");
        seller.setShopAddress("Bangkok");
        seller.setStatus(SellerStatus.ACTIVE);
        seller = sellers.save(seller);

        Product product = new Product();
        product.setSeller(seller);
        product.setName("Keyboard");
        product.setPrice(new BigDecimal("100.00"));
        product.setStock(1);
        product.addCategory(used);
        products.saveAndFlush(product);

        service.deleteCategory(empty.getCategoryId());
        assertFalse(categories.existsById(empty.getCategoryId()));

        assertThrows(IllegalStateException.class,
                () -> service.deleteCategory(used.getCategoryId()));
        assertTrue(categories.existsById(used.getCategoryId()));
    }
}

package project.project.Service;

import static org.junit.jupiter.api.Assertions.*;

import java.math.BigDecimal;
import java.util.NoSuchElementException;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import project.project.Entity.order.CartItem;
import project.project.Entity.product.Product;
import project.project.Entity.product.ProductStatus;
import project.project.Entity.seller.Seller;
import project.project.Entity.seller.SellerStatus;
import project.project.Entity.user.Customer;
import project.project.Entity.user.User;
import project.project.Entity.user.UserRole;
import project.project.Entity.user.UserStatus;
import project.project.Repository.*;
import project.project.Service.api.CartService;
import project.project.Service.api.ProductService;

@SpringBootTest(properties = {
    "spring.datasource.url=jdbc:h2:mem:cart-tests;DB_CLOSE_DELAY=-1;LOCK_TIMEOUT=10000",
    "spring.datasource.username=sa", "spring.datasource.password=",
    "spring.jpa.hibernate.ddl-auto=create-drop"
})
class CartServiceTest {
    @Autowired CartService service;
    @Autowired CartRepository carts;
    @Autowired CartItemRepository items;
    @Autowired CustomerRepository customers;
    @Autowired UserRepository users;
    @Autowired SellerRepository sellers;
    @Autowired ProductRepository products;
    @org.springframework.test.context.bean.override.mockito.MockitoBean
    ProductService productService;

    private Long customerId;
    private Long otherCustomerId;
    private Product firstProduct;
    private Product secondProduct;

    @BeforeEach
    void setup() {
        carts.deleteAll();
        products.deleteAll();
        customers.deleteAll();
        sellers.deleteAll();
        users.deleteAll();
        customerId = customer("customer").getCustomerId();
        otherCustomerId = customer("other").getCustomerId();
        service.createCart(customerId);
        service.createCart(otherCustomerId);
        firstProduct = product("first");
        secondProduct = product("second");
        org.mockito.Mockito.when(productService.getProductById(org.mockito.ArgumentMatchers.anyLong()))
                .thenAnswer(call -> products.findById(call.getArgument(0)).orElse(null));
    }

    private Customer customer(String username) {
        User user = users.save(new User(username, username + "@example.com", "test-hash",
                UserRole.CUSTOMER, UserStatus.ACTIVE));
        return customers.save(new Customer(user, "Test Customer", "0812345678"));
    }

    private Product product(String name) {
        User user = users.save(new User(name, name + "@example.com", "test-hash",
                UserRole.SELLER, UserStatus.ACTIVE));
        Seller seller = new Seller();
        seller.setUser(user);
        seller.setShopName(name);
        seller.setShopPhone("0812345678");
        seller.setShopEmail(name + "@example.com");
        seller.setShopAddress("Bangkok");
        seller.setStatus(SellerStatus.ACTIVE);
        seller = sellers.save(seller);
        Product product = new Product();
        product.setSeller(seller);
        product.setName(name);
        product.setPrice(new BigDecimal("100.00"));
        product.setStock(10);
        return products.save(product);
    }

    @Test
    void creationIsIdempotentAndRequiresExistingCustomer() {
        var first = service.createCart(customerId);
        assertEquals(first.getCartId(), service.createCart(customerId).getCartId());
        assertTrue(service.getCartByCustomerId(customerId).getItems().isEmpty());
        assertEquals(2, carts.count());
        assertThrows(NoSuchElementException.class, () -> service.createCart(Long.MAX_VALUE));
        assertThrows(IllegalArgumentException.class, () -> service.createCart(null));
        assertThrows(IllegalArgumentException.class, () -> service.getCartByCustomerId(-1L));
    }

    @Test
    void missingCartIsNotCreatedByReadOrAdd() {
        Long id = customer("without-cart").getCustomerId();
        assertThrows(NoSuchElementException.class, () -> service.getCartByCustomerId(id));
        assertThrows(NoSuchElementException.class, () -> service.addItemToCart(id, firstProduct.getProductId(), 1));
        assertEquals(2, carts.count());
    }

    @Test
    void repeatedAddMergesQuantityWithoutChangingStock() {
        CartItem first = service.addItemToCart(customerId, firstProduct.getProductId(), 2);
        CartItem again = service.addItemToCart(customerId, firstProduct.getProductId(), 3);
        assertEquals(first.getCartItemId(), again.getCartItemId());
        assertEquals(5, again.getQuantity());
        assertTrue(again.getIsSelected());
        assertEquals(1, service.getCartByCustomerId(customerId).getItems().size());
        assertEquals(10, products.findById(firstProduct.getProductId()).orElseThrow().getStock());
        org.mockito.Mockito.verify(productService, org.mockito.Mockito.times(2))
                .getProductById(firstProduct.getProductId());
    }

    @Test
    void rejectsInvalidQuantityMissingProductAndCombinedQuantityOverStock() {
        for (Integer quantity : new Integer[] {null, 0, -1}) {
            assertThrows(IllegalArgumentException.class,
                    () -> service.addItemToCart(customerId, firstProduct.getProductId(), quantity));
        }
        assertThrows(NoSuchElementException.class, () -> service.addItemToCart(customerId, Long.MAX_VALUE, 1));
        service.addItemToCart(customerId, firstProduct.getProductId(), 8);
        assertThrows(IllegalArgumentException.class, () -> service.addItemToCart(customerId, firstProduct.getProductId(), 3));
        assertThrows(IllegalArgumentException.class,
                () -> service.addItemToCart(customerId, firstProduct.getProductId(), Integer.MAX_VALUE));
        assertEquals(8, service.getCartByCustomerId(customerId).getItems().getFirst().getQuantity());
    }

    @Test
    void updateSetsAbsoluteQuantityAndRejectsInvalidChanges() {
        CartItem item = service.addItemToCart(customerId, firstProduct.getProductId(), 5);
        assertEquals(2, service.updateItemQuantity(customerId, item.getCartItemId(), 2).getQuantity());
        for (Integer quantity : new Integer[] {null, 0, -1, 11}) {
            assertThrows(IllegalArgumentException.class,
                    () -> service.updateItemQuantity(customerId, item.getCartItemId(), quantity));
        }
        assertEquals(2, items.findById(item.getCartItemId()).orElseThrow().getQuantity());
    }

    @Test
    void cannotUpdateOrRemoveAnotherCustomersItem() {
        CartItem item = service.addItemToCart(customerId, firstProduct.getProductId(), 1);
        assertThrows(NoSuchElementException.class,
                () -> service.updateItemQuantity(otherCustomerId, item.getCartItemId(), 2));
        assertThrows(NoSuchElementException.class,
                () -> service.removeItemFromCart(otherCustomerId, item.getCartItemId()));
        assertEquals(1, items.findById(item.getCartItemId()).orElseThrow().getQuantity());
    }

    @Test
    void removeDeletesOnlyRequestedItemAndClearKeepsCart() {
        CartItem first = service.addItemToCart(customerId, firstProduct.getProductId(), 1);
        service.addItemToCart(customerId, secondProduct.getProductId(), 1);
        CartItem other = service.addItemToCart(otherCustomerId, firstProduct.getProductId(), 1);
        service.removeItemFromCart(customerId, first.getCartItemId());
        assertFalse(items.existsById(first.getCartItemId()));
        assertEquals(1, service.getCartByCustomerId(customerId).getItems().size());
        assertThrows(NoSuchElementException.class, () -> service.removeItemFromCart(customerId, first.getCartItemId()));
        service.clearCart(customerId);
        service.clearCart(customerId);
        assertTrue(service.getCartByCustomerId(customerId).getItems().isEmpty());
        assertTrue(items.existsById(other.getCartItemId()));
        assertEquals(2, carts.count());
    }

    @Test
    void unavailableProductsAndShopsCannotBeAdded() {
        for (ProductStatus status : new ProductStatus[] {ProductStatus.INACTIVE, ProductStatus.OUT_OF_STOCK}) {
            firstProduct.setStatus(status);
            products.save(firstProduct);
            assertThrows(IllegalArgumentException.class, () -> service.addItemToCart(customerId, firstProduct.getProductId(), 1));
        }
        firstProduct.setStatus(ProductStatus.ACTIVE);
        firstProduct.setStock(0);
        products.save(firstProduct);
        assertThrows(IllegalArgumentException.class, () -> service.addItemToCart(customerId, firstProduct.getProductId(), 1));
        firstProduct.setStock(10);
        products.save(firstProduct);
        Seller seller = firstProduct.getSeller();
        for (SellerStatus status : new SellerStatus[] {SellerStatus.PENDING, SellerStatus.REJECTED, SellerStatus.SUSPENDED}) {
            seller.setStatus(status);
            sellers.save(seller);
            assertThrows(IllegalArgumentException.class, () -> service.addItemToCart(customerId, firstProduct.getProductId(), 1));
        }
        assertEquals(0, items.count());
    }

    @Test
    void stockChangesAreCheckedAgainBeforeCheckout() {
        CartItem item = service.addItemToCart(customerId, firstProduct.getProductId(), 5);
        assertTrue(service.validateCartStock(customerId));
        firstProduct.setStock(4);
        products.save(firstProduct);
        assertFalse(service.validateCartStock(customerId));
        assertThrows(IllegalArgumentException.class, () -> service.splitCartBySeller(customerId));
        assertThrows(IllegalArgumentException.class, () -> service.updateItemQuantity(customerId, item.getCartItemId(), 5));
        assertEquals(5, items.findById(item.getCartItemId()).orElseThrow().getQuantity());
    }

    @Test
    void suspendedSellerAccountInvalidatesExistingSelection() {
        service.addItemToCart(customerId, firstProduct.getProductId(), 1);
        User sellerUser = firstProduct.getSeller().getUser();
        sellerUser.setStatus(UserStatus.SUSPENDED);
        users.save(sellerUser);
        assertFalse(service.validateCartStock(customerId));
        assertThrows(IllegalArgumentException.class, () -> service.splitCartBySeller(customerId));
        assertThrows(IllegalArgumentException.class, () -> service.addItemToCart(customerId, firstProduct.getProductId(), 1));
    }

    @Test
    void checkoutUsesOnlySelectedItemsAndGroupsBySeller() {
        CartItem first = service.addItemToCart(customerId, firstProduct.getProductId(), 1);
        CartItem second = service.addItemToCart(customerId, secondProduct.getProductId(), 2);
        var groups = service.splitCartBySeller(customerId);
        assertEquals(2, groups.size());
        assertEquals(first.getCartItemId(), groups.get(firstProduct.getSeller().getSellerId()).getFirst().getCartItemId());
        assertEquals(second.getCartItemId(), groups.get(secondProduct.getSeller().getSellerId()).getFirst().getCartItemId());
        second.setIsSelected(false);
        items.save(second);
        secondProduct.setStock(0);
        products.save(secondProduct);
        assertTrue(service.validateCartStock(customerId));
        assertEquals(1, service.splitCartBySeller(customerId).size());
        // Reading the returned product is supported outside the service transaction.
        assertEquals("first", groups.get(firstProduct.getSeller().getSellerId()).getFirst().getProduct().getName());
    }

    @Test
    void multipleProductsFromSameSellerStayInOneGroup() {
        secondProduct.setSeller(firstProduct.getSeller());
        products.save(secondProduct);
        service.addItemToCart(customerId, firstProduct.getProductId(), 1);
        service.addItemToCart(customerId, secondProduct.getProductId(), 1);
        var groups = service.splitCartBySeller(customerId);
        assertEquals(1, groups.size());
        assertEquals(2, groups.get(firstProduct.getSeller().getSellerId()).size());
    }

    @Test
    void emptyOrUnselectedCartCannotCheckout() {
        assertFalse(service.validateCartStock(customerId));
        assertThrows(IllegalArgumentException.class, () -> service.splitCartBySeller(customerId));
        CartItem item = service.addItemToCart(customerId, firstProduct.getProductId(), 1);
        item.setIsSelected(false);
        items.save(item);
        assertFalse(service.validateCartStock(customerId));
        assertThrows(IllegalArgumentException.class, () -> service.splitCartBySeller(customerId));
        assertFalse(service.addItemToCart(customerId, firstProduct.getProductId(), 1).getIsSelected());
    }

    @Test
    void concurrentCreationReturnsOneCart() throws Exception {
        Long id = customer("concurrent").getCustomerId();
        runTogether(() -> service.createCart(id));
        assertEquals(3, carts.count());
        assertTrue(service.getCartByCustomerId(id).getItems().isEmpty());
    }

    @Test
    void concurrentAddsDoNotLoseQuantityOrDuplicateRows() throws Exception {
        runTogether(() -> service.addItemToCart(customerId, firstProduct.getProductId(), 2));
        var result = service.getCartByCustomerId(customerId).getItems();
        assertEquals(1, result.size());
        assertEquals(4, result.getFirst().getQuantity());
    }

    private void runTogether(Callable<?> action) throws Exception {
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        var executor = Executors.newFixedThreadPool(2);
        Callable<Object> task = () -> {
            ready.countDown();
            if (!start.await(10, TimeUnit.SECONDS)) throw new IllegalStateException("Start timeout");
            return action.call();
        };
        try {
            var first = executor.submit(task);
            var second = executor.submit(task);
            assertTrue(ready.await(10, TimeUnit.SECONDS));
            start.countDown();
            first.get(15, TimeUnit.SECONDS);
            second.get(15, TimeUnit.SECONDS);
        } finally {
            start.countDown();
            executor.shutdownNow();
            assertTrue(executor.awaitTermination(10, TimeUnit.SECONDS));
        }
    }
}

package project.project.Controller;

import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import project.project.Entity.seller.*;
import project.project.Entity.user.*;
import project.project.Repository.*;
import project.project.Security.*;
import project.project.Service.api.ProductService;
import project.project.Service.implement.CustomerCheckoutService;
import tools.jackson.databind.json.JsonMapper;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = {
    "spring.datasource.url=jdbc:h2:mem:customer-checkout;DB_CLOSE_DELAY=-1",
    "spring.datasource.username=sa", "spring.datasource.password=",
    "spring.jpa.hibernate.ddl-auto=create-drop",
    "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect",
    "spring.jpa.show-sql=false",
    "JWT_SECRET=0000000000000000000000000000000000000000000000000000000000000001"
})
@Transactional
class CustomerCheckoutTest {
    @Autowired CurrentUser currentUser;
    @Autowired CustomerCheckoutService checkout;
    @Autowired ProductService products;
    @Autowired UserRepository users;
    @Autowired CustomerRepository customers;
    @Autowired SellerRepository sellers;
    @Autowired OrderGroupRepository orderGroups;
    @Autowired ProductRepository productRepository;
    @Autowired EntityManager em;
    MockMvc mvc;
    Long customerId, product1, product2, seller1, seller2;

    @BeforeEach void setup() throws Exception {
        var user = users.save(new User("checkout-customer", "checkout@test.com", "hash", UserRole.CUSTOMER, UserStatus.ACTIVE));
        customerId = customers.save(new Customer(user, "Checkout Tester", "0812345678")).getCustomerId();
        SecurityContextHolder.getContext().setAuthentication(UsernamePasswordAuthenticationToken.authenticated(
                new AuthenticatedUser(user.getUserId(), UserRole.CUSTOMER), null, List.of()));
        mvc = MockMvcBuilders.standaloneSetup(new CustomerCheckoutController(currentUser, checkout), new ProductController(products, currentUser, sellers))
                .setControllerAdvice(new GlobalExceptionHandler()).build();
        seller1 = seller("one"); seller2 = seller("two");
        product1 = product(seller1, "First product", 100); product2 = product(seller2, "Second product", 200);
    }

    @AfterEach void clearIdentity() { SecurityContextHolder.clearContext(); }

    @Test void quotesMatchPersistedMethodsAndFeesForAllShops() throws Exception {
        var address = address();
        sync();
        sync(); // Same absolute quantities; no duplicates on retry.
        mvc.perform(get("/api/checkout")).andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items.length()").value(2))
                .andExpect(jsonPath("$.data.items[0].quantity").value(2))
                .andExpect(jsonPath("$.data.shippingMethods.length()").value(6));
        mvc.perform(post("/api/checkout/quote").contentType(MediaType.APPLICATION_JSON)
                .content("{\"sellerId\":" + seller1 + ",\"addressId\":" + address + ",\"shippingMethod\":\"J&T\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.shippingFee").value(45));
        var result = mvc.perform(post("/api/checkout/orders").contentType(MediaType.APPLICATION_JSON)
                .content(orderBody(address, "J&T", "EMS"))).andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.totalShippingFee").value(105))
                .andExpect(jsonPath("$.data.grandTotal").value(505))
                .andExpect(jsonPath("$.data.paymentStatus").value("PENDING"))
                .andExpect(jsonPath("$.data.subOrders.length()").value(2)).andReturn();
        long groupId = new JsonMapper().readTree(result.getResponse().getContentAsString()).path("data").path("orderGroupId").asLong();
        em.flush(); em.clear();
        var group = orderGroups.findById(groupId).orElseThrow();
        assertEquals(2, group.getSubOrders().size());
        assertTrue(group.getSubOrders().stream().anyMatch(o -> "J&T".equals(o.getShippingMethod()) && o.getShippingFee().intValueExact() == 45));
        assertTrue(group.getSubOrders().stream().anyMatch(o -> "EMS".equals(o.getShippingMethod()) && o.getShippingFee().intValueExact() == 60));
        assertEquals(18, productRepository.findById(product1).orElseThrow().getStock());
        assertEquals(19, productRepository.findById(product2).orElseThrow().getStock());
        mvc.perform(get("/api/checkout/orders/" + groupId)).andExpect(status().isOk()).andExpect(jsonPath("$.data.grandTotal").value(505));
        mvc.perform(get("/api/checkout/orders")).andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].orderGroupId").value(groupId))
                .andExpect(jsonPath("$.data[0].subOrders.length()").value(2))
                .andExpect(jsonPath("$.data[0].subOrders[0].items[0].productName").isString());
        assertTrue(checkout.orderHistory(Long.MAX_VALUE).isEmpty());
        mvc.perform(get("/api/checkout")).andExpect(jsonPath("$.data.items").isEmpty());
        mvc.perform(post("/api/checkout/orders").contentType(MediaType.APPLICATION_JSON).content(orderBody(address, "J&T", "EMS")))
                .andExpect(status().isBadRequest());
        assertEquals(1, orderGroups.count());
    }

    @Test void unsupportedMethodDoesNotCreateOrder() throws Exception {
        var address = address(); sync();
        mvc.perform(post("/api/checkout/orders").contentType(MediaType.APPLICATION_JSON)
                .content(orderBody(address, "FEDEX", "EMS"))).andExpect(status().isBadRequest());
        assertEquals(0, orderGroups.count());
        assertEquals(20, productRepository.findById(product1).orElseThrow().getStock());
    }

    @Test void addressMustBelongToSessionCustomer() throws Exception {
        sync();
        mvc.perform(post("/api/checkout/quote").contentType(MediaType.APPLICATION_JSON)
                .content("{\"sellerId\":" + seller1 + ",\"addressId\":999999,\"shippingMethod\":\"EMS\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test void unauthenticatedCannotReadCheckout() throws Exception {
        SecurityContextHolder.clearContext();
        mvc.perform(get("/api/checkout")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/checkout/orders")).andExpect(status().isUnauthorized());
    }

    @Test void orderHistoryIsScopedToSessionCustomer() throws Exception {
        var address = address(); sync();
        mvc.perform(post("/api/checkout/orders").contentType(MediaType.APPLICATION_JSON)
                .content(orderBody(address, "STANDARD", "EMS"))).andExpect(status().isCreated());
        var otherUser = users.save(new User("other-history-customer", "history-other@test.com", "hash", UserRole.CUSTOMER, UserStatus.ACTIVE));
        customers.save(new Customer(otherUser, "Other Customer", "0812345678"));
        SecurityContextHolder.getContext().setAuthentication(UsernamePasswordAuthenticationToken.authenticated(
                new AuthenticatedUser(otherUser.getUserId(), UserRole.CUSTOMER), null, List.of()));
        mvc.perform(get("/api/checkout/orders").param("customerId", customerId.toString()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data").isEmpty());
        SecurityContextHolder.getContext().setAuthentication(UsernamePasswordAuthenticationToken.authenticated(
                new AuthenticatedUser(otherUser.getUserId(), UserRole.SELLER), null, List.of()));
        mvc.perform(get("/api/checkout/orders")).andExpect(status().isForbidden());
    }

    @Test void synchronizationRejectsInvalidQuantity() throws Exception {
        mvc.perform(post("/api/checkout/cart").contentType(MediaType.APPLICATION_JSON)
                .content("{\"items\":[{\"productId\":" + product1 + ",\"quantity\":0}]}"))
                .andExpect(status().isBadRequest());
    }

    private void sync() throws Exception {
        mvc.perform(post("/api/checkout/cart").contentType(MediaType.APPLICATION_JSON)
                .content("{\"items\":[{\"productId\":" + product1 + ",\"quantity\":2},{\"productId\":" + product2 + ",\"quantity\":1}]}"))
                .andExpect(status().isOk());
    }
    private long address() throws Exception {
        var result = mvc.perform(post("/api/checkout/addresses").contentType(MediaType.APPLICATION_JSON)
                .content("{\"customerId\":99999,\"receiverName\":\"Checkout Tester\",\"phoneNumber\":\"0812345678\",\"addressLine\":\"123 Test Road\",\"district\":\"Pathum Wan\",\"province\":\"Bangkok\",\"postalCode\":\"10330\",\"isDefault\":true}"))
                .andExpect(status().isCreated()).andReturn();
        return new JsonMapper().readTree(result.getResponse().getContentAsString()).path("data").path("addressId").asLong();
    }
    private String orderBody(long address, String first, String second) {
        return "{\"shippingAddressId\":" + address + ",\"sellerShippingMethods\":{\"" + seller1 + "\":\"" + first + "\",\"" + seller2 + "\":\"" + second + "\"},\"paymentMethod\":\"PROMPTPAY\"}";
    }
    private Long seller(String suffix) {
        var user = users.save(new User("checkout-seller-" + suffix, suffix + "@test.com", "hash", UserRole.SELLER, UserStatus.ACTIVE));
        var seller = new Seller(); seller.setUser(user); seller.setShopName("Shop " + suffix);
        seller.setShopPhone("0812345678"); seller.setShopEmail(suffix + "@test.com"); seller.setShopAddress("Bangkok"); seller.setStatus(SellerStatus.ACTIVE);
        return sellers.save(seller).getSellerId();
    }
    private Long product(Long seller, String name, int price) throws Exception {
        var previous = SecurityContextHolder.getContext().getAuthentication();
        SecurityContextHolder.getContext().setAuthentication(UsernamePasswordAuthenticationToken.authenticated(
                new AuthenticatedUser(sellers.findById(seller).orElseThrow().getUser().getUserId(), UserRole.SELLER), null, List.of()));
        try {
        var result = mvc.perform(post("/api/products").param("sellerId", seller.toString()).contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"" + name + "\",\"description\":\"Checkout fixture\",\"price\":" + price + ",\"stock\":20,\"imageUrls\":[\"https://example.com/product.jpg\"]}"))
                .andExpect(status().isCreated()).andReturn();
        return new JsonMapper().readTree(result.getResponse().getContentAsString()).path("data").path("productId").asLong();
        } finally { SecurityContextHolder.getContext().setAuthentication(previous); }
    }
}

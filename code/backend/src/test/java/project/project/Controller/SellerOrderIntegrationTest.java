package project.project.Controller;

import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import project.project.DTO.order.SubOrderResponse;
import project.project.Entity.order.*;
import project.project.Entity.order.Order;
import project.project.Entity.product.Product;
import project.project.Entity.seller.*;
import project.project.Entity.user.*;
import project.project.Repository.*;
import project.project.Security.*;
import project.project.Service.api.*;

import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Supplier;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** Real H2 transactions and pessimistic locks; fixtures never touch the application database. */
@SpringBootTest(properties = {
    "spring.datasource.url=jdbc:h2:mem:seller-orders;DB_CLOSE_DELAY=-1;LOCK_TIMEOUT=10000",
    "spring.datasource.username=sa", "spring.datasource.password=",
    "spring.jpa.hibernate.ddl-auto=create-drop",
    "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect",
    "spring.jpa.show-sql=false",
    "JWT_SECRET=0000000000000000000000000000000000000000000000000000000000000001"
})
class SellerOrderIntegrationTest {
    @Autowired EntityManager em;
    @Autowired PlatformTransactionManager transactions;
    @Autowired OrderRepository orders;
    @Autowired OrderGroupRepository groups;
    @Autowired ProductRepository products;
    @Autowired PaymentRepository payments;
    @Autowired SellerRepository sellers;
    @Autowired NotificationRepository notifications;
    @Autowired SubOrderService subOrders;
    @Autowired ShippingService shipping;
    @Autowired PaymentService paymentService;
    @Autowired OrderAccess access;
    @Autowired SubOrderController orderController;
    @Autowired ShippingController shippingController;
    MockMvc mvc;
    Long groupId, customerId, customerUser, seller1, seller2, sellerUser1, sellerUser2, order1, order2, product1, product2;
    String gateway;

    <T> T tx(Supplier<T> work) { return new TransactionTemplate(transactions).execute(s -> work.get()); }
    void write(Runnable work) { tx(() -> { work.run(); return null; }); }
    void login(Long user, UserRole role) { SecurityContextHolder.getContext().setAuthentication(UsernamePasswordAuthenticationToken.authenticated(new AuthenticatedUser(user, role), null, List.of())); }

    @BeforeEach void setup() {
        mvc = MockMvcBuilders.standaloneSetup(orderController, shippingController)
                .setControllerAdvice(new GlobalExceptionHandler()).build();
        write(() -> {
            String unique = UUID.randomUUID().toString();
            User user = user("customer" + unique, UserRole.CUSTOMER); customerUser = user.getUserId();
            Customer customer = new Customer(user, "Order Customer", "0812345678"); em.persist(customer); customerId = customer.getCustomerId();
            Address address = new Address(null, customer, "Receiver", "0812345678", "123 Road", "District", "Province", "10000", true); em.persist(address);
            Seller first = shop("first" + unique), second = shop("second" + unique);
            seller1 = first.getSellerId(); sellerUser1 = first.getUser().getUserId(); seller2 = second.getSellerId(); sellerUser2 = second.getUser().getUserId();
            Product p1 = product(first, "First item", 50, 8), p2 = product(second, "Second item", 200, 9); product1 = p1.getProductId(); product2 = p2.getProductId();
            OrderGroup group = new OrderGroup(null, unique, customer, address, money(300), money(50), BigDecimal.ZERO, money(350), OrderGroupPaymentStatus.PENDING, null); em.persist(group); groupId = group.getOrderGroupId();
            Order firstOrder = order(group, first, p1, 2, 20), secondOrder = order(group, second, p2, 1, 30);
            order1 = firstOrder.getOrderId(); order2 = secondOrder.getOrderId();
            gateway = "TEST-" + unique;
            Payment payment = new Payment(null, group, PaymentMethod.CREDIT_CARD, money(350), BigDecimal.ZERO, PaymentStatus.PENDING, gateway, null); em.persist(payment);
        });
    }
    @AfterEach void clearIdentity() { SecurityContextHolder.clearContext(); }
    BigDecimal money(int value) { return BigDecimal.valueOf(value).setScale(2); }
    User user(String name, UserRole role) { User u = new User(name, name + "@test.local", "hash", role, UserStatus.ACTIVE); em.persist(u); return u; }
    Seller shop(String name) { Seller s = new Seller(); s.setUser(user(name, UserRole.SELLER)); s.setShopName(name); s.setShopEmail(name + "@test.local"); s.setShopAddress("Shop address"); s.setShopPhone("0812345678"); s.setStatus(SellerStatus.ACTIVE); em.persist(s); return s; }
    Product product(Seller seller, String name, int price, int stock) { Product p = new Product(); p.setSeller(seller); p.setName(name); p.setPrice(money(price)); p.setStock(stock); em.persist(p); return p; }
    Order order(OrderGroup group, Seller seller, Product product, int quantity, int fee) {
        BigDecimal subtotal = product.getPrice().multiply(BigDecimal.valueOf(quantity));
        Order order = new Order(null, UUID.randomUUID().toString(), group, seller, subtotal, money(fee), BigDecimal.ZERO, subtotal.add(money(fee)), OrderStatus.PENDING_PAYMENT, null, null, null, null);
        order.setShippingMethod("STANDARD"); em.persist(order);
        OrderItem item = new OrderItem(null, order, product, product.getName(), product.getPrice(), quantity, subtotal, false); em.persist(item); return order;
    }
    void paid() { paymentService.handleGatewayCallback(gateway, true); }
    OrderStatus orderState(Long id) { return tx(() -> orders.findById(id).orElseThrow().getOrderStatus()); }
    int stock(Long id) { return tx(() -> products.findById(id).orElseThrow().getStock()); }
    int notificationCount() { return tx(() -> notifications.findByRecipientUser_UserIdOrderByCreatedAtDescNotificationIdDesc(customerUser).size() + notifications.findByRecipientUser_UserIdOrderByCreatedAtDescNotificationIdDesc(sellerUser1).size() + notifications.findByRecipientUser_UserIdOrderByCreatedAtDescNotificationIdDesc(sellerUser2).size()); }

    @Test void paidToCompletedFlowAndDuplicateCallbacks() throws Exception {
        paid(); int count = notificationCount(); assertEquals(3, count);
        assertEquals(OrderStatus.WAITING_SELLER_CONFIRM, orderState(order1));
        paid(); assertEquals(count, notificationCount());
        login(sellerUser1, UserRole.SELLER);
        mvc.perform(post("/api/sub-orders/{id}/accept", order1).param("sellerId", seller1.toString())).andExpect(status().isOk());
        mvc.perform(post("/api/sub-orders/{id}/accept", order1).param("sellerId", seller1.toString())).andExpect(status().isConflict());
        mvc.perform(post("/api/shipping/orders/{id}/assign-tracking", order1).param("sellerId", seller1.toString()).contentType(MediaType.APPLICATION_JSON).content("{\"courierName\":\"Courier\",\"trackingNumber\":\"TRACK-123\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.trackingNumber").value("TRACK-123"));
        mvc.perform(post("/api/shipping/orders/{id}/assign-tracking", order1).param("sellerId", seller1.toString()).contentType(MediaType.APPLICATION_JSON).content("{\"courierName\":\"Courier\",\"trackingNumber\":\"TRACK-123\"}"))
                .andExpect(status().isConflict());
        login(customerUser, UserRole.CUSTOMER);
        mvc.perform(post("/api/sub-orders/{id}/confirm-delivered", order1).param("customerId", customerId.toString())).andExpect(status().isOk());
        assertEquals(OrderStatus.COMPLETED, orderState(order1));
        paid(); assertEquals(OrderStatus.COMPLETED, orderState(order1));
        var response = tx(() -> SubOrderResponse.fromEntity(orders.findById(order1).orElseThrow()));
        assertEquals("Order Customer", response.getCustomerName()); assertEquals("Receiver", response.getShippingAddress().receiverName());
        assertEquals(OrderGroupPaymentStatus.PAID, response.getPaymentStatus()); assertEquals(money(120), response.getTotalAmount());
        assertEquals("TRACK-123", response.getShipment().getTrackingNumber());
    }

    @Test void rejectRefundsOnlyOwnShopAndRestoresItsStockOnce() throws Exception {
        paid(); login(sellerUser1, UserRole.SELLER);
        mvc.perform(post("/api/sub-orders/{id}/reject", order1).param("sellerId", seller1.toString()).contentType(MediaType.APPLICATION_JSON).content("{\"reason\":\"Out of stock\"}"))
                .andExpect(status().isOk());
        assertEquals(OrderStatus.CANCELLED, orderState(order1)); assertEquals(OrderStatus.WAITING_SELLER_CONFIRM, orderState(order2));
        assertEquals(10, stock(product1)); assertEquals(9, stock(product2));
        var payment = tx(() -> payments.findByOrderGroup_OrderGroupId(groupId).orElseThrow());
        assertEquals(money(120), payment.getRefundedAmount()); assertEquals(PaymentStatus.PARTIALLY_REFUNDED, payment.getStatus());
        mvc.perform(post("/api/sub-orders/{id}/reject", order1).param("sellerId", seller1.toString()).contentType(MediaType.APPLICATION_JSON).content("{\"reason\":\"Repeat\"}"))
                .andExpect(status().isConflict());
        assertEquals(10, stock(product1));
        subOrders.sellerAcceptOrder(seller2, order2); assertEquals(OrderStatus.PREPARING, orderState(order2));
    }

    @Test void refundFailureRollsBackOrderStockGroupAndPayment() throws Exception {
        paid(); write(() -> payments.findByOrderGroup_OrderGroupId(groupId).orElseThrow().setStatus(PaymentStatus.FAILED));
        int count = notificationCount(); login(sellerUser1, UserRole.SELLER);
        mvc.perform(post("/api/sub-orders/{id}/reject", order1).param("sellerId", seller1.toString()).contentType(MediaType.APPLICATION_JSON).content("{\"reason\":\"Refund failure\"}"))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.success").value(false));
        assertEquals(OrderStatus.WAITING_SELLER_CONFIRM, orderState(order1)); assertEquals(8, stock(product1));
        assertNull(tx(() -> orders.findById(order1).orElseThrow().getRejectionReason()));
        assertEquals(OrderGroupPaymentStatus.PAID, tx(() -> groups.findById(groupId).orElseThrow().getPaymentStatus()));
        assertEquals(BigDecimal.ZERO.setScale(2), tx(() -> payments.findByOrderGroup_OrderGroupId(groupId).orElseThrow().getRefundedAmount()));
        assertEquals(count, notificationCount());
    }

    @Test void readAndMutationAuthorizationUsesSessionOwnership() throws Exception {
        paid(); SecurityContextHolder.clearContext();
        mvc.perform(post("/api/sub-orders/{id}/accept", order1).param("sellerId", seller1.toString())).andExpect(status().isUnauthorized());
        for (var role : List.of(UserRole.CUSTOMER, UserRole.ADMIN)) {
            login(customerUser, role);
            mvc.perform(post("/api/sub-orders/{id}/accept", order1).param("sellerId", seller1.toString())).andExpect(status().isForbidden());
            mvc.perform(post("/api/sub-orders/{id}/reject", order1).param("sellerId", seller1.toString()).contentType(MediaType.APPLICATION_JSON).content("{\"reason\":\"No\"}")) .andExpect(status().isForbidden());
            mvc.perform(post("/api/shipping/orders/{id}/assign-tracking", order1).param("sellerId", seller1.toString()).contentType(MediaType.APPLICATION_JSON).content("{\"courierName\":\"C\",\"trackingNumber\":\"T\"}")) .andExpect(status().isForbidden());
        }
        login(sellerUser1, UserRole.SELLER);
        mvc.perform(post("/api/sub-orders/{id}/accept", order2).param("sellerId", seller2.toString())).andExpect(status().isForbidden());
        mvc.perform(post("/api/sub-orders/{id}/accept", order2).param("sellerId", seller1.toString())).andExpect(status().isForbidden());
        mvc.perform(get("/api/sub-orders/seller/{id}", seller2)).andExpect(status().isForbidden());
        mvc.perform(get("/api/sub-orders/order-group/{id}", groupId)).andExpect(status().isForbidden());
        mvc.perform(get("/api/shipping/orders/{id}", order2)).andExpect(status().isForbidden());
        mvc.perform(patch("/api/shipping/shipments/1/status").contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"DELIVERED\"}")) .andExpect(status().isForbidden());
        write(() -> sellers.findById(seller1).orElseThrow().setStatus(SellerStatus.SUSPENDED));
        mvc.perform(post("/api/sub-orders/{id}/accept", order1).param("sellerId", seller1.toString())).andExpect(status().isForbidden());
        write(() -> sellers.findById(seller1).orElseThrow().setStatus(SellerStatus.ACTIVE));
        for (var identity : List.of(new AuthenticatedUser(sellerUser1, UserRole.SELLER), new AuthenticatedUser(customerUser, UserRole.CUSTOMER), new AuthenticatedUser(customerUser, UserRole.ADMIN))) {
            login(identity.userId(), identity.role());
            mvc.perform(get("/api/sub-orders/{id}", order1)).andExpect(status().isOk()).andExpect(jsonPath("$.data.paymentStatus").value("PAID"));
        }
        login(sellerUser2, UserRole.SELLER);
        mvc.perform(get("/api/sub-orders/{id}", order1)).andExpect(status().isForbidden());
        login(sellerUser2, UserRole.CUSTOMER);
        mvc.perform(get("/api/sub-orders/{id}", order1)).andExpect(status().isForbidden());
        mvc.perform(post("/api/sub-orders/{id}/confirm-delivered", order1).param("customerId", customerId.toString())).andExpect(status().isForbidden());
        assertEquals(OrderStatus.WAITING_SELLER_CONFIRM, orderState(order1));
    }

    @ParameterizedTest @EnumSource(value = OrderStatus.class, names = {"PREPARING", "SHIPPED", "COMPLETED", "CANCELLED"})
    void successCallbackNeverResetsAdvancedOrClosedOrders(OrderStatus advanced) {
        write(() -> orders.findById(order1).orElseThrow().setOrderStatus(advanced));
        paid(); assertEquals(advanced, orderState(order1)); assertEquals(OrderStatus.WAITING_SELLER_CONFIRM, orderState(order2));
    }

    @Test void unpaidOrLegacyPaidPendingOrdersCannotBeAcceptedOrRejected() {
        assertThrows(IllegalStateException.class, () -> subOrders.sellerAcceptOrder(seller1, order1));
        assertThrows(IllegalStateException.class, () -> subOrders.sellerRejectOrder(seller1, order1, "No"));
        write(() -> groups.findById(groupId).orElseThrow().setPaymentStatus(OrderGroupPaymentStatus.PAID));
        assertThrows(IllegalStateException.class, () -> subOrders.sellerAcceptOrder(seller1, order1));
        assertEquals(OrderStatus.PENDING_PAYMENT, orderState(order1));
    }

    @Test void validationBoundsAndCustomerCancellationRemainCorrect() throws Exception {
        paid(); login(sellerUser1, UserRole.SELLER);
        for (String reason : List.of(" ", "x".repeat(256)))
            mvc.perform(post("/api/sub-orders/{id}/reject", order1).param("sellerId", seller1.toString()).contentType(MediaType.APPLICATION_JSON).content("{\"reason\":\"" + reason + "\"}")) .andExpect(status().isBadRequest());
        mvc.perform(post("/api/sub-orders/{id}/reject", order1).param("sellerId", seller1.toString()).contentType(MediaType.APPLICATION_JSON).content("{\"reason\":\"" + "x".repeat(255) + "\"}")) .andExpect(status().isOk());
        assertEquals(255, tx(() -> orders.findById(order1).orElseThrow().getRejectionReason().length()));
        login(sellerUser2, UserRole.SELLER);
        mvc.perform(post("/api/sub-orders/{id}/accept", order2).param("sellerId", seller2.toString())).andExpect(status().isOk());
        for (String[] values : List.of(new String[]{" ", "T"}, new String[]{"C", " "}, new String[]{"x".repeat(101), "T"}, new String[]{"C", "x".repeat(101)}))
            mvc.perform(post("/api/shipping/orders/{id}/assign-tracking", order2).param("sellerId", seller2.toString()).contentType(MediaType.APPLICATION_JSON).content("{\"courierName\":\"" + values[0] + "\",\"trackingNumber\":\"" + values[1] + "\"}")) .andExpect(status().isBadRequest());
        mvc.perform(post("/api/sub-orders/{id}/cancel", order2).param("customerId", customerId.toString()).contentType(MediaType.APPLICATION_JSON).content("{\"reason\":\"Cancel\"}")) .andExpect(status().isForbidden());
        login(customerUser, UserRole.CUSTOMER);
        mvc.perform(post("/api/sub-orders/{id}/cancel", order2).param("customerId", customerId.toString()).contentType(MediaType.APPLICATION_JSON).content("{\"reason\":\"" + "c".repeat(255) + "\"}")) .andExpect(status().isOk());
        assertEquals(OrderStatus.CANCELLED, orderState(order2)); assertEquals(10, stock(product2));
        assertEquals(money(350), tx(() -> payments.findByOrderGroup_OrderGroupId(groupId).orElseThrow().getRefundedAmount()));
    }

    @Test void groupShopAndShipmentReadsKeepTheirSeparateRoleScopes() throws Exception {
        paid(); subOrders.sellerAcceptOrder(seller1, order1); shipping.assignTrackingNumber(seller1, order1, "Courier", "TRACK");
        for (var identity : List.of(new AuthenticatedUser(customerUser, UserRole.CUSTOMER), new AuthenticatedUser(customerUser, UserRole.ADMIN))) {
            login(identity.userId(), identity.role());
            mvc.perform(get("/api/sub-orders/order-group/{id}", groupId)).andExpect(status().isOk()).andExpect(jsonPath("$.data.length()").value(2));
            mvc.perform(get("/api/shipping/orders/{id}", order1)).andExpect(status().isOk());
        }
        Long shipmentId = tx(() -> orders.findById(order1).orElseThrow().getShipment().getShipmentId());
        mvc.perform(patch("/api/shipping/shipments/{id}/status", shipmentId).contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"DELIVERED\"}")) .andExpect(status().isOk());
        login(customerUser, UserRole.CUSTOMER);
        mvc.perform(get("/api/sub-orders/seller/{id}", seller1)).andExpect(status().isForbidden());
        login(sellerUser1, UserRole.SELLER);
        mvc.perform(get("/api/sub-orders/seller/{id}", seller1)).andExpect(status().isOk()).andExpect(jsonPath("$.data.length()").value(1));
        mvc.perform(get("/api/shipping/orders/{id}", order1)).andExpect(status().isOk());
        login(sellerUser2, UserRole.CUSTOMER);
        mvc.perform(get("/api/sub-orders/order-group/{id}", groupId)).andExpect(status().isForbidden());
        mvc.perform(get("/api/shipping/orders/{id}", order1)).andExpect(status().isForbidden());
    }

    @Test void legacyMissingResponseFieldsStayNull() {
        var legacy = SubOrderResponse.fromEntity(new Order());
        assertNull(legacy.getCustomerName()); assertNull(legacy.getShippingAddress()); assertNull(legacy.getPaymentStatus());
    }

    @Test void concurrentTrackingUpdatesHaveOneWinner() throws Exception {
        paid(); subOrders.sellerAcceptOrder(seller1, order1);
        race(() -> orders.findById(order1).orElseThrow().getOrderStatus(), () -> shipping.assignTrackingNumber(seller1, order1, "A", "A-TRACK"), () -> shipping.assignTrackingNumber(seller1, order1, "B", "B-TRACK"), 1);
        assertEquals(OrderStatus.SHIPPED, orderState(order1));
        String track = tx(() -> orders.findById(order1).orElseThrow().getShipment().getTrackingNumber());
        assertTrue(List.of("A-TRACK", "B-TRACK").contains(track));
    }

    @Test void concurrentCallbacksNotifyOnlyOnceAndConcurrentAcceptHasOneWinner() throws Exception {
        race(() -> { payments.findByGatewayTransactionId(gateway).orElseThrow().getStatus(); }, () -> paymentService.handleGatewayCallback(gateway, true), () -> paymentService.handleGatewayCallback(gateway, true), 2);
        assertEquals(3, notificationCount());
        race(() -> { orders.findById(order1).orElseThrow().getOrderStatus(); }, () -> subOrders.sellerAcceptOrder(seller1, order1), () -> subOrders.sellerAcceptOrder(seller1, order1), 1);
        assertEquals(OrderStatus.PREPARING, orderState(order1));
    }

    @Test void concurrentShopRejectsPreserveBothRefundsAndStocks() throws Exception {
        paid();
        race(() -> { groups.findById(groupId).orElseThrow().getPayment().getStatus(); }, () -> subOrders.sellerRejectOrder(seller1, order1, "No stock"), () -> subOrders.sellerRejectOrder(seller2, order2, "No stock"), 2);
        assertEquals(10, stock(product1)); assertEquals(10, stock(product2));
        assertEquals(money(350), tx(() -> payments.findByOrderGroup_OrderGroupId(groupId).orElseThrow().getRefundedAmount()));
        assertEquals(OrderGroupPaymentStatus.REFUNDED, tx(() -> groups.findById(groupId).orElseThrow().getPaymentStatus()));
    }

    @Test void concurrentAcceptAndRejectCannotApplyBothTransitions() throws Exception {
        paid();
        race(() -> orders.findById(order1).orElseThrow().getOrderStatus(), () -> subOrders.sellerAcceptOrder(seller1, order1), () -> subOrders.sellerRejectOrder(seller1, order1, "Cannot fulfil"), 1);
        boolean rejected = orderState(order1) == OrderStatus.CANCELLED;
        assertEquals(rejected ? OrderStatus.CANCELLED : OrderStatus.PREPARING, orderState(order1));
        assertEquals(rejected ? 10 : 8, stock(product1));
        assertEquals(rejected ? money(120) : money(0), tx(() -> payments.findByOrderGroup_OrderGroupId(groupId).orElseThrow().getRefundedAmount()));
        assertEquals(OrderStatus.WAITING_SELLER_CONFIRM, orderState(order2));
    }

    void race(Runnable preload, Runnable first, Runnable second, int successes) throws Exception {
        CyclicBarrier barrier = new CyclicBarrier(2);
        try (var executor = Executors.newFixedThreadPool(2)) {
            List<Future<Boolean>> tasks = new ArrayList<>();
            for (Runnable work : List.of(first, second)) tasks.add(executor.submit(() -> {
                try { write(() -> { preload.run(); try { barrier.await(10, TimeUnit.SECONDS); } catch(Exception e) { throw new RuntimeException(e); } work.run(); }); return true; }
                catch (IllegalStateException expectedConflict) { return false; }
            }));
            int count = 0; for (var result : tasks) if (result.get(20, TimeUnit.SECONDS)) count++;
            assertEquals(successes, count);
        }
    }
}

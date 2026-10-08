package project.project.Security;

import java.math.BigDecimal;
import java.util.UUID;
import jakarta.servlet.Filter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;
import project.project.Entity.order.*;
import project.project.Entity.user.*;
import project.project.Repository.*;
import project.project.Service.api.AuthService;
import project.project.Service.api.PaymentService;
import project.project.Service.implement.PasswordService;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:payment-simulation;DB_CLOSE_DELAY=-1",
        "spring.datasource.username=sa", "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop", "spring.jpa.show-sql=false", "app.seed-data=false"
})
@Transactional
class PaymentSimulationIntegrationTest {
    @Autowired WebApplicationContext context;
    @Autowired @Qualifier("springSecurityFilterChain") Filter security;
    @Autowired UserRepository users;
    @Autowired CustomerRepository customers;
    @Autowired AddressRepository addresses;
    @Autowired OrderGroupRepository groups;
    @Autowired PaymentRepository payments;
    @Autowired AuthService auth;
    @Autowired PaymentService paymentService;
    @Autowired PasswordService passwords;
    MockMvc mvc;

    private record Fixture(String token, Long groupId, String transactionId) {}

    @BeforeEach void setUp() {
        mvc = MockMvcBuilders.webAppContextSetup(context).addFilters(security).build();
    }

    private User user(UserRole role) {
        String name = "mock-" + UUID.randomUUID();
        return users.save(new User(name, name + "@example.test", passwords.hash("sample123"),
                role, UserStatus.ACTIVE));
    }

    private Fixture fixture() {
        User user = user(UserRole.CUSTOMER);
        Customer customer = customers.save(new Customer(user, "Mock Customer", "0812345678"));
        Address address = addresses.save(new Address(null, customer, "Mock Customer", "0812345678",
                "1 Test Street", "Test District", "Bangkok", "10100", true));
        BigDecimal total = new BigDecimal("100.00");
        OrderGroup group = groups.save(new OrderGroup(null, UUID.randomUUID().toString(), customer,
                address, total, BigDecimal.ZERO, BigDecimal.ZERO, total, OrderGroupPaymentStatus.PENDING, null));
        Payment payment = paymentService.initiatePayment(group.getOrderGroupId(), PaymentMethod.PROMPTPAY);
        return new Fixture(auth.login(user.getUsername(), "sample123"), group.getOrderGroupId(),
                payment.getGatewayTransactionId());
    }

    private String groupBody(Fixture fixture) {
        return "{\"orderGroupId\":" + fixture.groupId() + ",\"isSuccess\":true}";
    }

    private String transactionBody(Fixture fixture) {
        return "{\"gatewayTransactionId\":\"" + fixture.transactionId() + "\",\"isSuccess\":true}";
    }

    private void assertPending(Fixture fixture) {
        assertEquals(PaymentStatus.PENDING, payments.findByOrderGroup_OrderGroupId(fixture.groupId()).orElseThrow().getStatus());
        assertEquals(OrderGroupPaymentStatus.PENDING, groups.findById(fixture.groupId()).orElseThrow().getPaymentStatus());
    }

    @Test void customerCanConfirmOwnMockPaymentByOrderGroupAndRetryIdempotently() throws Exception {
        Fixture own = fixture();
        for (int attempt = 0; attempt < 2; attempt++) {
            mvc.perform(post("/api/payments/simulate").header("Authorization", "Bearer " + own.token())
                    .contentType(MediaType.APPLICATION_JSON).content(groupBody(own)))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.data.status").value("SUCCESS"))
                    .andExpect(jsonPath("$.data.orderGroupId").value(own.groupId()));
        }
        assertEquals(OrderGroupPaymentStatus.PAID, groups.findById(own.groupId()).orElseThrow().getPaymentStatus());
    }

    @Test void customerCanConfirmOwnMockPaymentByTransactionId() throws Exception {
        Fixture own = fixture();
        mvc.perform(post("/api/payments/simulate").header("Authorization", "Bearer " + own.token())
                .contentType(MediaType.APPLICATION_JSON).content(transactionBody(own)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.status").value("SUCCESS"));
    }

    @Test void foreignGroupAndTransactionAreDeniedWithoutChangingPayment() throws Exception {
        Fixture own = fixture();
        Fixture foreign = fixture();
        for (String body : new String[] {groupBody(foreign), transactionBody(foreign),
                "{\"orderGroupId\":" + own.groupId() + ",\"gatewayTransactionId\":\""
                        + foreign.transactionId() + "\",\"isSuccess\":true}"}) {
            mvc.perform(post("/api/payments/simulate").header("Authorization", "Bearer " + own.token())
                    .contentType(MediaType.APPLICATION_JSON).content(body))
                    .andExpect(status().isForbidden());
            assertPending(own);
            assertPending(foreign);
        }
    }

    @Test void mismatchedGroupAndOwnedTransactionAreRejectedWithoutChangingPayment() throws Exception {
        Fixture own = fixture();
        Fixture foreign = fixture();
        mvc.perform(post("/api/payments/simulate").header("Authorization", "Bearer " + own.token())
                .contentType(MediaType.APPLICATION_JSON).content("{\"orderGroupId\":" + foreign.groupId()
                        + ",\"gatewayTransactionId\":\"" + own.transactionId() + "\",\"isSuccess\":true}"))
                .andExpect(status().isBadRequest());
        assertPending(own);
        assertPending(foreign);
    }

    @Test void anonymousInvalidTokensAndSellersCannotSimulate() throws Exception {
        Fixture own = fixture();
        mvc.perform(post("/api/payments/simulate").contentType(MediaType.APPLICATION_JSON).content(groupBody(own)))
                .andExpect(status().isUnauthorized());
        mvc.perform(post("/api/payments/simulate").header("Authorization", "Bearer invalid.token.value")
                .contentType(MediaType.APPLICATION_JSON).content(groupBody(own)))
                .andExpect(status().isUnauthorized());
        User seller = user(UserRole.SELLER);
        mvc.perform(post("/api/payments/simulate").header("Authorization", "Bearer " + auth.login(seller.getUsername(), "sample123"))
                .contentType(MediaType.APPLICATION_JSON).content(groupBody(own)))
                .andExpect(status().isForbidden());
        assertPending(own);
    }

    @Test void adminCanStillSimulateCustomerPayment() throws Exception {
        Fixture own = fixture();
        User admin = user(UserRole.ADMIN);
        mvc.perform(post("/api/payments/simulate").header("Authorization", "Bearer " + auth.login(admin.getUsername(), "sample123"))
                .contentType(MediaType.APPLICATION_JSON).content(groupBody(own)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.status").value("SUCCESS"));
    }

    @Test void missingPaymentIdentifierIsRejected() throws Exception {
        Fixture own = fixture();
        mvc.perform(post("/api/payments/simulate").header("Authorization", "Bearer " + own.token())
                .contentType(MediaType.APPLICATION_JSON).content("{\"isSuccess\":true}"))
                .andExpect(status().isBadRequest());
        assertPending(own);
    }
}

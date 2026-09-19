package project.project.Service;

import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.server.ResponseStatusException;

import project.project.Controller.GlobalExceptionHandler;
import project.project.Controller.OrderOrchestrationController;
import project.project.Controller.RequestUserResolver;
import project.project.Entity.order.OrderGroup;
import project.project.Entity.order.OrderGroupPaymentStatus;
import project.project.Entity.order.PaymentMethod;
import project.project.Service.api.OrderOrchestrationService;
import project.project.Service.api.ResourceOwnershipService;

import java.math.BigDecimal;
import java.security.Principal;
import java.util.Map;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class OrderOrchestrationControllerTest {

    @Mock
    OrderOrchestrationService orderService;

    @Mock
    RequestUserResolver userResolver;

    @Mock
    ResourceOwnershipService ownershipService;

    private MockMvc mvc;
    private final Principal principal = () -> "customer1";

    private static final String VALID_REQUEST = """
            {
              "customerId": 5,
              "shippingAddressId": 20,
              "sellerShippingMethods": {"3": "standard"},
              "paymentMethod": "PROMPTPAY"
            }
            """;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.standaloneSetup(
                new OrderOrchestrationController(
                        orderService,
                        userResolver,
                        ownershipService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void createOrder_returns201AndChecksOwnershipFirst() throws Exception {
        when(userResolver.requireUserId(principal)).thenReturn(1L);

        OrderGroup group = new OrderGroup();
        group.setOrderGroupId(100L);

        when(orderService.createOrderGroupFromCart(
                5L,
                20L,
                Map.of(3L, "standard"),
                PaymentMethod.PROMPTPAY)).thenReturn(group);

        mvc.perform(
                post("/api/order-groups")
                        .principal(principal)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_REQUEST))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").value(100));

        var ordered = inOrder(ownershipService, orderService);

        ordered.verify(ownershipService).requireCustomerOwner(1L, 5L);

        ordered.verify(orderService).createOrderGroupFromCart(
                5L,
                20L,
                Map.of(3L, "standard"),
                PaymentMethod.PROMPTPAY);
    }

    @Test
    void createOrderWithMissingFields_returns400WithoutCallingServices()
            throws Exception {

        mvc.perform(
                post("/api/order-groups")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.customerId").exists())
                .andExpect(jsonPath("$.error.shippingAddressId").exists())
                .andExpect(jsonPath("$.error.sellerShippingMethods").exists())
                .andExpect(jsonPath("$.error.paymentMethod").exists());

        verifyNoInteractions(orderService, ownershipService, userResolver);
    }

    @Test
    void createOrderWithInvalidPaymentMethod_returns400() throws Exception {
        mvc.perform(
                post("/api/order-groups")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(
                                VALID_REQUEST.replace(
                                        "PROMPTPAY",
                                        "INVALID")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));

        verifyNoInteractions(orderService, ownershipService, userResolver);
    }

    @Test
    void createOrderWithoutLogin_returns401() throws Exception {
        when(userResolver.requireUserId(null))
                .thenThrow(new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED,
                        "Login required"));

        mvc.perform(
                post("/api/order-groups")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_REQUEST))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false));

        verifyNoInteractions(orderService, ownershipService);
    }

    @Test
    void createOrderForAnotherCustomer_returns404WithoutCreating()
            throws Exception {

        when(userResolver.requireUserId(principal)).thenReturn(1L);

        doThrow(new EntityNotFoundException("Customer not found"))
                .when(ownershipService)
                .requireCustomerOwner(1L, 5L);

        mvc.perform(
                post("/api/order-groups")
                        .principal(principal)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_REQUEST))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false));

        verifyNoInteractions(orderService);
    }

    @Test
    void createOrderWithInsufficientStock_returns409() throws Exception {
        when(userResolver.requireUserId(principal)).thenReturn(1L);

        when(orderService.createOrderGroupFromCart(
                5L,
                20L,
                Map.of(3L, "standard"),
                PaymentMethod.PROMPTPAY)).thenThrow(new IllegalStateException("Insufficient stock"));

        mvc.perform(
                post("/api/order-groups")
                        .principal(principal)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_REQUEST))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Insufficient stock"));
    }

    @Test
    void getOrder_returnsDtoAfterOwnershipCheck() throws Exception {
        when(userResolver.requireUserId(principal)).thenReturn(1L);

        OrderGroup group = new OrderGroup();
        group.setOrderGroupId(100L);
        group.setGroupNumber("GROUP-100");
        group.setGrandTotal(new BigDecimal("540.00"));
        group.setPaymentStatus(OrderGroupPaymentStatus.PENDING);

        when(orderService.getOrderGroupDetails(100L)).thenReturn(group);

        mvc.perform(
                get("/api/order-groups/100")
                        .principal(principal))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.orderGroupId").value(100))
                .andExpect(jsonPath("$.data.grandTotal").value(540.0))
                .andExpect(jsonPath("$.data.paymentStatus").value("PENDING"))
                .andExpect(jsonPath("$.data.customer").doesNotExist());

        var ordered = inOrder(ownershipService, orderService);

        ordered.verify(ownershipService).requireOrderGroupOwner(1L, 100L);
        ordered.verify(orderService).getOrderGroupDetails(100L);
    }

    @Test
    void getOtherCustomersOrder_returns404WithoutReading() throws Exception {
        when(userResolver.requireUserId(principal)).thenReturn(1L);

        doThrow(new EntityNotFoundException("Order group not found"))
                .when(ownershipService)
                .requireOrderGroupOwner(1L, 100L);

        mvc.perform(
                get("/api/order-groups/100")
                        .principal(principal))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false));

        verifyNoInteractions(orderService);
    }
}
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

import project.project.Controller.AddressController;
import project.project.Controller.GlobalExceptionHandler;
import project.project.Controller.RequestUserResolver;
import project.project.DTO.address.CreateAddressRequest;
import project.project.Entity.user.Address;
import project.project.Service.api.AddressService;
import project.project.Service.api.ResourceOwnershipService;

import java.security.Principal;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class AddressControllerTest {

    @Mock
    private AddressService addressService;

    @Mock
    private RequestUserResolver userResolver;

    @Mock
    private ResourceOwnershipService ownershipService;

    private MockMvc mvc;
    private final Principal principal = () -> "customer1";

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.standaloneSetup(
                new AddressController(
                        addressService,
                        userResolver,
                        ownershipService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void addAddressShouldReturn201AndDto() throws Exception {
        when(userResolver.requireUserId(principal)).thenReturn(100L);

        Address address = new Address();
        address.setAddressId(10L);
        address.setReceiverName("Panda");
        address.setIsDefault(true);

        when(addressService.addAddressToCustomerByCustomerId(
                eq(1L), any(CreateAddressRequest.class)))
                .thenReturn(address);

        mvc.perform(post("/api/customers/1/addresses")
                .principal(principal)
                .contentType(MediaType.APPLICATION_JSON)
                .content(validRequest()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.addressId").value(10))
                .andExpect(jsonPath("$.data.receiverName").value("Panda"))
                .andExpect(jsonPath("$.data.isDefault").value(true))
                .andExpect(jsonPath("$.data.customer").doesNotExist());

        var ordered = inOrder(ownershipService, addressService);
        ordered.verify(ownershipService).requireCustomerOwner(100L, 1L);
        ordered.verify(addressService).addAddressToCustomerByCustomerId(
                eq(1L), any(CreateAddressRequest.class));
    }

    @Test
    void invalidRequestShouldReturn400WithoutCallingService()
            throws Exception {

        mvc.perform(post("/api/customers/1/addresses")
                .principal(principal)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.receiverName").exists());

        verifyNoInteractions(addressService);
    }

    @Test
    void getAddressesShouldReturnList() throws Exception {
        when(userResolver.requireUserId(principal)).thenReturn(100L);
        when(addressService.getAllAddressesByCustomerId(1L))
                .thenReturn(List.of());

        mvc.perform(get("/api/customers/1/addresses")
                .principal(principal))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(0));

        verify(ownershipService).requireCustomerOwner(100L, 1L);
    }

    @Test
    void anotherCustomersAddressesShouldBeRejected() throws Exception {
        when(userResolver.requireUserId(principal)).thenReturn(100L);

        doThrow(new EntityNotFoundException("Customer not found"))
                .when(ownershipService)
                .requireCustomerOwner(100L, 2L);

        mvc.perform(get("/api/customers/2/addresses")
                .principal(principal))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false));

        verifyNoInteractions(addressService);
    }

    @Test
    void missingLoginShouldReturn401() throws Exception {
        when(userResolver.requireUserId(null))
                .thenThrow(new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED, "Login required"));

        mvc.perform(get("/api/customers/1/addresses"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false));

        verifyNoInteractions(addressService, ownershipService);
    }

    @Test
    void deleteAddressShouldReturnSuccess() throws Exception {
        when(userResolver.requireUserId(principal)).thenReturn(100L);

        when(addressService
                .removeAddressCustomerByCustomerIdAndAddressId(1L, 10L))
                .thenReturn(true);

        mvc.perform(delete("/api/customers/1/addresses/10")
                .principal(principal))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        verify(ownershipService).requireCustomerOwner(100L, 1L);
    }

    @Test
    void missingAddressShouldReturn404() throws Exception {
        when(userResolver.requireUserId(principal)).thenReturn(100L);

        when(addressService
                .removeAddressCustomerByCustomerIdAndAddressId(1L, 99L))
                .thenReturn(false);

        mvc.perform(delete("/api/customers/1/addresses/99")
                .principal(principal))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false));
    }

    private String validRequest() {
        return """
                {
                  "customerId": 1,
                  "receiverName": "Panda",
                  "phoneNumber": "0812345678",
                  "addressLine": "123 Main Road",
                  "district": "Chatuchak",
                  "province": "Bangkok",
                  "postalCode": "10900",
                  "isDefault": true
                }
                """;
    }
}
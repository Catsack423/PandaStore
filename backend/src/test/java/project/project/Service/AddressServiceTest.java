package project.project.Service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import project.project.DTO.address.CreateAddressRequest;
import project.project.Entity.user.Address;
import project.project.Entity.user.Customer;
import project.project.Repository.AddressRepository;
import project.project.Repository.CustomerRepository;
import project.project.Repository.OrderGroupRepository;
import project.project.Service.implement.AddressServiceImp;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AddressServiceTest {

    @Mock
    private AddressRepository addressRepository;

    @Mock
    private CustomerRepository customerRepository;

    @Mock
    private OrderGroupRepository orderGroupRepository;

    private AddressServiceImp service;
    private Customer customer;

    @BeforeEach
    void setUp() {
        service = new AddressServiceImp(
                addressRepository,
                customerRepository,
                orderGroupRepository);

        customer = new Customer();
        customer.setCustomerId(1L);
    }

    @Test
    void firstAddressShouldBecomeDefault() {
        when(customerRepository.findByIdForUpdate(1L))
                .thenReturn(Optional.of(customer));

        when(addressRepository
                .findByCustomer_CustomerIdOrderByIsDefaultDescAddressIdAsc(1L))
                .thenReturn(List.of());

        when(addressRepository.save(any(Address.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Address result = service.addAddressToCustomerByCustomerId(
                1L, request(1L, false));

        assertSame(customer, result.getCustomer());
        assertEquals("Panda", result.getReceiverName());
        assertEquals(Boolean.TRUE, result.getIsDefault());

        verify(addressRepository).save(result);
    }

    @Test
    void newDefaultShouldClearPreviousDefault() {
        Address previous = address(10L, true);

        when(customerRepository.findByIdForUpdate(1L))
                .thenReturn(Optional.of(customer));

        when(addressRepository
                .findByCustomer_CustomerIdOrderByIsDefaultDescAddressIdAsc(1L))
                .thenReturn(List.of(previous));

        when(addressRepository.save(any(Address.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Address result = service.addAddressToCustomerByCustomerId(
                1L, request(1L, true));

        assertEquals(Boolean.FALSE, previous.getIsDefault());
        assertEquals(Boolean.TRUE, result.getIsDefault());
    }

    @Test
    void mismatchedCustomerIdShouldBeRejected() {
        assertThrows(
                IllegalArgumentException.class,
                () -> service.addAddressToCustomerByCustomerId(
                        1L, request(2L, false)));

        verifyNoInteractions(
                addressRepository,
                customerRepository,
                orderGroupRepository);
    }

    @Test
    void addressUsedByOrderShouldNotBeDeleted() {
        Address address = address(10L, true);

        when(customerRepository.findByIdForUpdate(1L))
                .thenReturn(Optional.of(customer));

        when(addressRepository.findOwnedAddressForUpdate(1L, 10L))
                .thenReturn(Optional.of(address));

        when(orderGroupRepository.existsByShippingAddress_AddressId(10L))
                .thenReturn(true);

        assertThrows(
                IllegalStateException.class,
                () -> service.removeAddressCustomerByCustomerIdAndAddressId(
                        1L, 10L));

        verify(addressRepository, never()).delete(any(Address.class));
        verify(addressRepository, never()).flush();
    }

    @Test
    void deletingDefaultShouldPromoteRemainingAddress() {
        Address oldDefault = address(10L, true);
        Address remaining = address(11L, false);

        when(customerRepository.findByIdForUpdate(1L))
                .thenReturn(Optional.of(customer));

        when(addressRepository.findOwnedAddressForUpdate(1L, 10L))
                .thenReturn(Optional.of(oldDefault));

        when(orderGroupRepository.existsByShippingAddress_AddressId(10L))
                .thenReturn(false);

        when(addressRepository
                .findByCustomer_CustomerIdOrderByIsDefaultDescAddressIdAsc(1L))
                .thenReturn(List.of(remaining));

        boolean result = service.removeAddressCustomerByCustomerIdAndAddressId(
                1L, 10L);

        assertTrue(result);
        assertEquals(Boolean.TRUE, remaining.getIsDefault());

        var ordered = inOrder(addressRepository);
        ordered.verify(addressRepository)
                .findOwnedAddressForUpdate(1L, 10L);
        ordered.verify(addressRepository).delete(oldDefault);
        ordered.verify(addressRepository).flush();
        ordered.verify(addressRepository)
                .findByCustomer_CustomerIdOrderByIsDefaultDescAddressIdAsc(1L);
    }

    @Test
    void deletingAddressNotOwnedByCustomerShouldReturnFalse() {
        when(customerRepository.findByIdForUpdate(1L))
                .thenReturn(Optional.of(customer));

        when(addressRepository.findOwnedAddressForUpdate(1L, 99L))
                .thenReturn(Optional.empty());

        boolean result = service.removeAddressCustomerByCustomerIdAndAddressId(
                1L, 99L);

        assertFalse(result);
        verify(addressRepository, never()).delete(any(Address.class));
        verifyNoInteractions(orderGroupRepository);
    }

    @Test
    void getAddressesShouldReturnCustomersAddresses() {
        Address address = address(10L, true);

        when(customerRepository.existsById(1L)).thenReturn(true);

        when(addressRepository
                .findByCustomer_CustomerIdOrderByIsDefaultDescAddressIdAsc(1L))
                .thenReturn(List.of(address));

        assertEquals(
                List.of(address),
                service.getAllAddressesByCustomerId(1L));
    }

    private Address address(Long id, boolean isDefault) {
        Address address = new Address();
        address.setAddressId(id);
        address.setCustomer(customer);
        address.setIsDefault(isDefault);
        return address;
    }

    private CreateAddressRequest request(
            Long customerId,
            boolean isDefault) {

        return new CreateAddressRequest(
                customerId,
                "Panda",
                "0812345678",
                "123 Main Road",
                "Chatuchak",
                "Bangkok",
                "10900",
                isDefault);
    }
}
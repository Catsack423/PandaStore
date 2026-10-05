package project.project.Service.api;

import project.project.DTO.address.CreateAddressRequest;
import project.project.Entity.user.Address;

import java.util.List;

public interface AddressService {

    Address setDefaultAddress(Long customerId, Long addressId);

    Address addAddressToCustomerByCustomerId(Long customerId, CreateAddressRequest request);

    Address updateAddress(Long customerId, Long addressId, CreateAddressRequest request);

    boolean removeAddressCustomerByCustomerIdAndAddressId(Long customerId, Long addressId);

    List<Address> getAllAddressesByCustomerId(Long customerId);
}

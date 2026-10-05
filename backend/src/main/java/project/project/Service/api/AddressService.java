package project.project.Service.api;

import project.project.DTO.address.CreateAddressRequest;
import project.project.Entity.user.Address;

import java.util.List;

public interface AddressService {

    Address addAddressToCustomerByCustomerId(Long customerId, CreateAddressRequest request);

    boolean removeAddressCustomerByCustomerIdAndAddressId(Long customerId, Long addressId);

    List<Address> getAllAddressesByCustomerId(Long customerId);
}
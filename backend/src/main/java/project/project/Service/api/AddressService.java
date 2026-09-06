package project.project.Service.api;

import java.util.List;

import project.project.DTO.address.CreateAddressRequest;
import project.project.DTO.customer.CreateCustomerRequest;
import project.project.Entity.user.Address;


public interface AddressService {
    Address addAddressToCustomerByCustomerId(Long customerId,CreateAddressRequest request);
    boolean removeAddressCustomerByCustomerIdAndAddressId(Long customerId,Long addressId);
    List<Address> getAllAddressesByCustomerId(Long customerId);
}

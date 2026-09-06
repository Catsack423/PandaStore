package project.project.Service.api;

import java.util.List;

import project.project.DTO.customer.CreateCustomerRequest;
import project.project.DTO.customer.CustomerResponse;
import project.project.DTO.customer.UpdateCustomerRequest;

public interface CustomerService {
    long createCustomer(CreateCustomerRequest request);

    boolean deleteCustomer(long id);

    boolean updateCustomer(long id, UpdateCustomerRequest request);

    CustomerResponse getCustomer(long id);

    List<CustomerResponse> getAllCustomer();
}

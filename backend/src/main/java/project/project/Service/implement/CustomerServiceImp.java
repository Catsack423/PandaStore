package project.project.Service.implement;

import java.util.List;

import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import jakarta.transaction.Transactional;
import project.project.DTO.customer.CreateCustomerRequest;
import project.project.DTO.customer.CustomerResponse;
import project.project.DTO.customer.UpdateCustomerRequest;
import project.project.Entity.user.Customer;
import project.project.Entity.user.User;
import project.project.Entity.user.UserRole;
import project.project.Entity.user.UserStatus;
import project.project.Exception.DuplicateUserException;
import project.project.Exception.UserCreationException;
import project.project.Repository.CustomerRepository;
import project.project.Repository.UserRepository;
import project.project.Service.api.CustomerService;

@Service
public class CustomerServiceImp implements CustomerService {

    private final UserRepository userRepository;
    private final CustomerRepository customerRepository;

    public CustomerServiceImp(UserRepository userRepository, CustomerRepository customerRepository) {
        this.userRepository = userRepository;
        this.customerRepository = customerRepository;
    }

    @Override
    @Transactional
    public long createCustomer(CreateCustomerRequest request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new DuplicateUserException("Username '" + request.getUsername() + "' ถูกใช้ไปแล้ว");
        }
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new DuplicateUserException("Email '" + request.getEmail() + "' ถูกใช้ไปแล้ว");
        }

        User user = new User(
            request.getUsername(),
            request.getEmail(),
            request.getPassword(),
            UserRole.CUSTOMER,
            UserStatus.ACTIVE
        );

        User savedUser;
        try {
            savedUser = userRepository.save(user);
        } catch (DataIntegrityViolationException e) {
            throw new DuplicateUserException("Username หรือ Email นี้ถูกใช้ไปแล้ว");
        } catch (DataAccessException e) {
            throw new UserCreationException("บันทึกข้อมูล User ไม่สำเร็จ", e);
        }

        try {
            Customer customer = new Customer(savedUser, request.getFullName(), request.getPhoneNumber());
            Customer savedCustomer = customerRepository.save(customer);
            return savedCustomer.getCustomerId();

        } catch (DataAccessException e) {
            throw new UserCreationException("บันทึกข้อมูล Customer ไม่สำเร็จ", e);
        }
    }

    @Override
    @Transactional
    public boolean deleteCustomer(long id) {
        if (customerRepository.existsById(id)) {
            customerRepository.deleteById(id);
            return true;
        }
        return false;
    }

    @Override
    @Transactional
    public boolean updateCustomer(long id, UpdateCustomerRequest request) {
        return customerRepository.findById(id).map(existing -> {
            if (request.getFullName() != null && !request.getFullName().isBlank()) {
                existing.setFullName(request.getFullName());
            }
            if (request.getPhoneNumber() != null && !request.getPhoneNumber().isBlank()) {
                existing.setPhoneNumber(request.getPhoneNumber());
            }
            customerRepository.save(existing);
            return true;
        }).orElse(false);
    }

    @Override
    public CustomerResponse getCustomer(long id) {
        return customerRepository.findById(id)
                .map(CustomerResponse::fromEntity)
                .orElse(null);
    }

    @Override
    public List<CustomerResponse> getAllCustomer() {
        return customerRepository.findAll().stream()
                .map(CustomerResponse::fromEntity)
                .toList();
    }
}

package project.project.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.test.context.SpringBootTest;

import jakarta.validation.Validation;
import jakarta.validation.ValidatorFactory;
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
import project.project.Service.implement.CustomerServiceImp;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
@SpringBootTest
public class CustomerServiceTest {
    @InjectMocks
    private CustomerServiceImp customerService;

    @org.mockito.Spy
    private project.project.Service.implement.PasswordService passwords =
            new project.project.Service.implement.PasswordService();

    @Mock
    private CustomerRepository customerRepository;

    @Mock
    private UserRepository userRepository;

    @Test
    void testCreateCustomerSuccess() {
        // Act
        customerService.getAllCustomer();

        // Assert: เปลี่ยนจาก userRepository เป็น customerRepository
        verify(customerRepository, times(1)).findAll();
    }

    @Test
    @DisplayName("กรณีพบ Customer: ต้องอัปเดตข้อมูล, บันทึกลง Repository และคืนค่า true")
    void testUpdateCustomer_Success() {
        // 1. Arrange (เตรียม Mock Data)
        long customerId = 1L;
        Customer existingCustomer = new Customer();
        existingCustomer.setCustomerId(customerId);
        existingCustomer.setFullName("Old Name");
        existingCustomer.setPhoneNumber("0800000000");

        UpdateCustomerRequest request = new UpdateCustomerRequest();
        request.setFullName("New Name");
        request.setPhoneNumber("0899999999");

        when(customerRepository.findById(customerId)).thenReturn(Optional.of(existingCustomer));
        when(customerRepository.save(any(Customer.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // 2. Act (เรียกใช้งานฟังก์ชัน)
        boolean result = customerService.updateCustomer(customerId, request);

        // 3. Assert (ตรวจสอบผลลัพธ์)
        assertTrue(result);
        assertEquals("New Name", existingCustomer.getFullName());
        assertEquals("0899999999", existingCustomer.getPhoneNumber());

        // ตรวจสอบว่า save() ถูกเรียกจริง 1 ครั้ง
        verify(customerRepository, times(1)).findById(customerId);
        verify(customerRepository, times(1)).save(existingCustomer);
    }

    @Test
    @DisplayName("กรณีไม่พบ Customer: ต้องไม่เรียกคำสั่ง save และคืนค่า false")
    void testUpdateCustomer_NotFound() {
        // 1. Arrange
        long nonExistingId = 999L;
        UpdateCustomerRequest request = new UpdateCustomerRequest();
        request.setFullName("Any Name");

        when(customerRepository.findById(nonExistingId)).thenReturn(Optional.empty());

        // 2. Act
        boolean result = customerService.updateCustomer(nonExistingId, request);

        // 3. Assert
        assertFalse(result);

        // ตรวจสอบว่าเช็ก findById แล้ว แต่ต้องไม่มีการเรียก save()
        verify(customerRepository, times(1)).findById(nonExistingId);
        verify(customerRepository, never()).save(any(Customer.class));
    }

    @Test
    void testGetCustomerByIdSuccess() {
        // Arrange
        User user1 = new User("User1", "[EMAIL_ADDRESS]", "pass123", UserRole.CUSTOMER, UserStatus.ACTIVE);
        User user2 = new User("User2", "[EMAIL_ADDRESS]", "pass456", UserRole.CUSTOMER, UserStatus.ACTIVE);
        User user3 = new User("User3", "[EMAIL_ADDRESS]", "pass789", UserRole.CUSTOMER, UserStatus.ACTIVE);
        User user4 = new User("User4", "[EMAIL_ADDRESS]", "pass101", UserRole.CUSTOMER, UserStatus.ACTIVE);

        Customer customer1 = new Customer(user1, "John Doe", "0123456789");
        Customer customer2 = new Customer(user2, "Jane Smith", "0123456788");
        Customer customer3 = new Customer(user3, "Alice Johnson", "0123456787");
        Customer customer4 = new Customer(user4, "Bob Brown", "0123456786");

        List<Customer> customers = new ArrayList<>();
        customers.add(customer1);
        customers.add(customer2);
        customers.add(customer3);
        customers.add(customer4);

        Mockito.when(customerRepository.findAll()).thenReturn(customers);

        // Act
        List<CustomerResponse> result = customerService.getAllCustomer();

        // Assert
        assertNotNull(result);
        assertEquals(4, result.size());
        assertEquals("User1", result.get(0).getUsername());
        assertEquals("John Doe", result.get(0).getFullName());
        assertEquals("0123456789", result.get(0).getPhoneNumber());
        assertEquals("User2", result.get(1).getUsername());
        assertEquals("Jane Smith", result.get(1).getFullName());
        assertEquals("0123456788", result.get(1).getPhoneNumber());
        assertEquals("User3", result.get(2).getUsername());
        assertEquals("Alice Johnson", result.get(2).getFullName());
        assertEquals("0123456787", result.get(2).getPhoneNumber());
        assertEquals("User4", result.get(3).getUsername());
        assertEquals("Bob Brown", result.get(3).getFullName());
        assertEquals("0123456786", result.get(3).getPhoneNumber());
    }

    @Test
    void CreateSuccessCustomer() {
        var request = new CreateCustomerRequest("Favce", "piyapon@gmail.com", "3432432443", "2313123", "02313213213");
        var user = new User("Favce", "piyapon@gmail.com", "3432432443", UserRole.CUSTOMER, UserStatus.ACTIVE);
        var customer = new Customer(user, "2313123", "02313213213");

        when(userRepository.existsByUsername(request.getUsername())).thenReturn(false);
        when(userRepository.existsByEmail(request.getEmail())).thenReturn(false);

        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Customer mockSavedCustomer = new Customer();
        mockSavedCustomer.setCustomerId(1L); // กำหนด ID จำลอง
        when(customerRepository.save(any(Customer.class))).thenReturn(mockSavedCustomer);
        var l = customerService.createCustomer(request);

        assertEquals(l, mockSavedCustomer.getCustomerId());
        verify(userRepository, times(1)).save(any(User.class));
        verify(customerRepository, times(1)).save(any(Customer.class));
    }

    @Test
    void CreateUserAlreadyHaved() {
        var request = new CreateCustomerRequest("Favce", "piyapon@gmail.com", "3432432443", "2313123", "02313213213");
        var user = new User("Favce", "piyapon@gmail.com", "3432432443", UserRole.CUSTOMER, UserStatus.ACTIVE);
        var customer = new Customer(user, "2313123", "02313213213");
        when(userRepository.existsByUsername(anyString())).thenReturn(true);

        assertThrowsExactly(DuplicateUserException.class, () -> {
            customerService.createCustomer(request);
        });
    }

    @Test
    void CreateUserAlreadyHaved2() {
        var request = new CreateCustomerRequest("Favce", "piyapon@gmail.com", "3432432443", "2313123", "02313213213");
        var user = new User("Favce", "piyapon@gmail.com", "3432432443", UserRole.CUSTOMER, UserStatus.ACTIVE);
        var customer = new Customer(user, "2313123", "02313213213");
        when(userRepository.existsByEmail(request.getEmail())).thenReturn(true);

        assertThrowsExactly(DuplicateUserException.class, () -> {
            customerService.createCustomer(request);
        });
    }

    @Test
    void nullCreate() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        var validator = factory.getValidator();
        var request = new CreateCustomerRequest("", "piyapon@gmail.com", "3432432443", "2313123", "02313213213");

        var violations = validator.validate(request);

        // ตรวจสอบว่ามี violation เกิดขึ้นจริง
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getMessage().contains("Username ห้ามว่าง")));
    }
}

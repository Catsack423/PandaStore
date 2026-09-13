package project.project.Service;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import project.project.DTO.seller.CreateSellerApplicationRequest;
import project.project.DTO.seller.SellerApplicationResponse;
import project.project.Entity.notification.NotificationType;
import project.project.Entity.seller.Seller;
import project.project.Entity.seller.SellerApplication;
import project.project.Entity.seller.SellerApplicationStatus;
import project.project.Entity.seller.SellerBankAccount;
import project.project.Entity.seller.SellerStatus;
import project.project.Entity.user.User;
import project.project.Entity.user.UserRole;
import project.project.Entity.user.UserStatus;
import project.project.Exception.ResourceNotFoundException;
import project.project.Repository.SellerApplicationRepository;
import project.project.Repository.SellerBankAccountRepository;
import project.project.Repository.SellerRepository;
import project.project.Repository.UserRepository;
import project.project.Service.api.NotificationService;
import project.project.Service.implement.SellerApplicationServiceImp;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class SellerApplicationServiceTest {

    @Mock
    private SellerApplicationRepository applicationRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private SellerRepository sellerRepository;

    @Mock
    private SellerBankAccountRepository sellerBankAccountRepository;

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private SellerApplicationServiceImp sellerApplicationService;

    @Captor
    private ArgumentCaptor<SellerApplication> applicationArgumentCaptor;

    private User sampleUser;
    private User adminUser;
    private SellerApplication sampleApplication;

    @BeforeEach
    void setUp() {
        sampleUser = new User();
        sampleUser.setUserId(1L);
        sampleUser.setUsername("seller_somchai");
        sampleUser.setEmail("somchai@shop.com");
        sampleUser.setRole(UserRole.CUSTOMER);
        sampleUser.setStatus(UserStatus.ACTIVE);

        adminUser = new User();
        adminUser.setUserId(99L);
        adminUser.setUsername("admin_super");
        adminUser.setEmail("admin@pandastore.com");
        adminUser.setRole(UserRole.ADMIN);
        adminUser.setStatus(UserStatus.ACTIVE);

        sampleApplication = new SellerApplication();
        sampleApplication.setShopName("สมชาย อิเล็กทรอนิกส์");
        sampleApplication.setShopDescription("ศูนย์รวมอุปกรณ์ไอทีและอิเล็กทรอนิกส์");
        sampleApplication.setShopPhone("0812345678");
        sampleApplication.setShopEmail("somchai@shop.com");
        sampleApplication.setShopAddress("99/1 ถ.สุขุมวิท กทม.");
        sampleApplication.setSellerFirstName("สมชาย");
        sampleApplication.setSellerLastName("ใจดี");
        sampleApplication.setIdCardNumber("1234567890123");
        sampleApplication.setIdCardImageUrl("https://cdn.example.com/idcards/123.jpg");
        sampleApplication.setBankAccountName("สมชาย ใจดี");
        sampleApplication.setBankName("ธนาคารกสิกรไทย");
        sampleApplication.setBankAccountNumber("0123456789");
        sampleApplication.setBankBookImageUrl("https://cdn.example.com/banks/book.jpg");
    }

    @Test
    @DisplayName("UC3 Step 9, 10, 11: กรอกข้อมูลครบถ้วน บันทึกสำเร็จ และกำหนดสถานะเป็น PENDING")
    void testSubmitApplication_Success_SetsStatusPendingAndSaves() {
        // Arrange
        when(userRepository.findById(1L)).thenReturn(Optional.of(sampleUser));
        when(applicationRepository.save(any(SellerApplication.class))).thenAnswer(invocation -> {
            SellerApplication app = invocation.getArgument(0);
            app.setApplicationId(501L);
            return app;
        });

        // Act
        SellerApplication result = sellerApplicationService.submitApplication(1L, sampleApplication);

        // Assert (Step 10 & 11)
        assertNotNull(result);
        assertEquals(501L, result.getApplicationId());
        assertEquals("สมชาย อิเล็กทรอนิกส์", result.getShopName());

        // ตรวจสอบ Step 11: สถานะต้องเป็น PENDING
        assertEquals(SellerApplicationStatus.PENDING, result.getStatus());

        // ตรวจสอบด้วย ArgumentCaptor ว่าข้อมูลก่อนส่งเข้า save() ถูกต้อง
        verify(applicationRepository, times(1)).save(applicationArgumentCaptor.capture());
        SellerApplication captured = applicationArgumentCaptor.getValue();
        assertEquals(SellerApplicationStatus.PENDING, captured.getStatus());
        assertEquals("1234567890123", captured.getIdCardNumber());
        assertEquals(sampleUser, captured.getUser());

        // ตรวจสอบ Notification ส่งหาแอดมิน
        verify(notificationService, times(1)).notifyAdminNewSellerApplication(501L);
    }

    @Test
    @DisplayName("UC3 Step 9: ส่งข้อมูล Entity เป็น null ต้องโยน IllegalArgumentException")
    void testSubmitApplication_NullApplication_ThrowsException() {
        assertThrows(IllegalArgumentException.class, () -> {
            sellerApplicationService.submitApplication(1L, (SellerApplication) null);
        });

        verify(applicationRepository, never()).save(any());
        verify(notificationService, never()).notifyAdminNewSellerApplication(any());
    }

    @Test
    @DisplayName("UC3 Step 9: ส่งข้อมูล DTO เป็น null ต้องโยน IllegalArgumentException")
    void testSubmitApplication_NullDto_ThrowsException() {
        assertThrows(IllegalArgumentException.class, () -> {
            sellerApplicationService.submitApplication(1L, (CreateSellerApplicationRequest) null);
        });

        verify(applicationRepository, never()).save(any());
        verify(notificationService, never()).notifyAdminNewSellerApplication(any());
    }

    @Test
    @DisplayName("UC3 Step 10: ไม่พบ User ในระบบ ต้องโยน ResourceNotFoundException")
    void testSubmitApplication_UserNotFound_ThrowsException() {
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> {
            sellerApplicationService.submitApplication(999L, sampleApplication);
        });

        verify(applicationRepository, never()).save(any());
    }

    @Test
    @DisplayName("UC3: แอดมินอนุมัติคำขอเปิดร้านสำเร็จ -> สร้าง Seller และ SellerBankAccount พร้อมเปลี่ยน Role User เป็น SELLER")
    void testApproveApplication_Success() {
        // Arrange
        sampleApplication.setApplicationId(501L);
        sampleApplication.setUser(sampleUser);
        sampleApplication.setStatus(SellerApplicationStatus.PENDING);

        when(applicationRepository.findById(501L)).thenReturn(Optional.of(sampleApplication));
        when(userRepository.findById(99L)).thenReturn(Optional.of(adminUser));
        when(sellerRepository.save(any(Seller.class))).thenAnswer(inv -> {
            Seller s = inv.getArgument(0);
            s.setSellerId(10L);
            return s;
        });

        // Act
        sellerApplicationService.approveApplication(501L, 99L);

        // Assert
        assertEquals(SellerApplicationStatus.APPROVED, sampleApplication.getStatus());
        assertEquals(adminUser, sampleApplication.getReviewedBy());
        assertNotNull(sampleApplication.getReviewedAt());

        // ตรวจสอบว่าอัปเดต Role ผู้ใช้เป็น SELLER
        assertEquals(UserRole.SELLER, sampleUser.getRole());
        verify(userRepository, times(1)).save(sampleUser);

        // ตรวจสอบการสร้าง Seller
        ArgumentCaptor<Seller> sellerCaptor = ArgumentCaptor.forClass(Seller.class);
        verify(sellerRepository, times(1)).save(sellerCaptor.capture());
        Seller savedSeller = sellerCaptor.getValue();
        assertEquals(sampleUser, savedSeller.getUser());
        assertEquals("สมชาย อิเล็กทรอนิกส์", savedSeller.getShopName());
        assertEquals(SellerStatus.ACTIVE, savedSeller.getStatus());

        // ตรวจสอบการสร้าง Bank Account
        ArgumentCaptor<SellerBankAccount> bankCaptor = ArgumentCaptor.forClass(SellerBankAccount.class);
        verify(sellerBankAccountRepository, times(1)).save(bankCaptor.capture());
        SellerBankAccount savedBank = bankCaptor.getValue();
        assertEquals("ธนาคารกสิกรไทย", savedBank.getBankName());
        assertEquals("0123456789", savedBank.getAccountNumber());
        assertEquals("สมชาย ใจดี", savedBank.getAccountName());

        // ตรวจสอบการส่ง Notification แจ้งผลให้ผู้ใช้
        verify(notificationService, times(1)).sendNotification(
                eq(1L),
                contains("อนุมัติ"),
                contains("สมชาย อิเล็กทรอนิกส์"),
                eq(NotificationType.SELLER_APPROVED)
        );
    }

    @Test
    @DisplayName("UC3: อนุมัติใบสมัครซ้ำ ต้องโยน IllegalStateException")
    void testApproveApplication_AlreadyApproved_ThrowsException() {
        sampleApplication.setStatus(SellerApplicationStatus.APPROVED);
        when(applicationRepository.findById(501L)).thenReturn(Optional.of(sampleApplication));
        when(userRepository.findById(99L)).thenReturn(Optional.of(adminUser));

        assertThrows(IllegalStateException.class, () -> {
            sellerApplicationService.approveApplication(501L, 99L);
        });

        verify(sellerRepository, never()).save(any());
    }

    @Test
    @DisplayName("UC3: แอดมินปฏิเสธคำขอเปิดร้าน พร้อมบันทึกเหตุผลและแจ้งเตือนผู้ใช้")
    void testRejectApplication_Success() {
        sampleApplication.setApplicationId(501L);
        sampleApplication.setUser(sampleUser);
        sampleApplication.setStatus(SellerApplicationStatus.PENDING);

        when(applicationRepository.findById(501L)).thenReturn(Optional.of(sampleApplication));
        when(userRepository.findById(99L)).thenReturn(Optional.of(adminUser));

        sellerApplicationService.rejectApplication(501L, 99L, "เอกสารบัตรประชาชนไม่ชัดเจน");

        assertEquals(SellerApplicationStatus.REJECTED, sampleApplication.getStatus());
        assertEquals("เอกสารบัตรประชาชนไม่ชัดเจน", sampleApplication.getAdminNote());
        assertEquals(adminUser, sampleApplication.getReviewedBy());
        assertNotNull(sampleApplication.getReviewedAt());

        verify(applicationRepository, times(1)).save(sampleApplication);
        verify(sellerRepository, never()).save(any());

        verify(notificationService, times(1)).sendNotification(
                eq(1L),
                contains("ถูกปฏิเสธ"),
                contains("เอกสารบัตรประชาชนไม่ชัดเจน"),
                eq(NotificationType.SELLER_APPROVED)
        );
    }

    @Test
    @DisplayName("UC3: แอดมินขอเอกสารเพิ่มเติม ปรับสถานะเป็น NEED_MORE_DOC พร้อมบันทึกข้อความ")
    void testRequestMoreDocuments_Success() {
        sampleApplication.setApplicationId(501L);
        sampleApplication.setUser(sampleUser);
        sampleApplication.setStatus(SellerApplicationStatus.PENDING);

        when(applicationRepository.findById(501L)).thenReturn(Optional.of(sampleApplication));
        when(userRepository.findById(99L)).thenReturn(Optional.of(adminUser));

        sellerApplicationService.requestMoreDocuments(501L, 99L, "โปรดแนบรูปถ่ายคู่กับบัตรประชาชน");

        assertEquals(SellerApplicationStatus.NEED_MORE_DOC, sampleApplication.getStatus());
        assertEquals("โปรดแนบรูปถ่ายคู่กับบัตรประชาชน", sampleApplication.getAdminNote());

        verify(applicationRepository, times(1)).save(sampleApplication);
        verify(notificationService, times(1)).sendNotification(
                eq(1L),
                contains("ขอเอกสารเพิ่มเติม"),
                contains("โปรดแนบรูปถ่ายคู่กับบัตรประชาชน"),
                eq(NotificationType.SELLER_APPROVED)
        );
    }

    @Test
    @DisplayName("UC3: ดึงใบสมัครตาม ID สำเร็จ")
    void testGetApplicationById_Success() {
        sampleApplication.setApplicationId(501L);
        when(applicationRepository.findById(501L)).thenReturn(Optional.of(sampleApplication));

        SellerApplication result = sellerApplicationService.getApplicationById(501L);

        assertNotNull(result);
        assertEquals(501L, result.getApplicationId());
    }

    @Test
    @DisplayName("UC3: ดึงใบสมัครตาม ID ไม่พบ ต้องโยน ResourceNotFoundException")
    void testGetApplicationById_NotFound_ThrowsException() {
        when(applicationRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> {
            sellerApplicationService.getApplicationById(999L);
        });
    }

    @Test
    @DisplayName("UC3: ดึงรายการใบสมัครสถานะ PENDING ทั้งหมดสำเร็จ")
    void testGetPendingApplications_Success() {
        sampleApplication.setStatus(SellerApplicationStatus.PENDING);
        when(applicationRepository.findByStatus(SellerApplicationStatus.PENDING))
                .thenReturn(List.of(sampleApplication));

        List<SellerApplication> result = sellerApplicationService.getPendingApplications();

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(SellerApplicationStatus.PENDING, result.get(0).getStatus());
    }

    @Test
    @DisplayName("UC3 Step 9, 10, 11: ส่งข้อมูลด้วย CreateSellerApplicationRequest DTO สำเร็จและตั้งสถานะ PENDING")
    void testSubmitApplication_WithDto_Success() {
        CreateSellerApplicationRequest req = new CreateSellerApplicationRequest(
                "สมชาย อิเล็กทรอนิกส์",
                "ศูนย์รวมอุปกรณ์ไอทีและอิเล็กทรอนิกส์",
                "0812345678",
                "somchai@shop.com",
                "99/1 ถ.สุขุมวิท กทม.",
                "สมชาย",
                "ใจดี",
                "1234567890123",
                "https://cdn.example.com/idcards/123.jpg",
                "สมชาย ใจดี",
                "ธนาคารกสิกรไทย",
                "0123456789",
                "https://cdn.example.com/banks/book.jpg"
        );

        when(userRepository.findById(1L)).thenReturn(Optional.of(sampleUser));
        when(applicationRepository.save(any(SellerApplication.class))).thenAnswer(invocation -> {
            SellerApplication a = invocation.getArgument(0);
            a.setApplicationId(701L);
            return a;
        });

        SellerApplication result = sellerApplicationService.submitApplication(1L, req);

        assertNotNull(result);
        assertEquals(701L, result.getApplicationId());
        assertEquals(SellerApplicationStatus.PENDING, result.getStatus());
        assertEquals("สมชาย อิเล็กทรอนิกส์", result.getShopName());

        verify(applicationRepository, times(1)).save(applicationArgumentCaptor.capture());
        SellerApplication captured = applicationArgumentCaptor.getValue();
        assertEquals("1234567890123", captured.getIdCardNumber());
        assertEquals(SellerApplicationStatus.PENDING, captured.getStatus());
        assertEquals(sampleUser, captured.getUser());

        verify(notificationService, times(1)).notifyAdminNewSellerApplication(701L);
    }

    @Test
    @DisplayName("UC3 Step 9: Bean Validation ใน DTO ตรวจจับข้อมูลว่างและเลขบัตรประชาชนไม่ถูกต้อง")
    void testCreateSellerApplicationRequest_BeanValidation_DetectsViolations() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        Validator validator = factory.getValidator();

        CreateSellerApplicationRequest req = new CreateSellerApplicationRequest();
        req.setShopName(""); // Blank
        req.setIdCardNumber("12345"); // Invalid pattern
        req.setShopEmail("invalid-email"); // Invalid email

        Set<ConstraintViolation<CreateSellerApplicationRequest>> violations = validator.validate(req);
        assertFalse(violations.isEmpty());

        List<String> invalidProperties = violations.stream()
                .map(v -> v.getPropertyPath().toString())
                .toList();

        assertTrue(invalidProperties.contains("shopName"));
        assertTrue(invalidProperties.contains("idCardNumber"));
        assertTrue(invalidProperties.contains("shopEmail"));
    }

    @Test
    @DisplayName("UC3: ดึงข้อมูลใบสมัครในรูปแบบ SellerApplicationResponse สำเร็จ")
    void testGetApplicationResponseById_Success() {
        sampleApplication.setApplicationId(501L);
        sampleApplication.setUser(sampleUser);
        when(applicationRepository.findById(501L)).thenReturn(Optional.of(sampleApplication));

        SellerApplicationResponse response = sellerApplicationService.getApplicationResponseById(501L);

        assertNotNull(response);
        assertEquals(501L, response.getApplicationId());
        assertEquals(1L, response.getUserId());
        assertEquals("สมชาย อิเล็กทรอนิกส์", response.getShopName());
        assertEquals(SellerApplicationStatus.PENDING, response.getStatus());
    }

    @Test
    @DisplayName("UC3: ดึงรายการใบสมัคร PENDING ในรูปแบบ List<SellerApplicationResponse> สำเร็จ")
    void testGetPendingApplicationResponses_Success() {
        sampleApplication.setApplicationId(501L);
        sampleApplication.setUser(sampleUser);
        when(applicationRepository.findByStatus(SellerApplicationStatus.PENDING)).thenReturn(List.of(sampleApplication));

        List<SellerApplicationResponse> responses = sellerApplicationService.getPendingApplicationResponses();

        assertNotNull(responses);
        assertEquals(1, responses.size());
        assertEquals(501L, responses.get(0).getApplicationId());
        assertEquals("สมชาย อิเล็กทรอนิกส์", responses.get(0).getShopName());
    }
}

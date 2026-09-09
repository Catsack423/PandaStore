package project.project.Service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import project.project.Entity.notification.NotificationType;
import project.project.Entity.seller.Seller;
import project.project.Entity.seller.SellerApplication;
import project.project.Entity.seller.SellerApplicationStatus;
import project.project.Entity.seller.SellerBankAccount;
import project.project.Entity.seller.SellerStatus;
import project.project.Entity.user.User;
import project.project.Entity.user.UserRole;
import project.project.Entity.user.UserStatus;
import project.project.Exception.InvalidApplicationDataException;
import project.project.Exception.ResourceNotFoundException;
import project.project.Repository.SellerApplicationRepository;
import project.project.Repository.SellerBankAccountRepository;
import project.project.Repository.SellerRepository;
import project.project.Repository.UserRepository;
import project.project.Service.api.NotificationService;
import project.project.Service.implement.SellerApplicationServiceImp;

import java.util.List;
import java.util.Optional;

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
    @DisplayName("UC3 Step 9 (13A): ข้อมูลไม่ครบถ้วน ต้องแจ้งเตือนและไม่บันทึกข้อมูล")
    void testSubmitApplication_IncompleteData_ThrowsExceptionAndNeverSaves() {
        // Arrange: ข้อมูลชื่อร้านว่างเปล่า
        sampleApplication.setShopName("");

        // Act & Assert
        assertThrows(InvalidApplicationDataException.class, () -> {
            sellerApplicationService.submitApplication(1L, sampleApplication);
        });

        // ตรวจสอบว่าไม่เคยเรียก save และไม่ส่ง notification
        verify(applicationRepository, never()).save(any());
        verify(notificationService, never()).notifyAdminNewSellerApplication(any());
    }

    @Test
    @DisplayName("UC3 Step 9: เลขบัตรประชาชนไม่ครบ 13 หลัก ต้องโยน InvalidApplicationDataException")
    void testSubmitApplication_InvalidIdCard_ThrowsException() {
        sampleApplication.setIdCardNumber("12345"); // Not 13 digits

        assertThrows(InvalidApplicationDataException.class, () -> {
            sellerApplicationService.submitApplication(1L, sampleApplication);
        });

        verify(applicationRepository, never()).save(any());
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
        SellerApplication application = new SellerApplication();
        application.setApplicationId(501L);
        application.setUser(sampleUser);
        application.setShopName("สมชาย อิเล็กทรอนิกส์");
        application.setShopDescription("อุปกรณ์ไอที");
        application.setShopPhone("0812345678");
        application.setShopEmail("somchai@shop.com");
        application.setShopAddress("กทม.");
        application.setBankName("กสิกรไทย");
        application.setBankAccountNumber("0123456789");
        application.setBankAccountName("สมชาย ใจดี");
        application.setBankBookImageUrl("https://cdn.example.com/bank.jpg");
        application.setStatus(SellerApplicationStatus.PENDING);

        when(applicationRepository.findById(501L)).thenReturn(Optional.of(application));
        when(userRepository.findById(99L)).thenReturn(Optional.of(adminUser));
        when(sellerRepository.save(any(Seller.class))).thenAnswer(invocation -> {
            Seller s = invocation.getArgument(0);
            s.setSellerId(10L);
            return s;
        });

        // Act
        sellerApplicationService.approveApplication(501L, 99L);

        // Assert
        assertEquals(SellerApplicationStatus.APPROVED, application.getStatus());
        assertEquals(adminUser, application.getReviewedBy());
        assertNotNull(application.getReviewedAt());
        assertEquals(UserRole.SELLER, sampleUser.getRole());

        verify(applicationRepository, times(1)).save(application);
        verify(userRepository, times(1)).save(sampleUser);
        verify(sellerRepository, times(1)).save(any(Seller.class));
        verify(sellerBankAccountRepository, times(1)).save(any(SellerBankAccount.class));
        verify(notificationService, times(1)).sendNotification(
                eq(sampleUser.getUserId()),
                eq("คำขอเปิดร้านค้าได้รับการอนุมัติ"),
                anyString(),
                eq(NotificationType.SELLER_APPROVED)
        );
    }

    @Test
    @DisplayName("UC3: แอดมินปฏิเสธคำขอเปิดร้าน -> สถานะเป็น REJECTED พร้อมบันทึก adminNote")
    void testRejectApplication_Success() {
        SellerApplication application = new SellerApplication();
        application.setApplicationId(501L);
        application.setUser(sampleUser);
        application.setStatus(SellerApplicationStatus.PENDING);

        when(applicationRepository.findById(501L)).thenReturn(Optional.of(application));
        when(userRepository.findById(99L)).thenReturn(Optional.of(adminUser));

        sellerApplicationService.rejectApplication(501L, 99L, "เอกสารบัตรประชาชนไม่ชัดเจน");

        assertEquals(SellerApplicationStatus.REJECTED, application.getStatus());
        assertEquals("เอกสารบัตรประชาชนไม่ชัดเจน", application.getAdminNote());
        assertEquals(adminUser, application.getReviewedBy());
        verify(applicationRepository, times(1)).save(application);
    }

    @Test
    @DisplayName("UC3: แอดมินขอเอกสารเพิ่มเติม -> สถานะเป็น NEED_MORE_DOC")
    void testRequestMoreDocuments_Success() {
        SellerApplication application = new SellerApplication();
        application.setApplicationId(501L);
        application.setUser(sampleUser);
        application.setStatus(SellerApplicationStatus.PENDING);

        when(applicationRepository.findById(501L)).thenReturn(Optional.of(application));
        when(userRepository.findById(99L)).thenReturn(Optional.of(adminUser));

        sellerApplicationService.requestMoreDocuments(501L, 99L, "กรุณาแนบรูปหน้าสมุดบัญชีใหม่");

        assertEquals(SellerApplicationStatus.NEED_MORE_DOC, application.getStatus());
        assertEquals("กรุณาแนบรูปหน้าสมุดบัญชีใหม่", application.getAdminNote());
        verify(applicationRepository, times(1)).save(application);
    }

    @Test
    @DisplayName("ดึงรายการคำขอเปิดร้านที่รอตรวจสอบ (PENDING)")
    void testGetPendingApplications() {
        SellerApplication app1 = new SellerApplication();
        app1.setApplicationId(1L);
        app1.setStatus(SellerApplicationStatus.PENDING);

        when(applicationRepository.findByStatus(SellerApplicationStatus.PENDING))
                .thenReturn(List.of(app1));

        List<SellerApplication> pending = sellerApplicationService.getPendingApplications();
        assertEquals(1, pending.size());
        assertEquals(1L, pending.get(0).getApplicationId());
    }

    @Test
    @DisplayName("UC3 Step 9, 10, 11: ส่งข้อมูลด้วย SellerApplication Entity โดยตรง")
    void testSubmitApplication_WithEntity_Success() {
        SellerApplication app = new SellerApplication();
        app.setShopName("สมชาย อิเล็กทรอนิกส์");
        app.setShopDescription("ศูนย์รวมอุปกรณ์ไอทีและอิเล็กทรอนิกส์");
        app.setShopPhone("0812345678");
        app.setShopEmail("somchai@shop.com");
        app.setShopAddress("99/1 ถ.สุขุมวิท กทม.");
        app.setSellerFirstName("สมชาย");
        app.setSellerLastName("ใจดี");
        app.setIdCardNumber("1234567890123");
        app.setIdCardImageUrl("https://cdn.example.com/idcards/123.jpg");
        app.setBankAccountName("สมชาย ใจดี");
        app.setBankName("ธนาคารกสิกรไทย");
        app.setBankAccountNumber("0123456789");
        app.setBankBookImageUrl("https://cdn.example.com/banks/book.jpg");

        when(userRepository.findById(1L)).thenReturn(Optional.of(sampleUser));
        when(applicationRepository.save(any(SellerApplication.class))).thenAnswer(invocation -> {
            SellerApplication a = invocation.getArgument(0);
            a.setApplicationId(601L);
            return a;
        });

        SellerApplication result = sellerApplicationService.submitApplication(1L, app);

        assertNotNull(result);
        assertEquals(601L, result.getApplicationId());
        assertEquals(SellerApplicationStatus.PENDING, result.getStatus());
        assertEquals(sampleUser, result.getUser());
        verify(applicationRepository, times(1)).save(app);
    }
}

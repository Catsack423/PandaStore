package project.project.Service.implement;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import project.project.Entity.notification.NotificationType;
import project.project.Entity.seller.Seller;
import project.project.Entity.seller.SellerApplication;
import project.project.Entity.seller.SellerApplicationStatus;
import project.project.Entity.seller.SellerBankAccount;
import project.project.Entity.seller.SellerStatus;
import project.project.Entity.user.User;
import project.project.Entity.user.UserRole;
import project.project.Exception.InvalidApplicationDataException;
import project.project.Exception.ResourceNotFoundException;
import project.project.Repository.SellerApplicationRepository;
import project.project.Repository.SellerBankAccountRepository;
import project.project.Repository.SellerRepository;
import project.project.Repository.UserRepository;
import project.project.Service.api.NotificationService;
import project.project.Service.api.SellerApplicationService;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class SellerApplicationServiceImp implements SellerApplicationService {

    private final SellerApplicationRepository applicationRepository;
    private final UserRepository userRepository;
    private final SellerRepository sellerRepository;
    private final SellerBankAccountRepository sellerBankAccountRepository;
    private final NotificationService notificationService;

    public SellerApplicationServiceImp(SellerApplicationRepository applicationRepository,
                                       UserRepository userRepository,
                                       SellerRepository sellerRepository,
                                       SellerBankAccountRepository sellerBankAccountRepository,
                                       @Autowired(required = false) NotificationService notificationService) {
        this.applicationRepository = applicationRepository;
        this.userRepository = userRepository;
        this.sellerRepository = sellerRepository;
        this.sellerBankAccountRepository = sellerBankAccountRepository;
        this.notificationService = notificationService;
    }

    //     แบบที่ 2: submit ใบสมัคร → create seller
    // User กรอกฟอร์มสมัคร (เก็บใน SellerApplication) → Submit → Admin review → Approve → สร้าง Seller record จริง
        
    @Override
    @Transactional
    public SellerApplication submitApplication(Long userId, SellerApplication application) {
        if (userId == null) {
            throw new IllegalArgumentException("User ID cannot be null");
        }
        validateApplicationData(application);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        application.setUser(user);
        application.setStatus(SellerApplicationStatus.PENDING);

        SellerApplication saved = applicationRepository.save(application);

        if (notificationService != null) {
            notificationService.notifyAdminNewSellerApplication(saved.getApplicationId());
        }

        return saved;
    }



    @Override
    @Transactional
    public void approveApplication(Long applicationId, Long adminId) {
        if (applicationId == null || adminId == null) {
            throw new IllegalArgumentException("Application ID and Admin ID cannot be null");
        }
        SellerApplication application = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new ResourceNotFoundException("Application not found with id: " + applicationId));

        User admin = userRepository.findById(adminId)
                .orElseThrow(() -> new ResourceNotFoundException("Admin not found with id: " + adminId));

        if (application.getStatus() == SellerApplicationStatus.APPROVED) {
            throw new IllegalStateException("Application has already been approved");
        }

        application.setStatus(SellerApplicationStatus.APPROVED);
        application.setReviewedBy(admin);
        application.setReviewedAt(LocalDateTime.now());
        applicationRepository.save(application);

        User user = application.getUser();
        user.setRole(UserRole.SELLER);
        userRepository.save(user);

        Seller seller = new Seller();
        seller.setUser(user);
        seller.setShopName(application.getShopName());
        seller.setShopDescription(application.getShopDescription());
        seller.setShopPhone(application.getShopPhone());
        seller.setShopEmail(application.getShopEmail());
        seller.setShopAddress(application.getShopAddress());
        seller.setStatus(SellerStatus.ACTIVE);
        seller.setRating(BigDecimal.ZERO);
        Seller savedSeller = sellerRepository.save(seller);

        SellerBankAccount bankAccount = new SellerBankAccount();
        bankAccount.setSeller(savedSeller);
        bankAccount.setBankName(application.getBankName());
        bankAccount.setAccountNumber(application.getBankAccountNumber());
        bankAccount.setAccountName(application.getBankAccountName());
        bankAccount.setProofImageUrl(application.getBankBookImageUrl());
        sellerBankAccountRepository.save(bankAccount);

        if (notificationService != null) {
            notificationService.sendNotification(
                    user.getUserId(),
                    "คำขอเปิดร้านค้าได้รับการอนุมัติ",
                    "ยินดีด้วย! ร้านค้า " + application.getShopName() + " ได้รับการอนุมัติแล้ว",
                    NotificationType.SELLER_APPROVED
            );
        }
    }

    @Override
    @Transactional
    public void rejectApplication(Long applicationId, Long adminId, String reason) {
        if (applicationId == null || adminId == null) {
            throw new IllegalArgumentException("Application ID and Admin ID cannot be null");
        }
        SellerApplication application = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new ResourceNotFoundException("Application not found with id: " + applicationId));

        User admin = userRepository.findById(adminId)
                .orElseThrow(() -> new ResourceNotFoundException("Admin not found with id: " + adminId));

        application.setStatus(SellerApplicationStatus.REJECTED);
        application.setAdminNote(reason);
        application.setReviewedBy(admin);
        application.setReviewedAt(LocalDateTime.now());
        applicationRepository.save(application);

        if (notificationService != null && application.getUser() != null) {
            notificationService.sendNotification(
                    application.getUser().getUserId(),
                    "คำขอเปิดร้านค้าถูกปฏิเสธ",
                    "คำขอเปิดร้านค้าของคุณถูกปฏิเสธเนื่องจาก: " + reason,
                    NotificationType.SELLER_APPROVED
            );
        }
    }

    @Override
    @Transactional
    public void requestMoreDocuments(Long applicationId, Long adminId, String message) {
        if (applicationId == null || adminId == null) {
            throw new IllegalArgumentException("Application ID and Admin ID cannot be null");
        }
        SellerApplication application = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new ResourceNotFoundException("Application not found with id: " + applicationId));

        User admin = userRepository.findById(adminId)
                .orElseThrow(() -> new ResourceNotFoundException("Admin not found with id: " + adminId));

        application.setStatus(SellerApplicationStatus.NEED_MORE_DOC);
        application.setAdminNote(message);
        application.setReviewedBy(admin);
        application.setReviewedAt(LocalDateTime.now());
        applicationRepository.save(application);

        if (notificationService != null && application.getUser() != null) {
            notificationService.sendNotification(
                    application.getUser().getUserId(),
                    "ขอเอกสารเพิ่มเติมสำหรับการสมัครร้านค้า",
                    "โปรดส่งเอกสารเพิ่มเติม: " + message,
                    NotificationType.SELLER_APPROVED
            );
        }
    }

    @Override
    public SellerApplication getApplicationById(Long applicationId) {
        if (applicationId == null) {
            throw new IllegalArgumentException("Application ID cannot be null");
        }
        return applicationRepository.findById(applicationId)
                .orElseThrow(() -> new ResourceNotFoundException("Application not found with id: " + applicationId));
    }

    @Override
    public List<SellerApplication> getPendingApplications() {
        return applicationRepository.findByStatus(SellerApplicationStatus.PENDING);
    }

    //[Major] User Dto instead
    private void validateApplicationData(SellerApplication app) {
        if (app == null) {
            throw new InvalidApplicationDataException("ข้อมูลใบสมัครห้ามเป็นค่าว่าง");
        }
        if (isBlank(app.getShopName())) {
            throw new InvalidApplicationDataException("ชื่อร้านค้าห้ามว่าง");
        }
        if (isBlank(app.getShopDescription())) {
            throw new InvalidApplicationDataException("รายละเอียดร้านค้าห้ามว่าง");
        }
        if (isBlank(app.getShopPhone())) {
            throw new InvalidApplicationDataException("เบอร์โทรศัพท์ร้านค้าห้ามว่าง");
        }
        if (isBlank(app.getShopEmail())) {
            throw new InvalidApplicationDataException("อีเมลร้านค้าห้ามว่าง");
        }
        if (isBlank(app.getShopAddress())) {
            throw new InvalidApplicationDataException("ที่อยู่ร้านค้าห้ามว่าง");
        }
        if (isBlank(app.getSellerFirstName())) {
            throw new InvalidApplicationDataException("ชื่อผู้ขายห้ามว่าง");
        }
        if (isBlank(app.getSellerLastName())) {
            throw new InvalidApplicationDataException("นามสกุลผู้ขายห้ามว่าง");
        }
        if (isBlank(app.getIdCardNumber()) || !app.getIdCardNumber().matches("^[0-9]{13}$")) {
            throw new InvalidApplicationDataException("เลขประจำตัวประชาชนต้องเป็นตัวเลข 13 หลัก");
        }
        if (isBlank(app.getIdCardImageUrl())) {
            throw new InvalidApplicationDataException("รูปภาพบัตรประชาชนห้ามว่าง");
        }
        if (isBlank(app.getBankAccountName())) {
            throw new InvalidApplicationDataException("ชื่อบัญชีธนาคารห้ามว่าง");
        }
        if (isBlank(app.getBankName())) {
            throw new InvalidApplicationDataException("ชื่อธนาคารห้ามว่าง");
        }
        if (isBlank(app.getBankAccountNumber())) {
            throw new InvalidApplicationDataException("เลขที่บัญชีธนาคารห้ามว่าง");
        }
        if (isBlank(app.getBankBookImageUrl())) {
            throw new InvalidApplicationDataException("รูปภาพสมุดบัญชีห้ามว่าง");
        }
    }

    private boolean isBlank(String str) {
        return str == null || str.trim().isEmpty();
    }
}

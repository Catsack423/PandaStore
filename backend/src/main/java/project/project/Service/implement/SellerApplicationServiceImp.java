package project.project.Service.implement;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
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

    // แบบที่ 2: submit ใบสมัคร → create seller
    // User กรอกฟอร์มสมัคร (ตรวจสอบ Format ผ่าน Bean Validation ใน DTO) → Submit → Admin review → Approve → สร้าง Seller record จริง

    @Override
    @Transactional
    public SellerApplication submitApplication(Long userId, CreateSellerApplicationRequest request) {
        if (userId == null) {
            throw new IllegalArgumentException("User ID cannot be null");
        }
        if (request == null) {
            throw new IllegalArgumentException("Application request cannot be null");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        SellerApplication application = new SellerApplication();
        application.setUser(user);
        application.setShopName(request.getShopName());
        application.setShopDescription(request.getShopDescription());
        application.setShopPhone(request.getShopPhone());
        application.setShopEmail(request.getShopEmail());
        application.setShopAddress(request.getShopAddress());
        application.setSellerFirstName(request.getSellerFirstName());
        application.setSellerLastName(request.getSellerLastName());
        application.setIdCardNumber(request.getIdCardNumber());
        application.setIdCardImageUrl(request.getIdCardImageUrl());
        application.setBankAccountName(request.getBankAccountName());
        application.setBankName(request.getBankName());
        application.setBankAccountNumber(request.getBankAccountNumber());
        application.setBankBookImageUrl(request.getBankBookImageUrl());
        application.setStatus(SellerApplicationStatus.PENDING);
        application.setCreatedAt(LocalDateTime.now());

        SellerApplication saved = applicationRepository.save(application);

        if (notificationService != null) {
            notificationService.notifyAdminNewSellerApplication(saved.getApplicationId());
        }

        return saved;
    }

    @Override
    @Transactional
    public SellerApplication submitApplication(Long userId, SellerApplication application) {
        if (userId == null) {
            throw new IllegalArgumentException("User ID cannot be null");
        }
        if (application == null) {
            throw new IllegalArgumentException("Application cannot be null");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        application.setUser(user);
        application.setStatus(SellerApplicationStatus.PENDING);
        if (application.getCreatedAt() == null) {
            application.setCreatedAt(LocalDateTime.now());
        }

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
    public SellerApplicationResponse getApplicationResponseById(Long applicationId) {
        SellerApplication application = getApplicationById(applicationId);
        return SellerApplicationResponse.fromEntity(application);
    }

    @Override
    public List<SellerApplication> getPendingApplications() {
        return applicationRepository.findByStatus(SellerApplicationStatus.PENDING);
    }

    @Override
    public List<SellerApplicationResponse> getPendingApplicationResponses() {
        return getPendingApplications().stream()
                .map(SellerApplicationResponse::fromEntity)
                .toList();
    }
}

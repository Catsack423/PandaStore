package project.project.Service.implement;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import project.project.DTO.seller.CreateSellerRequest;
import project.project.DTO.seller.UpdateSellerRequest;
import project.project.Entity.seller.Seller;
import project.project.Entity.seller.SellerApplication;
import project.project.Entity.seller.SellerApplicationStatus;
import project.project.Entity.seller.SellerBankAccount;
import project.project.Entity.seller.SellerStatus;
import project.project.Entity.user.User;
import project.project.Entity.user.UserRole;
import project.project.Entity.user.UserStatus;
import project.project.Exception.DuplicateUserException;
import project.project.Exception.UserCreationException;
import project.project.Repository.SellerApplicationRepository;
import project.project.Repository.SellerBankAccountRepository;
import project.project.Repository.SellerRepository;
import project.project.Repository.UserRepository;
import project.project.Service.api.SellerService;

@Service
@Transactional
public class SellerServiceImp implements SellerService {

    private final UserRepository userRepository;
    private final SellerRepository sellerRepository;
    private final SellerApplicationRepository sellerApplicationRepository;
    private final SellerBankAccountRepository sellerBankAccountRepository;

    public SellerServiceImp(UserRepository userRepository,
                            SellerRepository sellerRepository,
                            SellerApplicationRepository sellerApplicationRepository,
                            SellerBankAccountRepository sellerBankAccountRepository) {
        this.userRepository = userRepository;
        this.sellerRepository = sellerRepository;
        this.sellerApplicationRepository = sellerApplicationRepository;
        this.sellerBankAccountRepository = sellerBankAccountRepository;
    }

    @Override
    public long createSeller(CreateSellerRequest request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new DuplicateUserException("Username '" + request.getUsername() + "' ถูกใช้ไปแล้ว");
        }
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new DuplicateUserException("Email '" + request.getEmail() + "' ถูกใช้ไปแล้ว");
        }
        if (sellerRepository.existsByShopName(request.getShopName())) {
            throw new DuplicateUserException("ชื่อร้านค้า '" + request.getShopName() + "' ถูกใช้ไปแล้ว");
        }

        User user = new User(
                request.getUsername(),
                request.getEmail(),
                request.getPassword(),
                UserRole.SELLER,
                UserStatus.ACTIVE);

        User savedUser;
        try {
            savedUser = userRepository.save(user);
        } catch (DataIntegrityViolationException e) {
            throw new DuplicateUserException("Username หรือ Email นี้ถูกใช้ไปแล้ว");
        } catch (DataAccessException e) {
            throw new UserCreationException("บันทึกข้อมูล User ไม่สำเร็จ", e);
        }

        Seller seller = new Seller();
        seller.setUser(savedUser);
        seller.setShopName(request.getShopName());
        seller.setShopDescription(request.getShopDescription());
        seller.setShopPhone(request.getShopPhone());
        seller.setShopEmail(request.getShopEmail());
        seller.setShopAddress(request.getShopAddress());
        seller.setStatus(SellerStatus.PENDING);
        seller.setRating(BigDecimal.ZERO);

        Seller savedSeller;
        try {
            savedSeller = sellerRepository.save(seller);
        } catch (DataIntegrityViolationException e) {
            throw new DuplicateUserException("ชื่อร้านค้า '" + request.getShopName() + "' ถูกใช้ไปแล้ว");
        } catch (DataAccessException e) {
            throw new UserCreationException("บันทึกข้อมูล Seller ไม่สำเร็จ", e);
        }

        try {
            SellerApplication application = new SellerApplication();
            application.setUser(savedUser);
            application.setShopName(request.getShopName());
            application.setShopDescription(request.getShopDescription());
            application.setShopPhone(request.getShopPhone());
            application.setShopEmail(request.getShopEmail());
            application.setShopAddress(request.getShopAddress());
            application.setSellerFirstName(request.getSellerFirstName());
            application.setSellerLastName(request.getSellerLastName());
            application.setIdCardNumber(request.getIdCardNumber());
            application.setIdCardImageUrl(request.getIdCardImageUrl() != null ? request.getIdCardImageUrl() : "");
            application.setBankName(request.getBankName());
            application.setBankAccountName(request.getBankAccountName());
            application.setBankAccountNumber(request.getBankAccountNumber());
            application.setBankBookImageUrl(request.getBankBookImageUrl() != null ? request.getBankBookImageUrl() : "");
            application.setStatus(SellerApplicationStatus.PENDING);
            sellerApplicationRepository.save(application);

            SellerBankAccount bankAccount = new SellerBankAccount();
            bankAccount.setSeller(savedSeller);
            bankAccount.setBankName(request.getBankName());
            bankAccount.setAccountNumber(request.getBankAccountNumber());
            bankAccount.setAccountName(request.getBankAccountName());
            bankAccount.setProofImageUrl(request.getProofImageUrl() != null ? request.getProofImageUrl() : "");
            sellerBankAccountRepository.save(bankAccount);

            return savedSeller.getSellerId();
        } catch (DataAccessException e) {
            throw new UserCreationException("บันทึกข้อมูลใบสมัครหรือบัญชีธนาคารร้านค้าไม่สำเร็จ", e);
        }
    }

    @Override
    public boolean deleteSeller(long id) {
        if (sellerRepository.existsById(id)) {
            sellerRepository.deleteById(id);
            return true;
        }
        return false;
    }

    @Override
    public boolean updateSeller(long id, UpdateSellerRequest request) {
        return sellerRepository.findById(id).map(existing -> {
            if (request.getShopName() != null && !request.getShopName().isBlank()) {
                existing.setShopName(request.getShopName());
            }
            if (request.getShopDescription() != null && !request.getShopDescription().isBlank()) {
                existing.setShopDescription(request.getShopDescription());
            }
            if (request.getShopPhone() != null && !request.getShopPhone().isBlank()) {
                existing.setShopPhone(request.getShopPhone());
            }
            if (request.getShopEmail() != null && !request.getShopEmail().isBlank()) {
                existing.setShopEmail(request.getShopEmail());
            }
            if (request.getShopAddress() != null && !request.getShopAddress().isBlank()) {
                existing.setShopAddress(request.getShopAddress());
            }
            sellerRepository.save(existing);
            return true;
        }).orElse(false);
    }

    @Override
    @Transactional(readOnly = true)
    public Seller getSeller(long id) {
        return sellerRepository.findById(id).orElse(null);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Seller> getAllSeller() {
        return sellerRepository.findAll();
    }
}


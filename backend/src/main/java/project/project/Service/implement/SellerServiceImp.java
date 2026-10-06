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
import project.project.Entity.seller.SellerBankAccount;
import project.project.Entity.seller.SellerStatus;
import project.project.Entity.user.User;
import project.project.Entity.user.UserRole;
import project.project.Entity.user.UserStatus;
import project.project.Exception.DuplicateUserException;
import project.project.Exception.UserCreationException;
import project.project.Repository.SellerBankAccountRepository;
import project.project.Repository.SellerRepository;
import project.project.Repository.UserRepository;
import project.project.Service.api.SellerApplicationService;
import project.project.Service.api.SellerService;

@Service
@Transactional
public class SellerServiceImp implements SellerService {

    private final UserRepository userRepository;
    private final SellerRepository sellerRepository;
    private final SellerApplicationService sellerApplicationService;
    private final SellerBankAccountRepository sellerBankAccountRepository;
    private final PasswordService passwords;

    public SellerServiceImp(UserRepository userRepository,
                            SellerRepository sellerRepository,
                            SellerApplicationService sellerApplicationService,
                            SellerBankAccountRepository sellerBankAccountRepository, PasswordService passwords) {
        this.userRepository = userRepository;
        this.sellerRepository = sellerRepository;
        this.sellerApplicationService = sellerApplicationService;
        this.sellerBankAccountRepository = sellerBankAccountRepository;
        this.passwords = passwords;
    }

    @Override
    public Seller createSeller(CreateSellerRequest request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new DuplicateUserException("Username '" + request.getUsername() + "' is already in use");
        }
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new DuplicateUserException("Email '" + request.getEmail() + "' is already in use");
        }
        if (sellerRepository.existsByShopName(request.getShopName())) {
            throw new DuplicateUserException("Shop name '" + request.getShopName() + "' is already in use");
        }

        User user = new User(
                request.getUsername(),
                request.getEmail(),
                passwords.hash(request.getPassword()),
                UserRole.CUSTOMER,
                UserStatus.ACTIVE);

        User savedUser;
        try {
            savedUser = userRepository.save(user);
        } catch (DataIntegrityViolationException e) {
            throw new DuplicateUserException("Username or email is already in use");
        } catch (DataAccessException e) {
            throw new UserCreationException("Unable to save user", e);
        }

        // Submit before creating the pending shop: application submission rejects existing sellers.
        sellerApplicationService.submitApplication(savedUser.getUserId(), request.toSellerApplicationRequest());
        savedUser.setRole(UserRole.SELLER);
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
            throw new DuplicateUserException("Shop name '" + request.getShopName() + "' is already in use");
        } catch (DataAccessException e) {
            throw new UserCreationException("Unable to save seller", e);
        }

        try {
            SellerBankAccount bankAccount = new SellerBankAccount();
            bankAccount.setSeller(savedSeller);
            bankAccount.setBankName(request.getBankName());
            bankAccount.setAccountNumber(request.getBankAccountNumber());
            bankAccount.setAccountName(request.getBankAccountName());
            bankAccount.setProofImageUrl(request.getProofImageUrl() != null ? request.getProofImageUrl() : "");
            sellerBankAccountRepository.save(bankAccount);

            return savedSeller;
        } catch (DataAccessException e) {
            throw new UserCreationException("Unable to save application or shop bank account", e);
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

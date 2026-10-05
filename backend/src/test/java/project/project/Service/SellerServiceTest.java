package project.project.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import project.project.DTO.seller.CreateSellerApplicationRequest;
import project.project.DTO.seller.CreateSellerRequest;
import project.project.DTO.seller.UpdateSellerRequest;
import project.project.Entity.seller.Seller;
import project.project.Entity.seller.SellerApplication;
import project.project.Entity.seller.SellerBankAccount;
import project.project.Entity.seller.SellerStatus;
import project.project.Entity.user.User;
import project.project.Entity.user.UserRole;
import project.project.Entity.user.UserStatus;
import project.project.Exception.DuplicateUserException;
import project.project.Repository.SellerApplicationRepository;
import project.project.Repository.SellerBankAccountRepository;
import project.project.Repository.SellerRepository;
import project.project.Repository.UserRepository;
import project.project.Service.api.SellerApplicationService;
import project.project.Service.implement.SellerServiceImp;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class SellerServiceTest {

    @InjectMocks
    private SellerServiceImp sellerService;

    @Mock
    private UserRepository userRepository;

    @Mock
    private SellerRepository sellerRepository;

    @Mock
    private SellerApplicationService sellerApplicationService;

    @Mock
    private SellerBankAccountRepository sellerBankAccountRepository;

    private CreateSellerRequest sampleRequest() {
        return new CreateSellerRequest(
                "testSeller", "seller@example.com", "password123", "Shop Name",
                "Shop description", "0812345678", "shop@example.com", "Bangkok",
                "Somchai", "Jaidee", "1234567890123", "Kasikorn",
                "Somchai Jaidee", "1234567890", "id.png", "book.png", "proof.png"
        );
    }

    @Test
    @DisplayName("createSeller สำเร็จ: สร้าง User, Seller, SellerApplication, SellerBankAccount")
    void testCreateSellerSuccess() {
        var request = sampleRequest();

        when(userRepository.existsByUsername(request.getUsername())).thenReturn(false);
        when(userRepository.existsByEmail(request.getEmail())).thenReturn(false);
        when(sellerRepository.existsByShopName(request.getShopName())).thenReturn(false);

        User savedUser = new User(request.getUsername(), request.getEmail(), request.getPassword(), UserRole.SELLER, UserStatus.ACTIVE);
        savedUser.setUserId(1L);
        when(userRepository.save(any(User.class))).thenReturn(savedUser);

        Seller savedSeller = new Seller();
        savedSeller.setSellerId(10L);
        savedSeller.setUser(savedUser);
        savedSeller.setShopName(request.getShopName());
        savedSeller.setStatus(SellerStatus.PENDING);
        savedSeller.setRating(BigDecimal.ZERO);
        when(sellerRepository.save(any(Seller.class))).thenReturn(savedSeller);

        long sellerId = sellerService.createSeller(request);

        assertEquals(10L, sellerId);
        verify(userRepository, times(1)).save(any(User.class));
        verify(sellerRepository, times(1)).save(any(Seller.class));
        verify(sellerApplicationService, times(1)).submitApplication(eq(1L), any(CreateSellerApplicationRequest.class));
        verify(sellerBankAccountRepository, times(1)).save(any(SellerBankAccount.class));
    }

    @Test
    @DisplayName("createSeller ล้มเหลวเมื่อ username ซ้ำ")
    void testCreateSellerDuplicateUsername() {
        var request = sampleRequest();
        when(userRepository.existsByUsername(request.getUsername())).thenReturn(true);

        assertThrows(DuplicateUserException.class, () -> sellerService.createSeller(request));
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("createSeller ล้มเหลวเมื่อ email ซ้ำ")
    void testCreateSellerDuplicateEmail() {
        var request = sampleRequest();
        when(userRepository.existsByUsername(request.getUsername())).thenReturn(false);
        when(userRepository.existsByEmail(request.getEmail())).thenReturn(true);

        assertThrows(DuplicateUserException.class, () -> sellerService.createSeller(request));
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("createSeller ล้มเหลวเมื่อ shopName ซ้ำ")
    void testCreateSellerDuplicateShopName() {
        var request = sampleRequest();
        when(userRepository.existsByUsername(request.getUsername())).thenReturn(false);
        when(userRepository.existsByEmail(request.getEmail())).thenReturn(false);
        when(sellerRepository.existsByShopName(request.getShopName())).thenReturn(true);

        assertThrows(DuplicateUserException.class, () -> sellerService.createSeller(request));
        verify(sellerRepository, never()).save(any(Seller.class));
    }

    @Test
    @DisplayName("updateSeller สำเร็จเมื่อพบ seller")
    void testUpdateSellerSuccess() {
        Seller existing = new Seller();
        existing.setSellerId(1L);
        existing.setShopName("Old Shop");
        existing.setShopPhone("0811111111");

        UpdateSellerRequest updateReq = new UpdateSellerRequest();
        updateReq.setShopName("New Shop");
        updateReq.setShopPhone("0822222222");

        when(sellerRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(sellerRepository.save(any(Seller.class))).thenAnswer(inv -> inv.getArgument(0));

        boolean updated = sellerService.updateSeller(1L, updateReq);

        assertTrue(updated);
        assertEquals("New Shop", existing.getShopName());
        assertEquals("0822222222", existing.getShopPhone());
        verify(sellerRepository, times(1)).save(existing);
    }

    @Test
    @DisplayName("updateSeller คืน false เมื่อไม่พบ seller")
    void testUpdateSellerNotFound() {
        when(sellerRepository.findById(999L)).thenReturn(Optional.empty());

        boolean updated = sellerService.updateSeller(999L, new UpdateSellerRequest());

        assertFalse(updated);
        verify(sellerRepository, never()).save(any(Seller.class));
    }

    @Test
    @DisplayName("deleteSeller สำเร็จเมื่อมี ID")
    void testDeleteSellerSuccess() {
        when(sellerRepository.existsById(1L)).thenReturn(true);

        boolean deleted = sellerService.deleteSeller(1L);

        assertTrue(deleted);
        verify(sellerRepository, times(1)).deleteById(1L);
    }

    @Test
    @DisplayName("deleteSeller คืน false เมื่อไม่มี ID")
    void testDeleteSellerNotFound() {
        when(sellerRepository.existsById(999L)).thenReturn(false);

        boolean deleted = sellerService.deleteSeller(999L);

        assertFalse(deleted);
        verify(sellerRepository, never()).deleteById(anyLong());
    }

    @Test
    @DisplayName("getSeller และ getAllSeller คืนค่าถูกต้อง")
    void testGetSellerAndGetAll() {
        Seller seller = new Seller();
        seller.setSellerId(1L);
        seller.setShopName("Shop 1");

        when(sellerRepository.findById(1L)).thenReturn(Optional.of(seller));
        when(sellerRepository.findAll()).thenReturn(List.of(seller));

        Seller found = sellerService.getSeller(1L);
        assertNotNull(found);
        assertEquals("Shop 1", found.getShopName());

        List<Seller> all = sellerService.getAllSeller();
        assertEquals(1, all.size());
    }
}


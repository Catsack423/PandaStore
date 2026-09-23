package project.project.Service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import project.project.Entity.seller.Seller;
import project.project.Entity.seller.SellerBankAccount;
import project.project.Entity.seller.SellerStatus;
import project.project.Entity.user.User;
import project.project.Repository.SellerBankAccountRepository;
import project.project.Repository.SellerRepository;
import project.project.Service.implement.SellerShopServiceImp;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class SellerShopServiceTest {

    @InjectMocks
    private SellerShopServiceImp sellerShopService;

    @Mock
    private SellerRepository sellerRepository;

    @Mock
    private SellerBankAccountRepository sellerBankAccountRepository;

    @Test
    @DisplayName("getShopBySellerId - สำเร็จเมื่อพบร้านค้า")
    void getShopBySellerId_Success() {
        Long sellerId = 1L;
        Seller mockSeller = new Seller();
        mockSeller.setSellerId(sellerId);
        mockSeller.setShopName("Panda Official Shop");

        when(sellerRepository.findById(sellerId)).thenReturn(Optional.of(mockSeller));

        Seller result = sellerShopService.getShopBySellerId(sellerId);

        assertNotNull(result);
        assertEquals("Panda Official Shop", result.getShopName());
        verify(sellerRepository, times(1)).findById(sellerId);
    }

    @Test
    @DisplayName("getShopBySellerId - โยน RuntimeException เมื่อไม่พบร้านค้า")
    void getShopBySellerId_NotFound_ThrowsException() {
        Long sellerId = 999L;
        when(sellerRepository.findById(sellerId)).thenReturn(Optional.empty());

        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            sellerShopService.getShopBySellerId(sellerId);
        });

        assertTrue(exception.getMessage().contains("ไม่พบร้านค้า sellerId: 999"));
        verify(sellerRepository, times(1)).findById(sellerId);
    }

    @Test
    @DisplayName("getShopByUserId - สำเร็จเมื่อพบร้านค้าตาม userId")
    void getShopByUserId_Success() {
        Long userId = 10L;
        User user = new User();
        user.setUserId(userId);

        Seller seller = new Seller();
        seller.setSellerId(1L);
        seller.setUser(user);
        seller.setShopName("My Store");

        when(sellerRepository.findByUser_UserId(userId)).thenReturn(Optional.of(seller));

        Seller result = sellerShopService.getShopByUserId(userId);

        assertNotNull(result);
        assertEquals("My Store", result.getShopName());
        verify(sellerRepository, times(1)).findByUser_UserId(userId);
    }

    @Test
    @DisplayName("getShopByUserId - โยน RuntimeException เมื่อไม่พบร้านค้าตาม userId")
    void getShopByUserId_NotFound_ThrowsException() {
        Long userId = 888L;
        when(sellerRepository.findByUser_UserId(userId)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> {
            sellerShopService.getShopByUserId(userId);
        });
    }

    @Test
    @DisplayName("updateShopProfile - สำเร็จเมื่อร้านค้าสถานะ ACTIVE")
    void updateShopProfile_Success() {
        Long sellerId = 1L;
        Seller existing = new Seller();
        existing.setSellerId(sellerId);
        existing.setStatus(SellerStatus.ACTIVE);
        existing.setShopName("Old Name");
        existing.setShopPhone("0811111111");

        Seller updateInfo = new Seller();
        updateInfo.setShopName("New Shop Name");
        updateInfo.setShopPhone("0822222222");
        updateInfo.setShopEmail("new@shop.com");
        updateInfo.setShopAddress("123 Bangkok");

        when(sellerRepository.findById(sellerId)).thenReturn(Optional.of(existing));
        when(sellerRepository.save(any(Seller.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Seller result = sellerShopService.updateShopProfile(sellerId, updateInfo);

        assertNotNull(result);
        assertEquals("New Shop Name", result.getShopName());
        assertEquals("0822222222", result.getShopPhone());
        assertEquals("new@shop.com", result.getShopEmail());
        assertEquals("123 Bangkok", result.getShopAddress());
        verify(sellerRepository, times(1)).save(existing);
    }

    @Test
    @DisplayName("updateShopProfile - โยน Exception เมื่อร้านค้าสถานะไม่ใช่ ACTIVE")
    void updateShopProfile_NotActive_ThrowsException() {
        Long sellerId = 1L;
        Seller existing = new Seller();
        existing.setSellerId(sellerId);
        existing.setStatus(SellerStatus.PENDING); // Not ACTIVE

        Seller updateInfo = new Seller();
        updateInfo.setShopName("New Shop Name");

        when(sellerRepository.findById(sellerId)).thenReturn(Optional.of(existing));

        RuntimeException ex = assertThrows(RuntimeException.class, () -> {
            sellerShopService.updateShopProfile(sellerId, updateInfo);
        });

        assertTrue(ex.getMessage().contains("ไม่สามารถแก้ไขร้านค้าที่มีสถานะ"));
        verify(sellerRepository, never()).save(any());
    }

    @Test
    @DisplayName("addOrUpdateBankAccount - เพิ่มบัญชีใหม่เมื่อยังไม่มีในระบบ")
    void addOrUpdateBankAccount_CreateNew() {
        Long sellerId = 1L;
        Seller seller = new Seller();
        seller.setSellerId(sellerId);

        SellerBankAccount newAccount = new SellerBankAccount();
        newAccount.setBankName("Kasikornbank");
        newAccount.setAccountNumber("1234567890");
        newAccount.setAccountName("Panda Co., Ltd.");

        when(sellerRepository.findById(sellerId)).thenReturn(Optional.of(seller));
        when(sellerBankAccountRepository.findBySeller_SellerId(sellerId)).thenReturn(Optional.empty());
        when(sellerBankAccountRepository.save(any(SellerBankAccount.class))).thenAnswer(inv -> inv.getArgument(0));

        SellerBankAccount result = sellerShopService.addOrUpdateBankAccount(sellerId, newAccount);

        assertNotNull(result);
        assertEquals(seller, result.getSeller());
        assertEquals("Kasikornbank", result.getBankName());
        verify(sellerBankAccountRepository, times(1)).save(newAccount);
    }

    @Test
    @DisplayName("addOrUpdateBankAccount - อัปเดตบัญชีเดิมเมื่อมีข้อมูลอยู่แล้ว")
    void addOrUpdateBankAccount_UpdateExisting() {
        Long sellerId = 1L;
        Seller seller = new Seller();
        seller.setSellerId(sellerId);

        SellerBankAccount existing = new SellerBankAccount();
        existing.setBankAccountId(10L);
        existing.setSeller(seller);
        existing.setBankName("SCB");
        existing.setAccountNumber("0000000000");

        SellerBankAccount updateData = new SellerBankAccount();
        updateData.setBankName("Bangkok Bank");
        updateData.setAccountNumber("9999999999");
        updateData.setAccountName("Updated Account Name");

        when(sellerRepository.findById(sellerId)).thenReturn(Optional.of(seller));
        when(sellerBankAccountRepository.findBySeller_SellerId(sellerId)).thenReturn(Optional.of(existing));
        when(sellerBankAccountRepository.save(any(SellerBankAccount.class))).thenAnswer(inv -> inv.getArgument(0));

        SellerBankAccount result = sellerShopService.addOrUpdateBankAccount(sellerId, updateData);

        assertNotNull(result);
        assertEquals(10L, result.getBankAccountId());
        assertEquals("Bangkok Bank", result.getBankName());
        assertEquals("9999999999", result.getAccountNumber());
        verify(sellerBankAccountRepository, times(1)).save(existing);
    }

    @Test
    @DisplayName("addOrUpdateBankAccount - โยน Exception เมื่อไม่พบ Seller")
    void addOrUpdateBankAccount_SellerNotFound_ThrowsException() {
        Long sellerId = 999L;
        when(sellerRepository.findById(sellerId)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> {
            sellerShopService.addOrUpdateBankAccount(sellerId, new SellerBankAccount());
        });
    }

    @Test
    @DisplayName("getBankAccountBySellerId - สำเร็จเมื่อพบบัญชีธนาคาร")
    void getBankAccountBySellerId_Success() {
        Long sellerId = 1L;
        SellerBankAccount account = new SellerBankAccount();
        account.setBankAccountId(5L);
        account.setBankName("Kasikornbank");

        when(sellerBankAccountRepository.findBySeller_SellerId(sellerId)).thenReturn(Optional.of(account));

        SellerBankAccount result = sellerShopService.getBankAccountBySellerId(sellerId);

        assertNotNull(result);
        assertEquals("Kasikornbank", result.getBankName());
    }

    @Test
    @DisplayName("getBankAccountBySellerId - โยน Exception เมื่อไม่พบบัญชี")
    void getBankAccountBySellerId_NotFound_ThrowsException() {
        Long sellerId = 999L;
        when(sellerBankAccountRepository.findBySeller_SellerId(sellerId)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> {
            sellerShopService.getBankAccountBySellerId(sellerId);
        });
    }
}

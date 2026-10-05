package project.project.Controller;

import java.util.Map;
import project.project.ApiResponse.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import project.project.Security.CurrentUser;
import project.project.Security.BearerTokens;
import org.springframework.security.core.context.SecurityContextHolder;
import project.project.DTO.auth.AuthRequests;
import project.project.DTO.auth.CurrentUserResponse;
import project.project.DTO.auth.LoginResponse;
import org.springframework.http.CacheControl;
import org.springframework.beans.factory.annotation.Autowired;
import project.project.Repository.CustomerRepository;
import project.project.Repository.SellerRepository;
import project.project.Repository.UserRepository;
import project.project.DTO.customer.CustomerResponse;
import project.project.Service.api.AuthService;
import project.project.Service.implement.PasswordService;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final AuthService auth;
    private final CurrentUser currentUser;
    private final PasswordService passwords;

    private CustomerRepository customerRepository;
    private SellerRepository sellerRepository;
    private UserRepository userRepository;

    public AuthController(AuthService auth, CurrentUser currentUser, PasswordService passwords) {
        this.auth = auth;
        this.currentUser = currentUser;
        this.passwords = passwords;
    }

    @Autowired
    public AuthController(AuthService auth, CurrentUser currentUser, PasswordService passwords,
            @Autowired(required = false) CustomerRepository customerRepository,
            @Autowired(required = false) SellerRepository sellerRepository,
            @Autowired(required = false) UserRepository userRepository) {
        this.auth = auth;
        this.currentUser = currentUser;
        this.passwords = passwords;
        this.customerRepository = customerRepository;
        this.sellerRepository = sellerRepository;
        this.userRepository = userRepository;
    }

    @PostMapping("/register/customer")
    public ResponseEntity<ApiResponse<CustomerResponse>> registerCustomer(
            @Valid @RequestBody AuthRequests.RegisterCustomer request) {
        var customer = auth.registerCustomer(request.username(), request.email(), request.password(),
                request.confirmPassword(), request.fullName(), request.phoneNumber());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("สมัครสมาชิกสำเร็จ", CustomerResponse.fromEntity(customer)));
    }

    @PostMapping("/register/seller")
    public ResponseEntity<ApiResponse<Map<String, Long>>> registerSeller(
            @Valid @RequestBody AuthRequests.RegisterSeller request) {
        var seller = auth.registerSeller(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("สมัครผู้ขายสำเร็จ", Map.of("sellerId", seller.getSellerId())));
    }

    @PostMapping("/login")
    public ApiResponse<LoginResponse> login(@Valid @RequestBody AuthRequests.Login request) {
        try {
            return ApiResponse.success("เข้าสู่ระบบสำเร็จ",
                    auth.loginWithUser(request.usernameOrEmail(), request.password()));
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "ชื่อผู้ใช้หรือรหัสผ่านไม่ถูกต้อง");
        }
    }

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<CurrentUserResponse>> me() {
        var user = currentUser.requireUser();
        String fullName = null;
        String phoneNumber = null;
        if (user.getRole() == project.project.Entity.user.UserRole.CUSTOMER && customerRepository != null) {
            var customerOpt = customerRepository.findByUser_UserId(user.getUserId());
            if (customerOpt.isPresent()) {
                fullName = customerOpt.get().getFullName();
                phoneNumber = customerOpt.get().getPhoneNumber();
            }
        } else if (user.getRole() == project.project.Entity.user.UserRole.SELLER && sellerRepository != null) {
            var sellerOpt = sellerRepository.findByUser_UserId(user.getUserId());
            if (sellerOpt.isPresent()) {
                fullName = sellerOpt.get().getShopName();
                phoneNumber = sellerOpt.get().getShopPhone();
            }
        } else {
            fullName = user.getUsername();
        }
        return ResponseEntity.ok().cacheControl(CacheControl.noStore())
                .body(ApiResponse.success("ดึงข้อมูลผู้ใช้ปัจจุบันสำเร็จ",
                        CurrentUserResponse.from(user, fullName, phoneNumber)));
    }

    @PutMapping("/me")
    public ResponseEntity<ApiResponse<CurrentUserResponse>> updateProfile(
            @Valid @RequestBody AuthRequests.UpdateProfile request) {
        var user = currentUser.requireUser();
        if (request.email() != null && !request.email().isBlank() && userRepository != null) {
            user.setEmail(request.email().trim());
            userRepository.save(user);
        }
        String fullName = null;
        String phoneNumber = null;
        if (user.getRole() == project.project.Entity.user.UserRole.CUSTOMER && customerRepository != null) {
            var customer = customerRepository.findByUser_UserId(user.getUserId())
                    .orElseGet(() -> new project.project.Entity.user.Customer(
                            user,
                            request.name() != null && !request.name().isBlank() ? request.name().trim() : user.getUsername(),
                            request.phone() != null ? request.phone().trim() : null));
            if (request.name() != null && !request.name().isBlank()) {
                customer.setFullName(request.name().trim());
            }
            if (request.phone() != null) {
                customer.setPhoneNumber(request.phone().trim());
            }
            customerRepository.save(customer);
            fullName = customer.getFullName();
            phoneNumber = customer.getPhoneNumber();
        } else if (user.getRole() == project.project.Entity.user.UserRole.SELLER && sellerRepository != null) {
            var sellerOpt = sellerRepository.findByUser_UserId(user.getUserId());
            if (sellerOpt.isPresent()) {
                var seller = sellerOpt.get();
                if (request.name() != null && !request.name().isBlank()) {
                    seller.setShopName(request.name().trim());
                }
                if (request.phone() != null) {
                    seller.setShopPhone(request.phone().trim());
                }
                sellerRepository.save(seller);
                fullName = seller.getShopName();
                phoneNumber = seller.getShopPhone();
            }
        } else {
            if (request.name() != null && !request.name().isBlank() && userRepository != null) {
                user.setUsername(request.name().trim());
                userRepository.save(user);
            }
            fullName = user.getUsername();
        }
        return ResponseEntity.ok()
                .body(ApiResponse.success("อัปเดตข้อมูลผู้ใช้สำเร็จ",
                        CurrentUserResponse.from(user, fullName, phoneNumber)));
    }

    @GetMapping("/token")
    public ApiResponse<Map<String, Boolean>> validateToken(
            @RequestHeader(value = "Authorization", required = false) String authorization) {
        return ApiResponse.success("ตรวจสอบโทเคนสำเร็จ",
                Map.of("valid", auth.validateToken(project.project.Security.BearerTokens.requireToken(authorization))));
    }

    @PostMapping("/logout")
    public ApiResponse<Void> logout(
            @RequestHeader(value = "Authorization", required = false) String authorization) {
        // The bearer token identifies this session, not every login belonging to the
        // user.
        if (!auth.logout(BearerTokens.requireToken(authorization))) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "โทเคนไม่ถูกต้องหรือหมดอายุ");
        }
        SecurityContextHolder.clearContext();
        return ApiResponse.success("ออกจากระบบสำเร็จ", null);
    }

    @PostMapping("/reset-password")
    public ApiResponse<Map<String, Object>> resetPassword(
            @Valid @RequestBody AuthRequests.ResetPassword request) {
        var user = currentUser.requireUser();
        if (!passwords.matches(request.currentPassword(), user.getPasswordHash())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "รหัสผ่านเดิมไม่ถูกต้อง");
        }
        if (!auth.resetPassword(user.getUserId(), request.password(), request.confirmPassword())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "รหัสผ่านใหม่ไม่ถูกต้องหรือไม่ตรงกัน");
        }
        var newSession = auth.loginWithUser(user.getUsername(), request.password());
        return ApiResponse.success("เปลี่ยนรหัสผ่านสำเร็จ", Map.of(
                "success", true,
                "token", newSession.token(),
                "user", newSession.user()
        ));
    }

}

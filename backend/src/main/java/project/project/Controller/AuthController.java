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
import org.springframework.http.CacheControl;
import project.project.DTO.customer.CustomerResponse;
import project.project.Service.api.AuthService;
import project.project.Service.implement.PasswordService;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final AuthService auth;
    private final CurrentUser currentUser;
    private final PasswordService passwords;

    public AuthController(AuthService auth, CurrentUser currentUser, PasswordService passwords) {
        this.auth = auth;
        this.currentUser = currentUser;
        this.passwords = passwords;
    }

    @PostMapping("/register/customer")
    public ResponseEntity<ApiResponse<CustomerResponse>> registerCustomer(@Valid @RequestBody AuthRequests.RegisterCustomer request) {
        var customer = auth.registerCustomer(request.username(), request.email(), request.password(),
                request.confirmPassword(), request.fullName(), request.phoneNumber());
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success("สมัครสมาชิกสำเร็จ", CustomerResponse.fromEntity(customer)));
    }

    @PostMapping("/register/seller")
    public ResponseEntity<ApiResponse<Map<String, Long>>> registerSeller(@Valid @RequestBody AuthRequests.RegisterSeller request) {
        var seller = auth.registerSeller(request.username(), request.email(), request.password(), request.confirmPassword());
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success("สมัครผู้ขายสำเร็จ", Map.of("sellerId", seller.getSellerId())));
    }

    @PostMapping("/login")
    public ApiResponse<Map<String, String>> login(@Valid @RequestBody AuthRequests.Login request) {
        try {
            return ApiResponse.success("เข้าสู่ระบบสำเร็จ", Map.of("token", auth.login(request.usernameOrEmail(), request.password()), "tokenType", "Bearer"));
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "ชื่อผู้ใช้หรือรหัสผ่านไม่ถูกต้อง");
        }
    }

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<CurrentUserResponse>> me() {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore())
                .body(ApiResponse.success("ดึงข้อมูลผู้ใช้ปัจจุบันสำเร็จ",
                        CurrentUserResponse.from(currentUser.requireUser())));
    }

    @GetMapping("/token")
    public ApiResponse<Map<String, Boolean>> validateToken(@RequestHeader(value = "Authorization", required = false) String authorization) {
        return ApiResponse.success("ตรวจสอบโทเคนสำเร็จ", Map.of("valid", auth.validateToken(project.project.Security.BearerTokens.requireToken(authorization))));
    }

    @PostMapping("/logout")
    public ApiResponse<Void> logout(
            @RequestHeader(value = "Authorization", required = false) String authorization) {
        // The bearer token identifies this session, not every login belonging to the user.
        if (!auth.logout(BearerTokens.requireToken(authorization))) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "โทเคนไม่ถูกต้องหรือหมดอายุ");
        }
        SecurityContextHolder.clearContext();
        return ApiResponse.success("ออกจากระบบสำเร็จ", null);
    }

    @PostMapping("/reset-password")
    public ApiResponse<Map<String, Boolean>> resetPassword(
            @Valid @RequestBody AuthRequests.ResetPassword request) {
        var user = currentUser.requireUser();
        if (!passwords.matches(request.currentPassword(), user.getPasswordHash())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "รหัสผ่านเดิมไม่ถูกต้อง");
        }
        if (!auth.resetPassword(user.getUserId(), request.password(), request.confirmPassword())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "รหัสผ่านใหม่ไม่ถูกต้องหรือไม่ตรงกัน");
        }
        return ApiResponse.success("เปลี่ยนรหัสผ่านสำเร็จ", Map.of("success", true));
    }

}

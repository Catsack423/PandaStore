package project.project.Controller;

import java.util.Map;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import project.project.Controller.support.CurrentUser;
import project.project.DTO.auth.AuthRequests;
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
    public ResponseEntity<CustomerResponse> registerCustomer(@Valid @RequestBody AuthRequests.RegisterCustomer request) {
        var customer = auth.registerCustomer(request.username(), request.email(), request.password(),
                request.confirmPassword(), request.fullName(), request.phoneNumber());
        return ResponseEntity.status(HttpStatus.CREATED).body(CustomerResponse.fromEntity(customer));
    }

    @PostMapping("/register/seller")
    public ResponseEntity<Map<String, Long>> registerSeller(@Valid @RequestBody AuthRequests.RegisterSeller request) {
        var seller = auth.registerSeller(request.username(), request.email(), request.password(), request.confirmPassword());
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of("sellerId", seller.getSellerId()));
    }

    @PostMapping("/login")
    public Map<String, String> login(@Valid @RequestBody AuthRequests.Login request) {
        try {
            return Map.of("token", auth.login(request.usernameOrEmail(), request.password()), "tokenType", "Bearer");
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "ชื่อผู้ใช้หรือรหัสผ่านไม่ถูกต้อง");
        }
    }

    @GetMapping("/token")
    public Map<String, Boolean> validateToken(@RequestHeader(value = "Authorization", required = false) String authorization) {
        return Map.of("valid", auth.validateToken(currentUser.token(authorization)));
    }

    @PostMapping("/reset-password")
    public Map<String, Boolean> resetPassword(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @Valid @RequestBody AuthRequests.ResetPassword request) {
        var user = currentUser.requireUser(authorization);
        if (!passwords.matches(request.currentPassword(), user.getPasswordHash())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "รหัสผ่านเดิมไม่ถูกต้อง");
        }
        if (!auth.resetPassword(user.getUserId(), request.password(), request.confirmPassword())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "รหัสผ่านใหม่ไม่ถูกต้องหรือไม่ตรงกัน");
        }
        return Map.of("success", true);
    }
}

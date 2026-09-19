package project.project.Controller;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import project.project.Entity.user.UserStatus;
import project.project.Repository.UserRepository;

import java.security.Principal;

@Component
public class RequestUserResolver {

    private final UserRepository userRepository;

    public RequestUserResolver(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public Long requireUserId(Principal principal) {
        if (principal == null) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "กรุณาเข้าสู่ระบบ");
        }

        var user = userRepository.findByUsername(principal.getName())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED,
                        "ไม่พบผู้ใช้ที่เข้าสู่ระบบ"));

        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "บัญชีนี้ไม่สามารถใช้งานได้");
        }

        return user.getUserId();
    }
}
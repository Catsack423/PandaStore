package project.project.Controller;

import java.util.Map;
import java.util.NoSuchElementException;
import jakarta.validation.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import project.project.Exception.DuplicateUserException;

@RestControllerAdvice(assignableTypes = {AuthController.class, CartController.class})
public class AuthCartExceptionHandler {
    @ExceptionHandler({IllegalArgumentException.class, ConstraintViolationException.class})
    public ResponseEntity<Map<String, String>> badRequest(Exception e) {
        return error(HttpStatus.BAD_REQUEST, "ข้อมูลไม่ถูกต้อง กรุณาตรวจสอบจำนวนสินค้าและข้อมูลที่กรอก");
    }

    @ExceptionHandler(NoSuchElementException.class)
    public ResponseEntity<Map<String, String>> notFound(Exception e) {
        return error(HttpStatus.NOT_FOUND, "ไม่พบข้อมูลที่ต้องการ");
    }

    @ExceptionHandler({DuplicateUserException.class, DataIntegrityViolationException.class})
    public ResponseEntity<Map<String, String>> conflict(Exception e) {
        return error(HttpStatus.CONFLICT, "ข้อมูลซ้ำหรือไม่สามารถบันทึกได้");
    }

    @ExceptionHandler(UnsupportedOperationException.class)
    public ResponseEntity<Map<String, String>> notImplemented(Exception e) {
        return error(HttpStatus.NOT_IMPLEMENTED, "การสมัครผู้ขายยังไม่พร้อมใช้งาน");
    }

    private ResponseEntity<Map<String, String>> error(HttpStatus status, String message) {
        return ResponseEntity.status(status).body(Map.of("message", message));
    }
}

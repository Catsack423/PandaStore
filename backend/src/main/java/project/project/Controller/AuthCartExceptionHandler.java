package project.project.Controller;

import project.project.ApiResponse.ApiResponse;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
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
    @ExceptionHandler({IllegalArgumentException.class, ConstraintViolationException.class,
            HttpMessageNotReadableException.class, MethodArgumentTypeMismatchException.class})
    public ResponseEntity<ApiResponse<Void>> badRequest(Exception e) {
        return error(HttpStatus.BAD_REQUEST, "ข้อมูลไม่ถูกต้อง กรุณาตรวจสอบจำนวนสินค้าและข้อมูลที่กรอก");
    }

    @ExceptionHandler(NoSuchElementException.class)
    public ResponseEntity<ApiResponse<Void>> notFound(Exception e) {
        return error(HttpStatus.NOT_FOUND, "ไม่พบข้อมูลที่ต้องการ");
    }

    @ExceptionHandler({DuplicateUserException.class, DataIntegrityViolationException.class})
    public ResponseEntity<ApiResponse<Void>> conflict(Exception e) {
        return error(HttpStatus.CONFLICT, "ข้อมูลซ้ำหรือไม่สามารถบันทึกได้");
    }

    @ExceptionHandler(UnsupportedOperationException.class)
    public ResponseEntity<ApiResponse<Void>> notImplemented(Exception e) {
        return error(HttpStatus.NOT_IMPLEMENTED, "การสมัครผู้ขายยังไม่พร้อมใช้งาน");
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ApiResponse<Void>> responseStatus(ResponseStatusException e) {
        return ResponseEntity.status(e.getStatusCode())
                .body(ApiResponse.error(e.getReason(), null));
    }

    private ResponseEntity<ApiResponse<Void>> error(HttpStatus status, String message) {
        return ResponseEntity.status(status).body(ApiResponse.error(message, null));
    }
}

package project.project.Controller;

import jakarta.persistence.EntityNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import project.project.ApiResponse.ApiResponse;
import project.project.Exception.DuplicateUserException;

import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.web.server.ResponseStatusException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Map<String, String>>> handleValidationExceptions(
            MethodArgumentNotValidException ex) {

        Map<String, String> errors = new LinkedHashMap<>();

        ex.getBindingResult().getAllErrors().forEach(error -> {
            String name = error instanceof FieldError fieldError
                    ? fieldError.getField()
                    : error.getObjectName();

            errors.putIfAbsent(name, error.getDefaultMessage());
        });

        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(
                        ApiResponse.error(
                                "ข้อมูลที่ส่งมาไม่ถูกต้อง",
                                errors));
    }

    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleNotFound(
            EntityNotFoundException ex) {
        return error(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResponse<Void>> handleInvalidArgument(
            IllegalArgumentException ex) {
        return error(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ApiResponse<Void>> handleInvalidState(
            IllegalStateException ex) {
        return error(HttpStatus.CONFLICT, ex.getMessage());
    }

    @ExceptionHandler(DuplicateUserException.class)
    public ResponseEntity<ApiResponse<Void>> handleDuplicateUser(
            DuplicateUserException ex) {
        return error(HttpStatus.CONFLICT, ex.getMessage());
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiResponse<Void>> handleDataIntegrity(
            DataIntegrityViolationException ex) {
        log.warn("Database constraint violation", ex);

        return error(
                HttpStatus.CONFLICT,
                "ไม่สามารถบันทึกข้อมูลได้ เนื่องจากข้อมูลขัดแย้งกับข้อมูลในระบบ");
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResponse<Void>> handleUnreadableBody(
            HttpMessageNotReadableException ex) {
        return error(
                HttpStatus.BAD_REQUEST,
                "รูปแบบ JSON หรือค่าข้อมูลที่ส่งมาไม่ถูกต้อง");
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiResponse<Void>> handleTypeMismatch(
            MethodArgumentTypeMismatchException ex) {
        return error(
                HttpStatus.BAD_REQUEST,
                "ชนิดข้อมูลของพารามิเตอร์ " + ex.getName() + " ไม่ถูกต้อง");
    }

    private ResponseEntity<ApiResponse<Void>> error(
            HttpStatus status,
            String message) {
        return ResponseEntity.status(status)
                .body(ApiResponse.<Void>error(message, null));
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ApiResponse<Void>> handleResponseStatus(
            ResponseStatusException ex) {
        String message = ex.getReason() != null
                ? ex.getReason()
                : "ไม่สามารถดำเนินการได้";

        return ResponseEntity.status(ex.getStatusCode())
                .body(ApiResponse.<Void>error(message, null));
    }
}
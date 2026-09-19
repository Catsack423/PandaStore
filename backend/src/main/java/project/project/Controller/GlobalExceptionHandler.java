package project.project.Controller;

import jakarta.persistence.EntityNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.server.ResponseStatusException;

import project.project.ApiResponse.ApiResponse;
import project.project.Exception.DuplicateUserException;

import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    private static final String INTERNAL_ERROR_MESSAGE = "เกิดข้อผิดพลาดภายในระบบ กรุณาลองใหม่ภายหลัง";

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidationExceptions(
            MethodArgumentNotValidException ex) {

        Map<String, String> errors = new LinkedHashMap<>();

        ex.getBindingResult().getAllErrors().forEach(error -> {
            String name = error instanceof FieldError fieldError
                    ? fieldError.getField()
                    : error.getObjectName();

            String message = error.getDefaultMessage() != null
                    ? error.getDefaultMessage()
                    : "ข้อมูลไม่ถูกต้อง";

            errors.putIfAbsent(name, message);
        });

        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.<Void>error(
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
                "ชนิดข้อมูลของพารามิเตอร์ "
                        + ex.getName()
                        + " ไม่ถูกต้อง");
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ApiResponse<Void>> handleResponseStatus(
            ResponseStatusException ex) {

        String message;

        if (ex.getStatusCode().is5xxServerError()) {
            log.error("Server response status exception", ex);
            message = INTERNAL_ERROR_MESSAGE;
        } else {
            message = messageOrDefault(ex.getReason());
        }

        return ResponseEntity.status(ex.getStatusCode())
                .headers(ex.getHeaders())
                .body(ApiResponse.<Void>error(message, null));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleUnexpectedException(
            Exception ex) {

        // รักษา status และ headers ของ error จาก Spring
        // เช่น 405 Method Not Allowed และ 415 Unsupported Media Type
        if (ex instanceof ErrorResponse response) {
            String message;

            if (response.getStatusCode().is5xxServerError()) {
                log.error("Unhandled server error", ex);
                message = INTERNAL_ERROR_MESSAGE;
            } else {
                message = messageOrDefault(
                        response.getBody().getTitle());
            }

            return ResponseEntity.status(response.getStatusCode())
                    .headers(response.getHeaders())
                    .body(ApiResponse.<Void>error(message, null));
        }

        // เก็บรายละเอียดใน log ไม่ส่งข้อมูลภายในกลับให้ผู้ใช้
        log.error("Unexpected error", ex);

        return error(
                HttpStatus.INTERNAL_SERVER_ERROR,
                INTERNAL_ERROR_MESSAGE);
    }

    private ResponseEntity<ApiResponse<Void>> error(
            HttpStatusCode status,
            String message) {

        return ResponseEntity.status(status).body(ApiResponse.<Void>error(messageOrDefault(message), null));
    }

    private String messageOrDefault(String message) {
        return message != null && !message.isBlank() ? message : "ไม่สามารถดำเนินการได้";
    }

    @ExceptionHandler(project.project.Exception.DuplicateUserException.class)
    public ResponseEntity<ApiResponse<Void>> handleDuplicateUserException(project.project.Exception.DuplicateUserException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ApiResponse.error(ex.getMessage(), null));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResponse<Void>> handleIllegalArgumentException(IllegalArgumentException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(ex.getMessage(), null));
    }

    @ExceptionHandler(org.springframework.dao.DataIntegrityViolationException.class)
    public ResponseEntity<ApiResponse<Void>> handleDataIntegrityViolation(org.springframework.dao.DataIntegrityViolationException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ApiResponse.error("ข้อมูลซ้ำหรือไม่สามารถบันทึกได้", null));
    }
}
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
    @ExceptionHandler(project.project.Exception.CartAvailabilityException.class)
    public ResponseEntity<ApiResponse<Void>> unavailable(project.project.Exception.CartAvailabilityException e) {
        return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage(),
                java.util.Map.of("code", e.getCode())));
    }

    @ExceptionHandler({IllegalArgumentException.class, ConstraintViolationException.class,
            HttpMessageNotReadableException.class, MethodArgumentTypeMismatchException.class})
    public ResponseEntity<ApiResponse<Void>> badRequest(Exception e) {
        return error(HttpStatus.BAD_REQUEST, "Invalid data. Please check the item quantities and entered information");
    }

    @ExceptionHandler(NoSuchElementException.class)
    public ResponseEntity<ApiResponse<Void>> notFound(Exception e) {
        return error(HttpStatus.NOT_FOUND, "Requested information not found");
    }

    @ExceptionHandler({DuplicateUserException.class, DataIntegrityViolationException.class})
    public ResponseEntity<ApiResponse<Void>> conflict(Exception e) {
        return error(HttpStatus.CONFLICT, "Duplicate data or unable to save");
    }

    @ExceptionHandler(UnsupportedOperationException.class)
    public ResponseEntity<ApiResponse<Void>> notImplemented(Exception e) {
        return error(HttpStatus.NOT_IMPLEMENTED, "Seller registration is unavailable");
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

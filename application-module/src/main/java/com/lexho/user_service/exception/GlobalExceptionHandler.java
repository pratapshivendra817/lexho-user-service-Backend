package com.lexho.user_service.exception;

import com.lexho.user_service.dto.ApiResponse;
import com.lexho.user_service.booking.exception.BookingValidationException;
import com.lexho.user_service.booking.exception.InstantBookingUnavailableException;
import com.lexho.user_service.notification.exception.NotificationValidationException;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.MultipartException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<?>> handleValidation(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(FieldError::getDefaultMessage)
                .orElse("Invalid request");
        return new ResponseEntity<>(ApiResponse.failure(message), HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler({ConstraintViolationException.class, HttpMessageNotReadableException.class})
    public ResponseEntity<ApiResponse<?>> handleMalformedRequest(Exception ex) {
        return new ResponseEntity<>(ApiResponse.failure("Invalid request"), HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(BadRequestException.class)
    public ResponseEntity<ApiResponse<?>> handleBadRequest(BadRequestException ex) {
        String message = ex.getMessage() == null || ex.getMessage().isBlank() ? "Invalid request" : ex.getMessage();
        return new ResponseEntity<>(ApiResponse.failure(message), HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(NotificationValidationException.class)
    public ResponseEntity<ApiResponse<?>> handleNotificationValidation(NotificationValidationException ex) {
        String message = ex.getMessage() == null || ex.getMessage().isBlank() ? "Invalid notification request" : ex.getMessage();
        return new ResponseEntity<>(ApiResponse.failure(message), HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(InstantBookingUnavailableException.class)
    public ResponseEntity<ApiResponse<?>> handleInstantUnavailable(InstantBookingUnavailableException ex) {
        java.util.Map<String, Object> payload = java.util.Map.of(
                "assignmentStatus", "REJECTED",
                "errorCode", ex.getErrorCode(),
                "message", ex.getMessage()
        );
        return new ResponseEntity<>(new ApiResponse<>(false, ex.getMessage(), payload), HttpStatus.CONFLICT);
    }

    @ExceptionHandler(BookingValidationException.class)
    public ResponseEntity<ApiResponse<?>> handleBookingValidation(BookingValidationException ex) {
        java.util.Map<String, Object> payload = java.util.Map.of(
                "errorCode", ex.getErrorCode(),
                "message", ex.getMessage()
        );
        HttpStatus status = HttpStatus.BAD_REQUEST;
        if ("BOOKING_OUTSIDE_SERVICE_ZONE".equals(ex.getErrorCode())
                || "BOOKING_OUTSIDE_GEOFENCE".equals(ex.getErrorCode())
                || "BOOKING_ADDRESS_COORDINATES_REQUIRED".equals(ex.getErrorCode())) {
            status = HttpStatus.CONFLICT;
        }
        return new ResponseEntity<>(new ApiResponse<>(false, ex.getMessage(), payload), status);
    }

    @ExceptionHandler(UnauthorizedException.class)
    public ResponseEntity<ApiResponse<?>> handleUnauthorized(UnauthorizedException ex) {
        String message = ex.getMessage() == null || ex.getMessage().isBlank() ? "Unauthorized" : ex.getMessage();
        return new ResponseEntity<>(ApiResponse.failure(message), HttpStatus.UNAUTHORIZED);
    }

    @ExceptionHandler(AccountLockedException.class)
    public ResponseEntity<ApiResponse<?>> handleAccountLocked(AccountLockedException ex) {
        String message = ex.getMessage() == null || ex.getMessage().isBlank() ? "Account locked" : ex.getMessage();
        return new ResponseEntity<>(ApiResponse.failure(message), HttpStatus.LOCKED);
    }

    @ExceptionHandler(InfrastructureException.class)
    public ResponseEntity<ApiResponse<?>> handleInfrastructure(InfrastructureException ex) {
        String message = ex.getMessage() == null || ex.getMessage().isBlank() ? "Service unavailable" : ex.getMessage();
        return new ResponseEntity<>(ApiResponse.failure(message), HttpStatus.SERVICE_UNAVAILABLE);
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiResponse<?>> handleResourceNotFound(ResourceNotFoundException ex) {
        String message = ex.getMessage() == null || ex.getMessage().isBlank() ? "Not found" : ex.getMessage();
        return new ResponseEntity<>(ApiResponse.failure(message), HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler({IllegalArgumentException.class, MissingServletRequestPartException.class})
    public ResponseEntity<ApiResponse<?>> handleBadInput(Exception ex) {
        String message = ex.getMessage() == null || ex.getMessage().isBlank() ? "Invalid request" : ex.getMessage();
        return new ResponseEntity<>(ApiResponse.failure(message), HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ApiResponse<?>> handleMaxUploadSize(MaxUploadSizeExceededException ex) {
        return new ResponseEntity<>(ApiResponse.failure("Image size must be <= 5MB"), HttpStatus.PAYLOAD_TOO_LARGE);
    }

    @ExceptionHandler(MultipartException.class)
    public ResponseEntity<ApiResponse<?>> handleMultipartError(MultipartException ex) {
        log.warn("Multipart parsing failed: {}", ex.getMessage());
        return new ResponseEntity<>(ApiResponse.failure("Invalid multipart request"), HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(TooManyRequestsException.class)
    public ResponseEntity<ApiResponse<?>> handleTooManyRequests(TooManyRequestsException ex) {
        String message = ex.getMessage() == null || ex.getMessage().isBlank() ? "Too many requests" : ex.getMessage();
        return new ResponseEntity<>(ApiResponse.failure(message), HttpStatus.TOO_MANY_REQUESTS);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResponse<?>> handleAccessDenied(AccessDeniedException ex) {
        return new ResponseEntity<>(ApiResponse.failure("Access denied"), HttpStatus.FORBIDDEN);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiResponse<?>> handleDataIntegrity(DataIntegrityViolationException ex) {
        return new ResponseEntity<>(ApiResponse.failure("Request conflicts with existing data"), HttpStatus.CONFLICT);
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ApiResponse<?>> handleNoResourceFound(NoResourceFoundException ex) {
        return new ResponseEntity<>(ApiResponse.failure("Endpoint not found"), HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<?>> handleGlobal(Exception ex) {
        log.error("Unhandled exception", ex);
        return new ResponseEntity<>(ApiResponse.failure("Something went wrong"), HttpStatus.INTERNAL_SERVER_ERROR);
    }
}

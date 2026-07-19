package com.lexho.user_service.exception;

import com.lexho.user_service.dto.ApiResponse;
import com.lexho.user_service.booking.exception.BookingValidationException;
import com.lexho.user_service.booking.exception.InstantBookingUnavailableException;
import com.lexho.user_service.notification.exception.NotificationValidationException;
import com.lexho.user_service.subscription.exception.SubscriptionValidationException;
import jakarta.servlet.http.HttpServletRequest;
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
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.MultipartException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex,
                                                          HttpServletRequest request) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(FieldError::getDefaultMessage)
                .orElse("Invalid request");
        return error("VALIDATION_FAILED", message, HttpStatus.BAD_REQUEST, request);
    }

    @ExceptionHandler({ConstraintViolationException.class, HttpMessageNotReadableException.class})
    public ResponseEntity<ApiResponse<?>> handleMalformedRequest(Exception ex) {
        return new ResponseEntity<>(ApiResponse.failure("Invalid request"), HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(BadRequestException.class)
    public ResponseEntity<ApiResponse<?>> handleBadRequest(BadRequestException ex) {
        String message = ex.getMessage() == null || ex.getMessage().isBlank() ? "Invalid request" : ex.getMessage();
        return new ResponseEntity<>(ApiResponse.failure(errorCodeFor(message), message), HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(CodedValidationException.class)
    public ResponseEntity<ErrorResponse> handleCodedValidation(CodedValidationException ex,
                                                               HttpServletRequest request) {
        String message = ex.getMessage() == null || ex.getMessage().isBlank() ? "Invalid request" : ex.getMessage();
        return error(ex.getErrorCode(), message, ex.getStatus(), request);
    }

    @ExceptionHandler(NotificationValidationException.class)
    public ResponseEntity<ApiResponse<?>> handleNotificationValidation(NotificationValidationException ex) {
        String message = ex.getMessage() == null || ex.getMessage().isBlank() ? "Invalid notification request" : ex.getMessage();
        return new ResponseEntity<>(ApiResponse.failure(message), HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(SubscriptionValidationException.class)
    public ResponseEntity<ApiResponse<?>> handleSubscriptionValidation(SubscriptionValidationException ex) {
        java.util.Map<String, Object> payload = java.util.Map.of("errors", ex.getErrors());
        return new ResponseEntity<>(new ApiResponse<>(false, "VALIDATION_FAILED", "Validation failed", payload), HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(InstantBookingUnavailableException.class)
    public ResponseEntity<ApiResponse<?>> handleInstantUnavailable(InstantBookingUnavailableException ex) {
        java.util.Map<String, Object> payload = java.util.Map.of(
                "assignmentStatus", "REJECTED",
                "errorCode", ex.getErrorCode(),
                "message", ex.getMessage()
        );
        return new ResponseEntity<>(new ApiResponse<>(false, ex.getErrorCode(), ex.getMessage(), payload), HttpStatus.CONFLICT);
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
                || "BOOKING_ADDRESS_COORDINATES_REQUIRED".equals(ex.getErrorCode())
                || "SAVED_ADDRESS_COORDINATES_MISSING".equals(ex.getErrorCode())) {
            status = HttpStatus.CONFLICT;
        }
        return new ResponseEntity<>(new ApiResponse<>(false, ex.getErrorCode(), ex.getMessage(), payload), status);
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
    public ResponseEntity<ErrorResponse> handleResourceNotFound(ResourceNotFoundException ex,
                                                                HttpServletRequest request) {
        String message = ex.getMessage() == null || ex.getMessage().isBlank() ? "Not found" : ex.getMessage();
        return error(errorCodeFor(message), message, HttpStatus.NOT_FOUND, request);
    }

    @ExceptionHandler({IllegalArgumentException.class, MissingServletRequestPartException.class})
    public ResponseEntity<ApiResponse<?>> handleBadInput(Exception ex) {
        String message = ex.getMessage() == null || ex.getMessage().isBlank() ? "Invalid request" : ex.getMessage();
        return new ResponseEntity<>(ApiResponse.failure(message), HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiResponse<?>> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
        String name = ex.getName() == null ? "parameter" : ex.getName();
        return new ResponseEntity<>(ApiResponse.failure("Invalid " + name), HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ApiResponse<?>> handleMissingRequestParam(MissingServletRequestParameterException ex) {
        String name = ex.getParameterName() == null ? "parameter" : ex.getParameterName();
        return new ResponseEntity<>(ApiResponse.failure(name + " is required"), HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ErrorResponse> handleMaxUploadSize(MaxUploadSizeExceededException ex,
                                                             HttpServletRequest request) {
        return error("FILE_TOO_LARGE", "File size must be 5 MB or less", HttpStatus.PAYLOAD_TOO_LARGE, request);
    }

    @ExceptionHandler(FileUploadException.class)
    public ResponseEntity<ErrorResponse> handleFileUpload(FileUploadException ex,
                                                          HttpServletRequest request) {
        HttpStatus status = "FILE_TOO_LARGE".equals(ex.getCode()) ? HttpStatus.PAYLOAD_TOO_LARGE : HttpStatus.BAD_REQUEST;
        return error(ex.getCode(), ex.getMessage(), status, request);
    }

    @ExceptionHandler(MultipartException.class)
    public ResponseEntity<ApiResponse<?>> handleMultipartError(MultipartException ex) {
        log.warn("Multipart parsing failed: {}", ex.getMessage());
        return new ResponseEntity<>(ApiResponse.failure("Invalid multipart request"), HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(TooManyRequestsException.class)
    public ResponseEntity<java.util.Map<String, Object>> handleTooManyRequests(TooManyRequestsException ex,
                                                                               HttpServletRequest request) {
        String message = ex.getMessage() == null || ex.getMessage().isBlank() ? "Too many requests" : ex.getMessage();
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                .header("Retry-After", String.valueOf(ex.getRetryAfterSeconds()))
                .body(java.util.Map.of(
                        "code", "RATE_LIMIT_EXCEEDED",
                        "message", message,
                        "retryAfterSeconds", ex.getRetryAfterSeconds()
                ));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDenied(AccessDeniedException ex,
                                                            HttpServletRequest request) {
        return error("ACCESS_DENIED", "Access denied", HttpStatus.FORBIDDEN, request);
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ErrorResponse> handleIllegalState(IllegalStateException ex,
                                                            HttpServletRequest request) {
        String message = ex.getMessage() == null || ex.getMessage().isBlank() ? "Invalid state" : ex.getMessage();
        return error("ILLEGAL_STATE", message, HttpStatus.CONFLICT, request);
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
    public ResponseEntity<ErrorResponse> handleGlobal(Exception ex, HttpServletRequest request) {
        log.error("Unhandled exception", ex);
        return error("INTERNAL_ERROR", "Something went wrong", HttpStatus.INTERNAL_SERVER_ERROR, request);
    }

    private ResponseEntity<ErrorResponse> error(String code, String message, HttpStatus status, HttpServletRequest request) {
        String path = request == null ? "" : request.getRequestURI();
        return ResponseEntity.status(status).body(new ErrorResponse(code, message, java.time.Instant.now(), path));
    }

    private String errorCodeFor(String message) {
        String normalized = message == null ? "" : message.toUpperCase(java.util.Locale.ROOT);
        if (normalized.contains("BOOKING NOT FOUND") || normalized.contains("ORDER NOT FOUND")) {
            return "ORDER_NOT_FOUND";
        }
        if (normalized.contains("PAYMENT") && normalized.contains("FAILED")) {
            return "PAYMENT_FAILED";
        }
        if (normalized.contains("PAYMENT") && normalized.contains("EXPIRED")) {
            return "PAYMENT_EXPIRED";
        }
        if (normalized.contains("SUBSCRIPTION") && normalized.contains("PAUSED")) {
            return "SUBSCRIPTION_PAUSED";
        }
        if (normalized.contains("SUBSCRIPTION") && normalized.contains("CANCELLED")) {
            return "SUBSCRIPTION_CANCELLED";
        }
        if (normalized.contains("PARTNER") && normalized.contains("UNAVAILABLE")) {
            return "PARTNER_UNAVAILABLE";
        }
        if (normalized.contains("TRACKING") && normalized.contains("DISABLED")) {
            return "TRACKING_DISABLED";
        }
        return "INVALID_REQUEST";
    }
}

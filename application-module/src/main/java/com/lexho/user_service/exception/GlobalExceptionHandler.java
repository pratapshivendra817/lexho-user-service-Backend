package com.lexho.user_service.exception;

import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import com.lexho.user_service.dto.ApiResponse;
import com.lexho.user_service.booking.exception.BookingValidationException;
import com.lexho.user_service.booking.exception.InstantBookingUnavailableException;
import com.lexho.user_service.notification.exception.NotificationValidationException;
import com.lexho.user_service.subscription.exception.SubscriptionValidationException;
import com.lexho.user_service.localization.service.PartnerLocalizationService;
import com.lexho.user_service.util.LexhoConstants;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
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
import org.springframework.web.bind.ServletRequestBindingException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.MultipartException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);
    private static final String CODE_KEY = "code";
    private static final String ERROR_CODE_KEY = "errorCode";
    private static final String MESSAGE_KEY = "message";
    private static final String INVALID_REQUEST_MESSAGE = "Invalid request";
    private static final String FILE_TOO_LARGE_CODE = "FILE_TOO_LARGE";
    private static final String RATE_LIMIT_EXCEEDED_CODE = "RATE_LIMIT_EXCEEDED";
    private final PartnerLocalizationService localizationService;

    public GlobalExceptionHandler() {
        this(null);
    }

    @Autowired
    public GlobalExceptionHandler(PartnerLocalizationService localizationService) {
        this.localizationService = localizationService;
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex,
                                                          HttpServletRequest request) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(FieldError::getDefaultMessage)
                .orElse(INVALID_REQUEST_MESSAGE);
        return error("VALIDATION_FAILED", message, HttpStatus.BAD_REQUEST, request);
    }

    @ExceptionHandler({ConstraintViolationException.class, HttpMessageNotReadableException.class})
    public ResponseEntity<ApiResponse<?>> handleMalformedRequest(Exception ex) {
        return new ResponseEntity<>(ApiResponse.failure(malformedRequestMessage(ex)), HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(BadRequestException.class)
    public ResponseEntity<ApiResponse<?>> handleBadRequest(BadRequestException ex) {
        String message = ex.getMessage() == null || ex.getMessage().isBlank() ? INVALID_REQUEST_MESSAGE : ex.getMessage();
        return new ResponseEntity<>(ApiResponse.failure(errorCodeFor(message), message), HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(CodedValidationException.class)
    public ResponseEntity<ErrorResponse> handleCodedValidation(CodedValidationException ex,
                                                               HttpServletRequest request) {
        String message = ex.getMessage() == null || ex.getMessage().isBlank() ? INVALID_REQUEST_MESSAGE : ex.getMessage();
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
                ERROR_CODE_KEY, ex.getErrorCode(),
                MESSAGE_KEY, ex.getMessage()
        );
        return new ResponseEntity<>(new ApiResponse<>(false, ex.getErrorCode(), ex.getMessage(), payload), HttpStatus.CONFLICT);
    }

    @ExceptionHandler(BookingValidationException.class)
    public ResponseEntity<ApiResponse<?>> handleBookingValidation(BookingValidationException ex) {
        java.util.Map<String, Object> payload = java.util.Map.of(
                ERROR_CODE_KEY, ex.getErrorCode(),
                MESSAGE_KEY, ex.getMessage()
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

    @ExceptionHandler(ServletRequestBindingException.class)
    public ResponseEntity<ApiResponse<?>> handleServletRequestBinding(ServletRequestBindingException ex) {
        String message = ex.getMessage() != null && ex.getMessage().contains("userId")
                ? "Authentication user id is required"
                : INVALID_REQUEST_MESSAGE;
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
        String message = ex.getMessage() == null || ex.getMessage().isBlank() ? INVALID_REQUEST_MESSAGE : ex.getMessage();
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
        return error(FILE_TOO_LARGE_CODE,
                "Upload payload is too large. Each file must be 5 MB or less and the total request must be 30 MB or less",
                HttpStatus.PAYLOAD_TOO_LARGE, request);
    }

    @ExceptionHandler(FileUploadException.class)
    public ResponseEntity<ErrorResponse> handleFileUpload(FileUploadException ex,
                                                          HttpServletRequest request) {
        HttpStatus status = FILE_TOO_LARGE_CODE.equals(ex.getCode()) ? HttpStatus.PAYLOAD_TOO_LARGE : HttpStatus.BAD_REQUEST;
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
                        CODE_KEY, RATE_LIMIT_EXCEEDED_CODE,
                        MESSAGE_KEY, message,
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
        String localized = isPartnerApi(path) && localizationService != null
                ? localizationService.translateMessage(message)
                : message;
        return ResponseEntity.status(status).body(new ErrorResponse(code, localized, java.time.Instant.now(), path));
    }

    private boolean isPartnerApi(String path) {
        return path != null && (path.startsWith("/api/partner/") || path.startsWith("/api/v1/partner/"));
    }

    private String malformedRequestMessage(Exception ex) {
        if (ex instanceof HttpMessageNotReadableException notReadable
                && notReadable.getCause() instanceof InvalidFormatException invalidFormat) {
            String field = invalidFormat.getPath().stream()
                    .map(com.fasterxml.jackson.databind.JsonMappingException.Reference::getFieldName)
                    .filter(java.util.Objects::nonNull)
                    .reduce((first, second) -> second)
                    .orElse("request body");
            if ("dateOfBirth".equals(field)) {
                return "dateOfBirth must use YYYY-MM-DD";
            }
            return "Invalid " + field;
        }
        if (ex instanceof HttpMessageNotReadableException) {
            return "Malformed JSON request body";
        }
        return INVALID_REQUEST_MESSAGE;
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
        if (normalized.contains(LexhoConstants.TYPE_SUBSCRIPTION) && normalized.contains("PAUSED")) {
            return "SUBSCRIPTION_PAUSED";
        }
        if (normalized.contains(LexhoConstants.TYPE_SUBSCRIPTION) && normalized.contains("CANCELLED")) {
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

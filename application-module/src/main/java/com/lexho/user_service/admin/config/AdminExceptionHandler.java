package com.lexho.user_service.admin.config;

import com.lexho.user.exception.InvalidRoleException;
import com.lexho.user.exception.UserAlreadyExistsException;
import com.lexho.user.exception.UserNotFoundException;
import com.lexho.user_service.banner.exception.BannerNotFoundException;
import com.lexho.user_service.banner.exception.BannerValidationException;
import com.lexho.user_service.exception.*;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.orm.jpa.JpaSystemException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.MultipartException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;

import java.util.Map;
import java.util.Set;

@Order(Ordered.HIGHEST_PRECEDENCE)
@RestControllerAdvice(basePackages = {
        "com.lexho.user_service.controller",
        "com.lexho.user_service.admin.controller",
        "com.lexho.user_service.subscription.controller",
        "com.lexho.user.controller"
})
public class AdminExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(AdminExceptionHandler.class);
    private static final String CODE_KEY = "code";
    private static final String MESSAGE_KEY = "message";
    private static final String INVALID_REQUEST_MESSAGE = "Invalid request";
    private static final String FILE_TOO_LARGE_CODE = "FILE_TOO_LARGE";
    private static final Set<Class<? extends Exception>> NOT_FOUND_EXCEPTIONS = Set.of(
            ResourceNotFoundException.class,
            UserNotFoundException.class,
            BannerNotFoundException.class
    );
    private static final Set<Class<? extends Exception>> BAD_REQUEST_EXCEPTIONS = Set.of(
            MissingServletRequestPartException.class,
            MissingServletRequestParameterException.class,
            MultipartException.class,
            DataIntegrityViolationException.class,
            JpaSystemException.class
    );

    @ExceptionHandler({
            BadRequestException.class,
            UnauthorizedException.class,
            AccountLockedException.class,
            TooManyRequestsException.class,
            ResourceNotFoundException.class,
            UserNotFoundException.class,
            UserAlreadyExistsException.class,
            InvalidRoleException.class,
            InfrastructureException.class,
            FileUploadException.class,
            DataIntegrityViolationException.class,
            BannerValidationException.class,
            BannerNotFoundException.class,
            MethodArgumentNotValidException.class,
            ConstraintViolationException.class,
            HttpMessageNotReadableException.class,
            JpaSystemException.class,
            IllegalArgumentException.class,
            MissingServletRequestPartException.class,
            MissingServletRequestParameterException.class,
            MultipartException.class,
            MaxUploadSizeExceededException.class
    })
    public ResponseEntity<Map<String, Object>> handleKnownExceptions(Exception ex) {
        HttpStatus status = status(ex);
        String message = resolveMessage(ex);
        if (ex instanceof FileUploadException fileUploadException) {
            return ResponseEntity.status(status).body(Map.of(
                    CODE_KEY, fileUploadException.getCode(),
                    MESSAGE_KEY, message
            ));
        }
        if (ex instanceof TooManyRequestsException tooManyRequestsException) {
            return ResponseEntity.status(status)
                    .header("Retry-After", String.valueOf(tooManyRequestsException.getRetryAfterSeconds()))
                    .body(Map.of(
                            CODE_KEY, "RATE_LIMIT_EXCEEDED",
                            MESSAGE_KEY, message,
                            "retryAfterSeconds", tooManyRequestsException.getRetryAfterSeconds()
                    ));
        }
        return ResponseEntity.status(status).body(Map.of(MESSAGE_KEY, message));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, String>> handleGeneric(Exception ex) {
        log.error("Unhandled admin exception", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(MESSAGE_KEY, "Something went wrong"));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Map<String, String>> handleAccessDenied(AccessDeniedException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of(MESSAGE_KEY, "Access denied"));
    }

    private String resolveMessage(Exception ex) {
        if (ex instanceof MethodArgumentNotValidException validationException) {
            return resolveValidationMessage(validationException);
        }
        if (ex instanceof MissingServletRequestPartException missingPartException) {
            return requiredParameterMessage(missingPartException.getRequestPartName(), "Required request part is missing");
        }
        if (ex instanceof MissingServletRequestParameterException missingParameterException) {
            return requiredParameterMessage(
                    missingParameterException.getParameterName(),
                    "Required request parameter is missing"
            );
        }
        String frameworkMessage = resolveFrameworkMessage(ex);
        if (frameworkMessage != null) {
            return frameworkMessage;
        }
        return ex.getMessage() == null ? INVALID_REQUEST_MESSAGE : ex.getMessage();
    }

    private HttpStatus status(Exception ex) {
        if (isInstanceOfAny(ex, NOT_FOUND_EXCEPTIONS)) {
            return HttpStatus.NOT_FOUND;
        }
        if (ex instanceof UnauthorizedException) {
            return HttpStatus.UNAUTHORIZED;
        }
        if (ex instanceof AccountLockedException) {
            return HttpStatus.LOCKED;
        }
        if (ex instanceof TooManyRequestsException) {
            return HttpStatus.TOO_MANY_REQUESTS;
        }
        if (ex instanceof InfrastructureException) {
            return HttpStatus.SERVICE_UNAVAILABLE;
        }
        if (ex instanceof MaxUploadSizeExceededException) {
            return HttpStatus.PAYLOAD_TOO_LARGE;
        }
        if (ex instanceof FileUploadException fileUploadException && FILE_TOO_LARGE_CODE.equals(fileUploadException.getCode())) {
            return HttpStatus.PAYLOAD_TOO_LARGE;
        }
        if (isInstanceOfAny(ex, BAD_REQUEST_EXCEPTIONS)) {
            return HttpStatus.BAD_REQUEST;
        }
        return HttpStatus.BAD_REQUEST;
    }

    private String resolveValidationMessage(MethodArgumentNotValidException validationException) {
        return validationException.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(field -> field.getDefaultMessage() == null ? INVALID_REQUEST_MESSAGE : field.getDefaultMessage())
                .orElse(INVALID_REQUEST_MESSAGE);
    }

    private String requiredParameterMessage(String parameterName, String defaultMessage) {
        return parameterName == null || parameterName.isBlank() ? defaultMessage : parameterName + " is required";
    }

    private String resolveFrameworkMessage(Exception ex) {
        if (ex instanceof HttpMessageNotReadableException readableException
                && readableException.getCause() instanceof InvalidFormatException invalidFormatException
                && invalidFormatException.getTargetType() != null
                && invalidFormatException.getTargetType().getName()
                .equals("com.lexho.user_service.subscription.enums.SubscriptionStatus")) {
            return "status must be one of: PENDING, ACTIVE, PAUSED, CANCELLED, COMPLETED";
        }
        if (ex instanceof MaxUploadSizeExceededException) {
            return "File size must be 5 MB or less";
        }
        if (ex instanceof MultipartException) {
            return "Invalid multipart request";
        }
        if (ex instanceof DataIntegrityViolationException) {
            return "Duplicate or invalid data";
        }
        if (ex instanceof JpaSystemException jpaEx && isInvalidDetailType(jpaEx)) {
            return "Invalid detail type. Allowed values: INCLUDED, EXCLUDED, FAQ, INSTRUCTION";
        }
        return null;
    }

    private boolean isInvalidDetailType(JpaSystemException jpaEx) {
        String detail = jpaEx.getMostSpecificCause() == null ? "" : jpaEx.getMostSpecificCause().getMessage();
        return detail.contains("Incorrect enum value") || detail.contains("Data truncated for column 'type'");
    }

    private boolean isInstanceOfAny(Exception ex, Set<Class<? extends Exception>> exceptionTypes) {
        return exceptionTypes.stream().anyMatch(type -> type.isInstance(ex));
    }
}

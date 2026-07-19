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
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.MultipartException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;

import java.util.Map;

@Order(Ordered.HIGHEST_PRECEDENCE)
@RestControllerAdvice(basePackages = {
        "com.lexho.user_service.controller",
        "com.lexho.user_service.admin.controller",
        "com.lexho.user.controller"
})
public class AdminExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(AdminExceptionHandler.class);

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
                    "code", fileUploadException.getCode(),
                    "message", message
            ));
        }
        if (ex instanceof TooManyRequestsException tooManyRequestsException) {
            return ResponseEntity.status(status)
                    .header("Retry-After", String.valueOf(tooManyRequestsException.getRetryAfterSeconds()))
                    .body(Map.of(
                            "code", "RATE_LIMIT_EXCEEDED",
                            "message", message,
                            "retryAfterSeconds", tooManyRequestsException.getRetryAfterSeconds()
                    ));
        }
        return ResponseEntity.status(status).body(Map.of("message", message));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, String>> handleGeneric(Exception ex) {
        log.error("Unhandled admin exception", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("message", "Something went wrong"));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Map<String, String>> handleAccessDenied(AccessDeniedException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("message", "Access denied"));
    }

    private String resolveMessage(Exception ex) {
        if (ex instanceof MethodArgumentNotValidException validationException) {
            return validationException.getBindingResult().getFieldErrors().stream()
                    .findFirst()
                    .map(field -> field.getDefaultMessage() == null ? "Invalid request" : field.getDefaultMessage())
                    .orElse("Invalid request");
        }
        if (ex instanceof MissingServletRequestPartException missingPartException) {
            String partName = missingPartException.getRequestPartName();
            return (partName == null || partName.isBlank() ? "Required request part is missing" : partName + " is required");
        }
        if (ex instanceof MissingServletRequestParameterException missingParameterException) {
            String parameterName = missingParameterException.getParameterName();
            return (parameterName == null || parameterName.isBlank() ? "Required request parameter is missing" : parameterName + " is required");
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
        if (ex instanceof JpaSystemException jpaEx) {
            String detail = jpaEx.getMostSpecificCause() == null ? "" : jpaEx.getMostSpecificCause().getMessage();
            if (detail.contains("Incorrect enum value") || detail.contains("Data truncated for column 'type'")) {
                return "Invalid detail type. Allowed values: INCLUDED, EXCLUDED, FAQ, INSTRUCTION";
            }
        }
        return ex.getMessage() == null ? "Invalid request" : ex.getMessage();
    }

    private HttpStatus status(Exception ex) {
        if (ex instanceof ResourceNotFoundException) {
            return HttpStatus.NOT_FOUND;
        }
        if (ex instanceof UserNotFoundException) {
            return HttpStatus.NOT_FOUND;
        }
        if (ex instanceof BannerNotFoundException) {
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
        if (ex instanceof MissingServletRequestPartException
                || ex instanceof MissingServletRequestParameterException
                || ex instanceof MultipartException) {
            return HttpStatus.BAD_REQUEST;
        }
        if (ex instanceof FileUploadException fileUploadException && "FILE_TOO_LARGE".equals(fileUploadException.getCode())) {
            return HttpStatus.PAYLOAD_TOO_LARGE;
        }
        if (ex instanceof DataIntegrityViolationException) {
            return HttpStatus.BAD_REQUEST;
        }
        if (ex instanceof JpaSystemException) {
            return HttpStatus.BAD_REQUEST;
        }
        return HttpStatus.BAD_REQUEST;
    }
}

package com.lexho.user_service.admin.config;

import com.lexho.user.exception.InvalidRoleException;
import com.lexho.user.exception.UserAlreadyExistsException;
import com.lexho.user.exception.UserNotFoundException;
import com.lexho.user_service.banner.exception.BannerNotFoundException;
import com.lexho.user_service.banner.exception.BannerValidationException;
import com.lexho.user_service.exception.*;
import jakarta.validation.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

@Order(Ordered.HIGHEST_PRECEDENCE)
@RestControllerAdvice(basePackages = {
        "com.lexho.user_service.controller",
        "com.lexho.user_service.admin.controller",
        "com.lexho.user.controller"
})
public class AdminExceptionHandler {

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
            DataIntegrityViolationException.class,
            BannerValidationException.class,
            BannerNotFoundException.class,
            MethodArgumentNotValidException.class,
            ConstraintViolationException.class,
            HttpMessageNotReadableException.class,
            IllegalArgumentException.class
    })
    public ResponseEntity<Map<String, String>> handleKnownExceptions(Exception ex) {
        HttpStatus status = status(ex);
        String message = resolveMessage(ex);
        return ResponseEntity.status(status).body(Map.of("message", message));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, String>> handleGeneric(Exception ex) {
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
        if (ex instanceof DataIntegrityViolationException) {
            return "Duplicate or invalid data";
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
        if (ex instanceof DataIntegrityViolationException) {
            return HttpStatus.BAD_REQUEST;
        }
        return HttpStatus.BAD_REQUEST;
    }
}

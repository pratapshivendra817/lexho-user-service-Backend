package com.lexho.user_service.banner.exception;

import jakarta.validation.ConstraintViolationException;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

@Order(Ordered.HIGHEST_PRECEDENCE)
@RestControllerAdvice(basePackages = "com.lexho.user_service.banner")
public class BannerExceptionHandler {

    private static final String MESSAGE_KEY = "message";
    private static final String INVALID_REQUEST_MESSAGE = "Invalid request";

    @ExceptionHandler(BannerNotFoundException.class)
    public ResponseEntity<Map<String, String>> handleNotFound(BannerNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of(MESSAGE_KEY, ex.getMessage()));
    }

    @ExceptionHandler(BannerValidationException.class)
    public ResponseEntity<Map<String, String>> handleValidation(BannerValidationException ex) {
        return ResponseEntity.badRequest().body(Map.of(MESSAGE_KEY, ex.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> handleMethodValidation(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(FieldError::getDefaultMessage)
                .orElse(INVALID_REQUEST_MESSAGE);
        return ResponseEntity.badRequest().body(Map.of(MESSAGE_KEY, message));
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<Map<String, String>> handleConstraintViolation(ConstraintViolationException ex) {
        return ResponseEntity.badRequest().body(Map.of(MESSAGE_KEY, INVALID_REQUEST_MESSAGE));
    }

    @ExceptionHandler({HttpMessageNotReadableException.class, IllegalArgumentException.class})
    public ResponseEntity<Map<String, String>> handleMalformedInput(Exception ex) {
        return ResponseEntity.badRequest().body(Map.of(MESSAGE_KEY, INVALID_REQUEST_MESSAGE));
    }
}

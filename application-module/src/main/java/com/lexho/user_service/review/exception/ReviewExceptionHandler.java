package com.lexho.user_service.review.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

@RestControllerAdvice(basePackages = "com.lexho.user_service.review")
public class ReviewExceptionHandler {

    @ExceptionHandler(ReviewNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleNotFound(ReviewNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", ex.getMessage()));
    }

    @ExceptionHandler(ReviewAccessDeniedException.class)
    public ResponseEntity<Map<String, Object>> handleForbidden(ReviewAccessDeniedException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("message", ex.getMessage()));
    }

    @ExceptionHandler(ReviewValidationException.class)
    public ResponseEntity<Map<String, Object>> handleValidation(ReviewValidationException ex) {
        return ResponseEntity.badRequest().body(Map.of("message", ex.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleRequestValidation(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors()
                .stream()
                .findFirst()
                .map(FieldError::getDefaultMessage)
                .orElse("Invalid request");
        return ResponseEntity.badRequest().body(Map.of("message", message));
    }
}

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

    private static final String CODE_KEY = "code";
    private static final String MESSAGE_KEY = "message";
    private static final String SUCCESS_KEY = "success";

    @ExceptionHandler(ReviewNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleNotFound(ReviewNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error(ex.getCode(), ex.getMessage()));
    }

    @ExceptionHandler(ReviewAccessDeniedException.class)
    public ResponseEntity<Map<String, Object>> handleForbidden(ReviewAccessDeniedException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(error(ex.getCode(), ex.getMessage()));
    }

    @ExceptionHandler(ReviewValidationException.class)
    public ResponseEntity<Map<String, Object>> handleValidation(ReviewValidationException ex) {
        return ResponseEntity.badRequest().body(error(ex.getCode(), ex.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleRequestValidation(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors()
                .stream()
                .findFirst()
                .map(FieldError::getDefaultMessage)
                .orElse("Invalid request");
        boolean ratingInvalid = ex.getBindingResult().getFieldErrors()
                .stream()
                .map(FieldError::getField)
                .anyMatch("rating"::equals);
        String code = ratingInvalid ? "INVALID_RATING" : "INVALID_REVIEW";
        return ResponseEntity.badRequest().body(error(code, message));
    }

    private Map<String, Object> error(String code, String message) {
        return Map.of(SUCCESS_KEY, false, CODE_KEY, code, MESSAGE_KEY, message);
    }
}

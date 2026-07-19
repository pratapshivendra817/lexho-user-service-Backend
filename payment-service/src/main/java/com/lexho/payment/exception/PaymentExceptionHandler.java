package com.lexho.payment.exception;

import com.lexho.payment.dto.PaymentApiResponse;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.bind.ServletRequestBindingException;

import java.util.stream.Collectors;

@RestControllerAdvice(basePackages = "com.lexho.payment")
@Order(Ordered.HIGHEST_PRECEDENCE)
public class PaymentExceptionHandler {

    private static final Logger LOG = LoggerFactory.getLogger(PaymentExceptionHandler.class);

    @ExceptionHandler(PaymentException.class)
    public ResponseEntity<PaymentApiResponse<Void>> handlePaymentException(PaymentException exception) {
        LOG.warn("Payment request rejected: {}", exception.getMessage());
        return ResponseEntity.badRequest().body(PaymentApiResponse.failure(errorCodeFor(exception.getMessage()), exception.getMessage()));
    }

    @ExceptionHandler({
            MethodArgumentNotValidException.class,
            BindException.class,
            ConstraintViolationException.class,
            MissingServletRequestParameterException.class,
            ServletRequestBindingException.class,
            HandlerMethodValidationException.class
    })
    public ResponseEntity<PaymentApiResponse<Void>> handleValidationException(Exception exception) {
        String message = validationMessageFor(exception);
        LOG.warn("Payment request validation failed: {}", message);
        return ResponseEntity.badRequest()
                .body(PaymentApiResponse.failure("INVALID_PAYMENT_REQUEST", message));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<PaymentApiResponse<Void>> handleUnexpectedException(Exception exception) {
        LOG.error("Unexpected payment error", exception);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(PaymentApiResponse.failure("Payment service is temporarily unavailable"));
    }

    private String errorCodeFor(String message) {
        String normalized = message == null ? "" : message.toUpperCase(java.util.Locale.ROOT);
        if (normalized.contains("EXPIRED")) {
            return "PAYMENT_EXPIRED";
        }
        if (normalized.contains("ALREADY PAID")) {
            return "PAYMENT_ALREADY_PAID";
        }
        if (normalized.contains("NOT FOUND")) {
            return "PAYMENT_NOT_FOUND";
        }
        return "PAYMENT_FAILED";
    }

    private String validationMessageFor(Exception exception) {
        if (exception instanceof MethodArgumentNotValidException methodArgumentNotValidException) {
            return methodArgumentNotValidException.getBindingResult().getFieldErrors().stream()
                    .map(error -> error.getField() + " " + error.getDefaultMessage())
                    .collect(Collectors.joining("; "));
        }
        if (exception instanceof BindException bindException) {
            return bindException.getBindingResult().getFieldErrors().stream()
                    .map(error -> error.getField() + " " + error.getDefaultMessage())
                    .collect(Collectors.joining("; "));
        }
        if (exception instanceof ConstraintViolationException constraintViolationException) {
            return constraintViolationException.getConstraintViolations().stream()
                    .map(violation -> violation.getPropertyPath() + " " + violation.getMessage())
                    .collect(Collectors.joining("; "));
        }
        if (exception instanceof MissingServletRequestParameterException missingServletRequestParameterException) {
            return missingServletRequestParameterException.getParameterName() + " parameter is required";
        }
        if (exception instanceof ServletRequestBindingException servletRequestBindingException) {
            return servletRequestBindingException.getMessage();
        }
        if (exception instanceof HandlerMethodValidationException handlerMethodValidationException) {
            return handlerMethodValidationException.getMessage();
        }
        return "Invalid payment request";
    }
}

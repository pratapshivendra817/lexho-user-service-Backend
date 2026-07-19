package com.lexho.booking.exception;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class BookingExceptionHandlerTest {

    private final BookingExceptionHandler handler = new BookingExceptionHandler();

    @Test
    void handleNotFoundReturns404() {
        ResponseEntity<Map<String, Object>> response =
                handler.handleNotFound(new BookingNotFoundException(12L));

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertEquals("Booking not found for id: 12", response.getBody().get("message"));
    }

    @Test
    void handleBadRequestSupportsOperationAndValidationExceptions() {
        ResponseEntity<Map<String, Object>> operation =
                handler.handleBadRequest(new BookingOperationException("operation failed"));
        ResponseEntity<Map<String, Object>> validation =
                handler.handleBadRequest(new BookingValidationException("validation failed"));

        assertEquals(HttpStatus.BAD_REQUEST, operation.getStatusCode());
        assertEquals("operation failed", operation.getBody().get("message"));
        assertEquals("validation failed", validation.getBody().get("message"));
    }

    @Test
    void handleValidationReturnsFirstFieldError() {
        MethodArgumentNotValidException exception = mock(MethodArgumentNotValidException.class);
        BindingResult bindingResult = mock(BindingResult.class);
        when(exception.getBindingResult()).thenReturn(bindingResult);
        when(bindingResult.getFieldErrors()).thenReturn(List.of(
                new FieldError("request", "amount", "Amount must be greater than zero"),
                new FieldError("request", "providerId", "Partner id is required")));

        ResponseEntity<Map<String, Object>> response = handler.handleValidation(exception);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("Amount must be greater than zero", response.getBody().get("message"));
    }

    @Test
    void handleValidationUsesFallbackWhenNoFieldErrorsExist() {
        MethodArgumentNotValidException exception = mock(MethodArgumentNotValidException.class);
        BindingResult bindingResult = mock(BindingResult.class);
        when(exception.getBindingResult()).thenReturn(bindingResult);
        when(bindingResult.getFieldErrors()).thenReturn(List.of());

        ResponseEntity<Map<String, Object>> response = handler.handleValidation(exception);

        assertEquals("Invalid request", response.getBody().get("message"));
    }
}

package com.lexho.user_service.exception;

import com.lexho.user_service.booking.exception.BookingValidationException;
import com.lexho.user_service.booking.exception.InstantBookingUnavailableException;
import com.lexho.user_service.notification.exception.NotificationValidationException;
import com.lexho.user_service.subscription.exception.SubscriptionValidationException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.ServletRequestBindingException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.MultipartException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();
    private final HttpServletRequest request = mock(HttpServletRequest.class);

    @Test
    void mapsBadRequestNotFoundAndKnownErrorCodes() {
        when(request.getRequestURI()).thenReturn("/api/orders/1");

        var badRequest = handler.handleBadRequest(new BadRequestException("payment failed"));
        var notFound = handler.handleResourceNotFound(new ResourceNotFoundException("Booking not found"), request);
        var illegalState = handler.handleIllegalState(new IllegalStateException(), request);

        assertThat(badRequest.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(badRequest.getBody().getCode()).isEqualTo("PAYMENT_FAILED");
        assertThat(notFound.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(notFound.getBody().code()).isEqualTo("ORDER_NOT_FOUND");
        assertThat(notFound.getBody().path()).isEqualTo("/api/orders/1");
        assertThat(illegalState.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(illegalState.getBody().message()).isEqualTo("Invalid state");
    }

    @Test
    void mapsUploadRateLimitSecurityAndInfrastructureErrors() {
        var uploadTooLarge = handler.handleMaxUploadSize(new MaxUploadSizeExceededException(30), request);
        assertThat(uploadTooLarge.getStatusCode()).isEqualTo(HttpStatus.PAYLOAD_TOO_LARGE);
        assertThat(uploadTooLarge.getBody().message()).contains("total request must be 30 MB or less");
        assertThat(handler.handleFileUpload(new FileUploadException("INVALID_FILE_TYPE", "bad"), request).getStatusCode())
                .isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(handler.handleTooManyRequests(new TooManyRequestsException(null, 9), request).getHeaders()
                .getFirst("Retry-After")).isEqualTo("9");
        assertThat(handler.handleAccessDenied(new AccessDeniedException("no"), request).getStatusCode())
                .isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(handler.handleInfrastructure(new InfrastructureException(null)).getStatusCode())
                .isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
    }

    @Test
    void mapsFrameworkAndUnexpectedExceptions() {
        assertThat(handler.handleMissingRequestParam(
                new MissingServletRequestParameterException("city", "String")).getStatusCode())
                .isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(handler.handleMultipartError(new MultipartException("bad")).getStatusCode())
                .isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(handler.handleDataIntegrity(new DataIntegrityViolationException("duplicate")).getStatusCode())
                .isEqualTo(HttpStatus.CONFLICT);
        assertThat(handler.handleServletRequestBinding(
                new ServletRequestBindingException("Missing request attribute 'userId'")).getStatusCode())
                .isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(handler.handleGlobal(new Exception("boom"), null).getStatusCode())
                .isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @ParameterizedTest
    @CsvSource({
            "'Booking not found',ORDER_NOT_FOUND",
            "'Order not found',ORDER_NOT_FOUND",
            "'Payment processing failed',PAYMENT_FAILED",
            "'Payment link expired',PAYMENT_EXPIRED",
            "'Subscription is paused',SUBSCRIPTION_PAUSED",
            "'Subscription was cancelled',SUBSCRIPTION_CANCELLED",
            "'Partner is unavailable',PARTNER_UNAVAILABLE",
            "'Live tracking is disabled',TRACKING_DISABLED",
            "'Unsupported operation',INVALID_REQUEST"
    })
    void mapsEveryBusinessErrorCode(String message, String expectedCode) {
        var response = handler.handleBadRequest(new BadRequestException(message));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().getCode()).isEqualTo(expectedCode);
        assertThat(response.getBody().getMessage()).isEqualTo(message);
    }

    @Test
    void appliesDefaultMessagesForBlankCustomExceptions() {
        assertThat(handler.handleBadRequest(new BadRequestException(" ")).getBody().getMessage())
                .isEqualTo("Invalid request");
        assertThat(handler.handleUnauthorized(new UnauthorizedException(null)).getBody().getMessage())
                .isEqualTo("Unauthorized");
        assertThat(handler.handleAccountLocked(new AccountLockedException(" ")).getBody().getMessage())
                .isEqualTo("Account locked");
        assertThat(handler.handleInfrastructure(new InfrastructureException("")).getBody().getMessage())
                .isEqualTo("Service unavailable");
        assertThat(handler.handleNotificationValidation(new NotificationValidationException(" "))
                .getBody().getMessage()).isEqualTo("Invalid notification request");
        assertThat(handler.handleBadInput(new IllegalArgumentException()).getBody().getMessage())
                .isEqualTo("Invalid request");
    }

    @Test
    void mapsCodedSubscriptionInstantAndStandardBookingValidation() {
        var coded = handler.handleCodedValidation(
                new CodedValidationException("INVALID_SLOT", "Slot unavailable", HttpStatus.CONFLICT),
                request);
        var subscription = handler.handleSubscriptionValidation(
                new SubscriptionValidationException(List.of("plan is required", "date is invalid")));
        var instant = handler.handleInstantUnavailable(
                new InstantBookingUnavailableException("NO_PARTNER", "No partner available"));
        var booking = handler.handleBookingValidation(
                new BookingValidationException("INVALID_DATE", "Date is invalid"));

        assertThat(coded.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(coded.getBody().code()).isEqualTo("INVALID_SLOT");
        assertThat(subscription.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(subscription.getBody().getData().toString()).contains("plan is required");
        assertThat(instant.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(instant.getBody().getCode()).isEqualTo("NO_PARTNER");
        assertThat(booking.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "BOOKING_OUTSIDE_SERVICE_ZONE",
            "BOOKING_OUTSIDE_GEOFENCE",
            "BOOKING_ADDRESS_COORDINATES_REQUIRED",
            "SAVED_ADDRESS_COORDINATES_MISSING"
    })
    void mapsLocationBookingValidationCodesToConflict(String errorCode) {
        var response = handler.handleBookingValidation(
                new BookingValidationException(errorCode, "Location is not serviceable"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody().getCode()).isEqualTo(errorCode);
    }

    @Test
    void extractsFirstFieldValidationErrorAndUsesFallbackWhenAbsent() {
        when(request.getRequestURI()).thenReturn("/api/validation");
        BindingResult bindingResult = mock(BindingResult.class);
        MethodArgumentNotValidException exception = mock(MethodArgumentNotValidException.class);
        when(exception.getBindingResult()).thenReturn(bindingResult);
        when(bindingResult.getFieldErrors()).thenReturn(List.of(
                new FieldError("request", "email", "must be a valid email")));

        var fieldError = handler.handleValidation(exception, request);
        when(bindingResult.getFieldErrors()).thenReturn(List.of());
        var fallback = handler.handleValidation(exception, request);

        assertThat(fieldError.getBody().message()).isEqualTo("must be a valid email");
        assertThat(fieldError.getBody().path()).isEqualTo("/api/validation");
        assertThat(fallback.getBody().message()).isEqualTo("Invalid request");
    }

    @Test
    void mapsMalformedTypeMismatchMissingPartAndMissingEndpoint() {
        MethodArgumentTypeMismatchException mismatch = mock(MethodArgumentTypeMismatchException.class);
        when(mismatch.getName()).thenReturn("bookingId");
        MethodArgumentTypeMismatchException unnamedMismatch = mock(MethodArgumentTypeMismatchException.class);
        when(unnamedMismatch.getName()).thenReturn(null);

        assertThat(handler.handleMalformedRequest(new HttpMessageNotReadableException("bad json")).getStatusCode())
                .isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(handler.handleMalformedRequest(mock(ConstraintViolationException.class)).getStatusCode())
                .isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(handler.handleTypeMismatch(mismatch).getBody().getMessage())
                .isEqualTo("Invalid bookingId");
        assertThat(handler.handleTypeMismatch(unnamedMismatch).getBody().getMessage())
                .isEqualTo("Invalid parameter");
        assertThat(handler.handleBadInput(new MissingServletRequestPartException("file")).getStatusCode())
                .isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(handler.handleNoResourceFound(
                new NoResourceFoundException(HttpMethod.GET, "/missing")).getStatusCode())
                .isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void mapsInvalidJsonFieldFormatToHelpfulMessage() {
        var cause = new com.fasterxml.jackson.databind.exc.InvalidFormatException(
                null, "bad date", "24-09-2026", java.time.LocalDate.class);
        cause.prependPath(new Object(), "dateOfBirth");
        HttpMessageNotReadableException exception = new HttpMessageNotReadableException("bad json", cause);

        var response = handler.handleMalformedRequest(exception);

        assertThat(response.getBody().getMessage()).isEqualTo("dateOfBirth must use YYYY-MM-DD");
    }

    @Test
    void mapsFileSizeRateLimitAndRequestPathDetails() {
        when(request.getRequestURI()).thenReturn("/api/upload");

        var fileTooLarge = handler.handleFileUpload(
                new FileUploadException("FILE_TOO_LARGE", "too large"), request);
        var rateLimited = handler.handleTooManyRequests(
                new TooManyRequestsException(" ", 12), request);
        var explicitState = handler.handleIllegalState(
                new IllegalStateException("Cannot transition"), request);

        assertThat(fileTooLarge.getStatusCode()).isEqualTo(HttpStatus.PAYLOAD_TOO_LARGE);
        assertThat(fileTooLarge.getBody().path()).isEqualTo("/api/upload");
        assertThat(rateLimited.getStatusCode()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
        assertThat(rateLimited.getBody()).containsEntry("message", "Too many requests");
        assertThat(explicitState.getBody().message()).isEqualTo("Cannot transition");
    }
}

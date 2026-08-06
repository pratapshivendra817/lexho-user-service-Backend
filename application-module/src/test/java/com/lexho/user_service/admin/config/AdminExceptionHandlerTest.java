package com.lexho.user_service.admin.config;

import com.lexho.user_service.exception.AccountLockedException;
import com.lexho.user_service.exception.FileUploadException;
import com.lexho.user_service.exception.InfrastructureException;
import com.lexho.user_service.exception.ResourceNotFoundException;
import com.lexho.user_service.exception.TooManyRequestsException;
import com.lexho.user_service.exception.UnauthorizedException;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.orm.jpa.JpaSystemException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.MultipartException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;

import static org.assertj.core.api.Assertions.assertThat;

class AdminExceptionHandlerTest {

    private final AdminExceptionHandler handler = new AdminExceptionHandler();

    @Test
    void mapsKnownExceptionsToExpectedStatusesAndBodies() {
        assertThat(handler.handleKnownExceptions(new ResourceNotFoundException("missing")).getStatusCode())
                .isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(handler.handleKnownExceptions(new UnauthorizedException("login")).getStatusCode())
                .isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(handler.handleKnownExceptions(new AccountLockedException("locked")).getStatusCode())
                .isEqualTo(HttpStatus.LOCKED);
        assertThat(handler.handleKnownExceptions(new InfrastructureException("down")).getStatusCode())
                .isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
        assertThat(handler.handleKnownExceptions(new DataIntegrityViolationException("duplicate")).getBody())
                .containsEntry("message", "Duplicate or invalid data");
    }

    @Test
    void mapsRateLimitAndFileUploadMetadata() {
        var limited = handler.handleKnownExceptions(new TooManyRequestsException("slow down", 27));
        var tooLarge = handler.handleKnownExceptions(new FileUploadException("FILE_TOO_LARGE", "large"));
        var invalidFile = handler.handleKnownExceptions(new FileUploadException("INVALID_FILE_TYPE", "bad"));

        assertThat(limited.getStatusCode()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
        assertThat(limited.getHeaders().getFirst("Retry-After")).isEqualTo("27");
        assertThat(limited.getBody()).containsEntry("code", "RATE_LIMIT_EXCEEDED")
                .containsEntry("retryAfterSeconds", 27L);
        assertThat(tooLarge.getStatusCode()).isEqualTo(HttpStatus.PAYLOAD_TOO_LARGE);
        assertThat(invalidFile.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void mapsMultipartFailuresToClientErrors() {
        var missingPart = handler.handleKnownExceptions(new MissingServletRequestPartException("file"));
        var missingParam = handler.handleKnownExceptions(
                new MissingServletRequestParameterException("file", "MultipartFile"));
        var multipart = handler.handleKnownExceptions(new MultipartException("bad multipart"));
        var maxUploadSize = handler.handleKnownExceptions(new MaxUploadSizeExceededException(6 * 1024 * 1024));

        assertThat(missingPart.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(missingPart.getBody()).containsEntry("message", "file is required");
        assertThat(missingParam.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(missingParam.getBody()).containsEntry("message", "file is required");
        assertThat(multipart.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(multipart.getBody()).containsEntry("message", "Invalid multipart request");
        assertThat(maxUploadSize.getStatusCode()).isEqualTo(HttpStatus.PAYLOAD_TOO_LARGE);
        assertThat(maxUploadSize.getBody()).containsEntry("message", "File size must be 5 MB or less");
    }

    @Test
    void mapsAccessDeniedAndGenericExceptions() {
        assertThat(handler.handleAccessDenied(new AccessDeniedException("no")).getStatusCode())
                .isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(handler.handleGeneric(new Exception("boom")).getStatusCode())
                .isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @Test
    void databaseExceptionsDoNotLeakSqlOrJdbcDetails() {
        var duplicate = handler.handleKnownExceptions(new DataIntegrityViolationException(
                "Duplicate entry '1' for key 'uq_x' while persisting admin credentials"));
        var jpa = handler.handleKnownExceptions(new JpaSystemException(
                new RuntimeException("Data truncated for column 'type' at row 1 during admin sync")));
        var generic = handler.handleGeneric(new Exception(
                "SQL syntax error near SELECT * FROM users during admin sync"));

        assertThat(duplicate.getBody()).containsEntry("message", "Duplicate or invalid data");
        assertThat(jpa.getBody()).containsEntry("message",
                "Invalid detail type. Allowed values: INCLUDED, EXCLUDED, FAQ, INSTRUCTION");
        assertThat(generic.getBody()).containsEntry("message", "Something went wrong");
        assertThat(duplicate.getBody().toString()).doesNotContain("jdbc:mysql").doesNotContain("SELECT *");
        assertThat(jpa.getBody().toString()).doesNotContain("jdbc:mysql").doesNotContain("Data truncated");
        assertThat(generic.getBody().toString()).doesNotContain("jdbc:mysql").doesNotContain("SELECT *");
    }
}

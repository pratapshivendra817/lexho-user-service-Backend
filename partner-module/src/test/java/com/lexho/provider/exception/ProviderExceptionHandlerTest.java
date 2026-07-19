package com.lexho.provider.exception;

import jakarta.validation.ConstraintViolationException;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class ProviderExceptionHandlerTest {

    private final ProviderExceptionHandler handler = new ProviderExceptionHandler();

    @Test
    void handleProviderNotFoundShouldReturnNotFoundMessage() {
        ResponseEntity<Map<String, String>> response = handler.handleProviderNotFound(new ProviderNotFoundException(42L));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).containsEntry("message", "Partner not found for id: 42");
    }

    @Test
    void handleDocumentNotFoundShouldReturnNotFoundMessage() {
        ResponseEntity<Map<String, String>> response = handler.handleDocumentNotFound(new DocumentNotFoundException(9L));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).containsEntry("message", "Document not found for id: 9");
    }

    @Test
    void handleProviderRuntimeShouldMapValidationAndOperationToBadRequest() {
        ResponseEntity<Map<String, String>> validation =
                handler.handleProviderRuntime(new ProviderValidationException("Invalid status"));
        ResponseEntity<Map<String, String>> operation =
                handler.handleProviderRuntime(new ProviderOperationException("Invalid transition"));

        assertThat(validation.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(validation.getBody()).containsEntry("message", "Invalid status");
        assertThat(operation.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(operation.getBody()).containsEntry("message", "Invalid transition");
    }

    @Test
    void handleProviderFileUploadShouldMapCodeSpecificStatus() {
        ResponseEntity<Map<String, Object>> invalidType = handler.handleProviderFileUpload(
                new ProviderFileUploadException("INVALID_FILE_TYPE", "PAN must be a PDF file"));
        ResponseEntity<Map<String, Object>> tooLarge = handler.handleProviderFileUpload(
                new ProviderFileUploadException("FILE_TOO_LARGE", "File too large"));

        assertThat(invalidType.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(invalidType.getBody()).containsEntry("code", "INVALID_FILE_TYPE")
                .containsEntry("message", "PAN must be a PDF file");
        assertThat(tooLarge.getStatusCode()).isEqualTo(HttpStatus.PAYLOAD_TOO_LARGE);
        assertThat(tooLarge.getBody()).containsEntry("code", "FILE_TOO_LARGE");
    }

    @Test
    void handleInfrastructureAndSecurityExceptionsShouldReturnStableMessages() {
        ResponseEntity<Map<String, String>> duplicate =
                handler.handleDataIntegrity(new DataIntegrityViolationException("duplicate key"));
        ResponseEntity<Map<String, String>> constraint =
                handler.handleConstraint(new ConstraintViolationException("bad request", null));
        ResponseEntity<Map<String, String>> denied =
                handler.handleAccessDenied(new AccessDeniedException("denied"));
        ResponseEntity<Map<String, String>> generic =
                handler.handleGeneric(new Exception("database down"));

        assertThat(duplicate.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(duplicate.getBody()).containsEntry("message", "Duplicate or invalid partner data");
        assertThat(constraint.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(constraint.getBody()).containsEntry("message", "Invalid request");
        assertThat(denied.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(denied.getBody()).containsEntry("message", "Access denied");
        assertThat(generic.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(generic.getBody()).containsEntry("message", "Something went wrong");
    }
}

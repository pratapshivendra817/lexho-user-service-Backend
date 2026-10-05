package com.lexho.user_service.upload;

import com.lexho.user_service.exception.FileUploadException;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class UploadSecurityServiceTest {

    private final UploadSecurityService service = new UploadSecurityService();

    @Test
    void validateDocumentShouldAcceptPdfWithMatchingMimeType() {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "invoice.pdf",
                "application/pdf",
                "%PDF-1.7".getBytes(StandardCharsets.UTF_8));

        assertEquals("pdf", service.validateDocument(file));
    }

    @Test
    void validateImageShouldRejectPdfExtension() {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "invoice.pdf",
                "application/pdf",
                "%PDF-1.7".getBytes(StandardCharsets.UTF_8));

        FileUploadException exception = assertThrows(FileUploadException.class,
                () -> service.validateImage(file));

        assertEquals("INVALID_FILE_TYPE", exception.getCode());
    }

    @Test
    void validateDocumentShouldRejectMimeMismatch() {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "photo.png",
                "application/pdf",
                "not-a-png".getBytes(StandardCharsets.UTF_8));

        FileUploadException exception = assertThrows(FileUploadException.class,
                () -> service.validateDocument(file));

        assertEquals("INVALID_FILE_TYPE", exception.getCode());
    }

    @Test
    void validateDocumentShouldRejectFilesLargerThanConfiguredLimit() {
        ReflectionTestUtils.setField(service, "maxFileSizeBytes", 5L * 1024L * 1024L);
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "large.jpg",
                "image/jpeg",
                new byte[(5 * 1024 * 1024) + 1]);

        FileUploadException exception = assertThrows(FileUploadException.class,
                () -> service.validateDocument(file));

        assertEquals("FILE_TOO_LARGE", exception.getCode());
    }

    @Test
    void validateDocumentShouldRejectExecutableMagicAndEicarPayloads() {
        MockMultipartFile executable = new MockMultipartFile(
                "file",
                "payload.jpg",
                "image/jpeg",
                new byte[] {'M', 'Z', 0, 0});
        MockMultipartFile eicar = new MockMultipartFile(
                "file",
                "scan.pdf",
                "application/pdf",
                "EICAR-STANDARD-ANTIVIRUS-TEST-FILE".getBytes(StandardCharsets.US_ASCII));

        assertEquals("FILE_SCAN_FAILED", assertThrows(FileUploadException.class,
                () -> service.validateDocument(executable)).getCode());
        assertEquals("FILE_SCAN_FAILED", assertThrows(FileUploadException.class,
                () -> service.validateDocument(eicar)).getCode());
    }

    @Test
    void sanitizedOriginalNameShouldRemovePathsAndUnsafeCharacters() {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "..\\unsafe folder/photo 1.png",
                "image/png",
                new byte[] {1});

        assertEquals("photo_1.png", service.sanitizedOriginalName(file));
    }
}

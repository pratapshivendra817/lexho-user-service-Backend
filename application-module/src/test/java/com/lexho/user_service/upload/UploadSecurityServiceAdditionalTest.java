package com.lexho.user_service.upload;

import com.lexho.user_service.exception.FileUploadException;
import org.junit.jupiter.api.Test;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

class UploadSecurityServiceAdditionalTest {

    private final UploadSecurityService service = new UploadSecurityService();

    @Test
    void validateDocumentRejectsMissingFileUnsupportedExtensionsAndExecutableExtensions() {
        assertThat(assertThrows(FileUploadException.class, () -> service.validateDocument(null)).getCode())
                .isEqualTo("INVALID_FILE_TYPE");

        MultipartFile noExtension = file("photo", "image/jpeg", new byte[] {1});
        MultipartFile executable = file("tool.jar", "application/pdf", "%PDF".getBytes(StandardCharsets.UTF_8));

        assertThat(assertThrows(FileUploadException.class, () -> service.validateDocument(noExtension)).getCode())
                .isEqualTo("INVALID_FILE_TYPE");
        assertThat(assertThrows(FileUploadException.class, () -> service.validateDocument(executable)).getCode())
                .isEqualTo("INVALID_FILE_TYPE");
    }

    @Test
    void validateDocumentRejectsScriptMagicAndIoFailures() {
        MultipartFile script = file("photo.jpg", "image/jpeg", "#!/bin/sh".getBytes(StandardCharsets.UTF_8));
        MultipartFile broken = new MultipartFile() {
            @Override public String getName() { return "file"; }
            @Override public String getOriginalFilename() { return "photo.jpg"; }
            @Override public String getContentType() { return "image/jpeg"; }
            @Override public boolean isEmpty() { return false; }
            @Override public long getSize() { return 10; }
            @Override public byte[] getBytes() { return new byte[0]; }
            @Override public InputStream getInputStream() throws IOException { throw new IOException("boom"); }
            @Override public void transferTo(java.io.File dest) {
                // No-op: this broken multipart is only used to exercise getInputStream failure handling.
            }
        };

        assertThat(assertThrows(FileUploadException.class, () -> service.validateDocument(script)).getCode())
                .isEqualTo("FILE_SCAN_FAILED");
        assertThat(assertThrows(FileUploadException.class, () -> service.validateDocument(broken)).getCode())
                .isEqualTo("FILE_SCAN_FAILED");
    }

    @Test
    void sanitizedOriginalNameFallsBackToFileWhenBlankOrUnsafe() {
        assertThat(service.sanitizedOriginalName(file("", "image/png", new byte[] {1}))).isEqualTo("file");
        assertThat(service.sanitizedOriginalName(file("???", "image/png", new byte[] {1}))).isEqualTo("___");
        assertThat(service.sanitizedOriginalName(file("/tmp/ok-name.pdf", "application/pdf", new byte[] {1})))
                .isEqualTo("ok-name.pdf");
    }

    private MultipartFile file(String originalName, String contentType, byte[] bytes) {
        return new org.springframework.mock.web.MockMultipartFile("file", originalName, contentType, bytes);
    }
}

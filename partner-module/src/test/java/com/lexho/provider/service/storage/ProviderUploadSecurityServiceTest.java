package com.lexho.provider.service.storage;

import com.lexho.provider.exception.ProviderFileUploadException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProviderUploadSecurityServiceTest {

    @Mock
    private MultipartFile multipartFile;

    private final ProviderUploadSecurityService service = new ProviderUploadSecurityService();

    @Test
    void validateAcceptsSupportedFilesCaseInsensitively() {
        assertEquals("pdf", service.validate(file("doc.PDF", "APPLICATION/PDF", "safe")));
        assertEquals("jpg", service.validate(file("photo.jpg", "image/jpeg", "safe")));
        assertEquals("jpeg", service.validate(file("photo.jpeg", "image/jpg", "safe")));
        assertEquals("png", service.validate(file("photo.png", "image/png", "safe")));
    }

    @Test
    void validateRejectsNullEmptyAndOversizedFiles() {
        assertCode("INVALID_FILE_TYPE", assertThrows(ProviderFileUploadException.class, () -> service.validate(null)));
        assertCode("INVALID_FILE_TYPE", assertThrows(ProviderFileUploadException.class,
                () -> service.validate(new MockMultipartFile("file", new byte[0]))));
        when(multipartFile.isEmpty()).thenReturn(false);
        when(multipartFile.getSize()).thenReturn(5L * 1024L * 1024L + 1);
        assertCode("FILE_TOO_LARGE", assertThrows(ProviderFileUploadException.class,
                () -> service.validate(multipartFile)));
    }

    @Test
    void validateRejectsMissingUnsupportedAndMismatchedExtensions() {
        assertCode("INVALID_FILE_TYPE", failure(file("document", "application/pdf", "safe")));
        assertCode("INVALID_FILE_TYPE", failure(file("document.", "application/pdf", "safe")));
        assertCode("INVALID_FILE_TYPE", failure(file("script.exe", "application/octet-stream", "safe")));
        assertCode("INVALID_FILE_TYPE", failure(file("photo.png", "image/jpeg", "safe")));
        assertCode("INVALID_FILE_TYPE", failure(file("photo.png", null, "safe")));
    }

    @Test
    void validateRejectsExecutableAndMalwareSignatures() {
        assertCode("FILE_SCAN_FAILED", failure(file("x.pdf", "application/pdf", new byte[]{'M', 'Z'})));
        assertCode("FILE_SCAN_FAILED", failure(file("x.pdf", "application/pdf", new byte[]{0x7F, 'E', 'L', 'F'})));
        assertCode("FILE_SCAN_FAILED", failure(file("x.pdf", "application/pdf",
                "EICAR-STANDARD-ANTIVIRUS-TEST-FILE".getBytes())));
    }

    @Test
    void validateWrapsInputStreamFailure() throws Exception {
        when(multipartFile.isEmpty()).thenReturn(false);
        when(multipartFile.getSize()).thenReturn(10L);
        when(multipartFile.getOriginalFilename()).thenReturn("doc.pdf");
        when(multipartFile.getContentType()).thenReturn("application/pdf");
        when(multipartFile.getInputStream()).thenThrow(new IOException("broken"));

        assertCode("FILE_SCAN_FAILED", assertThrows(ProviderFileUploadException.class,
                () -> service.validate(multipartFile)));
    }

    @Test
    void sanitizedOriginalNameRemovesPathsAndUnsafeCharacters() {
        assertEquals("document", service.sanitizedOriginalName(null));
        assertEquals("document", service.sanitizedOriginalName(file(null, "application/pdf", "x")));
        assertEquals("my_file_.pdf", service.sanitizedOriginalName(file("C:\\temp\\my file!.pdf", "application/pdf", "x")));
        assertEquals("document", service.sanitizedOriginalName(file("///", "application/pdf", "x")));
    }

    private ProviderFileUploadException failure(MockMultipartFile file) {
        return assertThrows(ProviderFileUploadException.class, () -> service.validate(file));
    }

    private void assertCode(String code, ProviderFileUploadException exception) {
        assertEquals(code, exception.getCode());
    }

    private MockMultipartFile file(String name, String contentType, String content) {
        return file(name, contentType, content.getBytes());
    }

    private MockMultipartFile file(String name, String contentType, byte[] content) {
        return new MockMultipartFile("file", name, contentType, content);
    }
}

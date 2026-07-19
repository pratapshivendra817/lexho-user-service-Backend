package com.lexho.provider.service.storage;

import com.lexho.provider.exception.ProviderOperationException;
import com.lexho.provider.enums.ProviderDocumentType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.core.io.Resource;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;

class HybridDocumentStorageServiceTest {

    @TempDir
    Path tempDir;

    @Test
    void loadResolvesRelativePartnerDocumentPathFromUploadRoot() throws Exception {
        Path uploadRoot = tempDir.resolve("uploads");
        Path document = uploadRoot.resolve("partner-documents/partner-4/test.png");
        Files.createDirectories(document.getParent());
        Files.writeString(document, "test");

        HybridDocumentStorageService service = new HybridDocumentStorageService("local", uploadRoot.toString());

        Resource resource = assertDoesNotThrow(() -> service.load("partner-documents/partner-4/test.png"));
        assertDoesNotThrow(() -> {
            try (InputStream inputStream = resource.getInputStream()) {
                inputStream.readAllBytes();
            }
        });
    }

    @Test
    void loadThrowsWhenRelativePathDoesNotExist() {
        Path uploadRoot = tempDir.resolve("uploads");
        HybridDocumentStorageService service = new HybridDocumentStorageService("local", uploadRoot.toString());

        assertThrows(ProviderOperationException.class,
                () -> service.load("partner-documents/partner-4/missing.png"));
    }

    @Test
    void storePersistsValidatedFileUnderProviderDirectory() throws Exception {
        Path uploadRoot = tempDir.resolve("uploads");
        HybridDocumentStorageService service = new HybridDocumentStorageService("LOCAL", uploadRoot.toString());
        MockMultipartFile file = new MockMultipartFile(
                "file", "identity.pdf", "application/pdf", "safe pdf".getBytes());

        StoredDocument stored = service.store(4L, ProviderDocumentType.PAN, file);

        assertEquals("identity.pdf", stored.getFileName());
        assertEquals("application/pdf", stored.getContentType());
        assertTrue(stored.getPath().startsWith("partner-documents/partner-4/pan-"));
        assertTrue(Files.exists(uploadRoot.resolve(stored.getPath())));
    }

    @Test
    void storeRejectsUnwiredAndUnknownProviders() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "identity.pdf", "application/pdf", "safe pdf".getBytes());

        assertThrows(ProviderOperationException.class,
                () -> new HybridDocumentStorageService("s3", tempDir.toString())
                        .store(1L, ProviderDocumentType.PAN, file));
        assertThrows(ProviderOperationException.class,
                () -> new HybridDocumentStorageService("cloudinary", tempDir.toString())
                        .store(1L, ProviderDocumentType.PAN, file));
        assertThrows(ProviderOperationException.class,
                () -> new HybridDocumentStorageService("unknown", tempDir.toString())
                        .store(1L, ProviderDocumentType.PAN, file));
    }

    @Test
    void loadRejectsEmptyPathAndAcceptsHttpUrl() {
        HybridDocumentStorageService service = new HybridDocumentStorageService("local", tempDir.toString());

        assertThrows(ProviderOperationException.class, () -> service.load(null));
        assertThrows(ProviderOperationException.class, () -> service.load("   "));
        Resource resource = assertDoesNotThrow(() -> service.load("https://example.com/document.pdf"));
        assertEquals("document.pdf", resource.getFilename());
    }
}

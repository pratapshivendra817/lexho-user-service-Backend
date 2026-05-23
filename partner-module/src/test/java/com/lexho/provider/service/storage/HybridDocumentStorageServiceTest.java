package com.lexho.partner.service.storage;

import com.lexho.partner.exception.ProviderOperationException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.core.io.Resource;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
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
}

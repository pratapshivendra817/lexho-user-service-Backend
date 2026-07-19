package com.lexho.user_service.lexhopartner.util;

import com.lexho.user_service.exception.BadRequestException;
import com.lexho.user_service.upload.UploadSecurityService;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class FileStorageUtilTest {

    private final UploadSecurityService uploadSecurityService = mock(UploadSecurityService.class);
    private final FileStorageUtil fileStorageUtil = new FileStorageUtil(uploadSecurityService);

    @Test
    void shouldStoreValidatedImageUnderUploadsFolder() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "avatar.png", "image/png", "png".getBytes());
        when(uploadSecurityService.validateImage(file)).thenReturn("png");

        String stored = fileStorageUtil.storeImage(file, "partner-test");

        assertThat(stored).startsWith("/uploads/partner-test/").endsWith(".png");
        Path target = Path.of("." + stored).normalize().toAbsolutePath();
        assertThat(Files.exists(target)).isTrue();
        Files.deleteIfExists(target);
        Files.deleteIfExists(target.getParent());
    }

    @Test
    void shouldRejectMissingImage() {
        assertThrows(BadRequestException.class, () -> fileStorageUtil.storeImage(null, "partner"));
        assertThrows(BadRequestException.class, () -> fileStorageUtil.storeImage(
                new MockMultipartFile("file", new byte[0]), "partner"));
    }

    @Test
    void shouldWrapStorageIoFailures() {
        MockMultipartFile broken = new MockMultipartFile("file", "avatar.png", "image/png", "png".getBytes()) {
            @Override
            public java.io.InputStream getInputStream() throws java.io.IOException {
                throw new java.io.IOException("boom");
            }
        };
        when(uploadSecurityService.validateImage(broken)).thenReturn("png");

        assertThrows(BadRequestException.class, () -> fileStorageUtil.storeImage(broken, "partner"));
    }
}

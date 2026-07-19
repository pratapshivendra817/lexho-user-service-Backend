package com.lexho.user_service.admin.service;

import com.lexho.user_service.exception.BadRequestException;
import com.lexho.user_service.exception.InfrastructureException;
import com.lexho.user_service.upload.UploadSecurityService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AdminFileUploadServiceTest {

    @Test
    void uploadServiceImageStoresFileUnderServicesFolder(@TempDir Path tempDir) throws Exception {
        UploadSecurityService uploadSecurityService = mock(UploadSecurityService.class);
        AdminFileUploadService service = new AdminFileUploadService(tempDir.toString(), uploadSecurityService);
        MockMultipartFile file = new MockMultipartFile("file", "image.png", "image/png", "hello".getBytes());
        when(uploadSecurityService.validateImage(file)).thenReturn("png");

        String path = service.uploadServiceImage(file);

        assertThat(path).startsWith("/images/services/");
        assertThat(Files.list(tempDir.resolve("services"))).hasSize(1);
    }

    @Test
    void uploadBannerImageStoresFileUnderBannersFolder(@TempDir Path tempDir) throws Exception {
        UploadSecurityService uploadSecurityService = mock(UploadSecurityService.class);
        AdminFileUploadService service = new AdminFileUploadService(tempDir.toString(), uploadSecurityService);
        MockMultipartFile file = new MockMultipartFile("file", "banner.jpg", "image/jpeg", "hello".getBytes());
        when(uploadSecurityService.validateImage(file)).thenReturn("jpg");

        String path = service.uploadBannerImage(file);

        assertThat(path).startsWith("/images/banners/");
        assertThat(Files.list(tempDir.resolve("banners"))).hasSize(1);
    }

    @Test
    void uploadMethodsRejectMissingFiles(@TempDir Path tempDir) {
        UploadSecurityService uploadSecurityService = mock(UploadSecurityService.class);
        AdminFileUploadService service = new AdminFileUploadService(tempDir.toString(), uploadSecurityService);

        assertThatThrownBy(() -> service.uploadServiceImage(null))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("File is required");
        assertThatThrownBy(() -> service.uploadBannerImage(new MockMultipartFile("file", new byte[0])))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("File is required");
    }

    @Test
    void uploadMethodsWrapIoFailures(@TempDir Path tempDir) throws Exception {
        UploadSecurityService uploadSecurityService = mock(UploadSecurityService.class);
        AdminFileUploadService service = new AdminFileUploadService(tempDir.toString(), uploadSecurityService);
        MultipartFile file = mock(MultipartFile.class);
        when(file.isEmpty()).thenReturn(false);
        when(uploadSecurityService.validateImage(file)).thenReturn("png");
        when(file.getInputStream()).thenThrow(new IOException("broken"));

        assertThatThrownBy(() -> service.uploadServiceImage(file))
                .isInstanceOf(InfrastructureException.class)
                .hasMessageContaining("Unable to store uploaded file");
        assertThatThrownBy(() -> service.uploadBannerImage(file))
                .isInstanceOf(InfrastructureException.class)
                .hasMessageContaining("Unable to store uploaded file");
    }
}

package com.lexho.user_service.customer.profile;

import com.lexho.user_service.customer.profile.dto.CustomerProfileResponse;
import com.lexho.user_service.customer.profile.entity.CustomerEntity;
import com.lexho.user_service.customer.profile.entity.CustomerGender;
import com.lexho.user_service.customer.profile.mapper.CustomerProfileMapper;
import com.lexho.user_service.customer.profile.storage.LocalImageStorageService;
import com.lexho.user_service.exception.BadRequestException;
import com.lexho.user_service.lexho.entity.LexhoAddress;
import com.lexho.user_service.upload.UploadSecurityService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CustomerProfileSupportTest {

    @TempDir
    Path tempDir;

    @Test
    void customerProfileMapperSplitsNamesBuildsAddressesAndComputesCompletion() {
        CustomerEntity entity = new CustomerEntity();
        entity.setId(7L);
        entity.setFullName("John Doe");
        entity.setPhoneNumber("9999");
        entity.setEmail("john@example.com");
        entity.setProfileImageUrl("/img.png");
        entity.setGender(CustomerGender.MALE);
        entity.setCreatedAt(Instant.now().minusSeconds(60));
        entity.setUpdatedAt(Instant.now());

        LexhoAddress address = new LexhoAddress();
        address.setHouseNo("221B");
        address.setArea("Baker Street");
        address.setCity("London");
        address.setLatitude(BigDecimal.valueOf(12.3));
        address.setLongitude(BigDecimal.valueOf(45.6));
        address.setDefault(true);

        CustomerProfileResponse response = new CustomerProfileMapper().toResponse(entity, List.of(address));

        assertThat(response.id).isEqualTo(7L);
        assertThat(response.mobileNumber).isEqualTo("9999");
        assertThat(response.gender).isEqualTo("MALE");
        assertThat(response.firstName).isEqualTo("John");
        assertThat(response.lastName).isEqualTo("Doe");
        assertThat(response.profileCompletionPercentage).isEqualTo(100);
        assertThat(response.addresses).hasSize(1);
        assertThat(response.addresses.get(0).addressLine).isEqualTo("221B, Baker Street, London");
        assertThat(response.addresses.get(0).isDefault).isTrue();
    }

    @Test
    void customerProfileMapperHandlesBlankNamesEmptyAddressesAndNullGender() {
        CustomerEntity entity = new CustomerEntity();
        entity.setFullName("Prince");
        entity.setPhoneNumber(" ");

        CustomerProfileResponse response = new CustomerProfileMapper().toResponse(entity, List.of());

        assertThat(response.firstName).isEqualTo("Prince");
        assertThat(response.lastName).isEmpty();
        assertThat(response.gender).isNull();
        assertThat(response.addresses).isEmpty();
        assertThat(response.profileCompletionPercentage).isEqualTo(20);
    }

    @Test
    void customerEntityLifecycleSetsDefaultsAndCopiesMobile() throws Exception {
        CustomerEntity entity = new CustomerEntity();
        entity.setPhoneNumber("8888");

        invokeLifecycle(entity, "prePersist");
        Instant createdAt = entity.getCreatedAt();

        assertThat(entity.getActive()).isTrue();
        assertThat(entity.getProfileEditLocked()).isFalse();
        assertThat(entity.getEmailVerified()).isFalse();
        assertThat(entity.getPhoneVerified()).isFalse();
        assertThat(entity.getProfileImageApproved()).isTrue();
        assertThat(entity.getMobile()).isEqualTo("8888");
        assertThat(createdAt).isNotNull();
        assertThat(entity.getUpdatedAt()).isEqualTo(createdAt);

        invokeLifecycle(entity, "preUpdate");
        assertThat(entity.getUpdatedAt()).isAfterOrEqualTo(createdAt);
    }

    @Test
    void localImageStorageServiceUploadsAndDeletesFilesAndRejectsInvalidInput() throws Exception {
        UploadSecurityService uploadSecurityService = mock(UploadSecurityService.class);
        when(uploadSecurityService.validateImage(org.mockito.ArgumentMatchers.any())).thenReturn("png");
        LocalImageStorageService storageService = new LocalImageStorageService(tempDir.toString(), uploadSecurityService);

        MockMultipartFile multipartFile = new MockMultipartFile(
                "file", "avatar.png", "image/png", "image-data".getBytes(StandardCharsets.UTF_8));

        String storedPath = storageService.upload(multipartFile);

        assertThat(storedPath).startsWith("/uploads/profiles/").endsWith(".png");
        Path storedFile = tempDir.resolve("profiles").resolve(storedPath.substring(storedPath.lastIndexOf('/') + 1));
        assertThat(Files.exists(storedFile)).isTrue();

        storageService.delete(storedPath);
        assertThat(Files.exists(storedFile)).isFalse();

        assertThrows(BadRequestException.class, () -> storageService.upload(null));
        assertDoesNotThrow(() -> storageService.delete(null));
        assertDoesNotThrow(() -> storageService.delete("invalid"));
    }

    @Test
    void localImageStorageServiceWrapsIoFailures() {
        UploadSecurityService uploadSecurityService = mock(UploadSecurityService.class);
        when(uploadSecurityService.validateImage(org.mockito.ArgumentMatchers.any())).thenReturn("png");
        LocalImageStorageService storageService = new LocalImageStorageService(tempDir.toString(), uploadSecurityService);

        MultipartFile broken = new MultipartFile() {
            @Override public String getName() { return "file"; }
            @Override public String getOriginalFilename() { return "avatar.png"; }
            @Override public String getContentType() { return "image/png"; }
            @Override public boolean isEmpty() { return false; }
            @Override public long getSize() { return 10; }
            @Override public byte[] getBytes() { return new byte[0]; }
            @Override public InputStream getInputStream() throws IOException { throw new IOException("broken"); }
            @Override public void transferTo(java.io.File dest) { }
        };

        assertThrows(BadRequestException.class, () -> storageService.upload(broken));
    }

    private void invokeLifecycle(Object target, String methodName) throws Exception {
        var method = target.getClass().getDeclaredMethod(methodName);
        method.setAccessible(true);
        method.invoke(target);
    }
}

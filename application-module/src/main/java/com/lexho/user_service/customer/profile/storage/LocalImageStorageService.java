package com.lexho.user_service.customer.profile.storage;

import com.lexho.user_service.exception.BadRequestException;
import com.lexho.user_service.upload.UploadSecurityService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

@Service
public class LocalImageStorageService implements ImageStorageService {
    private static final Logger log = LoggerFactory.getLogger(LocalImageStorageService.class);
    private final Path profileUploadDir;
    private final UploadSecurityService uploadSecurityService;

    public LocalImageStorageService(@Value("${app.upload.base-dir:uploads}") String uploadBaseDir,
                                    UploadSecurityService uploadSecurityService) {
        this.profileUploadDir = Paths.get(uploadBaseDir, "profiles").toAbsolutePath().normalize();
        this.uploadSecurityService = uploadSecurityService;
    }

    @Override
    public String upload(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("Image file is required");
        }
        String extension = uploadSecurityService.validateImage(file);
        String fileName = UUID.randomUUID().toString().replace("-", "") + "." + extension;
        try {
            Files.createDirectories(profileUploadDir);
            Path target = profileUploadDir.resolve(fileName).normalize();
            if (!target.startsWith(profileUploadDir)) {
                throw new BadRequestException("Invalid file name");
            }
            try (InputStream inputStream = file.getInputStream()) {
                Files.copy(inputStream, target, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException exception) {
            log.error("profile_image_upload_failed fileName={} error={}", fileName, exception.getMessage());
            throw new BadRequestException("Unable to store uploaded file");
        }
        return "/uploads/profiles/" + fileName;
    }

    @Override
    public void delete(String storedPathOrUrl) {
        if (storedPathOrUrl == null || storedPathOrUrl.isBlank()) {
            return;
        }
        String normalized = storedPathOrUrl.trim();
        int idx = normalized.lastIndexOf('/');
        if (idx < 0 || idx == normalized.length() - 1) {
            return;
        }
        String fileName = normalized.substring(idx + 1);
        Path target = profileUploadDir.resolve(fileName).normalize();
        if (!target.startsWith(profileUploadDir)) {
            return;
        }
        try {
            Files.deleteIfExists(target);
        } catch (IOException exception) {
            log.warn("profile_image_delete_failed file={} error={}", target, exception.getMessage());
        }
    }

}

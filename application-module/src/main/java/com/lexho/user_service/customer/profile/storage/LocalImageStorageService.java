package com.lexho.user_service.customer.profile.storage;

import com.lexho.user_service.exception.BadRequestException;
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
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
public class LocalImageStorageService implements ImageStorageService {
    private static final Logger log = LoggerFactory.getLogger(LocalImageStorageService.class);
    private static final Set<String> ALLOWED_EXTENSIONS = Set.of("jpg", "jpeg", "png", "webp");
    private static final Map<String, String> CONTENT_TYPE_TO_EXTENSION = Map.of(
            "image/jpeg", "jpg",
            "image/jpg", "jpg",
            "image/png", "png",
            "image/webp", "webp"
    );
    private static final long MAX_IMAGE_SIZE_BYTES = 5L * 1024L * 1024L;

    private final Path profileUploadDir;

    public LocalImageStorageService(@Value("${app.upload.base-dir:uploads}") String uploadBaseDir) {
        this.profileUploadDir = Paths.get(uploadBaseDir, "profiles").toAbsolutePath().normalize();
    }

    @Override
    public String upload(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("Image file is required");
        }
        if (file.getSize() > MAX_IMAGE_SIZE_BYTES) {
            throw new BadRequestException("Image size must be <= 5MB");
        }
        String extension = resolveExtension(file);
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

    private String resolveExtension(MultipartFile file) {
        String originalName = file.getOriginalFilename();
        if (originalName != null) {
            int lastDot = originalName.lastIndexOf('.');
            if (lastDot > -1 && lastDot < originalName.length() - 1) {
                String candidate = originalName.substring(lastDot + 1).toLowerCase(Locale.ROOT);
                if (ALLOWED_EXTENSIONS.contains(candidate)) {
                    return candidate;
                }
            }
        }
        String contentType = file.getContentType();
        if (contentType != null) {
            String mapped = CONTENT_TYPE_TO_EXTENSION.get(contentType.toLowerCase(Locale.ROOT));
            if (mapped != null) {
                return mapped;
            }
        }
        throw new BadRequestException("Only JPG, JPEG, PNG, WEBP images are allowed");
    }
}

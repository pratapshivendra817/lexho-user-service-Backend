package com.lexho.user_service.admin.service;

import com.lexho.user_service.exception.BadRequestException;
import com.lexho.user_service.exception.InfrastructureException;
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
public class AdminFileUploadService {

    private static final Set<String> ALLOWED_EXTENSIONS = Set.of("jpg", "jpeg", "png", "webp", "gif");
    private static final Map<String, String> CONTENT_TYPE_TO_EXTENSION = Map.of(
            "image/jpeg", "jpg",
            "image/png", "png",
            "image/webp", "webp",
            "image/gif", "gif"
    );

    private final Path servicesUploadDir;

    public AdminFileUploadService(@Value("${app.upload.base-dir:uploads}") String uploadBaseDir) {
        this.servicesUploadDir = Paths.get(uploadBaseDir, "services").toAbsolutePath().normalize();
    }

    public String uploadServiceImage(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("File is required");
        }

        String extension = resolveExtension(file);
        String fileName = UUID.randomUUID().toString().replace("-", "") + "." + extension;

        try {
            Files.createDirectories(servicesUploadDir);
            Path target = servicesUploadDir.resolve(fileName).normalize();
            if (!target.startsWith(servicesUploadDir)) {
                throw new BadRequestException("Invalid file name");
            }
            try (InputStream inputStream = file.getInputStream()) {
                Files.copy(inputStream, target, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException ex) {
            throw new InfrastructureException("Unable to store uploaded file");
        }

        return "/uploads/services/" + fileName;
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

        throw new BadRequestException("Only image files are allowed");
    }
}

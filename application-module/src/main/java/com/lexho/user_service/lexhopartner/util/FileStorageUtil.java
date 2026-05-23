package com.lexho.user_service.lexhopartner.util;

import com.lexho.user_service.exception.BadRequestException;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Set;
import java.util.UUID;

@Component
public class FileStorageUtil {

    private static final long MAX_FILE_SIZE = 5 * 1024 * 1024;
    private static final Set<String> ALLOWED_TYPES = Set.of("image/jpeg", "image/png", "image/webp");

    public String storeImage(MultipartFile file, String folder) {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("Image file is required");
        }
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new BadRequestException("Image size must be <= 5MB");
        }
        if (!ALLOWED_TYPES.contains(file.getContentType())) {
            throw new BadRequestException("Only JPG, PNG, WEBP images are allowed");
        }
        try {
            Path base = Paths.get("uploads").resolve(folder);
            Files.createDirectories(base);
            String ext = StringUtils.getFilenameExtension(file.getOriginalFilename());
            String filename = UUID.randomUUID() + (ext == null ? "" : "." + ext);
            Path target = base.resolve(filename);
            Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);
            return "/uploads/" + folder + "/" + filename;
        } catch (IOException ex) {
            throw new BadRequestException("Unable to store image");
        }
    }
}

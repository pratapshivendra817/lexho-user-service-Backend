package com.lexho.user_service.lexhopartner.util;

import com.lexho.user_service.exception.BadRequestException;
import com.lexho.user_service.upload.UploadSecurityService;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

@Component
public class FileStorageUtil {

    private final UploadSecurityService uploadSecurityService;

    public FileStorageUtil(UploadSecurityService uploadSecurityService) {
        this.uploadSecurityService = uploadSecurityService;
    }

    public String storeImage(MultipartFile file, String folder) {
        return store(file, folder, uploadSecurityService.validateImage(file));
    }

    public String storeDocument(MultipartFile file, String folder) {
        return store(file, folder, uploadSecurityService.validateDocument(file));
    }

    private String store(MultipartFile file, String folder, String ext) {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("File is required");
        }
        try {
            Path base = Paths.get("uploads").resolve(folder).toAbsolutePath().normalize();
            Files.createDirectories(base);
            String filename = UUID.randomUUID() + "." + ext;
            Path target = base.resolve(filename).normalize();
            if (!target.startsWith(base)) {
                throw new BadRequestException("Invalid file name");
            }
            Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);
            return "/uploads/" + folder + "/" + filename;
        } catch (IOException ex) {
            throw new BadRequestException("Unable to store image");
        }
    }
}

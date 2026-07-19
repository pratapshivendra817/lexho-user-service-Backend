package com.lexho.user_service.admin.service;

import com.lexho.user_service.exception.BadRequestException;
import com.lexho.user_service.exception.InfrastructureException;
import com.lexho.user_service.upload.UploadSecurityService;
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
public class AdminFileUploadService {

    private final Path servicesUploadDir;
    private final Path bannersUploadDir;
    private final UploadSecurityService uploadSecurityService;

    public AdminFileUploadService(@Value("${app.upload.base-dir:uploads}") String uploadBaseDir,
                                  UploadSecurityService uploadSecurityService) {
        this.servicesUploadDir = Paths.get(uploadBaseDir, "services").toAbsolutePath().normalize();
        this.bannersUploadDir = Paths.get(uploadBaseDir, "banners").toAbsolutePath().normalize();
        this.uploadSecurityService = uploadSecurityService;
    }

    public String uploadServiceImage(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("File is required");
        }

        String extension = uploadSecurityService.validateImage(file);
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

        return "/images/services/" + fileName;
    }

    public String uploadBannerImage(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("File is required");
        }

        String extension = uploadSecurityService.validateImage(file);
        String fileName = UUID.randomUUID().toString().replace("-", "") + "." + extension;

        try {
            Files.createDirectories(bannersUploadDir);
            Path target = bannersUploadDir.resolve(fileName).normalize();
            if (!target.startsWith(bannersUploadDir)) {
                throw new BadRequestException("Invalid file name");
            }
            try (InputStream inputStream = file.getInputStream()) {
                Files.copy(inputStream, target, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException ex) {
            throw new InfrastructureException("Unable to store uploaded file");
        }

        return "/images/banners/" + fileName;
    }

}

package com.lexho.partner.service.storage;

import com.lexho.partner.enums.ProviderDocumentType;
import com.lexho.partner.exception.ProviderOperationException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Locale;
import java.util.UUID;

@Service
public class HybridDocumentStorageService implements DocumentStorageService {

    private final String storageProvider;
    private final Path uploadRootPath;
    private final Path localBasePath;

    public HybridDocumentStorageService(@Value("${app.partner.documents.storage-partner:local}") String storageProvider,
                                        @Value("${app.upload.base-dir:uploads}") String uploadBaseDir) {
        this.storageProvider = storageProvider.toLowerCase(Locale.ROOT);
        this.uploadRootPath = Paths.get(uploadBaseDir).toAbsolutePath().normalize();
        this.localBasePath = Paths.get(uploadBaseDir, "partner-documents").toAbsolutePath().normalize();
    }

    @Override
    public StoredDocument store(Long providerId, ProviderDocumentType documentType, MultipartFile file) {
        return switch (storageProvider) {
            case "local" -> storeLocally(providerId, documentType, file);
            case "s3" -> throw new ProviderOperationException("S3 storage partner is configured but not wired yet");
            case "cloudinary" -> throw new ProviderOperationException("Cloudinary storage partner is configured but not wired yet");
            default -> throw new ProviderOperationException("Unsupported storage partner: " + storageProvider);
        };
    }

    @Override
    public Resource load(String storedPath) {
        try {
            if (storedPath == null || storedPath.isBlank()) {
                throw new ProviderOperationException("Document path is empty");
            }
            if (storedPath.startsWith("http://") || storedPath.startsWith("https://")) {
                return new UrlResource(storedPath);
            }
            Path candidate = Paths.get(storedPath).normalize();
            Path resolved = candidate.isAbsolute() ? candidate : uploadRootPath.resolve(candidate).normalize();
            if (!Files.exists(resolved)) {
                throw new ProviderOperationException("Document file does not exist");
            }
            return new UrlResource(resolved.toUri());
        } catch (MalformedURLException ex) {
            throw new ProviderOperationException("Unable to load document");
        }
    }

    private StoredDocument storeLocally(Long providerId, ProviderDocumentType documentType, MultipartFile file) {
        try {
            String originalName = StringUtils.hasText(file.getOriginalFilename()) ? file.getOriginalFilename() : "document";
            String extension = "";
            int dotIndex = originalName.lastIndexOf('.');
            if (dotIndex > -1) {
                extension = originalName.substring(dotIndex);
            }

            Path providerDir = localBasePath.resolve("partner-" + providerId);
            Files.createDirectories(providerDir);

            String storedName = documentType.name().toLowerCase(Locale.ROOT) + "-" + UUID.randomUUID() + extension;
            Path destination = providerDir.resolve(storedName).normalize();

            Files.copy(file.getInputStream(), destination, StandardCopyOption.REPLACE_EXISTING);

            String relativePath = Paths.get("partner-documents", "partner-" + providerId, storedName)
                    .toString()
                    .replace("\\", "/");

            String contentType = file.getContentType() == null ? "application/octet-stream" : file.getContentType();
            return new StoredDocument(relativePath, originalName, contentType);
        } catch (IOException ex) {
            throw new ProviderOperationException("Failed to store document");
        }
    }
}

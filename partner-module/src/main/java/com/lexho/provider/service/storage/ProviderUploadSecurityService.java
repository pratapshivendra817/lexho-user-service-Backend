package com.lexho.provider.service.storage;

import com.lexho.provider.exception.ProviderFileUploadException;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Service
public class ProviderUploadSecurityService {

    private static final long MAX_FILE_SIZE_BYTES = 5L * 1024L * 1024L;
    private static final Set<String> ALLOWED_EXTENSIONS = Set.of("jpg", "jpeg", "png", "pdf");
    private static final Map<String, Set<String>> EXTENSION_MIME_TYPES = Map.of(
            "jpg", Set.of("image/jpeg", "image/jpg"),
            "jpeg", Set.of("image/jpeg", "image/jpg"),
            "png", Set.of("image/png"),
            "pdf", Set.of("application/pdf")
    );

    public String validate(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ProviderFileUploadException("INVALID_FILE_TYPE", "Document file is required");
        }
        if (file.getSize() > MAX_FILE_SIZE_BYTES) {
            throw new ProviderFileUploadException("FILE_TOO_LARGE", "File size must be 5 MB or less");
        }
        String extension = extension(file.getOriginalFilename());
        String rawContentType = file.getContentType();
        String contentType = rawContentType == null ? "" : rawContentType.toLowerCase(Locale.ROOT);
        if (!EXTENSION_MIME_TYPES.getOrDefault(extension, Set.of()).contains(contentType)) {
            throw new ProviderFileUploadException("INVALID_FILE_TYPE", "File MIME type does not match extension");
        }
        scan(file);
        return extension;
    }

    public String sanitizedOriginalName(MultipartFile file) {
        String original = file == null ? null : file.getOriginalFilename();
        if (original == null || original.isBlank()) {
            return "document";
        }
        String sanitized = original.replace("\\", "/");
        int slash = sanitized.lastIndexOf('/');
        if (slash >= 0) {
            sanitized = sanitized.substring(slash + 1);
        }
        sanitized = sanitized.replaceAll("[^A-Za-z0-9._-]", "_");
        return sanitized.isBlank() ? "document" : sanitized;
    }

    private String extension(String originalName) {
        String sanitized = originalName == null ? "" : originalName.replace("\\", "/");
        int slash = sanitized.lastIndexOf('/');
        if (slash >= 0) {
            sanitized = sanitized.substring(slash + 1);
        }
        int dot = sanitized.lastIndexOf('.');
        if (dot < 0 || dot == sanitized.length() - 1) {
            throw new ProviderFileUploadException("INVALID_FILE_TYPE", "File extension is required");
        }
        String extension = sanitized.substring(dot + 1).toLowerCase(Locale.ROOT);
        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            throw new ProviderFileUploadException("INVALID_FILE_TYPE", "Only jpg, jpeg, png, and pdf files are allowed");
        }
        return extension;
    }

    private void scan(MultipartFile file) {
        try (InputStream inputStream = file.getInputStream()) {
            byte[] prefix = inputStream.readNBytes(4096);
            String text = new String(prefix, java.nio.charset.StandardCharsets.US_ASCII);
            if ((prefix.length >= 2 && prefix[0] == 'M' && prefix[1] == 'Z')
                    || (prefix.length >= 4 && prefix[0] == 0x7F && prefix[1] == 'E' && prefix[2] == 'L' && prefix[3] == 'F')
                    || text.contains("EICAR-STANDARD-ANTIVIRUS-TEST-FILE")) {
                throw new ProviderFileUploadException("FILE_SCAN_FAILED", "File could not be accepted");
            }
        } catch (IOException exception) {
            throw new ProviderFileUploadException("FILE_SCAN_FAILED", "File could not be accepted");
        }
    }
}

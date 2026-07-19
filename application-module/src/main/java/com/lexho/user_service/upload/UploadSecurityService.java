package com.lexho.user_service.upload;

import com.lexho.user_service.exception.FileUploadException;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Service
public class UploadSecurityService {

    private static final long MAX_FILE_SIZE_BYTES = 5L * 1024L * 1024L;
    private static final Set<String> IMAGE_EXTENSIONS = Set.of("jpg", "jpeg", "png");
    private static final Set<String> DOCUMENT_EXTENSIONS = Set.of("jpg", "jpeg", "png", "pdf");
    private static final Map<String, Set<String>> EXTENSION_MIME_TYPES = Map.of(
            "jpg", Set.of("image/jpeg", "image/jpg"),
            "jpeg", Set.of("image/jpeg", "image/jpg"),
            "png", Set.of("image/png"),
            "pdf", Set.of("application/pdf")
    );

    public String validateImage(MultipartFile file) {
        return validate(file, IMAGE_EXTENSIONS);
    }

    public String validateDocument(MultipartFile file) {
        return validate(file, DOCUMENT_EXTENSIONS);
    }

    private String validate(MultipartFile file, Set<String> allowedExtensions) {
        if (file == null || file.isEmpty()) {
            throw new FileUploadException("INVALID_FILE_TYPE", "File is required");
        }
        if (file.getSize() > MAX_FILE_SIZE_BYTES) {
            throw new FileUploadException("FILE_TOO_LARGE", "File size must be 5 MB or less");
        }
        String originalName = file.getOriginalFilename();
        if (originalName == null || originalName.isBlank()) {
            throw new FileUploadException("INVALID_FILE_TYPE", "File extension is required");
        }
        String extension = extension(originalName);
        if (!allowedExtensions.contains(extension)) {
            throw new FileUploadException("INVALID_FILE_TYPE", "Only jpg, jpeg, png, and pdf files are allowed");
        }
        String rawContentType = file.getContentType();
        String contentType = rawContentType == null ? "" : rawContentType.toLowerCase(Locale.ROOT);
        if (!EXTENSION_MIME_TYPES.getOrDefault(extension, Set.of()).contains(contentType)) {
            throw new FileUploadException("INVALID_FILE_TYPE", "File MIME type does not match extension");
        }
        scan(file);
        return extension;
    }

    public String sanitizedOriginalName(MultipartFile file) {
        String original = file == null ? null : file.getOriginalFilename();
        if (original == null || original.isBlank()) {
            return "file";
        }
        String sanitized = original.replace("\\", "/");
        int slash = sanitized.lastIndexOf('/');
        if (slash >= 0) {
            sanitized = sanitized.substring(slash + 1);
        }
        sanitized = sanitized.replaceAll("[^A-Za-z0-9._-]", "_");
        return sanitized.isBlank() ? "file" : sanitized;
    }

    private String extension(String originalName) {
        String sanitized = sanitizedOriginalName(new MultipartFileNameOnly(originalName));
        int dot = sanitized.lastIndexOf('.');
        if (dot < 0 || dot == sanitized.length() - 1) {
            throw new FileUploadException("INVALID_FILE_TYPE", "File extension is required");
        }
        String extension = sanitized.substring(dot + 1).toLowerCase(Locale.ROOT);
        if (extension.equals("exe") || extension.equals("dll") || extension.equals("sh") || extension.equals("bat")
                || extension.equals("cmd") || extension.equals("js") || extension.equals("jar")) {
            throw new FileUploadException("INVALID_FILE_TYPE", "Executable files are not allowed");
        }
        return extension;
    }

    private void scan(MultipartFile file) {
        try (InputStream inputStream = file.getInputStream()) {
            byte[] prefix = inputStream.readNBytes(4096);
            if (hasExecutableMagic(prefix) || containsEicar(prefix)) {
                throw new FileUploadException("FILE_SCAN_FAILED", "File could not be accepted");
            }
        } catch (IOException exception) {
            throw new FileUploadException("FILE_SCAN_FAILED", "File could not be accepted");
        }
    }

    private boolean hasExecutableMagic(byte[] bytes) {
        if (bytes.length >= 2 && bytes[0] == 'M' && bytes[1] == 'Z') {
            return true;
        }
        if (bytes.length >= 4 && bytes[0] == 0x7F && bytes[1] == 'E' && bytes[2] == 'L' && bytes[3] == 'F') {
            return true;
        }
        String text = new String(bytes, java.nio.charset.StandardCharsets.UTF_8).toLowerCase(Locale.ROOT);
        return text.startsWith("#!/") || text.contains("<script");
    }

    private boolean containsEicar(byte[] bytes) {
        String text = new String(bytes, java.nio.charset.StandardCharsets.US_ASCII);
        return text.contains("EICAR-STANDARD-ANTIVIRUS-TEST-FILE");
    }

    private record MultipartFileNameOnly(String name) implements MultipartFile {
        @Override public String getName() { return "file"; }
        @Override public String getOriginalFilename() { return name; }
        @Override public String getContentType() { return null; }
        @Override public boolean isEmpty() { return true; }
        @Override public long getSize() { return 0; }
        @Override public byte[] getBytes() { return new byte[0]; }
        @Override public InputStream getInputStream() { return InputStream.nullInputStream(); }
        @Override public void transferTo(java.io.File dest) { }
    }
}

package com.lexho.user_service.customer.profile.storage;

import org.springframework.web.multipart.MultipartFile;

public interface ImageStorageService {
    String upload(MultipartFile file);
    void delete(String storedPathOrUrl);
}

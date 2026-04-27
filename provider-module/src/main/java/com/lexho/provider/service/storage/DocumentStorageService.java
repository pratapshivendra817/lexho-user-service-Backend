package com.lexho.provider.service.storage;

import com.lexho.provider.enums.ProviderDocumentType;
import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

public interface DocumentStorageService {

    StoredDocument store(Long providerId, ProviderDocumentType documentType, MultipartFile file);

    Resource load(String storedPath);
}


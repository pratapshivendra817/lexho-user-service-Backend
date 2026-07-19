package com.lexho.provider.dto;

import com.lexho.provider.enums.ProviderDocumentType;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ProviderDocumentUploadResponse {
    private Long documentId;
    private Long providerId;
    private ProviderDocumentType documentType;
    private String fileUrl;
    private String fileName;
}


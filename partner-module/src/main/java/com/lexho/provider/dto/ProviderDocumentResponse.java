package com.lexho.provider.dto;

import com.lexho.provider.enums.ProviderDocumentType;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class ProviderDocumentResponse {
    private Long id;
    private ProviderDocumentType documentType;
    private String fileImage;
    private String fileName;
    private String fileType;
    private String contentType;
    private String previewUrl;
    private String downloadUrl;
    private LocalDateTime uploadedAt;
}

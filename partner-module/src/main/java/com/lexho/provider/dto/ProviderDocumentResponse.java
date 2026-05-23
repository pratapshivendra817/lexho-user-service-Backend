package com.lexho.partner.dto;

import com.lexho.partner.enums.ProviderDocumentType;
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

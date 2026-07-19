package com.lexho.user_service.addon.dto;

import java.time.LocalDateTime;

public class AddonMappingResponse {
    private Long mappingId;
    private Long addonId;
    private Long targetId;
    private String targetType;
    private LocalDateTime createdAt;

    public AddonMappingResponse(Long mappingId, Long addonId, Long targetId, String targetType, LocalDateTime createdAt) {
        this.mappingId = mappingId;
        this.addonId = addonId;
        this.targetId = targetId;
        this.targetType = targetType;
        this.createdAt = createdAt;
    }

    public Long getMappingId() { return mappingId; }
    public Long getAddonId() { return addonId; }
    public Long getTargetId() { return targetId; }
    public String getTargetType() { return targetType; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}

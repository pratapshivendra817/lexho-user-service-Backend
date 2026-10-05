package com.lexho.user_service.socialmedia.dto;

import com.lexho.user_service.socialmedia.enums.SocialMediaPlatform;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class SocialMediaLinkResponse {
    private Long id;
    private SocialMediaPlatform platform;
    private String url;
    private String label;
    private String icon;
    private Integer displayOrder;
    private boolean active;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Long createdBy;
    private Long updatedBy;
}

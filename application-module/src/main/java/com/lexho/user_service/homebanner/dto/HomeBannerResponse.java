package com.lexho.user_service.homebanner.dto;

import com.lexho.user_service.homebanner.entity.HomeBannerRedirectType;
import com.lexho.user_service.homebanner.entity.HomeBannerAudience;
import lombok.Builder;
import lombok.Value;

import java.time.LocalDateTime;

@Value
@Builder
public class HomeBannerResponse {
    Long id;
    String title;
    String subtitle;
    String imageUrl;
    HomeBannerRedirectType redirectType;
    Long redirectId;
    Integer priority;
    boolean active;
    String bannerType;
    HomeBannerAudience audience;
    LocalDateTime createdAt;
    LocalDateTime updatedAt;
}

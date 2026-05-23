package com.lexho.user_service.customer.banner.dto;

import com.lexho.user_service.customer.banner.entity.BannerRedirectType;
import lombok.Builder;
import lombok.Value;

import java.time.LocalDateTime;

@Value
@Builder
public class AdminCustomerBannerResponse {
    Long id;
    Long categoryId;
    String title;
    String imageUrl;
    BannerRedirectType redirectType;
    Long redirectId;
    String redirectUrl;
    boolean active;
    Integer priority;
    LocalDateTime createdAt;
    LocalDateTime updatedAt;
}

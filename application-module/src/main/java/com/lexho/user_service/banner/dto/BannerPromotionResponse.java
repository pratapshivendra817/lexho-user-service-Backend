package com.lexho.user_service.banner.dto;

import com.lexho.user_service.banner.enums.BannerPlatformType;
import com.lexho.user_service.banner.enums.BannerPlacement;
import com.lexho.user_service.banner.enums.BannerPromoType;
import com.lexho.user_service.banner.enums.BannerRedirectType;
import com.lexho.user_service.banner.enums.BannerStatus;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
public class BannerPromotionResponse {
    private Long id;
    private String title;
    private String subtitle;
    private String description;
    private String bannerImageUrl;
    private String imageUrl;
    private BannerPromoType promoType;
    private BannerStatus status;
    private Integer slotNumber;
    private Integer priority;
    private Double discountPercentage;
    private String couponCode;
    private Long couponId;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private String city;
    private Boolean newUserOnly;
    private Boolean premiumUserOnly;
    private BigDecimal minOrderValue;
    private BannerPlatformType platformType;
    private BannerPlacement placement;
    private BannerRedirectType redirectType;
    private String redirectValue;
    private Long clickCount;
    private Long impressionCount;
    private Long redemptionCount;
    private BigDecimal revenueGenerated;
    private Integer maxViewsPerUserPerDay;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

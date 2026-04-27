package com.lexho.user_service.banner.dto;

import com.lexho.user_service.banner.enums.BannerPlatformType;
import com.lexho.user_service.banner.enums.BannerPromoType;
import com.lexho.user_service.banner.enums.BannerRedirectType;
import com.lexho.user_service.banner.enums.BannerStatus;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
public class BannerUpsertRequest {

    @NotBlank(message = "Title is required")
    @Size(max = 255, message = "Title must be at most 255 characters")
    private String title;

    @Size(max = 255, message = "Subtitle must be at most 255 characters")
    private String subtitle;

    @Size(max = 1000, message = "Description must be at most 1000 characters")
    private String description;

    @NotBlank(message = "Banner image URL is required")
    @Size(max = 1000, message = "Banner image URL must be at most 1000 characters")
    private String bannerImageUrl;

    @NotNull(message = "Promo type is required")
    private BannerPromoType promoType;

    @NotNull(message = "Status is required")
    private BannerStatus status;

    @NotNull(message = "Slot number is required")
    private Integer slotNumber;

    @NotNull(message = "Priority is required")
    private Integer priority;

    @DecimalMin(value = "0.0", inclusive = true, message = "Discount percentage cannot be negative")
    @Max(value = 100, message = "Discount percentage cannot exceed 100")
    private Double discountPercentage;

    @Size(max = 80, message = "Coupon code must be at most 80 characters")
    private String couponCode;

    private Long couponId;

    @NotNull(message = "Start date is required")
    private LocalDateTime startDate;

    @NotNull(message = "End date is required")
    private LocalDateTime endDate;

    @NotNull(message = "Redirect type is required")
    private BannerRedirectType redirectType;

    @Size(max = 1000, message = "Redirect value must be at most 1000 characters")
    private String redirectValue;

    @Size(max = 100, message = "City must be at most 100 characters")
    private String city;

    private Long serviceCategoryId;

    private Boolean newUserOnly;

    private Boolean premiumUserOnly;

    @DecimalMin(value = "0.0", inclusive = true, message = "Minimum order value cannot be negative")
    private BigDecimal minOrderValue;

    @NotNull(message = "Platform type is required")
    private BannerPlatformType platformType;

    private Integer maxViewsPerUserPerDay;
}


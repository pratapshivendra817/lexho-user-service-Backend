package com.lexho.user_service.customer.banner.dto;

import com.lexho.user_service.customer.banner.entity.BannerRedirectType;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class AdminCustomerBannerUpsertRequest {
    private Long categoryId;

    @NotBlank(message = "title is required")
    @Size(max = 255, message = "title must be at most 255 characters")
    private String title;

    @Size(max = 1000, message = "imageUrl must be at most 1000 characters")
    private String imageUrl;

    @Size(max = 1000, message = "bannerImageUrl must be at most 1000 characters")
    private String bannerImageUrl;

    @NotNull(message = "redirectType is required")
    private BannerRedirectType redirectType;

    private Long redirectId;

    private String redirectValue;

    @Size(max = 1000, message = "redirectUrl must be at most 1000 characters")
    private String redirectUrl;

    @NotNull(message = "active is required")
    private Boolean active;

    @NotNull(message = "priority is required")
    @Min(value = 0, message = "priority must be non-negative")
    private Integer priority;
}

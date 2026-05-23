package com.lexho.user_service.homebanner.dto;

import com.lexho.user_service.homebanner.entity.HomeBannerRedirectType;
import com.lexho.user_service.homebanner.entity.HomeBannerAudience;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class HomeBannerUpsertRequest {

    @NotBlank(message = "title is required")
    @Size(max = 255, message = "title must be at most 255 characters")
    private String title;

    @Size(max = 500, message = "subtitle must be at most 500 characters")
    private String subtitle;

    @NotBlank(message = "image_url is required")
    @Size(max = 1000, message = "image_url must be at most 1000 characters")
    private String imageUrl;

    @NotNull(message = "redirect_type is required")
    private HomeBannerRedirectType redirectType;

    private Long redirectId;

    @NotNull(message = "priority is required")
    @Min(value = 0, message = "priority must be non-negative")
    private Integer priority;

    @NotNull(message = "is_active is required")
    private Boolean active;

    @Size(max = 50, message = "banner_type must be at most 50 characters")
    private String bannerType;

    @NotNull(message = "audience is required")
    private HomeBannerAudience audience;
}

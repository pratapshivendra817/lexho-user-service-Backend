package com.lexho.user_service.socialmedia.dto;

import com.lexho.user_service.socialmedia.enums.SocialMediaPlatform;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SocialMediaLinkRequest {
    @NotNull(message = "platform is required")
    private SocialMediaPlatform platform;

    @NotBlank(message = "url is required")
    @Size(max = 2048, message = "url must be at most 2048 characters")
    @Pattern(regexp = "^https?://.+", message = "url must be a valid HTTP/HTTPS URL")
    private String url;

    @NotBlank(message = "label is required")
    @Size(max = 100, message = "label must be at most 100 characters")
    private String label;

    @Size(max = 100, message = "icon must be at most 100 characters")
    private String icon;

    @NotNull(message = "displayOrder is required")
    @Min(value = 1, message = "displayOrder must be greater than 0")
    private Integer displayOrder;

    private Boolean isActive = Boolean.TRUE;
}

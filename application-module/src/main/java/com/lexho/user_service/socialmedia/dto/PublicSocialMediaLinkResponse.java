package com.lexho.user_service.socialmedia.dto;

import com.lexho.user_service.socialmedia.enums.SocialMediaPlatform;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PublicSocialMediaLinkResponse {
    private SocialMediaPlatform platform;
    private String url;
    private String label;
    private String icon;
    private Integer displayOrder;
}

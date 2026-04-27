package com.lexho.user_service.banner.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class BannerEventRequest {
    private Long userId;
    private String deviceId;
    private String city;
}


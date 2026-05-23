package com.lexho.user_service.customer.banner.dto;

import com.lexho.user_service.customer.banner.entity.BannerRedirectType;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class CustomerBannerResponse {
    Long id;
    String imageUrl;
    BannerRedirectType redirectType;
    Long redirectId;
    String redirectUrl;
}

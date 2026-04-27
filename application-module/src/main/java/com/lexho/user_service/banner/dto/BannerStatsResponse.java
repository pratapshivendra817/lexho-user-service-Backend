package com.lexho.user_service.banner.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@AllArgsConstructor
public class BannerStatsResponse {
    private long activeBanners;
    private double avgCtr;
    private long totalImpressions;
    private long totalClicks;
    private long promoRedemptions;
    private BigDecimal revenueViaPromotions;
}


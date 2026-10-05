package com.lexho.user_service.banner.mapper;

import com.lexho.user_service.banner.dto.BannerPromotionResponse;
import com.lexho.user_service.banner.dto.BannerUpsertRequest;
import com.lexho.user_service.banner.entity.BannerPromotion;
import org.springframework.stereotype.Component;

@Component
public class BannerPromotionMapper {

    public void apply(BannerPromotion banner, BannerUpsertRequest request) {
        banner.setTitle(request.getTitle());
        banner.setSubtitle(request.getSubtitle());
        banner.setDescription(request.getDescription());
        if (request.getBannerImageUrl() != null && !request.getBannerImageUrl().trim().isBlank()) {
            banner.setBannerImageUrl(request.getBannerImageUrl().trim());
        }
        banner.setPromoType(request.getPromoType());
        banner.setStatus(request.getStatus());
        banner.setSlotNumber(request.getSlotNumber());
        banner.setPriority(request.getPriority());
        banner.setDiscountPercentage(request.getDiscountPercentage());
        banner.setCouponCode(request.getCouponCode());
        banner.setCouponId(request.getCouponId());
        banner.setStartDate(request.getStartDate());
        banner.setEndDate(request.getEndDate());
        banner.setCity(request.getCity());
        banner.setNewUserOnly(request.getNewUserOnly());
        banner.setPremiumUserOnly(request.getPremiumUserOnly());
        banner.setMinOrderValue(request.getMinOrderValue());
        banner.setPlatformType(request.getPlatformType());
        banner.setPlacement(request.getPlacement());
        banner.setRedirectType(request.getRedirectType());
        banner.setRedirectValue(request.getRedirectValue());
        banner.setMaxViewsPerUserPerDay(request.getMaxViewsPerUserPerDay());
    }

    public BannerPromotionResponse toResponse(BannerPromotion banner) {
        BannerPromotionResponse response = new BannerPromotionResponse();
        response.setId(banner.getId());
        response.setTitle(banner.getTitle());
        response.setSubtitle(banner.getSubtitle());
        response.setDescription(banner.getDescription());
        response.setBannerImageUrl(banner.getBannerImageUrl());
        response.setImageUrl(banner.getBannerImageUrl());
        response.setPromoType(banner.getPromoType());
        response.setStatus(banner.getStatus());
        response.setSlotNumber(banner.getSlotNumber());
        response.setPriority(banner.getPriority());
        response.setDiscountPercentage(banner.getDiscountPercentage());
        response.setCouponCode(banner.getCouponCode());
        response.setCouponId(banner.getCouponId());
        response.setStartDate(banner.getStartDate());
        response.setEndDate(banner.getEndDate());
        response.setCity(banner.getCity());
        response.setNewUserOnly(banner.getNewUserOnly());
        response.setPremiumUserOnly(banner.getPremiumUserOnly());
        response.setMinOrderValue(banner.getMinOrderValue());
        response.setPlatformType(banner.getPlatformType());
        response.setPlacement(banner.getPlacement());
        response.setRedirectType(banner.getRedirectType());
        response.setRedirectValue(banner.getRedirectValue());
        response.setClickCount(banner.getClickCount());
        response.setImpressionCount(banner.getImpressionCount());
        response.setRedemptionCount(banner.getRedemptionCount());
        response.setRevenueGenerated(banner.getRevenueGenerated());
        response.setMaxViewsPerUserPerDay(banner.getMaxViewsPerUserPerDay());
        response.setCreatedAt(banner.getCreatedAt());
        response.setUpdatedAt(banner.getUpdatedAt());
        return response;
    }
}

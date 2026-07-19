package com.lexho.user_service.banner.mapper;

import com.lexho.user_service.banner.dto.BannerPromotionResponse;
import com.lexho.user_service.banner.dto.BannerUpsertRequest;
import com.lexho.user_service.banner.entity.BannerPromotion;
import com.lexho.user_service.banner.enums.BannerPlatformType;
import com.lexho.user_service.banner.enums.BannerPlacement;
import com.lexho.user_service.banner.enums.BannerStatus;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class BannerPromotionMapperTest {

    private final BannerPromotionMapper mapper = new BannerPromotionMapper();

    @Test
    void applyTransfersRequestAndTrimsNonBlankImage() {
        BannerPromotion banner = new BannerPromotion();
        BannerUpsertRequest request = new BannerUpsertRequest();
        request.setTitle("Summer");
        request.setSubtitle("Offer");
        request.setDescription("Description");
        request.setBannerImageUrl("  /summer.png  ");
        request.setStatus(BannerStatus.ACTIVE);
        request.setSlotNumber(2);
        request.setPriority(9);
        request.setDiscountPercentage(15.0);
        request.setCouponCode("SAVE15");
        request.setCouponId(4L);
        request.setStartDate(LocalDateTime.now());
        request.setEndDate(LocalDateTime.now().plusDays(1));
        request.setCity("Delhi");
        request.setNewUserOnly(true);
        request.setPremiumUserOnly(false);
        request.setMinOrderValue(new BigDecimal("500"));
        request.setPlatformType(BannerPlatformType.ANDROID);
        request.setPlacement(BannerPlacement.BOTTOM);
        request.setMaxViewsPerUserPerDay(3);

        mapper.apply(banner, request);

        assertThat(banner.getTitle()).isEqualTo("Summer");
        assertThat(banner.getBannerImageUrl()).isEqualTo("/summer.png");
        assertThat(banner.getPriority()).isEqualTo(9);
        assertThat(banner.getMinOrderValue()).isEqualByComparingTo("500");
        assertThat(banner.getPlacement()).isEqualTo(BannerPlacement.BOTTOM);
    }

    @Test
    void applyPreservesExistingImageWhenRequestImageIsBlank() {
        BannerPromotion banner = new BannerPromotion();
        banner.setBannerImageUrl("/existing.png");
        BannerUpsertRequest request = new BannerUpsertRequest();
        request.setBannerImageUrl(" ");

        mapper.apply(banner, request);

        assertThat(banner.getBannerImageUrl()).isEqualTo("/existing.png");
    }

    @Test
    void toResponseTransfersPublicAndAnalyticsFields() {
        BannerPromotion banner = new BannerPromotion();
        banner.setId(8L);
        banner.setTitle("Summer");
        banner.setBannerImageUrl("/summer.png");
        banner.setStatus(BannerStatus.ACTIVE);
        banner.setClickCount(10L);
        banner.setImpressionCount(20L);
        banner.setRedemptionCount(3L);
        banner.setRevenueGenerated(new BigDecimal("99.50"));
        banner.setCreatedAt(LocalDateTime.now().minusDays(1));
        banner.setUpdatedAt(LocalDateTime.now());

        BannerPromotionResponse response = mapper.toResponse(banner);

        assertThat(response.getId()).isEqualTo(8L);
        assertThat(response.getTitle()).isEqualTo("Summer");
        assertThat(response.getClickCount()).isEqualTo(10L);
        assertThat(response.getImpressionCount()).isEqualTo(20L);
        assertThat(response.getRedemptionCount()).isEqualTo(3L);
        assertThat(response.getRevenueGenerated()).isEqualByComparingTo("99.50");
    }
}

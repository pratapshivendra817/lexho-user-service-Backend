package com.lexho.user_service.banner.service;

import com.lexho.user_service.banner.dto.BannerEventRequest;
import com.lexho.user_service.banner.dto.BannerPromotionResponse;
import com.lexho.user_service.banner.dto.BannerRedeemRequest;
import com.lexho.user_service.banner.entity.BannerPromotion;
import com.lexho.user_service.banner.enums.BannerPlatformType;
import com.lexho.user_service.banner.enums.BannerPlacement;
import com.lexho.user_service.banner.enums.BannerStatus;
import com.lexho.user_service.banner.exception.BannerNotFoundException;
import com.lexho.user_service.banner.exception.BannerValidationException;
import com.lexho.user_service.banner.mapper.BannerPromotionMapper;
import com.lexho.user_service.banner.repository.BannerClickLogRepository;
import com.lexho.user_service.banner.repository.BannerImpressionLogRepository;
import com.lexho.user_service.banner.repository.BannerPromotionRepository;
import com.lexho.user_service.banner.repository.PromoRedemptionLogRepository;
import com.lexho.user_service.util.MediaUrlResolver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CustomerBannerServiceTest {

    @Mock private BannerPromotionRepository bannerRepository;
    @Mock private BannerImpressionLogRepository impressionRepository;
    @Mock private BannerClickLogRepository clickRepository;
    @Mock private PromoRedemptionLogRepository redemptionRepository;
    @Mock private BannerPromotionMapper mapper;
    @Mock private MediaUrlResolver mediaUrlResolver;

    private CustomerBannerService service;

    @BeforeEach
    void setUp() {
        service = new CustomerBannerService(bannerRepository, impressionRepository, clickRepository,
                redemptionRepository, mapper, mediaUrlResolver);
    }

    @Test
    void homepageBannersFiltersLimitsSortsAndResolvesMediaUrls() {
        BannerPromotion bottom = banner(2L, BannerPlacement.BOTTOM, 10, 1);
        BannerPromotion top = banner(1L, BannerPlacement.TOP, 5, 2);
        BannerPromotion limited = banner(3L, null, 20, 1);
        limited.setMaxViewsPerUserPerDay(1);
        BannerPromotion otherCity = banner(4L, BannerPlacement.TOP, 99, 1);
        otherCity.setCity("Mumbai");

        when(bannerRepository.findAll(any(Specification.class)))
                .thenReturn(List.of(bottom, limited, otherCity, top));
        when(impressionRepository.countViewsForDay(any(), any(), any(), any())).thenReturn(1L);
        when(mapper.toResponse(any())).thenAnswer(invocation -> {
            BannerPromotion source = invocation.getArgument(0);
            BannerPromotionResponse response = new BannerPromotionResponse();
            response.setId(source.getId());
            response.setBannerImageUrl("/images/" + source.getId());
            return response;
        });
        when(mediaUrlResolver.toPublicUrl(any())).thenAnswer(invocation -> "https://cdn" + invocation.getArgument(0));

        List<BannerPromotionResponse> result = service.homepageBanners(
                "Delhi", BannerPlatformType.ANDROID, false, false, BigDecimal.ZERO, 9L, "device");

        assertThat(result).extracting(BannerPromotionResponse::getId).containsExactly(1L, 2L);
        assertThat(result).extracting(BannerPromotionResponse::getBannerImageUrl)
                .containsExactly("https://cdn/images/1", "https://cdn/images/2");
    }

    @Test
    void homepageBannersAcceptsDefaultsAndAnonymousUsersWithoutApplyingViewLimit() {
        BannerPromotion banner = banner(1L, null, 1, 1);
        banner.setMaxViewsPerUserPerDay(2);
        BannerPromotionResponse response = new BannerPromotionResponse();
        response.setBannerImageUrl("image.png");
        when(bannerRepository.findAll(any(Specification.class))).thenReturn(List.of(banner));
        when(mapper.toResponse(banner)).thenReturn(response);
        when(mediaUrlResolver.toPublicUrl("image.png")).thenReturn("public/image.png");

        List<BannerPromotionResponse> result = service.homepageBanners(
                " ", null, null, null, null, null, " ");

        assertThat(result).singleElement()
                .extracting(BannerPromotionResponse::getBannerImageUrl)
                .isEqualTo("public/image.png");
    }

    @Test
    void clickImpressionAndRedemptionMutateCountersAndPersistLogs() {
        BannerPromotion banner = banner(7L, BannerPlacement.TOP, 1, 1);
        when(bannerRepository.findByIdAndDeletedFalse(7L)).thenReturn(Optional.of(banner));
        BannerEventRequest event = new BannerEventRequest();
        event.setUserId(11L);
        event.setDeviceId("d1");
        event.setCity("Delhi");
        BannerRedeemRequest redeem = new BannerRedeemRequest();
        redeem.setUserId(11L);
        redeem.setBookingId(21L);
        redeem.setDiscountApplied(new BigDecimal("25.50"));

        assertThat(service.trackClick(7L, event).getMessage()).isEqualTo("Click tracked");
        assertThat(service.trackImpression(7L, event).getMessage()).isEqualTo("Impression tracked");
        assertThat(service.redeem(7L, redeem).getMessage()).isEqualTo("Promo redeemed");

        assertThat(banner.getClickCount()).isEqualTo(1);
        assertThat(banner.getImpressionCount()).isEqualTo(1);
        assertThat(banner.getRedemptionCount()).isEqualTo(1);
        assertThat(banner.getRevenueGenerated()).isEqualByComparingTo("25.50");
        verify(clickRepository).save(any());
        verify(impressionRepository).save(any());
        verify(redemptionRepository).save(any());
    }

    @Test
    void nullEventPayloadsUseNullIdentifiersAndZeroDiscount() {
        BannerPromotion banner = banner(7L, BannerPlacement.TOP, 1, 1);
        when(bannerRepository.findByIdAndDeletedFalse(7L)).thenReturn(Optional.of(banner));

        service.trackClick(7L, null);
        service.trackImpression(7L, null);
        service.redeem(7L, null);

        assertThat(banner.getRevenueGenerated()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void eventOperationsRejectMissingAndInactiveBanners() {
        when(bannerRepository.findByIdAndDeletedFalse(1L)).thenReturn(Optional.empty());
        BannerPromotion inactive = banner(2L, BannerPlacement.TOP, 1, 1);
        inactive.setStatus(BannerStatus.DRAFT);
        when(bannerRepository.findByIdAndDeletedFalse(2L)).thenReturn(Optional.of(inactive));

        assertThatThrownBy(() -> service.trackClick(1L, null)).isInstanceOf(BannerNotFoundException.class);
        assertThatThrownBy(() -> service.trackImpression(2L, null)).isInstanceOf(BannerValidationException.class);
    }

    private BannerPromotion banner(Long id, BannerPlacement placement, int priority, int slot) {
        BannerPromotion banner = new BannerPromotion();
        banner.setId(id);
        banner.setStatus(BannerStatus.ACTIVE);
        banner.setPlatformType(BannerPlatformType.ALL);
        banner.setPlacement(placement);
        banner.setPriority(priority);
        banner.setSlotNumber(slot);
        banner.setClickCount(0L);
        banner.setImpressionCount(0L);
        banner.setRedemptionCount(0L);
        banner.setRevenueGenerated(BigDecimal.ZERO);
        banner.setNewUserOnly(false);
        banner.setPremiumUserOnly(false);
        return banner;
    }
}

package com.lexho.user_service.banner.service;

import com.lexho.user_service.banner.dto.BannerUpsertRequest;
import com.lexho.user_service.banner.dto.BannerPageResponse;
import com.lexho.user_service.banner.dto.BannerPromotionResponse;
import com.lexho.user_service.banner.dto.BannerStatsResponse;
import com.lexho.user_service.banner.entity.BannerPromotion;
import com.lexho.user_service.banner.enums.BannerPlacement;
import com.lexho.user_service.banner.enums.BannerPlatformType;
import com.lexho.user_service.banner.enums.BannerPromoType;
import com.lexho.user_service.banner.enums.BannerRedirectType;
import com.lexho.user_service.banner.enums.BannerStatus;
import com.lexho.user_service.banner.exception.BannerNotFoundException;
import com.lexho.user_service.banner.exception.BannerValidationException;
import com.lexho.user_service.banner.mapper.BannerPromotionMapper;
import com.lexho.user_service.banner.repository.BannerPromotionRepository;
import com.lexho.user_service.security.SecurityUtils;
import com.lexho.user_service.util.MediaUrlResolver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class AdminBannerPromotionServiceTest {

    @Mock
    private BannerPromotionRepository bannerPromotionRepository;

    @Mock
    private BannerPromotionMapper bannerPromotionMapper;

    @Mock
    private SecurityUtils securityUtils;
    @Mock
    private MediaUrlResolver mediaUrlResolver;

    @InjectMocks
    private AdminBannerPromotionService adminBannerPromotionService;

    @BeforeEach
    void setUp() {
        lenient().when(securityUtils.getCurrentUserId()).thenReturn(Optional.of(77L));
        lenient().when(mediaUrlResolver.toStorablePath(any())).thenAnswer(invocation -> invocation.getArgument(0));
        lenient().when(mediaUrlResolver.toPublicUrl(any())).thenAnswer(invocation -> invocation.getArgument(0));
        lenient().when(bannerPromotionMapper.toResponse(any(BannerPromotion.class))).thenAnswer(invocation -> {
            BannerPromotion banner = invocation.getArgument(0);
            BannerPromotionResponse response = new BannerPromotionResponse();
            response.setId(banner.getId());
            response.setTitle(banner.getTitle());
            response.setStatus(banner.getStatus());
            response.setPlacement(banner.getPlacement());
            response.setSlotNumber(banner.getSlotNumber());
            response.setBannerImageUrl(banner.getBannerImageUrl());
            response.setImageUrl(banner.getBannerImageUrl());
            response.setCouponCode(banner.getCouponCode());
            return response;
        });
    }

    @Test
    void createFailsWhenEndDateBeforeStartDate() {
        BannerUpsertRequest request = request();
        request.setEndDate(request.getStartDate().minusMinutes(1));

        assertThrows(BannerValidationException.class, () -> adminBannerPromotionService.create(request));
    }

    @Test
    void createFailsWhenActiveSlotAlreadyExists() {
        BannerUpsertRequest request = request();
        when(bannerPromotionRepository.existsByStatusAndPlacementAndSlotNumberAndDeletedFalse(
                BannerStatus.ACTIVE, BannerPlacement.TOP, 1)).thenReturn(true);

        assertThrows(BannerValidationException.class, () -> adminBannerPromotionService.create(request));
    }

    @Test
    void activateFailsWhenAnotherActiveBannerUsesSameSlot() {
        BannerPromotion banner = new BannerPromotion();
        banner.setId(10L);
        banner.setStatus(BannerStatus.DRAFT);
        banner.setPlacement(BannerPlacement.TOP);
        banner.setSlotNumber(2);

        when(bannerPromotionRepository.findByIdAndDeletedFalse(10L)).thenReturn(Optional.of(banner));
        when(bannerPromotionRepository.existsByStatusAndPlacementAndSlotNumberAndDeletedFalseAndIdNot(
                BannerStatus.ACTIVE, BannerPlacement.TOP, 2, 10L))
                .thenReturn(true);

        assertThrows(BannerValidationException.class, () -> adminBannerPromotionService.activate(10L));
    }

    @Test
    void createPersistsBannerWithNormalizedDefaultsAndUserMetadata() {
        BannerUpsertRequest request = request();
        request.setPlatformType(null);
        request.setNewUserOnly(null);
        request.setPremiumUserOnly(null);
        request.setBannerImageUrl("https://cdn.lexho.com/banners/promo.png");

        when(mediaUrlResolver.toStorablePath("https://cdn.lexho.com/banners/promo.png")).thenReturn("/banners/promo.png");
        when(bannerPromotionRepository.save(any(BannerPromotion.class))).thenAnswer(invocation -> {
            BannerPromotion saved = invocation.getArgument(0);
            saved.setId(15L);
            return saved;
        });

        BannerPromotionResponse response = adminBannerPromotionService.create(request);

        ArgumentCaptor<BannerPromotion> captor = ArgumentCaptor.forClass(BannerPromotion.class);
        verify(bannerPromotionRepository).save(captor.capture());
        BannerPromotion saved = captor.getValue();
        assertThat(saved.getBannerImageUrl()).isEqualTo("/banners/promo.png");
        assertThat(saved.getCreatedBy()).isEqualTo(77L);
        assertThat(saved.getUpdatedBy()).isEqualTo(77L);
        assertThat(saved.isDeleted()).isFalse();
        assertThat(request.getPlatformType()).isEqualTo(BannerPlatformType.ALL);
        assertThat(request.getNewUserOnly()).isFalse();
        assertThat(request.getPremiumUserOnly()).isFalse();
        assertThat(response.getId()).isEqualTo(15L);
        assertThat(response.getBannerImageUrl()).isEqualTo("/banners/promo.png");
        assertThat(response.getImageUrl()).isEqualTo("/banners/promo.png");
    }

    @Test
    void createAcceptsImageUrlAliasForAdminPreviewCompatibility() {
        BannerUpsertRequest request = request();
        request.setBannerImageUrl(null);
        request.setImageUrl("https://cdn.lexho.com/banners/promo.png");

        when(mediaUrlResolver.toStorablePath("https://cdn.lexho.com/banners/promo.png")).thenReturn("/banners/promo.png");
        when(bannerPromotionRepository.save(any(BannerPromotion.class))).thenAnswer(invocation -> {
            BannerPromotion saved = invocation.getArgument(0);
            saved.setId(16L);
            return saved;
        });

        BannerPromotionResponse response = adminBannerPromotionService.create(request);

        ArgumentCaptor<BannerPromotion> captor = ArgumentCaptor.forClass(BannerPromotion.class);
        verify(bannerPromotionRepository).save(captor.capture());
        assertThat(captor.getValue().getBannerImageUrl()).isEqualTo("/banners/promo.png");
        assertThat(response.getBannerImageUrl()).isEqualTo("/banners/promo.png");
        assertThat(response.getImageUrl()).isEqualTo("/banners/promo.png");
    }

    @Test
    void createRejectsMissingTitleImageDatesAndDuplicateCouponCode() {
        BannerUpsertRequest blankTitle = request();
        blankTitle.setTitle(" ");
        assertThrows(BannerValidationException.class, () -> adminBannerPromotionService.create(blankTitle));

        BannerUpsertRequest blankImage = request();
        blankImage.setBannerImageUrl(" ");
        assertThrows(BannerValidationException.class, () -> adminBannerPromotionService.create(blankImage));

        BannerUpsertRequest missingStartDate = request();
        missingStartDate.setStartDate(null);
        assertThrows(BannerValidationException.class, () -> adminBannerPromotionService.create(missingStartDate));

        BannerUpsertRequest duplicateCoupon = request();
        when(bannerPromotionRepository.existsByCouponCodeIgnoreCaseAndDeletedFalse("SAVE10")).thenReturn(true);
        assertThrows(BannerValidationException.class, () -> adminBannerPromotionService.create(duplicateCoupon));
    }

    @Test
    void updateShouldReuseExistingImageAndRejectDuplicateCoupon() {
        BannerPromotion existing = banner(21L, BannerStatus.DRAFT);
        existing.setBannerImageUrl("/existing.png");
        when(bannerPromotionRepository.findByIdAndDeletedFalse(21L)).thenReturn(Optional.of(existing));

        BannerUpsertRequest request = request();
        request.setBannerImageUrl(" ");
        when(bannerPromotionRepository.save(existing)).thenReturn(existing);

        BannerPromotionResponse response = adminBannerPromotionService.update(21L, request);

        assertThat(response.getId()).isEqualTo(21L);
        assertThat(existing.getBannerImageUrl()).isEqualTo("/existing.png");
        assertThat(existing.getUpdatedBy()).isEqualTo(77L);

        BannerUpsertRequest duplicateCouponRequest = request();
        when(bannerPromotionRepository.existsByCouponCodeIgnoreCaseAndDeletedFalseAndIdNot("SAVE10", 21L)).thenReturn(true);
        assertThrows(BannerValidationException.class, () -> adminBannerPromotionService.update(21L, duplicateCouponRequest));
    }

    @Test
    void getAllShouldNormalizePagingSortingAndMapResults() {
        BannerPromotion banner = banner(31L, BannerStatus.ACTIVE);
        when(bannerPromotionRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(banner)));

        BannerPageResponse<BannerPromotionResponse> response = adminBannerPromotionService.getAll(
                -1, 999, "bad", "asc", BannerStatus.ACTIVE, "Noida", "promo", "all", "cook");

        assertThat(response.getContent()).hasSize(1);
        assertThat(response.getPageNumber()).isZero();
        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(bannerPromotionRepository).findAll(any(Specification.class), pageableCaptor.capture());
        Pageable pageable = pageableCaptor.getValue();
        assertThat(pageable.getPageNumber()).isZero();
        assertThat(pageable.getPageSize()).isEqualTo(100);
        assertThat(pageable.getSort().getOrderFor("createdAt")).isNotNull();
        assertThat(pageable.getSort().getOrderFor("createdAt").isAscending()).isTrue();
    }

    @Test
    void activateDeactivateDraftAndDeleteShouldPersistExpectedState() {
        BannerPromotion draft = banner(41L, BannerStatus.DRAFT);
        when(bannerPromotionRepository.findByIdAndDeletedFalse(41L)).thenReturn(Optional.of(draft));
        when(bannerPromotionRepository.save(any(BannerPromotion.class))).thenAnswer(invocation -> invocation.getArgument(0));

        BannerPromotionResponse activeResponse = adminBannerPromotionService.activate(41L);
        assertThat(activeResponse.getStatus()).isEqualTo(BannerStatus.ACTIVE);

        BannerPromotionResponse inactiveResponse = adminBannerPromotionService.deactivate(41L);
        assertThat(inactiveResponse.getStatus()).isEqualTo(BannerStatus.INACTIVE);

        BannerPromotionResponse draftResponse = adminBannerPromotionService.moveToDraft(41L);
        assertThat(draftResponse.getStatus()).isEqualTo(BannerStatus.DRAFT);

        adminBannerPromotionService.softDelete(41L);
        assertThat(draft.isDeleted()).isTrue();
        assertThat(draft.getUpdatedBy()).isEqualTo(77L);
    }

    @Test
    void activateRejectsExpiredAndGetByIdOrDeleteRejectMissingBanner() {
        BannerPromotion expired = banner(51L, BannerStatus.EXPIRED);
        when(bannerPromotionRepository.findByIdAndDeletedFalse(51L)).thenReturn(Optional.of(expired));
        assertThrows(BannerValidationException.class, () -> adminBannerPromotionService.activate(51L));

        when(bannerPromotionRepository.findByIdAndDeletedFalse(99L)).thenReturn(Optional.empty());
        assertThrows(BannerNotFoundException.class, () -> adminBannerPromotionService.getById(99L));
        assertThrows(BannerNotFoundException.class, () -> adminBannerPromotionService.softDelete(99L));
    }

    @Test
    void dashboardStatsShouldIgnoreDeletedBannersAndRoundCtr() {
        BannerPromotion active = banner(61L, BannerStatus.ACTIVE);
        active.setImpressionCount(100L);
        active.setClickCount(12L);
        active.setRedemptionCount(3L);
        active.setRevenueGenerated(new BigDecimal("45.50"));

        BannerPromotion inactive = banner(62L, BannerStatus.INACTIVE);
        inactive.setImpressionCount(0L);
        inactive.setClickCount(0L);
        inactive.setRedemptionCount(1L);
        inactive.setRevenueGenerated(new BigDecimal("10.00"));

        BannerPromotion deleted = banner(63L, BannerStatus.ACTIVE);
        deleted.setDeleted(true);
        deleted.setImpressionCount(999L);
        deleted.setClickCount(999L);
        deleted.setRedemptionCount(999L);
        deleted.setRevenueGenerated(new BigDecimal("999.00"));

        when(bannerPromotionRepository.findAll()).thenReturn(List.of(active, inactive, deleted));

        BannerStatsResponse stats = adminBannerPromotionService.dashboardStats();

        assertThat(stats.getActiveBanners()).isEqualTo(1);
        assertThat(stats.getTotalImpressions()).isEqualTo(100L);
        assertThat(stats.getTotalClicks()).isEqualTo(12L);
        assertThat(stats.getPromoRedemptions()).isEqualTo(4L);
        assertThat(stats.getRevenueViaPromotions()).isEqualByComparingTo("55.50");
        assertThat(stats.getAvgCtr()).isEqualTo(12.0);
    }

    private BannerUpsertRequest request() {
        BannerUpsertRequest request = new BannerUpsertRequest();
        request.setTitle("Cook Offer");
        request.setBannerImageUrl("http://localhost:8082/uploads/services/banner.jpg");
        request.setPromoType(BannerPromoType.PROMO);
        request.setStatus(BannerStatus.ACTIVE);
        request.setPlacement(BannerPlacement.TOP);
        request.setRedirectType(BannerRedirectType.NONE);
        request.setSlotNumber(1);
        request.setPriority(10);
        request.setCouponCode("SAVE10");
        request.setStartDate(LocalDateTime.now().plusHours(1));
        request.setEndDate(LocalDateTime.now().plusDays(1));
        request.setPlatformType(BannerPlatformType.ALL);
        return request;
    }

    private BannerPromotion banner(Long id, BannerStatus status) {
        BannerPromotion banner = new BannerPromotion();
        banner.setId(id);
        banner.setTitle("Banner " + id);
        banner.setStatus(status);
        banner.setPlacement(BannerPlacement.TOP);
        banner.setSlotNumber(1);
        banner.setBannerImageUrl("/banner-" + id + ".png");
        banner.setPromoType(BannerPromoType.PROMO);
        banner.setRedirectType(BannerRedirectType.NONE);
        banner.setCouponCode("SAVE-" + id);
        banner.setNewUserOnly(false);
        banner.setPremiumUserOnly(false);
        banner.setRevenueGenerated(BigDecimal.ZERO);
        banner.setImpressionCount(0L);
        banner.setClickCount(0L);
        banner.setRedemptionCount(0L);
        return banner;
    }
}

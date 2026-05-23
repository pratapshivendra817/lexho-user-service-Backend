package com.lexho.user_service.banner.service;

import com.lexho.user_service.banner.dto.BannerUpsertRequest;
import com.lexho.user_service.banner.entity.BannerPromotion;
import com.lexho.user_service.banner.enums.BannerPlacement;
import com.lexho.user_service.banner.enums.BannerPlatformType;
import com.lexho.user_service.banner.enums.BannerPromoType;
import com.lexho.user_service.banner.enums.BannerStatus;
import com.lexho.user_service.banner.exception.BannerValidationException;
import com.lexho.user_service.banner.mapper.BannerPromotionMapper;
import com.lexho.user_service.banner.repository.BannerPromotionRepository;
import com.lexho.user_service.security.SecurityUtils;
import com.lexho.user_service.util.MediaUrlResolver;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.any;

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

    @Test
    void createFailsWhenEndDateBeforeStartDate() {
        when(mediaUrlResolver.toStorablePath(any())).thenAnswer(invocation -> invocation.getArgument(0));
        BannerUpsertRequest request = request();
        request.setEndDate(request.getStartDate().minusMinutes(1));

        assertThrows(BannerValidationException.class, () -> adminBannerPromotionService.create(request));
    }

    @Test
    void createFailsWhenActiveSlotAlreadyExists() {
        when(mediaUrlResolver.toStorablePath(any())).thenAnswer(invocation -> invocation.getArgument(0));
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

    private BannerUpsertRequest request() {
        BannerUpsertRequest request = new BannerUpsertRequest();
        request.setTitle("Cook Offer");
        request.setBannerImageUrl("http://localhost:8082/uploads/services/banner.jpg");
        request.setPromoType(BannerPromoType.PROMO);
        request.setStatus(BannerStatus.ACTIVE);
        request.setPlacement(BannerPlacement.TOP);
        request.setSlotNumber(1);
        request.setPriority(10);
        request.setStartDate(LocalDateTime.now().plusHours(1));
        request.setEndDate(LocalDateTime.now().plusDays(1));
        request.setPlatformType(BannerPlatformType.ALL);
        return request;
    }
}

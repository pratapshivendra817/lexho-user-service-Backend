package com.lexho.user_service.banner.service;

import com.lexho.user_service.banner.dto.BannerEventRequest;
import com.lexho.user_service.banner.dto.BannerEventResponse;
import com.lexho.user_service.banner.dto.BannerPromotionResponse;
import com.lexho.user_service.banner.dto.BannerRedeemRequest;
import com.lexho.user_service.banner.entity.BannerClickLog;
import com.lexho.user_service.banner.entity.BannerImpressionLog;
import com.lexho.user_service.banner.entity.BannerPromotion;
import com.lexho.user_service.banner.entity.PromoRedemptionLog;
import com.lexho.user_service.banner.enums.BannerPlatformType;
import com.lexho.user_service.banner.enums.BannerStatus;
import com.lexho.user_service.banner.exception.BannerNotFoundException;
import com.lexho.user_service.banner.exception.BannerValidationException;
import com.lexho.user_service.banner.mapper.BannerPromotionMapper;
import com.lexho.user_service.banner.repository.BannerClickLogRepository;
import com.lexho.user_service.banner.repository.BannerImpressionLogRepository;
import com.lexho.user_service.banner.repository.BannerPromotionRepository;
import com.lexho.user_service.banner.repository.PromoRedemptionLogRepository;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;

@Service
public class CustomerBannerService {

    private final BannerPromotionRepository bannerPromotionRepository;
    private final BannerImpressionLogRepository bannerImpressionLogRepository;
    private final BannerClickLogRepository bannerClickLogRepository;
    private final PromoRedemptionLogRepository promoRedemptionLogRepository;
    private final BannerPromotionMapper bannerPromotionMapper;

    public CustomerBannerService(BannerPromotionRepository bannerPromotionRepository,
                                 BannerImpressionLogRepository bannerImpressionLogRepository,
                                 BannerClickLogRepository bannerClickLogRepository,
                                 PromoRedemptionLogRepository promoRedemptionLogRepository,
                                 BannerPromotionMapper bannerPromotionMapper) {
        this.bannerPromotionRepository = bannerPromotionRepository;
        this.bannerImpressionLogRepository = bannerImpressionLogRepository;
        this.bannerClickLogRepository = bannerClickLogRepository;
        this.promoRedemptionLogRepository = promoRedemptionLogRepository;
        this.bannerPromotionMapper = bannerPromotionMapper;
    }

    @Transactional(readOnly = true)
    @Cacheable(value = "homepageBanners", key = "T(java.util.Objects).hash(#city,#serviceCategoryId,#platformType,#newUser,#premiumUser,#orderValue,#userId,#deviceId)")
    public List<BannerPromotionResponse> homepageBanners(String city,
                                                         Long serviceCategoryId,
                                                         BannerPlatformType platformType,
                                                         Boolean newUser,
                                                         Boolean premiumUser,
                                                         BigDecimal orderValue,
                                                         Long userId,
                                                         String deviceId) {
        LocalDateTime now = LocalDateTime.now();
        BannerPlatformType requestedPlatform = platformType == null ? BannerPlatformType.ALL : platformType;
        boolean isNewUser = newUser != null && newUser;
        boolean isPremiumUser = premiumUser != null && premiumUser;
        BigDecimal safeOrderValue = orderValue == null ? BigDecimal.ZERO : orderValue;

        Specification<BannerPromotion> spec = (root, query, cb) -> cb.and(
                cb.isFalse(root.get("deleted")),
                cb.equal(root.get("status"), BannerStatus.ACTIVE),
                cb.lessThanOrEqualTo(root.get("startDate"), now),
                cb.greaterThan(root.get("endDate"), now)
        );

        return bannerPromotionRepository.findAll(spec).stream()
                .filter(b -> city == null || city.isBlank() || b.getCity() == null || b.getCity().equalsIgnoreCase(city))
                .filter(b -> serviceCategoryId == null || b.getServiceCategoryId() == null || serviceCategoryId.equals(b.getServiceCategoryId()))
                .filter(b -> b.getPlatformType() == BannerPlatformType.ALL || b.getPlatformType() == requestedPlatform || requestedPlatform == BannerPlatformType.ALL)
                .filter(b -> !Boolean.TRUE.equals(b.getNewUserOnly()) || isNewUser)
                .filter(b -> !Boolean.TRUE.equals(b.getPremiumUserOnly()) || isPremiumUser)
                .filter(b -> b.getMinOrderValue() == null || safeOrderValue.compareTo(b.getMinOrderValue()) >= 0)
                .filter(b -> withinDailyViewLimit(b, userId, deviceId))
                .sorted(Comparator.comparing(BannerPromotion::getPriority).reversed().thenComparing(BannerPromotion::getSlotNumber))
                .map(bannerPromotionMapper::toResponse)
                .toList();
    }

    @Transactional
    public BannerEventResponse trackClick(Long id, BannerEventRequest request) {
        BannerPromotion banner = getActiveBanner(id);
        banner.setClickCount(banner.getClickCount() + 1);
        bannerPromotionRepository.save(banner);

        BannerClickLog log = new BannerClickLog();
        log.setBannerId(id);
        log.setUserId(request == null ? null : request.getUserId());
        log.setDeviceId(request == null ? null : request.getDeviceId());
        log.setCity(request == null ? null : request.getCity());
        bannerClickLogRepository.save(log);
        return new BannerEventResponse("Click tracked");
    }

    @Transactional
    public BannerEventResponse trackImpression(Long id, BannerEventRequest request) {
        BannerPromotion banner = getActiveBanner(id);
        banner.setImpressionCount(banner.getImpressionCount() + 1);
        bannerPromotionRepository.save(banner);

        BannerImpressionLog log = new BannerImpressionLog();
        log.setBannerId(id);
        log.setUserId(request == null ? null : request.getUserId());
        log.setDeviceId(request == null ? null : request.getDeviceId());
        log.setCity(request == null ? null : request.getCity());
        bannerImpressionLogRepository.save(log);
        return new BannerEventResponse("Impression tracked");
    }

    @Transactional
    public BannerEventResponse redeem(Long id, BannerRedeemRequest request) {
        BannerPromotion banner = getActiveBanner(id);
        banner.setRedemptionCount(banner.getRedemptionCount() + 1);
        BigDecimal discount = request == null || request.getDiscountApplied() == null ? BigDecimal.ZERO : request.getDiscountApplied();
        banner.setRevenueGenerated(banner.getRevenueGenerated().add(discount));
        bannerPromotionRepository.save(banner);

        PromoRedemptionLog log = new PromoRedemptionLog();
        log.setBannerId(id);
        log.setUserId(request == null ? null : request.getUserId());
        log.setBookingId(request == null ? null : request.getBookingId());
        log.setDiscountApplied(discount);
        promoRedemptionLogRepository.save(log);
        return new BannerEventResponse("Promo redeemed");
    }

    private boolean withinDailyViewLimit(BannerPromotion banner, Long userId, String deviceId) {
        if (banner.getMaxViewsPerUserPerDay() == null || banner.getMaxViewsPerUserPerDay() <= 0) {
            return true;
        }
        if (userId == null && (deviceId == null || deviceId.isBlank())) {
            return true;
        }
        LocalDateTime startOfDay = LocalDateTime.now().toLocalDate().atStartOfDay();
        long viewsToday = bannerImpressionLogRepository.countViewsForDay(banner.getId(), userId, deviceId, startOfDay);
        return viewsToday < banner.getMaxViewsPerUserPerDay();
    }

    private BannerPromotion getActiveBanner(Long id) {
        BannerPromotion banner = bannerPromotionRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new BannerNotFoundException(id));
        if (banner.getStatus() != BannerStatus.ACTIVE) {
            throw new BannerValidationException("Banner is not active");
        }
        return banner;
    }
}


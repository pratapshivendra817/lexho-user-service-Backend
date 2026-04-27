package com.lexho.user_service.banner.scheduler;

import com.lexho.user_service.banner.entity.BannerPromotion;
import com.lexho.user_service.banner.enums.BannerStatus;
import com.lexho.user_service.banner.repository.BannerPromotionRepository;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Component
public class BannerPromotionExpiryScheduler {

    private final BannerPromotionRepository bannerPromotionRepository;

    public BannerPromotionExpiryScheduler(BannerPromotionRepository bannerPromotionRepository) {
        this.bannerPromotionRepository = bannerPromotionRepository;
    }

    @Scheduled(cron = "0 */5 * * * *")
    @Transactional
    @CacheEvict(value = "homepageBanners", allEntries = true)
    public void expireBanners() {
        List<BannerPromotion> expired = bannerPromotionRepository
                .findByStatusAndEndDateBeforeAndDeletedFalse(BannerStatus.ACTIVE, LocalDateTime.now());
        for (BannerPromotion banner : expired) {
            banner.setStatus(BannerStatus.EXPIRED);
        }
        if (!expired.isEmpty()) {
            bannerPromotionRepository.saveAll(expired);
        }
    }
}


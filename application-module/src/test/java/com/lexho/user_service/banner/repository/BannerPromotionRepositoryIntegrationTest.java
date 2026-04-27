package com.lexho.user_service.banner.repository;

import com.lexho.user_service.banner.entity.BannerPromotion;
import com.lexho.user_service.banner.enums.BannerPlatformType;
import com.lexho.user_service.banner.enums.BannerPromoType;
import com.lexho.user_service.banner.enums.BannerRedirectType;
import com.lexho.user_service.banner.enums.BannerStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.TestPropertySource;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@TestPropertySource(properties = "spring.flyway.enabled=false")
class BannerPromotionRepositoryIntegrationTest {

    @Autowired
    private BannerPromotionRepository bannerPromotionRepository;

    @Autowired
    private TestEntityManager testEntityManager;

    @Test
    void existsByStatusAndSlotNumberAndDeletedFalseReturnsTrue() {
        BannerPromotion banner = new BannerPromotion();
        banner.setTitle("Cleaning Promo");
        banner.setBannerImageUrl("http://localhost/banner.jpg");
        banner.setPromoType(BannerPromoType.PROMO);
        banner.setStatus(BannerStatus.ACTIVE);
        banner.setSlotNumber(1);
        banner.setPriority(10);
        banner.setStartDate(LocalDateTime.now().minusHours(1));
        banner.setEndDate(LocalDateTime.now().plusDays(1));
        banner.setRedirectType(BannerRedirectType.CATEGORY);
        banner.setRedirectValue("CLEANING");
        banner.setPlatformType(BannerPlatformType.ALL);
        banner.setDeleted(false);
        testEntityManager.persistAndFlush(banner);

        boolean exists = bannerPromotionRepository.existsByStatusAndSlotNumberAndDeletedFalse(BannerStatus.ACTIVE, 1);
        assertThat(exists).isTrue();
    }
}


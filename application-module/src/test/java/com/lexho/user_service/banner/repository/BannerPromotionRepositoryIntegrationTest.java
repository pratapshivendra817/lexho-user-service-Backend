package com.lexho.user_service.banner.repository;

import com.lexho.testconfig.LexhoInfrastructureTest;
import com.lexho.user_service.banner.entity.BannerPromotion;
import com.lexho.user_service.banner.enums.BannerPlatformType;
import com.lexho.user_service.banner.enums.BannerPlacement;
import com.lexho.user_service.banner.enums.BannerPromoType;
import com.lexho.user_service.banner.enums.BannerStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@LexhoInfrastructureTest
@ActiveProfiles("it")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ContextConfiguration(classes = BannerPromotionRepositoryIntegrationTest.JpaTestConfig.class)
@TestPropertySource(properties = {
        "spring.flyway.enabled=false",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
class BannerPromotionRepositoryIntegrationTest {

    @Autowired
    private BannerPromotionRepository bannerPromotionRepository;

    @Autowired
    private TestEntityManager testEntityManager;

    @Test
    void existsByStatusAndPlacementAndSlotNumberAndDeletedFalseReturnsTrue() {
        BannerPromotion banner = new BannerPromotion();
        banner.setTitle("Cleaning Promo");
        banner.setBannerImageUrl("http://localhost/banner.jpg");
        banner.setPromoType(BannerPromoType.PROMO);
        banner.setStatus(BannerStatus.ACTIVE);
        banner.setPlacement(BannerPlacement.TOP);
        banner.setSlotNumber(1);
        banner.setPriority(10);
        banner.setStartDate(LocalDateTime.now().minusHours(1));
        banner.setEndDate(LocalDateTime.now().plusDays(1));
        banner.setPlatformType(BannerPlatformType.ALL);
        banner.setDeleted(false);
        testEntityManager.persistAndFlush(banner);

        boolean exists = bannerPromotionRepository.existsByStatusAndPlacementAndSlotNumberAndDeletedFalse(
                BannerStatus.ACTIVE, BannerPlacement.TOP, 1);
        assertThat(exists).isTrue();
    }

    @SpringBootConfiguration
    @EnableAutoConfiguration
    @EntityScan(basePackageClasses = BannerPromotion.class)
    @EnableJpaRepositories(
            basePackageClasses = BannerPromotionRepository.class,
            includeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = BannerPromotionRepository.class)
    )
    static class JpaTestConfig {
    }
}

package com.lexho.user_service.banner.repository;

import com.lexho.user_service.banner.entity.BannerClickLog;
import com.lexho.user_service.banner.entity.BannerImpressionLog;
import com.lexho.user_service.banner.entity.PromoRedemptionLog;
import com.lexho.testconfig.LexhoMySqlDataJpaTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.test.context.ContextConfiguration;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@LexhoMySqlDataJpaTest
@ContextConfiguration(classes = BannerLogRepositoryTest.JpaConfig.class)
class BannerLogRepositoryTest {

    @Autowired
    private BannerClickLogRepository clickLogRepository;

    @Autowired
    private BannerImpressionLogRepository impressionLogRepository;

    @Autowired
    private PromoRedemptionLogRepository redemptionLogRepository;

    @Test
    void repositoriesPersistLogsAndCountBannerViewsForTheDay() {
        BannerClickLog click = clickLogRepository.save(click(10L, 100L, "device-1", "Delhi"));
        PromoRedemptionLog redemption = redemptionLogRepository.save(redemption(10L, 100L, 7001L, "50.00"));

        BannerImpressionLog userView = impressionLogRepository.save(impression(10L, 100L, "device-1"));
        impressionLogRepository.save(impression(10L, 100L, "device-1"));
        impressionLogRepository.save(impression(10L, null, "device-2"));
        impressionLogRepository.save(impression(10L, 100L, "device-1"));

        assertThat(click.getTimestamp()).isNotNull();
        assertThat(redemption.getDiscountApplied()).isEqualByComparingTo("50.00");
        assertThat(userView.getCity()).isEqualTo("Delhi");

        LocalDateTime startOfDay = LocalDateTime.now().withHour(0).withMinute(0).withSecond(0).withNano(0);
        assertThat(impressionLogRepository.countViewsForDay(10L, 100L, "device-1", startOfDay)).isEqualTo(3);
        assertThat(impressionLogRepository.countViewsForDay(10L, null, "device-2", startOfDay)).isEqualTo(1);
        assertThat(impressionLogRepository.countViewsForDay(10L, null, null, startOfDay)).isEqualTo(4);
        assertThat(impressionLogRepository.countViewsForDay(10L, 100L, "device-1", startOfDay.plusDays(1))).isZero();
    }

    private BannerClickLog click(Long bannerId, Long userId, String deviceId, String city) {
        BannerClickLog log = new BannerClickLog();
        log.setBannerId(bannerId);
        log.setUserId(userId);
        log.setDeviceId(deviceId);
        log.setCity(city);
        return log;
    }

    private BannerImpressionLog impression(Long bannerId, Long userId, String deviceId) {
        BannerImpressionLog log = new BannerImpressionLog();
        log.setBannerId(bannerId);
        log.setUserId(userId);
        log.setDeviceId(deviceId);
        log.setCity("Delhi");
        return log;
    }

    private PromoRedemptionLog redemption(Long bannerId, Long userId, Long bookingId, String discount) {
        PromoRedemptionLog log = new PromoRedemptionLog();
        log.setBannerId(bannerId);
        log.setUserId(userId);
        log.setBookingId(bookingId);
        log.setDiscountApplied(new BigDecimal(discount));
        return log;
    }

    @SpringBootConfiguration
    @EnableAutoConfiguration
    @EntityScan(basePackageClasses = {BannerClickLog.class, BannerImpressionLog.class, PromoRedemptionLog.class})
    @EnableJpaRepositories(
            basePackageClasses = {BannerClickLogRepository.class, BannerImpressionLogRepository.class, PromoRedemptionLogRepository.class},
            includeFilters = {
                    @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = BannerClickLogRepository.class),
                    @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = BannerImpressionLogRepository.class),
                    @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = PromoRedemptionLogRepository.class)
            }
    )
    static class JpaConfig {
    }
}


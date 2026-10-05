package com.lexho.user_service.office.repository;

import com.lexho.testconfig.LexhoMySqlDataJpaTest;
import com.lexho.user_service.office.entity.OfficeLocation;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.test.context.ContextConfiguration;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

@LexhoMySqlDataJpaTest
@ContextConfiguration(classes = OfficeLocationRepositoryTest.JpaConfig.class)
class OfficeLocationRepositoryTest {

    @Autowired
    private OfficeLocationRepository repository;

    @Test
    void activeScreeningCentreCanBeSavedAndRetrievedWithExactCoordinates() {
        OfficeLocation location = location("Lexho Screening Centre", "28.5123456", "77.6123456", true);

        OfficeLocation saved = repository.save(location);

        OfficeLocation active = repository.findFirstByTypeAndActiveTrueOrderByUpdatedAtDesc("SCREENING_CENTRE").orElseThrow();
        assertThat(active.getId()).isEqualTo(saved.getId());
        assertThat(active.getLatitude()).isEqualByComparingTo("28.5123456");
        assertThat(active.getLongitude()).isEqualByComparingTo("77.6123456");
    }

    @Test
    void inactiveScreeningCentreIsNotReturnedAsActive() {
        repository.save(location("Old Centre", "28.1000000", "77.1000000", false));

        assertThat(repository.findFirstByTypeAndActiveTrueOrderByUpdatedAtDesc("SCREENING_CENTRE")).isEmpty();
    }

    @Test
    void deactivateActiveByTypeRemovesActiveExposure() {
        OfficeLocation saved = repository.save(location("Active Centre", "28.2000000", "77.2000000", true));

        int changed = repository.deactivateActiveByType("SCREENING_CENTRE", "ADMIN:1");

        assertThat(changed).isEqualTo(1);
        assertThat(repository.findFirstByTypeAndActiveTrueOrderByUpdatedAtDesc("SCREENING_CENTRE")).isEmpty();
        OfficeLocation deactivated = repository.findById(saved.getId()).orElseThrow();
        assertThat(deactivated.getUpdatedBy()).isEqualTo("ADMIN:1");
        assertThat(deactivated.getUpdatedAt()).isAfterOrEqualTo(deactivated.getCreatedAt());
    }

    private OfficeLocation location(String name, String latitude, String longitude, boolean active) {
        OfficeLocation location = new OfficeLocation();
        location.setName(name);
        location.setAddress("Gt-53, Noida Sector-117");
        location.setLatitude(new BigDecimal(latitude));
        location.setLongitude(new BigDecimal(longitude));
        location.setPhone("+919876543210");
        location.setType("SCREENING_CENTRE");
        location.setActive(active);
        return location;
    }

    @SpringBootConfiguration
    @EnableAutoConfiguration
    @EntityScan(basePackageClasses = OfficeLocation.class)
    @EnableJpaRepositories(
            basePackageClasses = OfficeLocationRepository.class,
            includeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = OfficeLocationRepository.class)
    )
    static class JpaConfig {
    }
}

package com.lexho.user_service.dispatch.repository;

import com.lexho.user_service.dispatch.entity.CurrentPartnerLocationEntity;
import com.lexho.testconfig.LexhoMySqlDataJpaTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ContextConfiguration;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@LexhoMySqlDataJpaTest
@ContextConfiguration(classes = CurrentPartnerLocationRepositoryTest.JpaConfig.class)
class CurrentPartnerLocationRepositoryTest {

    @Autowired
    private CurrentPartnerLocationRepository repository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void ensureSpatialColumnsExist() {
        Integer geoPointColumnCount = jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM information_schema.columns
                WHERE table_schema = DATABASE()
                  AND table_name = 'current_partner_locations'
                  AND column_name = 'geo_point'
                """, Integer.class);
        if (geoPointColumnCount != null && geoPointColumnCount == 0) {
            jdbcTemplate.execute("""
                    ALTER TABLE current_partner_locations
                    ADD COLUMN geo_point POINT SRID 4326 NULL
                    """);
        }
    }

    @Test
    void findNearbyPartnerIdsOrdersByDistanceAndIgnoresDeletedRows() {
        CurrentPartnerLocationEntity near = location(2001L, "28.6139000", "77.2090000", false);
        CurrentPartnerLocationEntity mid = location(2002L, "28.6200000", "77.2200000", false);
        CurrentPartnerLocationEntity deleted = location(2003L, "28.6141000", "77.2092000", true);

        repository.save(near);
        repository.save(mid);
        repository.save(deleted);

        repository.refreshGeoPoint(2001L);
        repository.refreshGeoPoint(2002L);

        List<Long> ids = repository.findNearbyPartnerIds(28.6139, 77.2090, 3.0, 10);

        assertThat(ids).containsExactly(2001L, 2002L);
        assertThat(ids).doesNotContain(2003L);
    }

    @Test
    void refreshGeoPointPopulatesSpatialColumn() {
        repository.save(location(3001L, "12.9716000", "77.5946000", false));

        repository.refreshGeoPoint(3001L);

        String wkt = jdbcTemplate.queryForObject(
                "SELECT ST_AsText(geo_point) FROM current_partner_locations WHERE partner_id = ?",
                String.class,
                3001L
        );
        Integer srid = jdbcTemplate.queryForObject(
                "SELECT ST_SRID(geo_point) FROM current_partner_locations WHERE partner_id = ?",
                Integer.class,
                3001L
        );

        assertThat(wkt).isEqualTo("POINT(12.9716 77.5946)");
        assertThat(srid).isEqualTo(4326);
    }

    private CurrentPartnerLocationEntity location(Long partnerId, String latitude, String longitude, boolean deleted) {
        CurrentPartnerLocationEntity entity = new CurrentPartnerLocationEntity();
        entity.setPartnerId(partnerId);
        entity.setLatitude(new BigDecimal(latitude));
        entity.setLongitude(new BigDecimal(longitude));
        entity.setPincode("560001");
        entity.setSource("GPS");
        entity.setDeleted(deleted);
        return entity;
    }

    @SpringBootConfiguration
    @EnableAutoConfiguration
    @EntityScan(basePackageClasses = CurrentPartnerLocationEntity.class)
    @EnableJpaRepositories(
            basePackageClasses = CurrentPartnerLocationRepository.class,
            includeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = CurrentPartnerLocationRepository.class)
    )
    static class JpaConfig {
    }
}


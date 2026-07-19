package com.lexho.user_service.dispatch.repository;

import com.lexho.user_service.dispatch.entity.CurrentPartnerLocationEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface CurrentPartnerLocationRepository extends JpaRepository<CurrentPartnerLocationEntity, Long> {
    Optional<CurrentPartnerLocationEntity> findFirstByPartnerId(Long partnerId);
    Optional<CurrentPartnerLocationEntity> findByPartnerIdAndDeletedFalse(Long partnerId);
    List<CurrentPartnerLocationEntity> findByPartnerIdInAndDeletedFalse(Collection<Long> partnerIds);
    List<CurrentPartnerLocationEntity> findByDeletedFalseAndUpdatedAtBefore(LocalDateTime cutoff);

    @Query(value = """
            SELECT cpl.partner_id
            FROM current_partner_locations cpl
            WHERE cpl.deleted = false
              AND cpl.latitude IS NOT NULL
              AND cpl.longitude IS NOT NULL
              AND ST_Distance_Sphere(POINT(cpl.longitude, cpl.latitude), POINT(:lng, :lat)) <= (:radiusKm * 1000)
            ORDER BY ST_Distance_Sphere(POINT(cpl.longitude, cpl.latitude), POINT(:lng, :lat)) ASC
            LIMIT :limitRows
            """, nativeQuery = true)
    List<Long> findNearbyPartnerIds(@Param("lat") double lat,
                                    @Param("lng") double lng,
                                    @Param("radiusKm") double radiusKm,
                                    @Param("limitRows") int limitRows);

    @Modifying
    @Transactional
    @Query(value = """
            UPDATE current_partner_locations
            SET geo_point = ST_SRID(POINT(longitude, latitude), 4326)
            WHERE partner_id = :partnerId
              AND deleted = false
            """, nativeQuery = true)
    void refreshGeoPoint(@Param("partnerId") Long partnerId);

    default Optional<CurrentPartnerLocationEntity> findByPartnerId(Long partnerId) {
        return findByPartnerIdAndDeletedFalse(partnerId);
    }

    default Optional<CurrentPartnerLocationEntity> findAnyByPartnerId(Long partnerId) {
        return findFirstByPartnerId(partnerId);
    }
}

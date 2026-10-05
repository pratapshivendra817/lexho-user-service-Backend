package com.lexho.user_service.office.repository;

import com.lexho.user_service.office.entity.OfficeLocation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface OfficeLocationRepository extends JpaRepository<OfficeLocation, Long> {

    Optional<OfficeLocation> findFirstByTypeAndActiveTrueOrderByUpdatedAtDesc(String type);

    List<OfficeLocation> findByTypeAndActiveTrue(String type);

    Optional<OfficeLocation> findFirstByTypeOrderByUpdatedAtDesc(String type);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update OfficeLocation location set location.active = false, location.updatedBy = :updatedBy, "
            + "location.updatedAt = CURRENT_TIMESTAMP "
            + "where location.type = :type and location.active = true")
    int deactivateActiveByType(@Param("type") String type, @Param("updatedBy") String updatedBy);
}

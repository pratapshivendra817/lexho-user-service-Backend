package com.lexho.user_service.emergency.repository;

import com.lexho.user_service.emergency.entity.EmergencyEvent;
import com.lexho.user_service.emergency.entity.EmergencyEventStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface EmergencyAdminQueryRepository extends JpaRepository<EmergencyEvent, Long> {

    @Query("""
            select e from EmergencyEvent e
            where (:status is null or e.status = :status)
              and (:partnerId is null or e.partnerId = :partnerId)
            order by e.createdAt desc
            """)
    Page<EmergencyEvent> search(@Param("status") EmergencyEventStatus status,
                                @Param("partnerId") Long partnerId,
                                Pageable pageable);

    @EntityGraph(attributePaths = "locations")
    Optional<EmergencyEvent> findWithLocationsById(Long id);
}

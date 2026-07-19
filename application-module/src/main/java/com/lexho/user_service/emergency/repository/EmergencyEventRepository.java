package com.lexho.user_service.emergency.repository;

import com.lexho.user_service.emergency.entity.EmergencyEvent;
import org.springframework.data.jpa.repository.EntityGraph;

import java.util.List;
import java.util.Optional;

public interface EmergencyEventRepository extends EmergencyAdminQueryRepository {

    @EntityGraph(attributePaths = "locations")
    List<EmergencyEvent> findByPartnerIdOrderByCreatedAtDesc(Long partnerId);

    @EntityGraph(attributePaths = "locations")
    Optional<EmergencyEvent> findByIdAndPartnerId(Long id, Long partnerId);
}

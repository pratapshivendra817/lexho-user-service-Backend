package com.lexho.user_service.subscription.repository;

import com.lexho.user_service.subscription.entity.SubscriptionVisitPartnerAssignment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SubscriptionVisitPartnerAssignmentRepository extends JpaRepository<SubscriptionVisitPartnerAssignment, Long> {
    Optional<SubscriptionVisitPartnerAssignment> findByVisitId(Long visitId);
    List<SubscriptionVisitPartnerAssignment> findBySubscriptionIdOrderByVisitIdAsc(Long subscriptionId);
}

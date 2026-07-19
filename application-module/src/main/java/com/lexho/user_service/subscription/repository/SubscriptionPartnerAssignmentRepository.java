package com.lexho.user_service.subscription.repository;

import com.lexho.user_service.subscription.entity.SubscriptionPartnerAssignment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SubscriptionPartnerAssignmentRepository extends JpaRepository<SubscriptionPartnerAssignment, Long> {
    Optional<SubscriptionPartnerAssignment> findBySubscriptionId(Long subscriptionId);
}

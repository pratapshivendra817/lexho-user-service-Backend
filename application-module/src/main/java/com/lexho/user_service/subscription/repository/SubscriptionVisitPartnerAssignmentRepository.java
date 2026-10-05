package com.lexho.user_service.subscription.repository;

import com.lexho.user_service.subscription.entity.SubscriptionVisitPartnerAssignment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;

import java.util.List;
import java.util.Optional;

public interface SubscriptionVisitPartnerAssignmentRepository extends JpaRepository<SubscriptionVisitPartnerAssignment, Long> {
    Optional<SubscriptionVisitPartnerAssignment> findByVisitId(Long visitId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select a from SubscriptionVisitPartnerAssignment a where a.visitId = :visitId")
    Optional<SubscriptionVisitPartnerAssignment> findByVisitIdForUpdate(@Param("visitId") Long visitId);

    List<SubscriptionVisitPartnerAssignment> findBySubscriptionIdOrderByVisitIdAsc(Long subscriptionId);
}

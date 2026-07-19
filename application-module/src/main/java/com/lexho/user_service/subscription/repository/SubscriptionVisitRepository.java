package com.lexho.user_service.subscription.repository;

import com.lexho.user_service.subscription.entity.SubscriptionVisit;
import com.lexho.user_service.subscription.enums.VisitStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface SubscriptionVisitRepository extends JpaRepository<SubscriptionVisit, Long> {
    List<SubscriptionVisit> findBySubscriptionIdOrderByVisitDateAscIdAsc(Long subscriptionId);
    List<SubscriptionVisit> findBySubscriptionIdInOrderBySubscriptionIdAscVisitDateAscIdAsc(Collection<Long> subscriptionIds);
    Optional<SubscriptionVisit> findByIdAndSubscriptionId(Long id, Long subscriptionId);
    boolean existsBySubscriptionIdAndVisitDateAndIdNot(Long subscriptionId, LocalDate visitDate, Long id);

    long countBySubscriptionIdAndStatus(Long subscriptionId, VisitStatus status);

    @Modifying
    @Query("""
            update SubscriptionVisit v
            set v.status = :newStatus
            where v.subscriptionId = :subscriptionId
              and v.status = :currentStatus
              and v.visitDate > :today
            """)
    int cancelFutureScheduled(@Param("subscriptionId") Long subscriptionId,
                              @Param("currentStatus") VisitStatus currentStatus,
                              @Param("newStatus") VisitStatus newStatus,
                              @Param("today") LocalDate today);
}

package com.lexho.user_service.subscription.repository;

import com.lexho.user_service.subscription.entity.SubscriptionDurationPrice;
import org.springframework.data.jpa.repository.JpaRepository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface SubscriptionDurationPriceRepository extends JpaRepository<SubscriptionDurationPrice, Long> {
    List<SubscriptionDurationPrice> findByActiveTrueOrderByServiceIdAscDurationHoursAsc();
    List<SubscriptionDurationPrice> findByServiceIdAndActiveTrueOrderByDurationHoursAsc(Long serviceId);
    List<SubscriptionDurationPrice> findByServiceIdIsNullAndActiveTrueOrderByDurationHoursAsc();
    Optional<SubscriptionDurationPrice> findByServiceIdAndDurationHoursAndActiveTrue(Long serviceId, BigDecimal durationHours);
    Optional<SubscriptionDurationPrice> findByServiceIdIsNullAndDurationHoursAndActiveTrue(BigDecimal durationHours);
}

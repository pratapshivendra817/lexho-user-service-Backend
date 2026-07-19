package com.lexho.user_service.subscription.repository;

import com.lexho.user_service.subscription.entity.SubscriptionFrequency;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface SubscriptionFrequencyRepository extends JpaRepository<SubscriptionFrequency, Long> {
    List<SubscriptionFrequency> findBySubscriptionIdOrderByIdAsc(Long subscriptionId);
    List<SubscriptionFrequency> findBySubscriptionIdInOrderBySubscriptionIdAscIdAsc(Collection<Long> subscriptionIds);
}

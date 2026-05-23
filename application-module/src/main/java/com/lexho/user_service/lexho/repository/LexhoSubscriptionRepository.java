package com.lexho.user_service.lexho.repository;

import com.lexho.user_service.lexho.entity.LexhoSubscription;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface LexhoSubscriptionRepository extends JpaRepository<LexhoSubscription, Long> {
    List<LexhoSubscription> findByUserIdOrderByIdDesc(Long userId);
}

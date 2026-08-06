package com.lexho.user_service.subscription.repository;

import com.lexho.user_service.subscription.entity.SubscriptionMaster;
import com.lexho.user_service.subscription.enums.SubscriptionStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SubscriptionMasterRepository extends JpaRepository<SubscriptionMaster, Long> {
    List<SubscriptionMaster> findByCustomerIdOrderByIdDesc(Long customerId);
    List<SubscriptionMaster> findByStatusOrderByIdDesc(SubscriptionStatus status);
    Page<SubscriptionMaster> findByOrderByIdDesc(Pageable pageable);
    Page<SubscriptionMaster> findByCustomerIdOrderByIdDesc(Long customerId, Pageable pageable);
    Page<SubscriptionMaster> findByStatusOrderByIdDesc(SubscriptionStatus status, Pageable pageable);
    Page<SubscriptionMaster> findByCustomerIdAndStatusOrderByIdDesc(Long customerId, SubscriptionStatus status, Pageable pageable);
    Optional<SubscriptionMaster> findByIdAndCustomerId(Long id, Long customerId);
    Optional<SubscriptionMaster> findByBookingId(Long bookingId);
    Optional<SubscriptionMaster> findByBookingIdAndCustomerId(Long bookingId, Long customerId);
}

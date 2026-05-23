package com.lexho.user_service.customer.profile.repository;

import com.lexho.user_service.customer.profile.entity.CustomerAccountDeleteRequestEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CustomerAccountDeleteRequestRepository extends JpaRepository<CustomerAccountDeleteRequestEntity, Long> {
    boolean existsByUserIdAndResolvedFalse(Long userId);
    Page<CustomerAccountDeleteRequestEntity> findByResolved(boolean resolved, Pageable pageable);
}

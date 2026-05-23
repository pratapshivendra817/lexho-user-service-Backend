package com.lexho.user_service.customer.repository;

import com.lexho.user_service.customer.entity.CustomerCartItemEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CustomerCartItemRepository extends JpaRepository<CustomerCartItemEntity, Long> {
    Optional<CustomerCartItemEntity> findByUserIdAndServiceId(Long userId, Long serviceId);
    Optional<CustomerCartItemEntity> findByIdAndUserId(Long id, Long userId);
    List<CustomerCartItemEntity> findByUserId(Long userId);
    void deleteByUserId(Long userId);
}

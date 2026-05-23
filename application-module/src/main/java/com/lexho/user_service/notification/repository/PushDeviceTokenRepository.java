package com.lexho.user_service.notification.repository;

import com.lexho.user_service.notification.entity.PushDeviceTokenEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PushDeviceTokenRepository extends JpaRepository<PushDeviceTokenEntity, Long> {
    Optional<PushDeviceTokenEntity> findByUserIdAndRoleAndDeviceId(Long userId, String role, String deviceId);
    Optional<PushDeviceTokenEntity> findByToken(String token);
    List<PushDeviceTokenEntity> findByUserIdAndRoleAndActiveTrue(Long userId, String role);
}

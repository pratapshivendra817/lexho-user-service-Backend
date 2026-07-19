package com.lexho.user_service.lexhopartner.repository;

import com.lexho.user_service.lexhopartner.entity.PartnerDeviceToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PartnerDeviceTokenRepository extends JpaRepository<PartnerDeviceToken, Long> {
    Optional<PartnerDeviceToken> findByPartnerIdAndToken(Long partnerId, String token);
}

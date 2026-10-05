package com.lexho.user_service.referral.repository;

import com.lexho.user_service.referral.entity.ReferralConfiguration;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReferralConfigurationRepository extends JpaRepository<ReferralConfiguration, Long> {
}

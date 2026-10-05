package com.lexho.user_service.lexho.repository;

import com.lexho.user_service.lexho.entity.LexhoUserProfile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface LexhoUserProfileRepository extends JpaRepository<LexhoUserProfile, Long> {
    Optional<LexhoUserProfile> findByMobile(String mobile);
    Optional<LexhoUserProfile> findByMobileAndRole(String mobile, com.lexho.common.enums.Role role);
}

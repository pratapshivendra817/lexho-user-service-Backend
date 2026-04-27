package com.lexho.notification.repository;

import com.lexho.notification.entity.UserDeviceToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface UserDeviceTokenRepository extends JpaRepository<UserDeviceToken, Long> {

    List<UserDeviceToken> findByUserIdAndActiveTrue(Long userId);
}

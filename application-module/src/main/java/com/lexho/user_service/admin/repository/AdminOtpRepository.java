package com.lexho.user_service.admin.repository;

import com.lexho.user_service.admin.entity.AdminOtp;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AdminOtpRepository extends JpaRepository<AdminOtp, Long> {
    Optional<AdminOtp> findTopByAdmin_IdOrderByCreatedAtDesc(Long adminId);
}

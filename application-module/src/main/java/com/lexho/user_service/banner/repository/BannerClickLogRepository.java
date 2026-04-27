package com.lexho.user_service.banner.repository;

import com.lexho.user_service.banner.entity.BannerClickLog;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BannerClickLogRepository extends JpaRepository<BannerClickLog, Long> {
}


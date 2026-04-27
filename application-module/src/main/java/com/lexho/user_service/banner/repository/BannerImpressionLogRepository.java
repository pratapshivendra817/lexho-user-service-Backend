package com.lexho.user_service.banner.repository;

import com.lexho.user_service.banner.entity.BannerImpressionLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;

public interface BannerImpressionLogRepository extends JpaRepository<BannerImpressionLog, Long> {

    @Query("""
            SELECT COUNT(l) FROM BannerImpressionLog l
            WHERE l.bannerId = :bannerId
              AND (:userId IS NULL OR l.userId = :userId)
              AND (:deviceId IS NULL OR l.deviceId = :deviceId)
              AND l.timestamp >= :startOfDay
            """)
    long countViewsForDay(@Param("bannerId") Long bannerId,
                          @Param("userId") Long userId,
                          @Param("deviceId") String deviceId,
                          @Param("startOfDay") LocalDateTime startOfDay);
}


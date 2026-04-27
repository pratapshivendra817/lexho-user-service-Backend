package com.lexho.notification.repository;

import com.lexho.notification.entity.NotificationLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface NotificationLogRepository extends JpaRepository<NotificationLog, Long> {

    List<NotificationLog> findByNotificationIdOrderByCreatedAtDesc(Long notificationId);
}

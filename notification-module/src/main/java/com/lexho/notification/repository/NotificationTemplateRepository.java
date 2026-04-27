package com.lexho.notification.repository;

import com.lexho.notification.entity.NotificationTemplate;
import com.lexho.notification.enums.NotificationType;
import com.lexho.notification.enums.TemplateLanguage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface NotificationTemplateRepository extends JpaRepository<NotificationTemplate, Long> {

    Optional<NotificationTemplate> findByTemplateNameAndActiveTrue(String templateName);

    Optional<NotificationTemplate> findByNotificationTypeAndLanguageAndActiveTrue(NotificationType notificationType,
                                                                                   TemplateLanguage language);
}

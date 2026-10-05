package com.lexho.user_service.notification.repository;

import com.lexho.user_service.notification.entity.NotificationTemplateEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository("userServiceNotificationTemplateRepository")
public interface NotificationTemplateRepository extends JpaRepository<NotificationTemplateEntity, Long> {

    Optional<NotificationTemplateEntity> findByTemplateNameAndLanguageIgnoreCaseAndActiveTrue(String templateName,
                                                                                              String language);

    Optional<NotificationTemplateEntity> findByTemplateNameAndActiveTrue(String templateName);

    Optional<NotificationTemplateEntity> findByTemplateNameAndLanguageIgnoreCase(String templateName, String language);

    List<NotificationTemplateEntity> findAllByOrderByTemplateNameAscLanguageAsc();
}

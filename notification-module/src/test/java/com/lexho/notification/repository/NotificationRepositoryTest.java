package com.lexho.notification.repository;

import com.lexho.notification.entity.Notification;
import com.lexho.notification.enums.ChannelType;
import com.lexho.notification.enums.NotificationPriority;
import com.lexho.notification.enums.NotificationStatus;
import com.lexho.notification.enums.NotificationType;
import com.lexho.notification.enums.NotificationUserType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.test.context.ContextConfiguration;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest(properties = "spring.jpa.hibernate.ddl-auto=create-drop")
@ContextConfiguration(classes = NotificationRepositoryTest.JpaConfig.class)
class NotificationRepositoryTest {

    @Autowired
    private NotificationRepository repository;

    @Test
    void repositoryQueriesSupportDuplicateDetectionSearchAndDeliveryQueues() {
        Notification queued = repository.save(notification(5001L, "Booking created", "Your booking is confirmed", NotificationType.BOOKING, ChannelType.IN_APP, NotificationStatus.QUEUED, false));
        queued.setScheduledTime(LocalDateTime.now().minusMinutes(5));
        queued.setExpiryTime(LocalDateTime.now().plusDays(1));
        repository.save(queued);

        Notification failed = repository.save(notification(5001L, "Payment pending", "Retry card payment", NotificationType.PAYMENT, ChannelType.PUSH, NotificationStatus.FAILED, false));
        failed.setRetryCount(1);
        repository.save(failed);

        Notification read = repository.save(notification(5001L, "Promo", "Festival offer", NotificationType.PROMOTION, ChannelType.EMAIL, NotificationStatus.READ, false));
        Notification expired = repository.save(notification(5002L, "Reminder", "Expired reminder", NotificationType.REMINDER, ChannelType.SMS, NotificationStatus.SENT, false));
        expired.setExpiryTime(LocalDateTime.now().minusMinutes(1));
        repository.save(expired);

        Notification deleted = repository.save(notification(5001L, "Deleted", "Ignore me", NotificationType.SYSTEM, ChannelType.IN_APP, NotificationStatus.QUEUED, true));

        assertThat(repository.findByIdAndDeletedFalse(queued.getId())).contains(queued);
        assertThat(repository.findByIdAndDeletedFalse(deleted.getId())).isEmpty();
        assertThat(repository.findByUserIdAndDeletedFalseOrderByCreatedAtDesc(5001L)).hasSize(3);
        assertThat(repository.findByUserIdAndChannelTypeAndDeletedFalseOrderByCreatedAtDesc(5001L, ChannelType.IN_APP))
                .extracting(Notification::getTitle).containsExactly("Booking created");

        assertThat(repository.findByUserIdAndStatusNotAndDeletedFalseOrderByCreatedAtDesc(5001L, NotificationStatus.READ))
                .extracting(Notification::getTitle).containsExactlyInAnyOrder("Booking created", "Payment pending");
        assertThat(repository.countByUserIdAndStatusNotAndDeletedFalse(5001L, NotificationStatus.READ)).isEqualTo(2);
        assertThat(repository.countByUserIdAndChannelTypeAndStatusNotAndDeletedFalse(5001L, ChannelType.PUSH, NotificationStatus.READ)).isEqualTo(1);
        assertThat(repository.countByStatusAndDeletedFalse(NotificationStatus.QUEUED)).isEqualTo(1);
        assertThat(repository.countByDeletedFalse()).isEqualTo(4);
        assertThat(repository.countByChannelTypeAndStatusAndDeletedFalse(ChannelType.PUSH, NotificationStatus.FAILED)).isEqualTo(1);

        assertThat(repository.findByStatusAndScheduledTimeLessThanEqualAndDeletedFalse(NotificationStatus.QUEUED, LocalDateTime.now()))
                .extracting(Notification::getId).containsExactly(queued.getId());
        assertThat(repository.findByStatusAndDeletedFalseAndRetryCountLessThan(NotificationStatus.FAILED, 3))
                .extracting(Notification::getId).containsExactly(failed.getId());
        assertThat(repository.findByExpiryTimeBeforeAndDeletedFalse(LocalDateTime.now()))
                .extracting(Notification::getId).containsExactly(expired.getId());

        assertThat(repository.existsRecentDuplicate(
                5001L,
                NotificationUserType.CUSTOMER,
                NotificationType.BOOKING,
                ChannelType.IN_APP,
                "Booking created",
                "Your booking is confirmed",
                null,
                null,
                null,
                null,
                LocalDateTime.now().minusHours(1)
        )).isTrue();

        Page<Notification> adminSearch = repository.searchAdmin(
                "payment",
                NotificationUserType.CUSTOMER,
                NotificationType.PAYMENT,
                ChannelType.PUSH,
                NotificationStatus.FAILED,
                PageRequest.of(0, 10)
        );
        assertThat(adminSearch.getContent()).extracting(Notification::getTitle)
                .containsExactly("Payment pending");

        Page<Notification> genericSearch = repository.search("promo", "promotion", PageRequest.of(0, 10));
        assertThat(genericSearch.getContent()).extracting(Notification::getTitle)
                .containsExactly("Promo");
        assertThat(repository.search("missing", "system", PageRequest.of(0, 10)).getContent()).isEmpty();
    }

    private Notification notification(Long userId,
                                      String title,
                                      String message,
                                      NotificationType type,
                                      ChannelType channelType,
                                      NotificationStatus status,
                                      boolean deleted) {
        Notification entity = new Notification();
        entity.setUserId(userId);
        entity.setUserType(NotificationUserType.CUSTOMER);
        entity.setTitle(title);
        entity.setMessage(message);
        entity.setNotificationType(type);
        entity.setChannelType(channelType);
        entity.setPriority(NotificationPriority.MEDIUM);
        entity.setStatus(status);
        entity.setRetryCount(0);
        entity.setDeleted(deleted);
        return entity;
    }

    @SpringBootConfiguration
    @EnableAutoConfiguration
    @EntityScan(basePackageClasses = Notification.class)
    @EnableJpaRepositories(basePackageClasses = NotificationRepository.class)
    static class JpaConfig {
    }
}

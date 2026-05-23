package com.lexho.notification.repository;

import com.lexho.notification.entity.Notification;
import com.lexho.notification.enums.ChannelType;
import com.lexho.notification.enums.NotificationStatus;
import com.lexho.notification.enums.NotificationType;
import com.lexho.notification.enums.NotificationUserType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    Optional<Notification> findByIdAndDeletedFalse(Long id);

    List<Notification> findByUserIdAndDeletedFalseOrderByCreatedAtDesc(Long userId);

    List<Notification> findByUserIdAndStatusNotAndDeletedFalseOrderByCreatedAtDesc(Long userId, NotificationStatus status);

    long countByUserIdAndStatusNotAndDeletedFalse(Long userId, NotificationStatus status);

    long countByStatusAndDeletedFalse(NotificationStatus status);

    long countByDeletedFalse();

    long countByChannelTypeAndStatusAndDeletedFalse(ChannelType channelType, NotificationStatus status);

    List<Notification> findByStatusAndScheduledTimeLessThanEqualAndDeletedFalse(NotificationStatus status, LocalDateTime now);

    List<Notification> findByStatusAndDeletedFalseAndRetryCountLessThan(NotificationStatus status, Integer retryCount);

    List<Notification> findByExpiryTimeBeforeAndDeletedFalse(LocalDateTime now);

    @Query("""
            SELECT CASE WHEN COUNT(n) > 0 THEN true ELSE false END
            FROM Notification n
            WHERE n.deleted = false
              AND n.userId = :userId
              AND n.userType = :userType
              AND n.notificationType = :notificationType
              AND n.channelType = :channelType
              AND n.title = :title
              AND n.message = :message
              AND COALESCE(n.referenceType, '') = COALESCE(:referenceType, '')
              AND COALESCE(n.referenceId, -1) = COALESCE(:referenceId, -1)
              AND COALESCE(n.redirectType, '') = COALESCE(:redirectType, '')
              AND COALESCE(n.redirectValue, '') = COALESCE(:redirectValue, '')
              AND n.createdAt >= :createdAfter
            """)
    boolean existsRecentDuplicate(@Param("userId") Long userId,
                                  @Param("userType") NotificationUserType userType,
                                  @Param("notificationType") NotificationType notificationType,
                                  @Param("channelType") ChannelType channelType,
                                  @Param("title") String title,
                                  @Param("message") String message,
                                  @Param("referenceType") String referenceType,
                                  @Param("referenceId") Long referenceId,
                                  @Param("redirectType") String redirectType,
                                  @Param("redirectValue") String redirectValue,
                                  @Param("createdAfter") LocalDateTime createdAfter);

    @Query("""
            SELECT n FROM Notification n
            WHERE n.deleted = false
              AND (:search IS NULL OR LOWER(COALESCE(n.title, '')) LIKE LOWER(CONCAT('%', :search, '%'))
                   OR LOWER(COALESCE(n.message, '')) LIKE LOWER(CONCAT('%', :search, '%')))
              AND (:userType IS NULL OR n.userType = :userType)
              AND (:notificationType IS NULL OR n.notificationType = :notificationType)
              AND (:channelType IS NULL OR n.channelType = :channelType)
              AND (:status IS NULL OR n.status = :status)
            """)
    Page<Notification> searchAdmin(@Param("search") String search,
                                   @Param("userType") NotificationUserType userType,
                                   @Param("notificationType") NotificationType notificationType,
                                   @Param("channelType") ChannelType channelType,
                                   @Param("status") NotificationStatus status,
                                   Pageable pageable);

    @Query("""
            SELECT n FROM Notification n
            WHERE n.deleted = false
              AND (:search IS NULL OR LOWER(COALESCE(n.title, '')) LIKE LOWER(CONCAT('%', :search, '%'))
                   OR LOWER(COALESCE(n.message, '')) LIKE LOWER(CONCAT('%', :search, '%')))
              AND (:type IS NULL OR FUNCTION('UPPER', n.notificationType) = FUNCTION('UPPER', :type))
            """)
    Page<Notification> search(@Param("search") String search,
                              @Param("type") String type,
                              Pageable pageable);
}

package com.lexho.user_service.notification.service;

import com.lexho.admin.repository.AdminRepository;
import com.lexho.common.enums.Role;
import com.lexho.notification.entity.Notification;
import com.lexho.notification.entity.NotificationLog;
import com.lexho.notification.entity.NotificationPreference;
import com.lexho.notification.entity.UserDeviceToken;
import com.lexho.notification.enums.ChannelType;
import com.lexho.notification.enums.DeliveryStatus;
import com.lexho.notification.enums.NotificationPriority;
import com.lexho.notification.enums.NotificationStatus;
import com.lexho.notification.enums.NotificationType;
import com.lexho.notification.enums.NotificationUserType;
import com.lexho.notification.repository.NotificationLogRepository;
import com.lexho.notification.repository.NotificationPreferenceRepository;
import com.lexho.notification.repository.NotificationRepository;
import com.lexho.notification.repository.UserDeviceTokenRepository;
import com.lexho.user.entity.User;
import com.lexho.user.enums.UserStatus;
import com.lexho.user.repository.UserRepository;
import com.lexho.user_service.lexho.entity.LexhoUserProfile;
import com.lexho.user_service.lexho.repository.LexhoUserProfileRepository;
import com.lexho.user_service.notification.dto.AdminBulkNotificationRequest;
import com.lexho.user_service.notification.dto.ChannelStatResponse;
import com.lexho.user_service.notification.dto.NotificationPageResponse;
import com.lexho.user_service.notification.dto.NotificationResponse;
import com.lexho.user_service.notification.dto.NotificationSendResponse;
import com.lexho.user_service.notification.dto.NotificationStatsResponse;
import com.lexho.user_service.notification.event.NotificationDispatchEvent;
import com.lexho.user_service.notification.exception.NotificationAccessDeniedException;
import com.lexho.user_service.notification.exception.NotificationNotFoundException;
import com.lexho.user_service.notification.exception.NotificationValidationException;
import com.lexho.user_service.notification.mapper.NotificationMapper;
import com.lexho.user_service.security.SecurityUtils;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class NotificationManagementService {

    private static final Logger log = LoggerFactory.getLogger(NotificationManagementService.class);
    private static final int MAX_RETRY = 3;
    private static final int PER_TYPE_MINUTE_LIMIT = 5;
    private static final int DUPLICATE_GUARD_SECONDS = 10;

    private final NotificationRepository notificationRepository;
    private final NotificationLogRepository notificationLogRepository;
    private final NotificationPreferenceRepository notificationPreferenceRepository;
    private final UserDeviceTokenRepository userDeviceTokenRepository;
    private final UserRepository userRepository;
    private final LexhoUserProfileRepository lexhoUserProfileRepository;
    private final AdminRepository adminRepository;
    private final NotificationMapper notificationMapper;
    private final ApplicationEventPublisher eventPublisher;
    private final SecurityUtils securityUtils;

    public NotificationManagementService(NotificationRepository notificationRepository,
                                         NotificationLogRepository notificationLogRepository,
                                         NotificationPreferenceRepository notificationPreferenceRepository,
                                         UserDeviceTokenRepository userDeviceTokenRepository,
                                         UserRepository userRepository,
                                         LexhoUserProfileRepository lexhoUserProfileRepository,
                                         AdminRepository adminRepository,
                                         NotificationMapper notificationMapper,
                                         ApplicationEventPublisher eventPublisher,
                                         SecurityUtils securityUtils) {
        this.notificationRepository = notificationRepository;
        this.notificationLogRepository = notificationLogRepository;
        this.notificationPreferenceRepository = notificationPreferenceRepository;
        this.userDeviceTokenRepository = userDeviceTokenRepository;
        this.userRepository = userRepository;
        this.lexhoUserProfileRepository = lexhoUserProfileRepository;
        this.adminRepository = adminRepository;
        this.notificationMapper = notificationMapper;
        this.eventPublisher = eventPublisher;
        this.securityUtils = securityUtils;
    }

    @Transactional
    public NotificationSendResponse sendBulkNotification(AdminBulkNotificationRequest request) {
        try {
            validateBulkRequest(request);
            Set<Long> targets = resolveTargetUsers(request);
            log.info("admin_notification_send_request type={} channels={} targetUserType={} city={} scheduledTime={} targetCount={}",
                    request.getNotificationType(),
                    request.getChannels(),
                    request.getTargetUserType(),
                    request.getCity(),
                    request.getScheduledTime(),
                    targets.size());
            if (targets.isEmpty()) {
                log.warn("admin_notification_send_no_targets type={} targetUserType={} city={}",
                        request.getNotificationType(), request.getTargetUserType(), request.getCity());
                return new NotificationSendResponse(0, 0, "No target users matched");
            }

            int created = 0;
            for (Long userId : targets) {
                for (ChannelType channelType : request.getChannels()) {
                    if (!isChannelAllowedByPreference(userId, request.getNotificationType(), channelType)) {
                        continue;
                    }
                    if (isRateLimited(userId, request.getNotificationType())) {
                        log.warn("Notification skipped due to per-minute rate-limit. userId={}, type={}", userId, request.getNotificationType());
                        continue;
                    }
                    Notification notification = buildNotification(request, userId, channelType);
                    if (isRecentDuplicate(notification)) {
                        log.warn("Duplicate notification skipped. userId={}, channel={}, type={}, title={}",
                                notification.getUserId(), notification.getChannelType(),
                                notification.getNotificationType(), notification.getTitle());
                        continue;
                    }
                    if (channelType == ChannelType.IN_APP) {
                        // Keep in-app notifications real-time for app UX.
                        notification.setScheduledTime(null);
                        notification.setStatus(NotificationStatus.SENT);
                        notification.setSentAt(LocalDateTime.now());
                    }
                    Notification saved = notificationRepository.save(notification);
                    created++;
                    log.info("admin_notification_created notificationId={} userId={} channel={} type={} status={}",
                            saved.getId(), userId, channelType, saved.getNotificationType(), saved.getStatus());
                    if (channelType == ChannelType.IN_APP
                            || saved.getScheduledTime() == null
                            || !saved.getScheduledTime().isAfter(LocalDateTime.now())) {
                        publishDispatch(saved);
                    }
                }
            }
            log.info("admin_notification_send_completed targetCount={} createdCount={}", targets.size(), created);
            return new NotificationSendResponse(targets.size(), created, "Notifications queued successfully");
        } catch (NotificationValidationException ex) {
            throw ex;
        } catch (RuntimeException ex) {
            log.error("Bulk notification send failed", ex);
            throw new IllegalArgumentException("Invalid notification request data");
        }
    }

    public NotificationPageResponse<NotificationResponse> getCustomerNotifications(int page, int size) {
        Long userId = currentUserId();
        return byUser(page, size, userId);
    }

    public NotificationPageResponse<NotificationResponse> getProviderNotifications(int page, int size) {
        Long userId = currentUserId();
        return byUser(page, size, userId);
    }

    @Cacheable(value = "notificationUnreadCount", key = "#userId")
    public long unreadCountByUserId(Long userId) {
        return notificationRepository.countByUserIdAndStatusNotAndDeletedFalse(userId, NotificationStatus.READ);
    }

    public long unreadCountForCurrentUser() {
        return unreadCountByUserId(currentUserId());
    }

    @Transactional
    @CacheEvict(value = "notificationUnreadCount", key = "#result.userId", condition = "#result != null")
    public Notification markAsReadForCurrentUser(Long notificationId) {
        Notification notification = findActive(notificationId);
        Long userId = currentUserId();
        if (!notification.getUserId().equals(userId)) {
            throw new NotificationAccessDeniedException("You can access only your own notifications");
        }
        notification.setStatus(NotificationStatus.READ);
        notification.setReadAt(LocalDateTime.now());
        return notificationRepository.save(notification);
    }

    @Transactional
    @CacheEvict(value = "notificationUnreadCount", key = "#result.userId", condition = "#result != null")
    public Notification softDeleteForCurrentUser(Long notificationId) {
        Notification notification = findActive(notificationId);
        Long userId = currentUserId();
        if (!notification.getUserId().equals(userId)) {
            throw new NotificationAccessDeniedException("You can access only your own notifications");
        }
        notification.setDeleted(true);
        return notificationRepository.save(notification);
    }

    public NotificationPageResponse<NotificationResponse> getAdminNotifications(int page,
                                                                                int size,
                                                                                String sortBy,
                                                                                String sortDir,
                                                                                NotificationUserType userType,
                                                                                NotificationType notificationType,
                                                                                ChannelType channelType,
                                                                                NotificationStatus status,
                                                                                String search) {
        Pageable pageable = buildPageable(page, size, sortBy, sortDir);
        Page<Notification> result = notificationRepository.searchAdmin(
                normalize(search),
                userType,
                notificationType,
                channelType,
                status,
                pageable
        );
        return mapPage(result);
    }

    public NotificationResponse getAdminNotificationById(Long id) {
        return toResponseWithFailure(findActive(id));
    }

    @Transactional
    public NotificationResponse retryNotification(Long id) {
        try {
            Notification notification = findActive(id);
            if (notification.getStatus() != NotificationStatus.FAILED && notification.getStatus() != NotificationStatus.PENDING) {
                throw new NotificationValidationException("Only failed/pending notifications can be retried");
            }
            int currentRetryCount = notification.getRetryCount() == null ? 0 : notification.getRetryCount();
            if (currentRetryCount >= MAX_RETRY) {
                throw new NotificationValidationException("Max retry reached for notification");
            }
            notification.setStatus(NotificationStatus.PENDING);
            notification.setRetryCount(currentRetryCount + 1);
            Notification saved = notificationRepository.save(notification);
            try {
                publishDispatch(saved);
            } catch (RuntimeException publishException) {
                log.error("Failed to publish retry dispatch event. notificationId={}", id, publishException);
                saveLog(saved.getId(), DeliveryStatus.FAILED,
                        "Retry dispatch enqueue failed: " + publishException.getMessage(),
                        saved.getRetryCount());
            }
            return toResponseWithFailure(saved);
        } catch (NotificationValidationException ex) {
            throw ex;
        } catch (RuntimeException ex) {
            log.error("Notification retry failed. notificationId={}", id, ex);
            String message = ex.getMessage() == null || ex.getMessage().isBlank()
                    ? "Retry failed due to internal processing error"
                    : ex.getMessage();
            throw new NotificationValidationException("Retry failed: " + message);
        }
    }

    public NotificationStatsResponse getStats() {
        long totalSent = notificationRepository.countByStatusAndDeletedFalse(NotificationStatus.SENT)
                + notificationRepository.countByStatusAndDeletedFalse(NotificationStatus.DELIVERED);
        long totalFailed = notificationRepository.countByStatusAndDeletedFalse(NotificationStatus.FAILED);
        long totalRead = notificationRepository.countByStatusAndDeletedFalse(NotificationStatus.READ);
        long totalPending = notificationRepository.countByStatusAndDeletedFalse(NotificationStatus.PENDING);
        long totalClicked = notificationRepository.countByStatusAndDeletedFalse(NotificationStatus.CLICKED);

        List<ChannelStatResponse> channelWise = new ArrayList<>();
        for (ChannelType channelType : ChannelType.values()) {
            long sent = notificationRepository.countByChannelTypeAndStatusAndDeletedFalse(channelType, NotificationStatus.SENT)
                    + notificationRepository.countByChannelTypeAndStatusAndDeletedFalse(channelType, NotificationStatus.DELIVERED)
                    + notificationRepository.countByChannelTypeAndStatusAndDeletedFalse(channelType, NotificationStatus.READ)
                    + notificationRepository.countByChannelTypeAndStatusAndDeletedFalse(channelType, NotificationStatus.CLICKED);
            long failed = notificationRepository.countByChannelTypeAndStatusAndDeletedFalse(channelType, NotificationStatus.FAILED);
            channelWise.add(new ChannelStatResponse(channelType, sent, failed));
        }

        return new NotificationStatsResponse(totalSent, totalFailed, totalRead, totalPending, totalClicked, channelWise);
    }

    @Transactional
    @CacheEvict(value = "notificationUnreadCount", allEntries = true)
    public void processScheduledNotifications() {
        List<Notification> due = notificationRepository.findByStatusAndScheduledTimeLessThanEqualAndDeletedFalse(
                NotificationStatus.PENDING,
                LocalDateTime.now()
        );
        due.forEach(this::publishDispatch);
    }

    @Transactional
    @CacheEvict(value = "notificationUnreadCount", allEntries = true)
    public void retryFailedNotifications() {
        List<Notification> failed = notificationRepository.findByStatusAndDeletedFalseAndRetryCountLessThan(
                NotificationStatus.FAILED,
                MAX_RETRY
        );
        for (Notification notification : failed) {
            notification.setStatus(NotificationStatus.PENDING);
            notification.setRetryCount(notification.getRetryCount() + 1);
            Notification saved = notificationRepository.save(notification);
            publishDispatch(saved);
        }
    }

    @Transactional
    @CacheEvict(value = "notificationUnreadCount", allEntries = true)
    public void cleanupExpiredNotifications() {
        List<Notification> expired = notificationRepository.findByExpiryTimeBeforeAndDeletedFalse(LocalDateTime.now());
        expired.forEach(item -> item.setDeleted(true));
        notificationRepository.saveAll(expired);
    }

    @Transactional
    @CacheEvict(value = "notificationUnreadCount", key = "#notification.userId")
    public void markDispatched(Notification notification) {
        notification.setStatus(NotificationStatus.SENT);
        notification.setSentAt(LocalDateTime.now());
        notificationRepository.save(notification);
        saveLog(notification.getId(), DeliveryStatus.SUCCESS, null, notification.getRetryCount());
    }

    @Transactional
    @CacheEvict(value = "notificationUnreadCount", key = "#notification.userId")
    public void markFailed(Notification notification, String errorMessage) {
        notification.setStatus(NotificationStatus.FAILED);
        notificationRepository.save(notification);
        String normalizedError = (errorMessage == null || errorMessage.isBlank())
                ? "Dispatch failed due to unknown error"
                : errorMessage;
        saveLog(notification.getId(), DeliveryStatus.FAILED, normalizedError, notification.getRetryCount());
    }

    public Notification findActive(Long id) {
        return notificationRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new NotificationNotFoundException(id));
    }

    public List<UserDeviceToken> activeDeviceTokens(Long userId) {
        return userDeviceTokenRepository.findByUserIdAndActiveTrue(userId);
    }

    private void validateBulkRequest(AdminBulkNotificationRequest request) {
        if (request.getScheduledTime() != null && request.getScheduledTime().isBefore(LocalDateTime.now())) {
            request.setScheduledTime(null);
        }
        if (request.getExpiryTime() != null && request.getScheduledTime() != null
                && !request.getExpiryTime().isAfter(request.getScheduledTime())) {
            throw new NotificationValidationException("expiryTime must be greater than scheduledTime");
        }
    }

    private Notification buildNotification(AdminBulkNotificationRequest request, Long userId, ChannelType channelType) {
        Notification notification = new Notification();
        notification.setUserId(userId);
        notification.setUserType(request.getTargetUserType());
        notification.setTitle(request.getTitle());
        notification.setMessage(applyVariables(request.getMessage(), request));
        notification.setNotificationType(request.getNotificationType());
        notification.setChannelType(channelType);
        notification.setPriority(request.getPriority() == null ? NotificationPriority.MEDIUM : request.getPriority());
        notification.setReferenceId(request.getReferenceId());
        notification.setReferenceType(request.getReferenceType());
        notification.setRedirectType(request.getRedirectType());
        notification.setRedirectValue(request.getRedirectValue());
        notification.setScheduledTime(request.getScheduledTime());
        notification.setExpiryTime(request.getExpiryTime());
        notification.setStatus(NotificationStatus.PENDING);
        notification.setRetryCount(0);
        notification.setDeleted(false);
        return notification;
    }

    private NotificationPageResponse<NotificationResponse> byUser(int page, int size, Long userId) {
        Pageable pageable = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 100), Sort.by(Sort.Direction.DESC, "createdAt"));
        List<Notification> all = notificationRepository.findByUserIdAndDeletedFalseOrderByCreatedAtDesc(userId)
                .stream()
                .filter(this::notExpired)
                .toList();
        int start = Math.min((int) pageable.getOffset(), all.size());
        int end = Math.min(start + pageable.getPageSize(), all.size());
        List<NotificationResponse> content = all.subList(start, end).stream().map(this::toResponseWithFailure).toList();
        int totalPages = pageable.getPageSize() == 0 ? 0 : (int) Math.ceil((double) all.size() / pageable.getPageSize());
        return new NotificationPageResponse<>(content, all.size(), totalPages, pageable.getPageNumber());
    }

    private NotificationPageResponse<NotificationResponse> mapPage(Page<Notification> page) {
        List<NotificationResponse> content = page.getContent().stream().map(this::toResponseWithFailure).toList();
        return new NotificationPageResponse<>(content, page.getTotalElements(), page.getTotalPages(), page.getNumber());
    }

    private NotificationResponse toResponseWithFailure(Notification notification) {
        NotificationResponse response = notificationMapper.toResponse(notification);
        String failureReason = notificationLogRepository.findByNotificationIdOrderByCreatedAtDesc(notification.getId()).stream()
                .map(NotificationLog::getErrorMessage)
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(message -> !message.isEmpty())
                .findFirst()
                .orElse(null);
        response.setFailureReason(failureReason);
        return response;
    }

    private Pageable buildPageable(int page, int size, String sortBy, String sortDir) {
        Sort.Direction direction = "asc".equalsIgnoreCase(sortDir) ? Sort.Direction.ASC : Sort.Direction.DESC;
        String allowed = switch (sortBy == null ? "" : sortBy) {
            case "createdAt", "updatedAt", "status", "notificationType", "userId" -> sortBy;
            default -> "createdAt";
        };
        return PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 100), Sort.by(direction, allowed));
    }

    private String normalize(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private void publishDispatch(Notification notification) {
        eventPublisher.publishEvent(new NotificationDispatchEvent(notification.getId(), notification.getChannelType()));
    }

    private boolean isChannelAllowedByPreference(Long userId, NotificationType type, ChannelType channelType) {
        NotificationPreference preference = notificationPreferenceRepository.findByUserId(userId).orElse(null);
        if (preference == null) {
            return true;
        }
        if (type == NotificationType.PROMOTION && !preference.isPromotionalEnabled()) {
            return false;
        }
        if (type == NotificationType.BOOKING && !preference.isBookingEnabled()) {
            return false;
        }
        return switch (channelType) {
            case IN_APP -> true;
            case PUSH -> preference.isPushEnabled();
            case EMAIL -> preference.isEmailEnabled();
            case SMS -> preference.isSmsEnabled();
            case WHATSAPP -> preference.isWhatsappEnabled();
        };
    }

    private boolean isRateLimited(Long userId, NotificationType type) {
        LocalDateTime threshold = LocalDateTime.now().minusMinutes(1);
        long recentCount = notificationRepository.findByUserIdAndDeletedFalseOrderByCreatedAtDesc(userId).stream()
                .filter(item -> item.getNotificationType() == type)
                .filter(item -> item.getCreatedAt() != null && item.getCreatedAt().isAfter(threshold))
                .count();
        return recentCount >= PER_TYPE_MINUTE_LIMIT;
    }

    private boolean isRecentDuplicate(Notification notification) {
        LocalDateTime threshold = LocalDateTime.now().minusSeconds(DUPLICATE_GUARD_SECONDS);
        return notificationRepository.existsRecentDuplicate(
                notification.getUserId(),
                notification.getUserType(),
                notification.getNotificationType(),
                notification.getChannelType(),
                notification.getTitle(),
                notification.getMessage(),
                notification.getReferenceType(),
                notification.getReferenceId(),
                notification.getRedirectType(),
                notification.getRedirectValue(),
                threshold
        );
    }

    private Set<Long> resolveTargetUsers(AdminBulkNotificationRequest request) {
        Set<Long> targetIds = new HashSet<>();
        if (request.getUserIds() != null && !request.getUserIds().isEmpty()) {
            targetIds.addAll(request.getUserIds());
            return targetIds;
        }

        if (request.getTargetUserType() == NotificationUserType.ADMIN) {
            targetIds.addAll(adminRepository.findAll().stream().map(admin -> admin.getId()).collect(Collectors.toSet()));
            return targetIds;
        }

        // Customer app authenticates via Lexho profile ids, so include those ids for customer broadcasts.
        if (request.getTargetUserType() == NotificationUserType.CUSTOMER) {
            boolean hasLegacyOnlyFilter = (request.getCity() != null && !request.getCity().isBlank())
                    || Boolean.TRUE.equals(request.getInactiveUsers())
                    || Boolean.TRUE.equals(request.getPremiumUsers());
            if (!hasLegacyOnlyFilter) {
                List<LexhoUserProfile> lexhoCustomers = lexhoUserProfileRepository.findAll().stream()
                        .filter(profile -> profile.getRole() == Role.CUSTOMER)
                        .toList();
                targetIds.addAll(lexhoCustomers.stream().map(LexhoUserProfile::getId).collect(Collectors.toSet()));
            }
        }

        List<User> users = userRepository.findAll().stream()
                .filter(user -> !user.isDeleted())
                .toList();

        users = users.stream()
                .filter(user -> matchUserType(user, request.getTargetUserType()))
                .toList();

        if (request.getCity() != null && !request.getCity().isBlank()) {
            String city = request.getCity().trim().toLowerCase(Locale.ROOT);
            users = users.stream()
                    .filter(user -> user.getCity() != null && user.getCity().trim().toLowerCase(Locale.ROOT).equals(city))
                    .toList();
        }
        if (Boolean.TRUE.equals(request.getInactiveUsers())) {
            users = users.stream()
                    .filter(user -> user.getStatus() != UserStatus.ACTIVE)
                    .toList();
        }
        if (Boolean.TRUE.equals(request.getPremiumUsers())) {
            users = users.stream()
                    .filter(user -> user.getTotalBookings() != null && user.getTotalBookings() >= 50)
                    .toList();
        }

        targetIds.addAll(users.stream().map(User::getId).collect(Collectors.toSet()));
        return targetIds;
    }

    private boolean matchUserType(User user, NotificationUserType userType) {
        if (userType == NotificationUserType.CUSTOMER) {
            return user.getRole() == Role.CUSTOMER;
        }
        if ("PARTNER".equals(userType.name())) {
            return user.getRole() == Role.PARTNER;
        }
        return false;
    }

    private String applyVariables(String message, AdminBulkNotificationRequest request) {
        if (message == null || request.getTemplateVariables() == null || request.getTemplateVariables().isEmpty()) {
            return message;
        }
        String output = message;
        for (var entry : request.getTemplateVariables().entrySet()) {
            output = output.replace("{" + entry.getKey() + "}", entry.getValue());
        }
        return output;
    }

    private void saveLog(Long notificationId, DeliveryStatus status, String error, Integer retryCount) {
        NotificationLog logItem = new NotificationLog();
        logItem.setNotificationId(notificationId);
        logItem.setDeliveryStatus(status);
        logItem.setErrorMessage(error);
        logItem.setRetryCount(retryCount == null ? 0 : retryCount);
        notificationLogRepository.save(logItem);
    }

    private boolean notExpired(Notification notification) {
        return notification.getExpiryTime() == null || notification.getExpiryTime().isAfter(LocalDateTime.now());
    }

    private Long currentUserId() {
        return securityUtils.getCurrentUserId()
                .orElseThrow(() -> new NotificationAccessDeniedException("Authenticated user not found"));
    }
}

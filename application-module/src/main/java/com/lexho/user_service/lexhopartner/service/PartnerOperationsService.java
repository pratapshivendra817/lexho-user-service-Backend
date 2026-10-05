package com.lexho.user_service.lexhopartner.service;

import com.lexho.user_service.audit.service.AuditLogService;
import com.lexho.user_service.dispatch.repository.PartnerShiftRepository;
import com.lexho.user_service.exception.BadRequestException;
import com.lexho.user_service.exception.ResourceNotFoundException;
import com.lexho.user_service.lexhopartner.dto.PartnerLeaveRequest;
import com.lexho.user_service.lexhopartner.dto.PartnerServiceAreaRequest;
import com.lexho.user_service.lexhopartner.dto.PartnerWarningCommentRequest;
import com.lexho.user_service.lexhopartner.entity.OrderStatus;
import com.lexho.user_service.lexhopartner.entity.PartnerLeave;
import com.lexho.user_service.lexhopartner.entity.PartnerPenalty;
import com.lexho.user_service.lexhopartner.entity.PartnerServiceArea;
import com.lexho.user_service.lexhopartner.entity.PartnerWarningComment;
import com.lexho.user_service.lexhopartner.repository.PartnerLeaveRepository;
import com.lexho.user_service.lexhopartner.repository.PartnerOrderRepository;
import com.lexho.user_service.lexhopartner.repository.PartnerPenaltyRepository;
import com.lexho.user_service.lexhopartner.repository.PartnerOperationalProfileRepository;
import com.lexho.user_service.lexhopartner.repository.PartnerServiceAreaRepository;
import com.lexho.user_service.lexhopartner.repository.PartnerWarningCommentRepository;
import com.lexho.user_service.localization.dynamic.DynamicContentTranslationService;
import com.lexho.user_service.localization.service.PartnerLocalizationService;
import com.lexho.user_service.notification.service.NotificationManagementService;
import com.lexho.user_service.review.repository.ReviewRepository;
import com.lexho.user_service.util.LexhoConstants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

@Service
public class PartnerOperationsService {
    private static final String FIELD_REASON = "reason";
    private static final String FIELD_STATUS = "status";

    private final PartnerLeaveRepository leaveRepository;
    private final PartnerServiceAreaRepository serviceAreaRepository;
    private final PartnerPenaltyRepository penaltyRepository;
    private final PartnerWarningCommentRepository warningCommentRepository;
    private final PartnerOrderRepository orderRepository;
    private final PartnerOperationalProfileRepository profileRepository;
    private final PartnerShiftRepository shiftRepository;
    private final ReviewRepository reviewRepository;
    private final AuditLogService auditLogService;
    private final NotificationManagementService notificationManagementService;
    private final PartnerWarningRealtimeService partnerWarningRealtimeService;
    private final PartnerLocalizationService localizationService;
    private final DynamicContentTranslationService dynamicTranslationService;

    public PartnerOperationsService(PartnerLeaveRepository leaveRepository,
                                    PartnerServiceAreaRepository serviceAreaRepository,
                                    PartnerPenaltyRepository penaltyRepository,
                                    PartnerWarningCommentRepository warningCommentRepository,
                                    PartnerOrderRepository orderRepository,
                                    PartnerOperationalProfileRepository profileRepository,
                                    PartnerShiftRepository shiftRepository,
                                    ReviewRepository reviewRepository,
                                    AuditLogService auditLogService,
                                    NotificationManagementService notificationManagementService,
                                    PartnerWarningRealtimeService partnerWarningRealtimeService) {
        this(leaveRepository,
                serviceAreaRepository,
                penaltyRepository,
                warningCommentRepository,
                orderRepository,
                profileRepository,
                shiftRepository,
                reviewRepository,
                auditLogService,
                notificationManagementService,
                partnerWarningRealtimeService,
                null,
                Optional.empty());
    }

    public PartnerOperationsService(PartnerLeaveRepository leaveRepository,
                                    PartnerServiceAreaRepository serviceAreaRepository,
                                    PartnerPenaltyRepository penaltyRepository,
                                    PartnerWarningCommentRepository warningCommentRepository,
                                    PartnerOrderRepository orderRepository,
                                    PartnerOperationalProfileRepository profileRepository,
                                    PartnerShiftRepository shiftRepository,
                                    ReviewRepository reviewRepository,
                                    AuditLogService auditLogService,
                                    NotificationManagementService notificationManagementService,
                                    PartnerWarningRealtimeService partnerWarningRealtimeService,
                                    PartnerLocalizationService localizationService) {
        this(leaveRepository,
                serviceAreaRepository,
                penaltyRepository,
                warningCommentRepository,
                orderRepository,
                profileRepository,
                shiftRepository,
                reviewRepository,
                auditLogService,
                notificationManagementService,
                partnerWarningRealtimeService,
                localizationService,
                Optional.empty());
    }

    @Autowired
    public PartnerOperationsService(PartnerLeaveRepository leaveRepository,
                                    PartnerServiceAreaRepository serviceAreaRepository,
                                    PartnerPenaltyRepository penaltyRepository,
                                    PartnerWarningCommentRepository warningCommentRepository,
                                    PartnerOrderRepository orderRepository,
                                    PartnerOperationalProfileRepository profileRepository,
                                    PartnerShiftRepository shiftRepository,
                                    ReviewRepository reviewRepository,
                                    AuditLogService auditLogService,
                                    NotificationManagementService notificationManagementService,
                                    PartnerWarningRealtimeService partnerWarningRealtimeService,
                                    PartnerLocalizationService localizationService,
                                    Optional<DynamicContentTranslationService> dynamicTranslationService) {
        this.leaveRepository = leaveRepository;
        this.serviceAreaRepository = serviceAreaRepository;
        this.penaltyRepository = penaltyRepository;
        this.warningCommentRepository = warningCommentRepository;
        this.orderRepository = orderRepository;
        this.profileRepository = profileRepository;
        this.shiftRepository = shiftRepository;
        this.reviewRepository = reviewRepository;
        this.auditLogService = auditLogService;
        this.notificationManagementService = notificationManagementService;
        this.partnerWarningRealtimeService = partnerWarningRealtimeService;
        this.localizationService = localizationService;
        this.dynamicTranslationService = dynamicTranslationService.orElse(null);
    }

    @Transactional
    public Map<String, Object> createLeave(Long partnerId, PartnerLeaveRequest request) {
        if (request.toDate().isBefore(request.fromDate())) {
            throw new BadRequestException(PartnerServiceText.MSG_TO_DATE_AFTER_FROM_DATE);
        }
        PartnerLeave leave = new PartnerLeave();
        leave.setPartnerId(partnerId);
        leave.setFromDate(request.fromDate());
        leave.setToDate(request.toDate());
        leave.setReason(request.reason() == null ? null : request.reason().trim());
        leave.setStatus(PartnerServiceText.STATUS_REQUESTED);
        PartnerLeave saved = leaveRepository.save(leave);
        profileRepository.findById(partnerId).ifPresent(profile -> {
            profile.setOnline(false);
            profileRepository.save(profile);
        });
        shiftRepository.findByPartnerId(partnerId).ifPresent(shift -> {
            shift.setOnline(false);
            shiftRepository.save(shift);
        });
        return leaveMap(saved);
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> leaves(Long partnerId) {
        return leaveRepository.findByPartnerIdOrderByFromDateDesc(partnerId).stream().map(this::leaveMap).toList();
    }

    @Transactional
    public Map<String, Object> cancelLeave(Long partnerId, Long leaveId) {
        PartnerLeave leave = leaveRepository.findByIdAndPartnerId(leaveId, partnerId)
                .orElseThrow(() -> new ResourceNotFoundException(PartnerServiceText.MSG_LEAVE_NOT_FOUND));
        leave.setStatus(PartnerServiceText.STATUS_CANCELLED);
        return leaveMap(leaveRepository.save(leave));
    }

    @Transactional
    public List<Map<String, Object>> updateServiceAreas(Long partnerId, PartnerServiceAreaRequest request) {
        return replaceServiceAreas(partnerId, request.areas());
    }

    @Transactional
    public List<Map<String, Object>> updateServiceAreas(Long partnerId, List<PartnerServiceAreaRequest.ServiceAreaItem> areas) {
        return replaceServiceAreas(partnerId, areas);
    }

    private List<Map<String, Object>> replaceServiceAreas(Long partnerId,
                                                          List<PartnerServiceAreaRequest.ServiceAreaItem> areas) {
        serviceAreaRepository.deleteByPartnerId(partnerId);
        serviceAreaRepository.flush();
        List<PartnerServiceArea> saved = areas.stream().map(item -> {
            if (item.pincode() == null || item.pincode().isBlank()) {
                throw new BadRequestException(PartnerServiceText.MSG_PINCODE_REQUIRED);
            }
            PartnerServiceArea area = new PartnerServiceArea();
            area.setPartnerId(partnerId);
            area.setPincode(item.pincode().trim());
            area.setCity(item.city() == null ? null : item.city().trim());
            return area;
        }).map(serviceAreaRepository::save).toList();
        return saved.stream().map(this::areaMap).toList();
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> serviceAreas(Long partnerId) {
        return serviceAreaRepository.findByPartnerIdOrderByPincodeAsc(partnerId).stream().map(this::areaMap).toList();
    }

    @Transactional(readOnly = true)
    public Map<String, Object> performance(Long partnerId) {
        long assigned = orderRepository.countByAssignedPartnerId(partnerId);
        long accepted = orderRepository.countByAssignedPartnerIdAndStatus(partnerId, OrderStatus.ACCEPTED)
                + orderRepository.countByAssignedPartnerIdAndStatus(partnerId, OrderStatus.STARTED)
                + orderRepository.countByAssignedPartnerIdAndStatus(partnerId, OrderStatus.COMPLETED);
        long completed = orderRepository.countByAssignedPartnerIdAndStatus(partnerId, OrderStatus.COMPLETED);
        long rejected = orderRepository.countByAssignedPartnerIdAndStatus(partnerId, OrderStatus.REJECTED);
        double rating = profileRepository.findById(partnerId)
                .map(p -> p.getRating() == null ? 0.0 : p.getRating())
                .orElse(0.0);
        double acceptanceRate = percent(accepted, assigned);
        double completionRate = percent(completed, accepted);
        double cancellationRate = percent(rejected, assigned);
        double onTimeArrivalRate = 0.0;
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("acceptanceRate", acceptanceRate);
        response.put("completionRate", completionRate);
        response.put("cancellationRate", cancellationRate);
        response.put("averageRating", rating);
        response.put(LexhoConstants.FIELD_RATING, rating);
        response.put("onTimeArrivalRate", onTimeArrivalRate);
        response.put("onTimeArrivalPercentage", onTimeArrivalRate);
        return response;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> ratings(Long partnerId, int page, int size) {
        var result = reviewRepository.findByProviderIdAndDeletedFalse(
                partnerId,
                PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 100),
                        Sort.by(LexhoConstants.FIELD_CREATED_AT).descending())
        );
        List<Map<String, Object>> reviews = result.getContent().stream().map(review -> {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("id", review.getId());
            row.put("bookingId", review.getBookingId());
            row.put("customerName", review.getCustomerId() == null ? "Anonymous" : "Customer " + review.getCustomerId());
        row.put(LexhoConstants.FIELD_RATING, review.getRating());
            row.put("comment", review.getReviewComment());
        row.put(LexhoConstants.FIELD_CREATED_AT, review.getCreatedAt());
            return row;
        }).toList();
        double average = result.getTotalElements() == 0 ? 0.0
                : reviewRepository.findByProviderIdAndDeletedFalse(partnerId, PageRequest.of(0, 1000)).stream()
                .mapToInt(review -> review.getRating() == null ? 0 : review.getRating()).average().orElse(0.0);
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("averageRating", Math.round(average * 100.0) / 100.0);
        response.put(LexhoConstants.FIELD_RATING, Math.round(average * 100.0) / 100.0);
        response.put("totalReviews", result.getTotalElements());
        response.put("feedback", reviews);
        response.put("reviews", Map.of(
                "content", reviews,
                "page", result.getNumber(),
                "size", result.getSize(),
                "totalElements", result.getTotalElements(),
                "totalPages", result.getTotalPages(),
                "last", result.isLast()
        ));
        return response;
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> warnings(Long partnerId) {
        return penaltyRepository.findByPartnerIdOrderByCreatedAtDesc(partnerId).stream()
                .filter(penalty -> PartnerServiceText.STATUS_WARNING.equalsIgnoreCase(penalty.getStatus()))
                .map(this::warningMap)
                .toList();
    }

    @Transactional(readOnly = true)
    public Map<String, Object> warningDetail(Long partnerId, Long warningId) {
        return warningMap(requireWarning(partnerId, warningId));
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> penalties(Long partnerId) {
        return penaltyRepository.findByPartnerIdOrderByCreatedAtDesc(partnerId).stream().map(this::penaltyMap).toList();
    }

    @Transactional
    public Map<String, Object> addPenalty(Long partnerId, String status, String reason, Long adminId) {
        if (status == null || status.isBlank()) {
            throw new BadRequestException(PartnerServiceText.MSG_STATUS_REQUIRED);
        }
        String normalized = status.trim().toUpperCase();
        if (!java.util.Set.of(PartnerServiceText.STATUS_WARNING, PartnerServiceText.STATUS_SUSPENSION,
                PartnerServiceText.STATUS_BLOCKED).contains(normalized)) {
            throw new BadRequestException(PartnerServiceText.MSG_INVALID_PENALTY_STATUS);
        }
        PartnerPenalty penalty = new PartnerPenalty();
        penalty.setPartnerId(partnerId);
        penalty.setStatus(normalized);
        penalty.setReason(reason == null ? null : reason.trim());
        penalty.setCreatedByAdminId(adminId);
        PartnerPenalty saved = penaltyRepository.save(penalty);
        auditLogService.log(adminId, PartnerServiceText.AUDIT_ADMIN, PartnerServiceText.AUDIT_PENALTY_UPDATED,
                PartnerServiceText.AUDIT_PARTNER,
                partnerId.toString(), null, normalized);
        if (PartnerServiceText.STATUS_WARNING.equals(normalized)) {
            notificationManagementService.createPartnerWarningNotification(partnerId, saved.getId(), saved.getReason());
            publishWarningEvent(saved, PartnerServiceText.EVENT_WARNING_CREATED);
        }
        return penaltyMap(saved);
    }

    @Transactional
    public Map<String, Object> createWarning(Long partnerId, String reason, Long adminId, boolean sendNotification) {
        PartnerPenalty warning = saveWarning(partnerId, reason, adminId);
        if (sendNotification) {
            notificationManagementService.createPartnerWarningNotification(partnerId, warning.getId(), warning.getReason());
        }
        publishWarningEvent(warning, PartnerServiceText.EVENT_WARNING_CREATED);
        return warningMap(warning);
    }

    @Transactional
    public Map<String, Object> acknowledgeWarning(Long partnerId, Long warningId) {
        PartnerPenalty warning = requireWarning(partnerId, warningId);
        if (warning.getAcknowledgedAt() == null) {
            warning.setAcknowledgedAt(LocalDateTime.now());
            warning = penaltyRepository.save(warning);
            publishWarningEvent(warning, PartnerServiceText.EVENT_WARNING_ACKNOWLEDGED);
        }
        return warningMap(warning);
    }

    @Transactional
    public Map<String, Object> disputeWarning(Long partnerId,
                                              Long warningId,
                                              String disputeReason,
                                              List<String> attachmentUrls) {
        if (disputeReason == null || disputeReason.isBlank()) {
            throw new BadRequestException(PartnerServiceText.MSG_DISPUTE_REASON_REQUIRED);
        }
        PartnerPenalty warning = requireWarning(partnerId, warningId);
        String currentStatus = warning.getDisputeStatus();
        if (PartnerServiceText.STATUS_OPEN.equalsIgnoreCase(currentStatus)
                || PartnerServiceText.STATUS_UNDER_REVIEW.equalsIgnoreCase(currentStatus)) {
            throw new BadRequestException(PartnerServiceText.MSG_DISPUTE_ALREADY_ACTIVE);
        }
        warning.setDisputeStatus(PartnerServiceText.STATUS_OPEN);
        warning.setDisputeReason(disputeReason.trim());
        warning.setDisputedAt(LocalDateTime.now());
        warning = penaltyRepository.save(warning);
        addComment(warning, PartnerServiceText.SENDER_PARTNER, partnerId, disputeReason.trim(), attachmentUrls);
        auditLogService.log(partnerId, PartnerServiceText.AUDIT_PARTNER, PartnerServiceText.AUDIT_WARNING_DISPUTED,
                PartnerServiceText.AUDIT_PARTNER_WARNING,
                String.valueOf(warningId), null, warning.getDisputeStatus());
        publishWarningEvent(warning, PartnerServiceText.EVENT_WARNING_DISPUTED);
        return warningMap(warning);
    }

    @Transactional
    public Map<String, Object> addWarningComment(Long partnerId, Long warningId, PartnerWarningCommentRequest request) {
        PartnerPenalty warning = requireWarning(partnerId, warningId);
        String message = normalizeCommentMessage(request.message(), request.attachmentUrls());
        addComment(warning, PartnerServiceText.SENDER_PARTNER, partnerId, message, request.attachmentUrls());
        publishWarningEvent(warning, PartnerServiceText.EVENT_WARNING_COMMENT);
        return warningMap(warning);
    }

    public SseEmitter warningStream(Long partnerId, Long warningId) {
        PartnerPenalty warning = requireWarning(partnerId, warningId);
        return partnerWarningRealtimeService.subscribe(partnerId, warning.getId());
    }

    private PartnerPenalty saveWarning(Long partnerId, String reason, Long adminId) {
        PartnerPenalty warning = new PartnerPenalty();
        warning.setPartnerId(partnerId);
        warning.setStatus(PartnerServiceText.STATUS_WARNING);
        warning.setReason(reason == null ? null : reason.trim());
        warning.setCreatedByAdminId(adminId);
        PartnerPenalty saved = penaltyRepository.save(warning);
        auditLogService.log(adminId, PartnerServiceText.AUDIT_ADMIN, PartnerServiceText.AUDIT_WARNING_CREATED,
                PartnerServiceText.AUDIT_PARTNER, partnerId.toString(), null, PartnerServiceText.STATUS_WARNING);
        return saved;
    }

    private double percent(long numerator, long denominator) {
        if (denominator <= 0) {
            return 0.0;
        }
        return Math.round((numerator * 10000.0 / denominator)) / 100.0;
    }

    private Map<String, Object> leaveMap(PartnerLeave leave) {
        String status = leave.getStatus();
        if (status == null || status.isBlank()) {
            status = deriveLeaveStatus(leave);
        }
        return Map.of("id", leave.getId(), "leaveId", leave.getId(), "fromDate", leave.getFromDate(), "toDate", leave.getToDate(),
                FIELD_REASON, leave.getReason() == null ? "" : leave.getReason(), FIELD_STATUS, status);
    }

    private Map<String, Object> areaMap(PartnerServiceArea area) {
        String city = area.getCity() == null ? "" : area.getCity();
        String name = city.isBlank() ? area.getPincode() : city + " - " + area.getPincode();
        return Map.of("id", area.getId(), "name", name, "pincode", area.getPincode(), "city", city, "selected", true);
    }

    private Map<String, Object> penaltyMap(PartnerPenalty penalty) {
        return Map.of("id", penalty.getId(), FIELD_STATUS, penalty.getStatus(),
                FIELD_REASON, penalty.getReason() == null ? "" : penalty.getReason(),
                LexhoConstants.FIELD_CREATED_AT, penalty.getCreatedAt());
    }

    private Map<String, Object> warningMap(PartnerPenalty penalty) {
        String reason = translatedWarningText(
                penalty.getPartnerId(), penalty.getId(), FIELD_REASON, penalty.getReason() == null ? "" : penalty.getReason());
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("id", penalty.getId());
        row.put("warningId", penalty.getId());
        row.put("type", PartnerServiceText.STATUS_WARNING);
        row.put("category", PartnerServiceText.STATUS_WARNING);
        row.put(FIELD_REASON, reason);
        row.put("message", reason);
        row.put("title", text("warning.title", PartnerServiceText.MSG_WARNING_TITLE));
        row.put("guidance", guidanceFor(penalty.getStatus()));
        row.put("resolutionGuidance", guidanceFor(penalty.getStatus()));
        row.put(FIELD_STATUS, penalty.getStatus());
        row.put("warningStatus", penalty.getStatus());
        row.put("active", !PartnerServiceText.STATUS_RESOLVED.equalsIgnoreCase(penalty.getStatus()));
        row.put("acknowledged", penalty.getAcknowledgedAt() != null);
        row.put("acknowledgedAt", penalty.getAcknowledgedAt());
        row.put("disputeStatus", defaultDisputeStatus(penalty.getDisputeStatus()));
        row.put("disputeReason", translatedWarningText(
                penalty.getPartnerId(), penalty.getId(), "dispute_reason",
                penalty.getDisputeReason() == null ? "" : penalty.getDisputeReason()));
        row.put("disputedAt", penalty.getDisputedAt());
        row.put("disputeReviewNote", penalty.getDisputeReviewNote() == null ? "" : penalty.getDisputeReviewNote());
        row.put("disputeReviewedByAdminId", penalty.getDisputeReviewedByAdminId());
        row.put("disputeReviewedAt", penalty.getDisputeReviewedAt());
        List<Map<String, Object>> comments = warningCommentRepository.findByWarning_IdOrderByCreatedAtAsc(penalty.getId())
                .stream()
                .map(this::warningCommentMap)
                .toList();
        row.put("comments", comments);
        row.put("commentCount", comments.size());
        row.put("latestCommentAt",
                comments.isEmpty() ? null : comments.get(comments.size() - 1).get(LexhoConstants.FIELD_CREATED_AT));
        row.put("notificationTitle", text("notification.warning.title",
                PartnerServiceText.MSG_WARNING_NOTIFICATION_TITLE));
        row.put("notificationMessage", text("notification.warning.message",
                PartnerServiceText.MSG_WARNING_ACCOUNT));
        row.put("redirectType", PartnerServiceText.STATUS_WARNING);
        row.put("redirectValue", String.valueOf(penalty.getId()));
        row.put(LexhoConstants.FIELD_CREATED_AT, penalty.getCreatedAt());
        return row;
    }

    private String translatedWarningText(Long partnerId, Long warningId, String fieldName, String englishText) {
        if (dynamicTranslationService == null || englishText == null || englishText.isBlank()) {
            return englishText;
        }
        return dynamicTranslationService.translateForPartner(
                partnerId, null, "partner_warning", warningId, fieldName, englishText);
    }

    private Map<String, Object> warningCommentMap(PartnerWarningComment comment) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("id", comment.getId());
        row.put("senderType", comment.getSenderType());
        row.put("senderId", comment.getSenderId());
        row.put("message", comment.getMessage());
        row.put("attachmentUrls", PartnerAttachmentUrlSupport.deserializeAttachmentUrls(comment.getAttachmentUrls()));
        row.put(LexhoConstants.FIELD_CREATED_AT, comment.getCreatedAt());
        return row;
    }

    private String deriveLeaveStatus(PartnerLeave leave) {
        LocalDate today = LocalDate.now();
        if (leave.getToDate() != null && leave.getToDate().isBefore(today)) {
            return PartnerServiceText.STATUS_COMPLETED;
        }
        if (leave.getFromDate() != null && leave.getFromDate().isAfter(today)) {
            return PartnerServiceText.STATUS_SCHEDULED;
        }
        return PartnerServiceText.STATUS_ACTIVE;
    }

    private String guidanceFor(String status) {
        if (PartnerServiceText.STATUS_BLOCKED.equalsIgnoreCase(status)
                || PartnerServiceText.STATUS_SUSPENSION.equalsIgnoreCase(status)) {
            return text("warning.guidance.blocked", PartnerServiceText.MSG_WARNING_WORK_GUIDANCE);
        }
        if (PartnerServiceText.STATUS_WARNING.equalsIgnoreCase(status)) {
            return text("warning.guidance.warning", PartnerServiceText.MSG_REVIEW_ISSUE_GUIDANCE);
        }
        return text("warning.guidance.default", PartnerServiceText.MSG_FOLLOW_ADMIN_GUIDANCE);
    }

    private PartnerPenalty requireWarning(Long partnerId, Long warningId) {
        PartnerPenalty warning = penaltyRepository.findByIdAndPartnerId(warningId, partnerId)
                .orElseThrow(() -> new ResourceNotFoundException(PartnerServiceText.MSG_WARNING_NOT_FOUND));
        if (!Objects.equals(PartnerServiceText.STATUS_WARNING,
                warning.getStatus() == null ? null : warning.getStatus().toUpperCase())) {
            throw new ResourceNotFoundException(PartnerServiceText.MSG_WARNING_NOT_FOUND);
        }
        return warning;
    }

    private String defaultDisputeStatus(String disputeStatus) {
        return disputeStatus == null || disputeStatus.isBlank() ? PartnerServiceText.STATUS_NONE : disputeStatus;
    }

    private void addComment(PartnerPenalty warning,
                            String senderType,
                            Long senderId,
                            String message,
                            List<String> attachmentUrls) {
        PartnerWarningComment comment = new PartnerWarningComment();
        comment.setWarning(warning);
        comment.setSenderType(senderType);
        comment.setSenderId(senderId);
        comment.setMessage(message);
        comment.setAttachmentUrls(PartnerAttachmentUrlSupport.serializeAttachmentUrls(attachmentUrls));
        warningCommentRepository.save(comment);
    }

    private void publishWarningEvent(PartnerPenalty warning, String eventName) {
        partnerWarningRealtimeService.publish(warning.getPartnerId(), warning.getId(), eventName, warningMap(warning));
    }

    private String normalizeCommentMessage(String message, List<String> attachmentUrls) {
        String normalized = message == null ? "" : message.trim();
        if (normalized.isEmpty() && (attachmentUrls == null || attachmentUrls.isEmpty())) {
            throw new BadRequestException(PartnerServiceText.MSG_COMMENT_OR_ATTACHMENTS_REQUIRED);
        }
        return normalized.isEmpty() ? PartnerServiceText.MSG_ATTACHMENT_ADDED : normalized;
    }

    private String text(String key, String fallback) {
        if (localizationService == null) {
            return fallback;
        }
        String translated = localizationService.translate(key, fallback);
        return translated == null || translated.isBlank() ? fallback : translated;
    }
}

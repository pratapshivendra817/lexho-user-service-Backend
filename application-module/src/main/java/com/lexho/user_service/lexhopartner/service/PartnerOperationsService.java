package com.lexho.user_service.lexhopartner.service;

import com.lexho.user_service.dispatch.repository.PartnerShiftRepository;
import com.lexho.user_service.audit.service.AuditLogService;
import com.lexho.user_service.exception.BadRequestException;
import com.lexho.user_service.exception.ResourceNotFoundException;
import com.lexho.user_service.lexhopartner.dto.PartnerLeaveRequest;
import com.lexho.user_service.lexhopartner.dto.PartnerServiceAreaRequest;
import com.lexho.user_service.lexhopartner.entity.OrderStatus;
import com.lexho.user_service.lexhopartner.entity.PartnerLeave;
import com.lexho.user_service.lexhopartner.entity.PartnerPenalty;
import com.lexho.user_service.lexhopartner.entity.PartnerServiceArea;
import com.lexho.user_service.lexhopartner.repository.PartnerLeaveRepository;
import com.lexho.user_service.lexhopartner.repository.PartnerOrderRepository;
import com.lexho.user_service.lexhopartner.repository.PartnerPenaltyRepository;
import com.lexho.user_service.lexhopartner.repository.PartnerProfileRepository;
import com.lexho.user_service.lexhopartner.repository.PartnerServiceAreaRepository;
import com.lexho.user_service.notification.service.NotificationManagementService;
import com.lexho.user_service.review.repository.ReviewRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class PartnerOperationsService {

    private final PartnerLeaveRepository leaveRepository;
    private final PartnerServiceAreaRepository serviceAreaRepository;
    private final PartnerPenaltyRepository penaltyRepository;
    private final PartnerOrderRepository orderRepository;
    private final PartnerProfileRepository profileRepository;
    private final PartnerShiftRepository shiftRepository;
    private final ReviewRepository reviewRepository;
    private final AuditLogService auditLogService;
    private final NotificationManagementService notificationManagementService;

    public PartnerOperationsService(PartnerLeaveRepository leaveRepository,
                                    PartnerServiceAreaRepository serviceAreaRepository,
                                    PartnerPenaltyRepository penaltyRepository,
                                    PartnerOrderRepository orderRepository,
                                    PartnerProfileRepository profileRepository,
                                    PartnerShiftRepository shiftRepository,
                                    ReviewRepository reviewRepository,
                                    AuditLogService auditLogService,
                                    NotificationManagementService notificationManagementService) {
        this.leaveRepository = leaveRepository;
        this.serviceAreaRepository = serviceAreaRepository;
        this.penaltyRepository = penaltyRepository;
        this.orderRepository = orderRepository;
        this.profileRepository = profileRepository;
        this.shiftRepository = shiftRepository;
        this.reviewRepository = reviewRepository;
        this.auditLogService = auditLogService;
        this.notificationManagementService = notificationManagementService;
    }

    @Transactional
    public Map<String, Object> createLeave(Long partnerId, PartnerLeaveRequest request) {
        if (request.toDate().isBefore(request.fromDate())) {
            throw new BadRequestException("toDate must be greater than or equal to fromDate");
        }
        PartnerLeave leave = new PartnerLeave();
        leave.setPartnerId(partnerId);
        leave.setFromDate(request.fromDate());
        leave.setToDate(request.toDate());
        leave.setReason(request.reason() == null ? null : request.reason().trim());
        leave.setStatus("REQUESTED");
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
                .orElseThrow(() -> new ResourceNotFoundException("Leave not found"));
        leave.setStatus("CANCELLED");
        return leaveMap(leaveRepository.save(leave));
    }

    @Transactional
    public List<Map<String, Object>> updateServiceAreas(Long partnerId, PartnerServiceAreaRequest request) {
        return updateServiceAreas(partnerId, request.areas());
    }

    @Transactional
    public List<Map<String, Object>> updateServiceAreas(Long partnerId, List<PartnerServiceAreaRequest.ServiceAreaItem> areas) {
        serviceAreaRepository.deleteByPartnerId(partnerId);
        serviceAreaRepository.flush();
        List<PartnerServiceArea> saved = areas.stream().map(item -> {
            if (item.pincode() == null || item.pincode().isBlank()) {
                throw new BadRequestException("pincode is required");
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
        response.put("rating", rating);
        response.put("onTimeArrivalRate", onTimeArrivalRate);
        response.put("onTimeArrivalPercentage", onTimeArrivalRate);
        return response;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> ratings(Long partnerId, int page, int size) {
        var result = reviewRepository.findByProviderIdAndDeletedFalse(
                partnerId,
                PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 100), Sort.by("createdAt").descending())
        );
        List<Map<String, Object>> reviews = result.getContent().stream().map(review -> {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("id", review.getId());
            row.put("bookingId", review.getBookingId());
            row.put("customerName", review.getCustomerId() == null ? "Anonymous" : "Customer " + review.getCustomerId());
            row.put("rating", review.getRating());
            row.put("comment", review.getReviewComment());
            row.put("createdAt", review.getCreatedAt());
            return row;
        }).toList();
        double average = result.getTotalElements() == 0 ? 0.0
                : reviewRepository.findByProviderIdAndDeletedFalse(partnerId, PageRequest.of(0, 1000)).stream()
                .mapToInt(review -> review.getRating() == null ? 0 : review.getRating()).average().orElse(0.0);
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("averageRating", Math.round(average * 100.0) / 100.0);
        response.put("rating", Math.round(average * 100.0) / 100.0);
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
                .filter(penalty -> "WARNING".equalsIgnoreCase(penalty.getStatus()))
                .map(this::warningMap)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> penalties(Long partnerId) {
        return penaltyRepository.findByPartnerIdOrderByCreatedAtDesc(partnerId).stream().map(this::penaltyMap).toList();
    }

    @Transactional
    public Map<String, Object> addPenalty(Long partnerId, String status, String reason, Long adminId) {
        if (status == null || status.isBlank()) {
            throw new BadRequestException("status is required");
        }
        String normalized = status.trim().toUpperCase();
        if (!java.util.Set.of("WARNING", "SUSPENSION", "BLOCKED").contains(normalized)) {
            throw new BadRequestException("Invalid penalty status");
        }
        PartnerPenalty penalty = new PartnerPenalty();
        penalty.setPartnerId(partnerId);
        penalty.setStatus(normalized);
        penalty.setReason(reason == null ? null : reason.trim());
        penalty.setCreatedByAdminId(adminId);
        PartnerPenalty saved = penaltyRepository.save(penalty);
        auditLogService.log(adminId, "ADMIN", "PENALTY_UPDATED", "PARTNER",
                partnerId.toString(), null, normalized);
        if ("WARNING".equals(normalized)) {
            notificationManagementService.createPartnerWarningNotification(partnerId, saved.getId(), saved.getReason());
        }
        return penaltyMap(saved);
    }

    @Transactional
    public Map<String, Object> createWarning(Long partnerId, String reason, Long adminId, boolean sendNotification) {
        PartnerPenalty warning = saveWarning(partnerId, reason, adminId);
        if (sendNotification) {
            notificationManagementService.createPartnerWarningNotification(partnerId, warning.getId(), warning.getReason());
        }
        return warningMap(warning);
    }

    private PartnerPenalty saveWarning(Long partnerId, String reason, Long adminId) {
        PartnerPenalty warning = new PartnerPenalty();
        warning.setPartnerId(partnerId);
        warning.setStatus("WARNING");
        warning.setReason(reason == null ? null : reason.trim());
        warning.setCreatedByAdminId(adminId);
        PartnerPenalty saved = penaltyRepository.save(warning);
        auditLogService.log(adminId, "ADMIN", "WARNING_CREATED", "PARTNER",
                partnerId.toString(), null, "WARNING");
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
                "reason", leave.getReason() == null ? "" : leave.getReason(), "status", status);
    }

    private Map<String, Object> areaMap(PartnerServiceArea area) {
        String city = area.getCity() == null ? "" : area.getCity();
        String name = city.isBlank() ? area.getPincode() : city + " - " + area.getPincode();
        return Map.of("id", area.getId(), "name", name, "pincode", area.getPincode(), "city", city, "selected", true);
    }

    private Map<String, Object> penaltyMap(PartnerPenalty penalty) {
        return Map.of("id", penalty.getId(), "status", penalty.getStatus(),
                "reason", penalty.getReason() == null ? "" : penalty.getReason(),
                "createdAt", penalty.getCreatedAt());
    }

    private Map<String, Object> warningMap(PartnerPenalty penalty) {
        String reason = penalty.getReason() == null ? "" : penalty.getReason();
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("id", penalty.getId());
        row.put("warningId", penalty.getId());
        row.put("type", "WARNING");
        row.put("category", "WARNING");
        row.put("reason", reason);
        row.put("message", reason);
        row.put("title", "WARNING");
        row.put("guidance", guidanceFor(penalty.getStatus()));
        row.put("resolutionGuidance", guidanceFor(penalty.getStatus()));
        row.put("status", penalty.getStatus());
        row.put("warningStatus", penalty.getStatus());
        row.put("active", !"RESOLVED".equalsIgnoreCase(penalty.getStatus()));
        row.put("notificationTitle", "New Warning Issued");
        row.put("notificationMessage", "A new warning has been issued to your account. Tap to view the details.");
        row.put("redirectType", "WARNING");
        row.put("redirectValue", String.valueOf(penalty.getId()));
        row.put("createdAt", penalty.getCreatedAt());
        return row;
    }

    private String deriveLeaveStatus(PartnerLeave leave) {
        LocalDate today = LocalDate.now();
        if (leave.getToDate() != null && leave.getToDate().isBefore(today)) {
            return "COMPLETED";
        }
        if (leave.getFromDate() != null && leave.getFromDate().isAfter(today)) {
            return "SCHEDULED";
        }
        return "ACTIVE";
    }

    private String guidanceFor(String status) {
        if ("BLOCKED".equalsIgnoreCase(status) || "SUSPENSION".equalsIgnoreCase(status)) {
            return "Contact support or admin before taking new jobs.";
        }
        if ("WARNING".equalsIgnoreCase(status)) {
            return "Review the issue and avoid repeating it on future jobs.";
        }
        return "Follow admin guidance.";
    }
}

package com.lexho.user_service.lexhopartner.service;

import com.lexho.user_service.admin.entity.Payout;
import com.lexho.user_service.admin.repository.PayoutRepository;
import com.lexho.provider.repository.PartnerRepository;
import com.lexho.user_service.audit.service.AuditLogService;
import com.lexho.user_service.dispatch.dto.PartnerSkillsUpdateRequest;
import com.lexho.user_service.dispatch.repository.PartnerShiftRepository;
import com.lexho.user_service.dispatch.service.PartnerSkillService;
import com.lexho.user_service.exception.BadRequestException;
import com.lexho.user_service.exception.ResourceNotFoundException;
import com.lexho.user_service.lexhopartner.entity.PartnerPenalty;
import com.lexho.user_service.lexhopartner.entity.PartnerOperationalProfile;
import com.lexho.user_service.lexhopartner.entity.PartnerWarningComment;
import com.lexho.user_service.lexhopartner.repository.PartnerPenaltyRepository;
import com.lexho.user_service.lexhopartner.repository.PartnerOperationalProfileRepository;
import com.lexho.user_service.lexhopartner.repository.PartnerWarningCommentRepository;
import com.lexho.user_service.lexhopartner.entity.PartnerWithdrawalRequest;
import com.lexho.user_service.lexhopartner.repository.PartnerWithdrawalRequestRepository;
import com.lexho.user_service.notification.service.NotificationManagementService;
import com.lexho.user_service.util.LexhoConstants;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Map;

@Service
public class AdminPartnerGovernanceService {
    private static final String FIELD_ITEMS = "items";
    private static final String FIELD_TOTAL_ELEMENTS = "totalElements";
    private static final String FIELD_TOTAL_PAGES = "totalPages";
    private static final String FIELD_PARTNER_ID = "partnerId";
    private static final String FIELD_STATUS = "status";
    private static final String STATUS_PENDING = "PENDING";
    private static final String STATUS_FAILED = "FAILED";
    private static final String STATUS_APPROVED = "APPROVED";
    private static final String STATUS_REJECTED = "REJECTED";
    private static final String STATUS_UNDER_REVIEW = "UNDER_REVIEW";
    private static final String PARTNER_NOT_FOUND = "Partner not found";
    private static final String TARGET_PARTNER = "PARTNER";

    private final PartnerOperationalProfileRepository partnerProfileRepository;
    private final PartnerShiftRepository partnerShiftRepository;
    private final PartnerSkillService partnerSkillService;
    private final PayoutRepository payoutRepository;
    private final PartnerWithdrawalRequestRepository withdrawalRepository;
    private final PartnerPenaltyRepository partnerPenaltyRepository;
    private final PartnerWarningCommentRepository partnerWarningCommentRepository;
    private final AuditLogService auditLogService;
    private final NotificationManagementService notificationManagementService;
    private final PartnerWarningRealtimeService partnerWarningRealtimeService;
    private PartnerOperationalStatusTransitionService statusTransitionService;
    private PartnerRepository legacyPartnerRepository;

    @org.springframework.beans.factory.annotation.Autowired
    public void setLegacyPartnerRepository(PartnerRepository legacyPartnerRepository) {
        this.legacyPartnerRepository = legacyPartnerRepository;
    }

    @org.springframework.beans.factory.annotation.Autowired
    public void setStatusTransitionService(PartnerOperationalStatusTransitionService statusTransitionService) {
        this.statusTransitionService = statusTransitionService;
    }

    public AdminPartnerGovernanceService(PartnerOperationalProfileRepository partnerProfileRepository,
                                         PartnerShiftRepository partnerShiftRepository,
                                         PartnerSkillService partnerSkillService,
                                         PayoutRepository payoutRepository,
                                         PartnerWithdrawalRequestRepository withdrawalRepository,
                                         PartnerPenaltyRepository partnerPenaltyRepository,
                                         AuditLogService auditLogService,
                                         PartnerWarningCommentRepository partnerWarningCommentRepository,
                                         NotificationManagementService notificationManagementService,
                                         PartnerWarningRealtimeService partnerWarningRealtimeService) {
        this.partnerProfileRepository = partnerProfileRepository;
        this.partnerShiftRepository = partnerShiftRepository;
        this.partnerSkillService = partnerSkillService;
        this.payoutRepository = payoutRepository;
        this.withdrawalRepository = withdrawalRepository;
        this.partnerPenaltyRepository = partnerPenaltyRepository;
        this.partnerWarningCommentRepository = partnerWarningCommentRepository;
        this.auditLogService = auditLogService;
        this.notificationManagementService = notificationManagementService;
        this.partnerWarningRealtimeService = partnerWarningRealtimeService;
        this.statusTransitionService = new PartnerOperationalStatusTransitionService();
    }

    @Transactional(readOnly = true)
    public Map<String, Object> settlements(String search, String status, int page, int size) {
        var pageable = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 100));
        var result = payoutRepository.search(search == null || search.isBlank() ? null : search.trim(),
                status == null || status.isBlank() ? null : status.trim(), pageable);
        return Map.of(
                FIELD_ITEMS, result.getContent().stream().map(this::toPayoutRow).toList(),
                "page", result.getNumber(),
                "size", result.getSize(),
                FIELD_TOTAL_ELEMENTS, result.getTotalElements(),
                FIELD_TOTAL_PAGES, result.getTotalPages()
        );
    }

    @Transactional
    public Map<String, Object> updateSettlementStatus(Long payoutId, String status, String remarks) {
        if (status == null || status.isBlank()) {
            throw new BadRequestException("status is required");
        }
        String normalized = status.trim().toUpperCase();
        if (!java.util.Set.of(STATUS_PENDING, "PROCESSING", "SETTLED", STATUS_FAILED, "REVERSED",
                "PARTIALLY_REVERSED").contains(normalized)) {
            throw new BadRequestException("Invalid settlement status");
        }
        Payout payout = payoutRepository.findById(payoutId).orElseThrow(() -> new ResourceNotFoundException("Payout not found"));
        payout.setStatus(normalized);
        payout.setRemarks(remarks == null ? null : remarks.trim());
        payout.setProcessedAt(LocalDateTime.now());
        return toPayoutRow(payoutRepository.save(payout));
    }

    @Transactional(readOnly = true)
    public Map<String, Object> withdrawals(int page, int size) {
        var result = withdrawalRepository.findAll(
                PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 100)));
        return Map.of(
                FIELD_ITEMS, result.getContent().stream().map(this::toWithdrawalRow).toList(),
                "page", result.getNumber(),
                "size", result.getSize(),
                FIELD_TOTAL_ELEMENTS, result.getTotalElements(),
                FIELD_TOTAL_PAGES, result.getTotalPages()
        );
    }

    @Transactional(readOnly = true)
    public Map<String, Object> warningDisputes(String disputeStatus, int page, int size) {
        var result = partnerPenaltyRepository.findWarningDisputes(
                disputeStatus == null || disputeStatus.isBlank() ? null : disputeStatus.trim(),
                PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 100))
        );
        return Map.of(
                FIELD_ITEMS, result.getContent().stream().map(this::toWarningDisputeRow).toList(),
                "page", result.getNumber(),
                "size", result.getSize(),
                FIELD_TOTAL_ELEMENTS, result.getTotalElements(),
                FIELD_TOTAL_PAGES, result.getTotalPages()
        );
    }

    @Transactional
    public Map<String, Object> reviewWarningDispute(Long partnerId,
                                                    Long warningId,
                                                    String status,
                                                    String reviewNote,
                                                    Long adminId) {
        String normalized = requireStatus(status, java.util.Set.of(STATUS_UNDER_REVIEW, STATUS_APPROVED, STATUS_REJECTED));
        PartnerPenalty warning = partnerPenaltyRepository.findByIdAndPartnerId(warningId, partnerId)
                .orElseThrow(() -> new ResourceNotFoundException("Warning not found"));
        if (!"WARNING".equalsIgnoreCase(warning.getStatus())) {
            throw new ResourceNotFoundException("Warning not found");
        }
        if (warning.getDisputeStatus() == null || warning.getDisputeStatus().isBlank() || "NONE".equalsIgnoreCase(warning.getDisputeStatus())) {
            throw new BadRequestException("No active dispute found for this warning");
        }
        warning.setDisputeStatus(normalized);
        warning.setDisputeReviewNote(trim(reviewNote));
        warning.setDisputeReviewedByAdminId(adminId);
        warning.setDisputeReviewedAt(LocalDateTime.now());
        if (STATUS_APPROVED.equals(normalized)) {
            warning.setStatus("RESOLVED");
        }
        PartnerPenalty saved = partnerPenaltyRepository.save(warning);
        PartnerWarningComment comment = new PartnerWarningComment();
        comment.setWarning(saved);
        comment.setSenderType(LexhoConstants.ACTOR_ADMIN);
        comment.setSenderId(adminId);
        comment.setMessage(trim(reviewNote) == null ? "Dispute status updated to " + normalized : trim(reviewNote));
        partnerWarningCommentRepository.save(comment);
        auditLogService.log(adminId, LexhoConstants.ACTOR_ADMIN, "WARNING_DISPUTE_" + normalized, "PARTNER_WARNING",
                warningId.toString(), null, reviewNote);
        notificationManagementService.createPartnerWarningDisputeStatusNotification(partnerId, warningId, normalized);
        partnerWarningRealtimeService.publish(partnerId, warningId, "warning-review", toWarningDisputeRow(saved));
        return toWarningDisputeRow(saved);
    }

    @Transactional
    public Map<String, Object> updateWithdrawalStatus(Long withdrawalId, String status, String remarks) {
        if (status == null || status.isBlank()) {
            throw new BadRequestException("status is required");
        }
        String normalized = status.trim().toUpperCase();
        if (!java.util.Set.of("REQUESTED", STATUS_APPROVED, STATUS_REJECTED, "PROCESSING", "PAID", STATUS_FAILED)
                .contains(normalized)) {
            throw new BadRequestException("Invalid withdrawal status");
        }
        PartnerWithdrawalRequest request = withdrawalRepository.findById(withdrawalId)
                .orElseThrow(() -> new ResourceNotFoundException("Withdrawal request not found"));
        request.setStatus(normalized);
        request.setRemarks(remarks == null ? null : remarks.trim());
        if (java.util.Set.of(STATUS_REJECTED, "PAID", STATUS_FAILED).contains(normalized)) {
            request.setProcessedAt(LocalDateTime.now());
        }
        return toWithdrawalRow(withdrawalRepository.save(request));
    }

    @Transactional
    public Map<String, Object> reviewBank(Long partnerId, String status, String reason) {
        PartnerOperationalProfile partner = partnerProfileRepository.findById(partnerId)
                .orElseThrow(() -> new ResourceNotFoundException(PARTNER_NOT_FOUND));
        String normalized = requireStatus(status, java.util.Set.of(STATUS_PENDING, "VERIFIED", STATUS_REJECTED));
        partner.setBankVerificationStatus(normalized);
        partner.setBankRejectionReason(STATUS_REJECTED.equals(normalized) ? trim(reason) : null);
        partnerProfileRepository.save(partner);
        auditLogService.log(null, LexhoConstants.ACTOR_ADMIN, "BANK_" + normalized, TARGET_PARTNER,
                partnerId.toString(), null, reason);
        Map<String, Object> response = new java.util.LinkedHashMap<>();
        response.put(FIELD_PARTNER_ID, partnerId);
        response.put("bankVerificationStatus", partner.getBankVerificationStatus());
        response.put("rejectionReason", partner.getBankRejectionReason());
        return response;
    }

    @Transactional
    public Map<String, Object> forceAvailability(Long partnerId, boolean online, Integer lockMinutes, String reason) {
        PartnerOperationalProfile partner = partnerProfileRepository.findById(partnerId)
                .orElseThrow(() -> new ResourceNotFoundException(PARTNER_NOT_FOUND));
        if (online
                && (partner.getPartnerOperationalStatus() == com.lexho.user_service.lexhopartner.model.PartnerOperationalStatus.BLOCKED
                || partner.getPartnerOperationalStatus() == com.lexho.user_service.lexhopartner.model.PartnerOperationalStatus.REJECTED)) {
            throw new BadRequestException("Blocked or rejected partners cannot be activated");
        }
        statusTransitionService.setAvailability(partner, online);
        partner.setAvailabilityLockReason(reason == null ? null : reason.trim());
        if (lockMinutes != null && lockMinutes > 0) {
            partner.setAvailabilityLockedUntil(LocalDateTime.now().plusMinutes(lockMinutes));
        } else {
            partner.setAvailabilityLockedUntil(null);
        }
        partnerProfileRepository.save(partner);
        syncLegacyMarketplace(partner);
        auditLogService.log(null, LexhoConstants.ACTOR_ADMIN, "PARTNER_STATUS_CHANGED", TARGET_PARTNER,
                partnerId.toString(), null, online ? "ONLINE" : "OFFLINE");
        partnerShiftRepository.findByPartnerId(partnerId).ifPresent(shift -> {
            shift.setOnline(online);
        shift.setUpdatedBy(LexhoConstants.ACTOR_ADMIN);
            partnerShiftRepository.save(shift);
        });
        return Map.of(
                FIELD_PARTNER_ID, partnerId,
                "online", partner.isOnline(),
                "availabilityLockedUntil", partner.getAvailabilityLockedUntil(),
                "availabilityLockReason", partner.getAvailabilityLockReason()
        );
    }

    @Transactional
    public Map<String, Object> governSkills(Long partnerId, PartnerSkillsUpdateRequest request) {
        return partnerSkillService.upsertSkills(partnerId, request);
    }

    private void syncLegacyMarketplace(PartnerOperationalProfile profile) {
        if (legacyPartnerRepository == null) {
            return;
        }
        legacyPartnerRepository.findById(profile.getId()).ifPresent(legacy -> {
            legacy.setName(profile.getFullName());
            legacy.setAvailable(profile.isOnline());
            legacy.setVerificationStatus(profile.isVerified()
                    ? "VERIFIED"
                    : profile.getPartnerOperationalStatus().name());
            legacyPartnerRepository.save(legacy);
        });
    }

    @Transactional
    public Map<String, Object> riskAction(Long partnerId, String action, String reason) {
        PartnerOperationalProfile partner = partnerProfileRepository.findById(partnerId)
                .orElseThrow(() -> new ResourceNotFoundException(PARTNER_NOT_FOUND));
        if (action == null || action.isBlank()) {
            throw new BadRequestException("action is required");
        }
        String normalized = action.trim().toUpperCase();
        switch (normalized) {
            case "SOFT_BLOCK" -> {
                partner.setOnline(false);
                partner.setRiskStatus("SOFT_BLOCKED");
            }
            case "HARD_BLOCK" -> {
                partner.setOnline(false);
                partner.setRiskStatus("HARD_BLOCKED");
                partner.setFraudFlag(true);
            }
            case "UNBLOCK" -> {
                partner.setRiskStatus("NORMAL");
                partner.setFraudFlag(false);
                partner.setAvailabilityLockedUntil(null);
                partner.setAvailabilityLockReason(null);
            }
            case "FLAG_FRAUD" -> {
                partner.setFraudFlag(true);
                partner.setRiskStatus(STATUS_UNDER_REVIEW);
            }
            default -> throw new BadRequestException("Unsupported action");
        }
        partner.setAdminNotes(reason == null ? null : reason.trim());
        partnerProfileRepository.save(partner);
        syncLegacyMarketplace(partner);
        auditLogService.log(null, LexhoConstants.ACTOR_ADMIN, "PARTNER_STATUS_CHANGED", TARGET_PARTNER,
                partnerId.toString(), null, normalized);
        return Map.of(
                FIELD_PARTNER_ID, partnerId,
                "action", normalized,
                "riskStatus", partner.getRiskStatus(),
                "fraudFlag", partner.isFraudFlag(),
                "online", partner.isOnline(),
                "adminNotes", partner.getAdminNotes()
        );
    }

    private Map<String, Object> toPayoutRow(Payout payout) {
        Map<String, Object> row = new java.util.LinkedHashMap<>();
        row.put("id", payout.getId());
        row.put("providerId", payout.getProviderId());
        row.put(LexhoConstants.FIELD_AMOUNT, payout.getAmount());
        row.put("grossAmount", payout.getGrossAmount());
        row.put("commissionPercent", payout.getCommissionPercent());
        row.put("commissionAmount", payout.getCommissionAmount());
        row.put("currency", payout.getCurrency());
        row.put("reversedAmount", payout.getReversedAmount());
        row.put(FIELD_STATUS, payout.getStatus());
        row.put("referenceBookingId", payout.getReferenceBookingId());
        row.put("remarks", payout.getRemarks());
        row.put("processedAt", payout.getProcessedAt());
        row.put(LexhoConstants.FIELD_CREATED_AT, payout.getCreatedAt());
        return row;
    }

    private Map<String, Object> toWithdrawalRow(PartnerWithdrawalRequest request) {
        Map<String, Object> row = new java.util.LinkedHashMap<>();
        row.put("id", request.getId());
        row.put(FIELD_PARTNER_ID, request.getPartnerId());
        row.put(LexhoConstants.FIELD_AMOUNT, request.getAmount());
        row.put(FIELD_STATUS, request.getStatus());
        row.put("remarks", request.getRemarks());
        row.put(LexhoConstants.FIELD_CREATED_AT, request.getCreatedAt());
        row.put(LexhoConstants.FIELD_UPDATED_AT, request.getUpdatedAt());
        row.put("processedAt", request.getProcessedAt());
        return row;
    }

    private String requireStatus(String status, java.util.Set<String> allowed) {
        if (status == null || status.isBlank()) {
            throw new BadRequestException(FIELD_STATUS + " is required");
        }
        String normalized = status.trim().toUpperCase();
        if (!allowed.contains(normalized)) {
            throw new BadRequestException("Invalid status");
        }
        return normalized;
    }

    private String trim(String value) {
        return value == null ? null : value.trim();
    }

    private Map<String, Object> toWarningDisputeRow(PartnerPenalty warning) {
        Map<String, Object> row = new java.util.LinkedHashMap<>();
        row.put("id", warning.getId());
        row.put("warningId", warning.getId());
        row.put(FIELD_PARTNER_ID, warning.getPartnerId());
        row.put("warningStatus", warning.getStatus());
        row.put("reason", warning.getReason());
        row.put("acknowledgedAt", warning.getAcknowledgedAt());
        row.put("disputeStatus", warning.getDisputeStatus() == null || warning.getDisputeStatus().isBlank() ? "NONE" : warning.getDisputeStatus());
        row.put("disputeReason", warning.getDisputeReason());
        row.put("disputedAt", warning.getDisputedAt());
        row.put("disputeReviewNote", warning.getDisputeReviewNote());
        row.put("disputeReviewedByAdminId", warning.getDisputeReviewedByAdminId());
        row.put("disputeReviewedAt", warning.getDisputeReviewedAt());
        row.put("createdByAdminId", warning.getCreatedByAdminId());
        row.put(LexhoConstants.FIELD_CREATED_AT, warning.getCreatedAt());
        row.put(LexhoConstants.FIELD_UPDATED_AT, warning.getUpdatedAt());
        return row;
    }
}

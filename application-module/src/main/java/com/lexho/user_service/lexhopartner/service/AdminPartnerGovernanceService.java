package com.lexho.user_service.lexhopartner.service;

import com.lexho.user_service.admin.entity.Payout;
import com.lexho.user_service.admin.repository.PayoutRepository;
import com.lexho.user_service.dispatch.dto.PartnerSkillsUpdateRequest;
import com.lexho.user_service.dispatch.repository.PartnerShiftRepository;
import com.lexho.user_service.dispatch.service.PartnerSkillService;
import com.lexho.user_service.exception.BadRequestException;
import com.lexho.user_service.exception.ResourceNotFoundException;
import com.lexho.user_service.lexhopartner.entity.PartnerDocument;
import com.lexho.user_service.lexhopartner.entity.PartnerProfile;
import com.lexho.user_service.lexhopartner.entity.VerificationStatus;
import com.lexho.user_service.lexhopartner.repository.PartnerDocumentRepository;
import com.lexho.user_service.lexhopartner.repository.PartnerProfileRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Map;

@Service
public class AdminPartnerGovernanceService {

    private final PartnerProfileRepository partnerProfileRepository;
    private final PartnerDocumentRepository partnerDocumentRepository;
    private final PartnerShiftRepository partnerShiftRepository;
    private final PartnerSkillService partnerSkillService;
    private final PayoutRepository payoutRepository;

    public AdminPartnerGovernanceService(PartnerProfileRepository partnerProfileRepository,
                                         PartnerDocumentRepository partnerDocumentRepository,
                                         PartnerShiftRepository partnerShiftRepository,
                                         PartnerSkillService partnerSkillService,
                                         PayoutRepository payoutRepository) {
        this.partnerProfileRepository = partnerProfileRepository;
        this.partnerDocumentRepository = partnerDocumentRepository;
        this.partnerShiftRepository = partnerShiftRepository;
        this.partnerSkillService = partnerSkillService;
        this.payoutRepository = payoutRepository;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> settlements(String search, String status, int page, int size) {
        var pageable = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 100));
        var result = payoutRepository.search(search == null || search.isBlank() ? null : search.trim(),
                status == null || status.isBlank() ? null : status.trim(), pageable);
        return Map.of(
                "items", result.getContent().stream().map(this::toPayoutRow).toList(),
                "page", result.getNumber(),
                "size", result.getSize(),
                "totalElements", result.getTotalElements(),
                "totalPages", result.getTotalPages()
        );
    }

    @Transactional
    public Map<String, Object> updateSettlementStatus(Long payoutId, String status, String remarks) {
        if (status == null || status.isBlank()) {
            throw new BadRequestException("status is required");
        }
        String normalized = status.trim().toUpperCase();
        if (!java.util.Set.of("PENDING", "PROCESSING", "SETTLED", "FAILED", "REVERSED").contains(normalized)) {
            throw new BadRequestException("Invalid settlement status");
        }
        Payout payout = payoutRepository.findById(payoutId).orElseThrow(() -> new ResourceNotFoundException("Payout not found"));
        payout.setStatus(normalized);
        payout.setRemarks(remarks == null ? null : remarks.trim());
        payout.setProcessedAt(LocalDateTime.now());
        return toPayoutRow(payoutRepository.save(payout));
    }

    @Transactional
    public Map<String, Object> reviewKycDocument(Long partnerId, Long documentId, String decision, String reason, Long adminId) {
        PartnerProfile partner = partnerProfileRepository.findById(partnerId)
                .orElseThrow(() -> new ResourceNotFoundException("Partner not found"));
        PartnerDocument document = partnerDocumentRepository.findById(documentId)
                .orElseThrow(() -> new ResourceNotFoundException("Document not found"));
        if (!document.getPartner().getId().equals(partnerId)) {
            throw new BadRequestException("Document does not belong to partner");
        }
        if (decision == null || decision.isBlank()) {
            throw new BadRequestException("decision is required");
        }
        VerificationStatus next;
        try {
            next = VerificationStatus.valueOf(decision.trim().toUpperCase());
        } catch (Exception ex) {
            throw new BadRequestException("decision must be APPROVED or REJECTED");
        }
        if (next == VerificationStatus.PENDING) {
            throw new BadRequestException("decision must be APPROVED or REJECTED");
        }
        document.setVerificationStatus(next);
        document.setReviewReason(reason == null ? null : reason.trim());
        document.setReviewedByAdminId(adminId);
        document.setReviewedAt(LocalDateTime.now());
        partnerDocumentRepository.save(document);

        boolean allApproved = partnerDocumentRepository.findByPartnerId(partnerId).stream()
                .allMatch(d -> d.getVerificationStatus() == VerificationStatus.APPROVED);
        partner.setVerified(allApproved);
        partnerProfileRepository.save(partner);
        return Map.of(
                "partnerId", partnerId,
                "documentId", documentId,
                "verificationStatus", document.getVerificationStatus().name(),
                "reason", document.getReviewReason(),
                "partnerVerified", partner.isVerified()
        );
    }

    @Transactional
    public Map<String, Object> forceAvailability(Long partnerId, boolean online, Integer lockMinutes, String reason) {
        PartnerProfile partner = partnerProfileRepository.findById(partnerId)
                .orElseThrow(() -> new ResourceNotFoundException("Partner not found"));
        partner.setOnline(online);
        partner.setAvailabilityLockReason(reason == null ? null : reason.trim());
        if (lockMinutes != null && lockMinutes > 0) {
            partner.setAvailabilityLockedUntil(LocalDateTime.now().plusMinutes(lockMinutes));
        } else {
            partner.setAvailabilityLockedUntil(null);
        }
        partnerProfileRepository.save(partner);
        partnerShiftRepository.findByPartnerId(partnerId).ifPresent(shift -> {
            shift.setOnline(online);
            shift.setUpdatedBy("ADMIN");
            partnerShiftRepository.save(shift);
        });
        return Map.of(
                "partnerId", partnerId,
                "online", partner.isOnline(),
                "availabilityLockedUntil", partner.getAvailabilityLockedUntil(),
                "availabilityLockReason", partner.getAvailabilityLockReason()
        );
    }

    @Transactional
    public Map<String, Object> governSkills(Long partnerId, PartnerSkillsUpdateRequest request) {
        return partnerSkillService.upsertSkills(partnerId, request);
    }

    @Transactional
    public Map<String, Object> riskAction(Long partnerId, String action, String reason) {
        PartnerProfile partner = partnerProfileRepository.findById(partnerId)
                .orElseThrow(() -> new ResourceNotFoundException("Partner not found"));
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
                partner.setRiskStatus("UNDER_REVIEW");
            }
            default -> throw new BadRequestException("Unsupported action");
        }
        partner.setAdminNotes(reason == null ? null : reason.trim());
        partnerProfileRepository.save(partner);
        return Map.of(
                "partnerId", partnerId,
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
        row.put("amount", payout.getAmount());
        row.put("status", payout.getStatus());
        row.put("referenceBookingId", payout.getReferenceBookingId());
        row.put("remarks", payout.getRemarks());
        row.put("processedAt", payout.getProcessedAt());
        row.put("createdAt", payout.getCreatedAt());
        return row;
    }
}

package com.lexho.user_service.lexhopartner.service;

import com.lexho.user_service.admin.entity.Payout;
import com.lexho.user_service.admin.repository.PayoutRepository;
import com.lexho.user_service.audit.service.AuditLogService;
import com.lexho.user_service.dispatch.dto.PartnerSkillsUpdateRequest;
import com.lexho.user_service.dispatch.entity.PartnerShiftEntity;
import com.lexho.user_service.dispatch.repository.PartnerShiftRepository;
import com.lexho.user_service.dispatch.service.PartnerSkillService;
import com.lexho.user_service.exception.BadRequestException;
import com.lexho.user_service.exception.ResourceNotFoundException;
import com.lexho.user_service.lexhopartner.entity.PartnerPenalty;
import com.lexho.user_service.lexhopartner.entity.PartnerOperationalProfile;
import com.lexho.user_service.lexhopartner.repository.PartnerWarningCommentRepository;
import com.lexho.user_service.lexhopartner.entity.PartnerWithdrawalRequest;
import com.lexho.user_service.lexhopartner.repository.PartnerPenaltyRepository;
import com.lexho.user_service.lexhopartner.repository.PartnerOperationalProfileRepository;
import com.lexho.user_service.lexhopartner.repository.PartnerWithdrawalRequestRepository;
import com.lexho.user_service.notification.service.NotificationManagementService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminPartnerGovernanceServiceTest {

    @Mock PartnerOperationalProfileRepository partnerProfileRepository;
    @Mock PartnerShiftRepository partnerShiftRepository;
    @Mock PartnerSkillService partnerSkillService;
    @Mock PayoutRepository payoutRepository;
    @Mock PartnerWithdrawalRequestRepository withdrawalRepository;
    @Mock PartnerPenaltyRepository partnerPenaltyRepository;
    @Mock PartnerWarningCommentRepository partnerWarningCommentRepository;
    @Mock AuditLogService auditLogService;
    @Mock NotificationManagementService notificationManagementService;
    @Mock PartnerWarningRealtimeService partnerWarningRealtimeService;
    @InjectMocks AdminPartnerGovernanceService service;

    @Test
    void settlementsShouldNormalizeFiltersAndClampPaging() {
        Payout payout = payout(1L);
        when(payoutRepository.search(eq("partner"), eq("SETTLED"), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(payout)));

        Map<String, Object> response = service.settlements(" partner ", " SETTLED ", -2, 500);

        assertEquals(1L, response.get("totalElements"));
        assertEquals(1, ((List<?>) response.get("items")).size());
        verify(payoutRepository).search(eq("partner"), eq("SETTLED"), argThat(pageable ->
                pageable.getPageNumber() == 0 && pageable.getPageSize() == 100));
    }

    @Test
    void updateSettlementStatusShouldValidateAndPersist() {
        Payout payout = payout(1L);
        when(payoutRepository.findById(1L)).thenReturn(Optional.of(payout));
        when(payoutRepository.save(payout)).thenReturn(payout);

        Map<String, Object> response = service.updateSettlementStatus(1L, " settled ", " done ");

        assertEquals("SETTLED", response.get("status"));
        assertEquals("done", response.get("remarks"));
        assertNotNull(payout.getProcessedAt());
        assertThrows(BadRequestException.class, () -> service.updateSettlementStatus(1L, " ", null));
        assertThrows(BadRequestException.class, () -> service.updateSettlementStatus(1L, "UNKNOWN", null));
        when(payoutRepository.findById(99L)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class,
                () -> service.updateSettlementStatus(99L, "FAILED", "reason"));
    }

    @Test
    void withdrawalsAndStatusUpdateShouldMapAndSetProcessedTime() {
        PartnerWithdrawalRequest withdrawal = withdrawal(2L);
        when(withdrawalRepository.findAll(any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(withdrawal)));
        assertEquals(1, ((List<?>) service.withdrawals(0, 20).get("items")).size());

        when(withdrawalRepository.findById(2L)).thenReturn(Optional.of(withdrawal));
        when(withdrawalRepository.save(withdrawal)).thenReturn(withdrawal);
        Map<String, Object> response = service.updateWithdrawalStatus(2L, " paid ", " complete ");
        assertEquals("PAID", response.get("status"));
        assertNotNull(withdrawal.getProcessedAt());
        assertThrows(BadRequestException.class, () -> service.updateWithdrawalStatus(2L, "bad", null));
    }

    @Test
    void warningDisputesAndReviewShouldMapAndPersistDecision() {
        PartnerPenalty warning = new PartnerPenalty();
        ReflectionTestUtils.setField(warning, "id", 44L);
        warning.setPartnerId(10L);
        warning.setStatus("WARNING");
        warning.setReason("document mismatch");
        warning.setDisputeStatus("OPEN");
        warning.setDisputeReason("already submitted correction");
        when(partnerPenaltyRepository.findWarningDisputes(eq("OPEN"), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(warning)));
        when(partnerPenaltyRepository.findByIdAndPartnerId(44L, 10L)).thenReturn(Optional.of(warning));
        when(partnerPenaltyRepository.save(warning)).thenReturn(warning);

        Map<String, Object> disputes = service.warningDisputes("OPEN", -1, 500);
        Map<String, Object> reviewed = service.reviewWarningDispute(10L, 44L, " approved ", " valid dispute ", 99L);

        assertEquals(1L, disputes.get("totalElements"));
        assertEquals("APPROVED", reviewed.get("disputeStatus"));
        assertEquals("RESOLVED", reviewed.get("warningStatus"));
        assertEquals("valid dispute", reviewed.get("disputeReviewNote"));
        verify(auditLogService).log(99L, "ADMIN", "WARNING_DISPUTE_APPROVED", "PARTNER_WARNING", "44", null, " valid dispute ");
        verify(notificationManagementService).createPartnerWarningDisputeStatusNotification(10L, 44L, "APPROVED");
        verify(partnerWarningCommentRepository).save(any());
        verify(partnerWarningRealtimeService).publish(eq(10L), eq(44L), eq("warning-review"), any(Map.class));
    }

    @Test
    void reviewWarningDisputeShouldRejectMissingDispute() {
        PartnerPenalty warning = new PartnerPenalty();
        ReflectionTestUtils.setField(warning, "id", 45L);
        warning.setPartnerId(10L);
        warning.setStatus("WARNING");
        when(partnerPenaltyRepository.findByIdAndPartnerId(45L, 10L)).thenReturn(Optional.of(warning));

        assertThrows(BadRequestException.class,
                () -> service.reviewWarningDispute(10L, 45L, "REJECTED", "not applicable", 99L));
    }

    @Test
    void bankReviewShouldSetRejectionStateAndValidateStatus() {
        PartnerOperationalProfile partner = partner(10L);
        when(partnerProfileRepository.findById(10L)).thenReturn(Optional.of(partner));

        Map<String, Object> bank = service.reviewBank(10L, " rejected ", " invalid account ");
        assertEquals("REJECTED", bank.get("bankVerificationStatus"));
        assertEquals("invalid account", bank.get("rejectionReason"));
        assertThrows(BadRequestException.class, () -> service.reviewBank(10L, null, null));
    }

    @Test
    void forceAvailabilityShouldUpdatePartnerAndShift() {
        PartnerOperationalProfile partner = partner(10L);
        PartnerShiftEntity shift = new PartnerShiftEntity();
        shift.setPartnerId(10L);
        when(partnerProfileRepository.findById(10L)).thenReturn(Optional.of(partner));
        when(partnerShiftRepository.findByPartnerId(10L)).thenReturn(Optional.of(shift));

        Map<String, Object> response = service.forceAvailability(10L, true, 30, " admin override ");

        assertEquals(true, response.get("online"));
        assertEquals("admin override", response.get("availabilityLockReason"));
        assertNotNull(partner.getAvailabilityLockedUntil());
        assertTrue(shift.isOnline());
        assertEquals("ADMIN", shift.getUpdatedBy());
        verify(partnerShiftRepository).save(shift);
    }

    @Test
    void governSkillsShouldDelegateUnchangedRequest() {
        PartnerSkillsUpdateRequest request = new PartnerSkillsUpdateRequest();
        request.setCategoryIds(List.of(1L));
        when(partnerSkillService.upsertSkills(10L, request)).thenReturn(Map.of("updated", true));

        assertEquals(true, service.governSkills(10L, request).get("updated"));
        verify(partnerSkillService).upsertSkills(10L, request);
    }

    @Test
    void riskActionsShouldApplyEachSupportedTransition() {
        PartnerOperationalProfile partner = partner(10L);
        partner.setOnline(true);
        when(partnerProfileRepository.findById(10L)).thenReturn(Optional.of(partner));

        assertEquals("SOFT_BLOCKED", service.riskAction(10L, "SOFT_BLOCK", " review ").get("riskStatus"));
        assertFalse(partner.isOnline());
        assertEquals("HARD_BLOCKED", service.riskAction(10L, "HARD_BLOCK", "fraud").get("riskStatus"));
        assertTrue(partner.isFraudFlag());
        assertEquals("UNDER_REVIEW", service.riskAction(10L, "FLAG_FRAUD", "flag").get("riskStatus"));
        assertEquals("NORMAL", service.riskAction(10L, "UNBLOCK", "cleared").get("riskStatus"));
        assertFalse(partner.isFraudFlag());
        assertThrows(BadRequestException.class, () -> service.riskAction(10L, " ", "reason"));
        assertThrows(BadRequestException.class, () -> service.riskAction(10L, "DELETE", "reason"));
        verify(partnerProfileRepository, times(4)).save(partner);
    }

    @Test
    void missingPartnerShouldRaiseResourceNotFound() {
        when(partnerProfileRepository.findById(404L)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class,
                () -> service.forceAvailability(404L, true, 10, "reason"));
    }

    private static PartnerOperationalProfile partner(Long id) {
        PartnerOperationalProfile partner = new PartnerOperationalProfile();
        partner.setId(id);
        partner.setName("Partner " + id);
        return partner;
    }

    private static Payout payout(Long id) {
        Payout payout = new Payout();
        ReflectionTestUtils.setField(payout, "id", id);
        payout.setProviderId(10L);
        payout.setAmount(100.0);
        payout.setGrossAmount(BigDecimal.valueOf(120));
        payout.setCommissionPercent(BigDecimal.TEN);
        payout.setCommissionAmount(BigDecimal.valueOf(20));
        payout.setReversedAmount(BigDecimal.ZERO);
        payout.setStatus("PENDING");
        return payout;
    }

    private static PartnerWithdrawalRequest withdrawal(Long id) {
        PartnerWithdrawalRequest request = new PartnerWithdrawalRequest();
        ReflectionTestUtils.setField(request, "id", id);
        request.setPartnerId(10L);
        request.setAmount(BigDecimal.valueOf(50));
        request.setStatus("REQUESTED");
        return request;
    }
}

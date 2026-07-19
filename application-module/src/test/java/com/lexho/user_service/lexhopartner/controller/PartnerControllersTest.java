package com.lexho.user_service.lexhopartner.controller;

import com.lexho.user_service.chat.service.ChatService;
import com.lexho.user_service.dispatch.dto.PartnerSkillsUpdateRequest;
import com.lexho.user_service.exception.BadRequestException;
import com.lexho.user_service.lexhopartner.dto.CreateWithdrawalRequest;
import com.lexho.user_service.lexhopartner.dto.PartnerDeviceTokenRequest;
import com.lexho.user_service.lexhopartner.dto.PartnerLeaveRequest;
import com.lexho.user_service.lexhopartner.dto.PartnerServiceAreaRequest;
import com.lexho.user_service.lexhopartner.dto.UpdateAvailabilityRequest;
import com.lexho.user_service.lexhopartner.dto.UpdatePartnerBankDetailsRequest;
import com.lexho.user_service.lexhopartner.dto.UpdatePartnerProfileRequest;
import com.lexho.user_service.lexhopartner.service.AdminPartnerGovernanceService;
import com.lexho.user_service.lexhopartner.service.PartnerBannerService;
import com.lexho.user_service.lexhopartner.service.PartnerDeviceTokenService;
import com.lexho.user_service.lexhopartner.service.PartnerOperationsService;
import com.lexho.user_service.lexhopartner.service.PartnerOrderService;
import com.lexho.user_service.lexhopartner.service.PartnerProfileService;
import com.lexho.user_service.lexhopartner.service.PartnerSettlementService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PartnerControllersTest {

    @Mock
    private PartnerProfileService profileService;
    @Mock
    private PartnerOrderService orderService;
    @Mock
    private PartnerBannerService bannerService;
    @Mock
    private PartnerSettlementService settlementService;
    @Mock
    private PartnerDeviceTokenService tokenService;
    @Mock
    private PartnerOperationsService operationsService;
    @Mock
    private AdminPartnerGovernanceService governanceService;
    @Mock
    private ChatService chatService;

    private LexhoPartnerController partnerController;
    private LexhoPartnerOrderController orderController;
    private AdminPartnerControlController adminController;
    private PartnerDispatchCompatibilityController compatibilityController;

    @BeforeEach
    void setUp() {
        partnerController = new LexhoPartnerController(profileService, orderService, bannerService,
                settlementService, tokenService, operationsService);
        orderController = new LexhoPartnerOrderController(orderService);
        adminController = new AdminPartnerControlController(profileService, orderService, bannerService,
                governanceService, operationsService);
        compatibilityController = new PartnerDispatchCompatibilityController(chatService, orderService, null);
    }

    @Test
    void partnerProfileDeviceAvailabilityAndBannerEndpointsDelegate() {
        Long partnerId = 7L;
        UpdatePartnerProfileRequest profileRequest = mock(UpdatePartnerProfileRequest.class);
        UpdateAvailabilityRequest availabilityRequest = mock(UpdateAvailabilityRequest.class);
        PartnerDeviceTokenRequest tokenRequest = mock(PartnerDeviceTokenRequest.class);
        MockMultipartFile file = new MockMultipartFile("file", "photo.jpg", "image/jpeg", new byte[]{1});

        assertThat(partnerController.profile(partnerId).isSuccess()).isTrue();
        assertThat(partnerController.update(partnerId, profileRequest).isSuccess()).isTrue();
        assertThat(partnerController.profileImage(partnerId, file).isSuccess()).isTrue();
        assertThat(partnerController.documents(partnerId, file, file, file, null).isSuccess()).isTrue();
        assertThat(partnerController.registerDeviceToken(partnerId, tokenRequest).isSuccess()).isTrue();
        assertThat(partnerController.deleteDeviceToken(partnerId, tokenRequest).isSuccess()).isTrue();
        partnerController.availability(partnerId, availabilityRequest);
        partnerController.banners(2, 6);

        verify(profileService).getProfile(partnerId);
        verify(profileService).updateProfile(partnerId, profileRequest);
        verify(profileService).updateProfileImage(partnerId, file);
        verify(profileService).uploadDocuments(partnerId, file, file, file, null);
        verify(tokenService).register(partnerId, tokenRequest);
        verify(tokenService).delete(partnerId, tokenRequest);
        verify(profileService).updateAvailability(partnerId, availabilityRequest);
        verify(bannerService).partnerBanners(2, 6);
    }

    @Test
    void partnerFinanceOperationsAndPreferenceEndpointsDelegate() {
        Long partnerId = 8L;
        PartnerLeaveRequest leave = mock(PartnerLeaveRequest.class);
        PartnerServiceAreaRequest areas = mock(PartnerServiceAreaRequest.class);
        UpdatePartnerBankDetailsRequest bank = mock(UpdatePartnerBankDetailsRequest.class);
        CreateWithdrawalRequest withdrawal = mock(CreateWithdrawalRequest.class);
        when(settlementService.withdrawableBalance(partnerId)).thenReturn(BigDecimal.valueOf(1200));

        partnerController.dashboardStats(partnerId);
        partnerController.earningsSummary(partnerId);
        partnerController.earningTransactions(partnerId, "2026-01-01", "2026-01-31",
                1, 25, "amount", "asc");
        partnerController.payouts(partnerId, 2, 10);
        partnerController.performance(partnerId);
        partnerController.ratings(partnerId, 3, 15);
        partnerController.createLeave(partnerId, leave);
        partnerController.leaves(partnerId);
        partnerController.serviceAreas(partnerId);
        partnerController.updateServiceAreas(partnerId, areas);
        partnerController.payout(partnerId, 91L);
        partnerController.bankDetails(partnerId);
        partnerController.updateBankDetails(partnerId, bank);
        partnerController.requestWithdrawal(partnerId, withdrawal);
        partnerController.withdrawals(partnerId, 4, 12);
        partnerController.withdrawalBalance(partnerId);

        verify(orderService).dashboard(partnerId);
        verify(orderService).earningsSummary(partnerId);
        verify(orderService).earningTransactions(partnerId, LocalDate.of(2026, 1, 1),
                LocalDate.of(2026, 1, 31), 1, 25, "amount", "asc");
        verify(settlementService).payouts(partnerId, 2, 10);
        verify(operationsService).performance(partnerId);
        verify(operationsService).ratings(partnerId, 3, 15);
        verify(operationsService).createLeave(partnerId, leave);
        verify(operationsService).leaves(partnerId);
        verify(operationsService).serviceAreas(partnerId);
        verify(operationsService).updateServiceAreas(partnerId, areas);
        verify(settlementService).payout(partnerId, 91L);
        verify(profileService).bankDetails(partnerId);
        verify(profileService).updateBankDetails(partnerId, bank);
        verify(settlementService).requestWithdrawal(partnerId, withdrawal);
        verify(settlementService).withdrawals(partnerId, 4, 12);
        verify(settlementService).withdrawableBalance(partnerId);
    }

    @Test
    void transactionDatesAcceptBlanksAndRejectMalformedValues() {
        partnerController.earningTransactions(7L, null, " ", 0, 20, "updatedAt", "desc");
        verify(orderService).earningTransactions(7L, null, null, 0, 20, "updatedAt", "desc");

        assertThatThrownBy(() -> partnerController.earningTransactions(
                7L, "12-06-2026", null, 0, 20, "updatedAt", "desc"))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("YYYY-MM-DD");
    }

    @Test
    void partnerOrderEndpointsDelegateAllLifecycleActions() {
        Long partnerId = 9L;
        MockMultipartFile image = new MockMultipartFile("image", "job.jpg", "image/jpeg", new byte[]{1});

        orderController.newOrders(1, 10, "createdAt", "asc");
        orderController.getOrder(partnerId, 101L);
        orderController.timeline(partnerId, 101L);
        orderController.accept(partnerId, 101L);
        orderController.reject(partnerId, 102L);
        orderController.start(partnerId, 103L, image);
        orderController.complete(partnerId, 104L, image);
        orderController.history(partnerId, 2, 30, "updatedAt", "desc");
        orderController.dashboard(partnerId);

        verify(orderService).getNewOrders(1, 10, "createdAt", "asc");
        verify(orderService).getOrder(partnerId, 101L);
        verify(orderService).timeline(partnerId, 101L);
        verify(orderService).acceptOrder(partnerId, 101L);
        verify(orderService).rejectOrder(partnerId, 102L);
        verify(orderService).startOrder(partnerId, 103L, image);
        verify(orderService).completeOrder(partnerId, 104L, image);
        verify(orderService).getHistory(partnerId, 2, 30, "updatedAt", "desc");
        verify(orderService).dashboard(partnerId);
    }

    @Test
    void compatibilityChatMessagesEndpointDelegatesToChatService() {
        compatibilityController.orderChatMessages(101L, 0, 30);

        verify(chatService).listMessagesByOrderId(101L, 0, 30);
    }

    @Test
    void adminPartnerGovernanceEndpointsDelegateAllCommands() {
        UpdateAvailabilityRequest availability = mock(UpdateAvailabilityRequest.class);
        PartnerSkillsUpdateRequest skills = mock(PartnerSkillsUpdateRequest.class);
        AdminPartnerControlController.SettlementStatusRequest status =
                new AdminPartnerControlController.SettlementStatusRequest();
        status.status = "PAID";
        status.remarks = "settled";
        AdminPartnerControlController.KycDecisionRequest kyc =
                new AdminPartnerControlController.KycDecisionRequest();
        kyc.decision = "APPROVED";
        kyc.reason = "valid";
        AdminPartnerControlController.VerificationDecisionRequest verification =
                new AdminPartnerControlController.VerificationDecisionRequest();
        verification.status = "VERIFIED";
        verification.reason = "checked";
        AdminPartnerControlController.ForceAvailabilityRequest force =
                new AdminPartnerControlController.ForceAvailabilityRequest();
        force.online = true;
        force.lockMinutes = 30;
        force.reason = "support";
        AdminPartnerControlController.RiskActionRequest risk =
                new AdminPartnerControlController.RiskActionRequest();
        risk.action = "REVIEW";
        risk.reason = "signal";
        AdminPartnerControlController.PenaltyRequest penalty =
                new AdminPartnerControlController.PenaltyRequest();
        penalty.status = "WARNING";
        penalty.reason = "late";
        AdminPartnerControlController.WarningRequest warning =
                new AdminPartnerControlController.WarningRequest();
        warning.reason = "documents missing";
        warning.sendNotification = false;

        adminController.dashboardStats(4L);
        adminController.updateAvailability(4L, availability);
        adminController.partnerBanners(4L, 1, 7);
        adminController.settlements("name", "PENDING", 2, 9);
        adminController.updateSettlementStatus(11L, status);
        adminController.withdrawals(3, 8);
        adminController.updateWithdrawalStatus(12L, status);
        adminController.reviewKycDocument(4L, 13L, 99L, kyc);
        adminController.reviewPartnerKyc(4L, verification);
        adminController.reviewBank(4L, verification);
        adminController.forceAvailability(4L, force);
        adminController.governSkills(4L, skills);
        adminController.riskAction(4L, risk);
        adminController.addPenalty(4L, 99L, penalty);
        adminController.penalties(4L);
        adminController.createWarning(4L, 99L, warning);

        verify(orderService).dashboard(4L);
        verify(profileService).updateAvailability(4L, availability);
        verify(bannerService).partnerBanners(1, 7);
        verify(governanceService).settlements("name", "PENDING", 2, 9);
        verify(governanceService).updateSettlementStatus(11L, "PAID", "settled");
        verify(governanceService).withdrawals(3, 8);
        verify(governanceService).updateWithdrawalStatus(12L, "PAID", "settled");
        verify(governanceService).reviewKycDocument(4L, 13L, "APPROVED", "valid", 99L);
        verify(governanceService).reviewPartnerKyc(4L, "VERIFIED", "checked");
        verify(governanceService).reviewBank(4L, "VERIFIED", "checked");
        verify(governanceService).forceAvailability(4L, true, 30, "support");
        verify(governanceService).governSkills(4L, skills);
        verify(governanceService).riskAction(4L, "REVIEW", "signal");
        verify(operationsService).addPenalty(4L, "WARNING", "late", 99L);
        verify(operationsService).penalties(4L);
        verify(operationsService).createWarning(4L, "documents missing", 99L, false);
    }
}

package com.lexho.user_service.lexhopartner.controller;

import com.lexho.user_service.chat.service.ChatService;
import com.lexho.user_service.dispatch.dto.PartnerSkillsUpdateRequest;
import com.lexho.user_service.exception.BadRequestException;
import com.lexho.user_service.lexhopartner.dto.CreateWithdrawalRequest;
import com.lexho.user_service.lexhopartner.dto.PartnerDeviceTokenRequest;
import com.lexho.user_service.lexhopartner.dto.PartnerLeaveRequest;
import com.lexho.user_service.lexhopartner.dto.PartnerOnboardingRequest;
import com.lexho.user_service.lexhopartner.dto.PartnerServiceAreaRequest;
import com.lexho.user_service.lexhopartner.dto.PartnerWarningCommentRequest;
import com.lexho.user_service.lexhopartner.dto.PartnerWarningDisputeRequest;
import com.lexho.user_service.lexhopartner.dto.UpdateAvailabilityRequest;
import com.lexho.user_service.lexhopartner.dto.UpdatePartnerBankDetailsRequest;
import com.lexho.user_service.lexhopartner.service.AdminPartnerGovernanceService;
import com.lexho.user_service.lexhopartner.service.PartnerActivityFeedService;
import com.lexho.user_service.lexhopartner.service.PartnerBannerService;
import com.lexho.user_service.lexhopartner.service.PartnerCatalogService;
import com.lexho.user_service.lexhopartner.service.PartnerDeviceTokenService;
import com.lexho.user_service.lexhopartner.service.PartnerOperationsService;
import com.lexho.user_service.lexhopartner.service.PartnerOrderService;
import com.lexho.user_service.lexhopartner.service.PartnerOperationalProfileService;
import com.lexho.user_service.lexhopartner.service.PartnerSettlementService;
import com.lexho.user_service.lexhopartner.util.FileStorageUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.mock.web.MockMultipartHttpServletRequest;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import java.math.BigDecimal;
import java.lang.reflect.Method;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PartnerControllersTest {

    @Mock
    private PartnerOperationalProfileService profileService;
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
    private PartnerActivityFeedService activityFeedService;
    @Mock
    private PartnerCatalogService catalogService;
    @Mock
    private FileStorageUtil fileStorageUtil;
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
                settlementService, tokenService, operationsService, activityFeedService, catalogService, fileStorageUtil);
        orderController = new LexhoPartnerOrderController(orderService);
        adminController = new AdminPartnerControlController(profileService, orderService, bannerService,
                governanceService, operationsService);
        compatibilityController = new PartnerDispatchCompatibilityController(chatService, orderService, null);
    }

    @Test
    void partnerProfileDeviceAvailabilityAndBannerEndpointsDelegate() {
        Long partnerId = 7L;
        UpdateAvailabilityRequest availabilityRequest = mock(UpdateAvailabilityRequest.class);
        PartnerDeviceTokenRequest tokenRequest = mock(PartnerDeviceTokenRequest.class);

        partnerController.onboarding(partnerId, "9876543210");
        assertThat(partnerController.registerDeviceToken(partnerId, tokenRequest).isSuccess()).isTrue();
        assertThat(partnerController.deleteDeviceToken(partnerId, tokenRequest).isSuccess()).isTrue();
        partnerController.availability(partnerId, availabilityRequest);
        partnerController.banners(partnerId, 2, 6);
        partnerController.serviceTypes(partnerId, 25);

        verify(tokenService).register(partnerId, tokenRequest);
        verify(tokenService).delete(partnerId, tokenRequest);
        verify(profileService).updateAvailability(partnerId, availabilityRequest);
        verify(profileService).onboardingDetails(partnerId);
        verify(bannerService).partnerBanners(partnerId, 2, 6);
        verify(catalogService).categoryDropdown(partnerId, 25);
    }

    @Test
    void partnerProfileRouteIsCompatibilityAliasForOnboarding() throws Exception {
        Method method = LexhoPartnerController.class.getMethod("onboarding", Long.class, String.class);
        GetMapping mapping = method.getAnnotation(GetMapping.class);

        assertThat(mapping.value()).contains("/onboarding", "/profile");
    }

    @Test
    void partnerOnboardingSubmitAcceptsPostAndPutMultipartOnlyForCompatibility() throws Exception {
        Method method = LexhoPartnerController.class.getMethod("submitOnboardingMultipart", Long.class,
                String.class,
                jakarta.servlet.http.HttpServletRequest.class);
        RequestMapping mapping = method.getAnnotation(RequestMapping.class);

        assertThat(mapping.value()).contains("/onboarding");
        assertThat(mapping.method()).contains(RequestMethod.POST, RequestMethod.PUT);
        assertThat(mapping.consumes()).contains(MediaType.MULTIPART_FORM_DATA_VALUE);
    }

    @Test
    void partnerFinanceOperationsAndPreferenceEndpointsDelegate() {
        Long partnerId = 8L;
        PartnerLeaveRequest leave = mock(PartnerLeaveRequest.class);
        PartnerServiceAreaRequest areas = mock(PartnerServiceAreaRequest.class);
        PartnerWarningDisputeRequest dispute = new PartnerWarningDisputeRequest(
                "Please review this warning", java.util.List.of("https://cdn.lexho.test/evidence.jpg"));
        PartnerWarningCommentRequest commentRequest = new PartnerWarningCommentRequest(
                "Adding follow-up", java.util.List.of("https://cdn.lexho.test/follow-up.jpg"));
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
        partnerController.warnings(partnerId);
        partnerController.warning(partnerId, 71L);
        partnerController.acknowledgeWarning(partnerId, 71L);
        partnerController.disputeWarning(partnerId, 71L, dispute);
        partnerController.warningComment(partnerId, 71L, commentRequest);
        partnerController.warningStream(partnerId, 71L);
        partnerController.payout(partnerId, 91L);
        partnerController.bankDetails(partnerId);
        partnerController.updateBankDetails(partnerId, bank);
        partnerController.requestWithdrawal(partnerId, withdrawal);
        partnerController.withdrawals(partnerId, 4, 12);
        var balance = partnerController.withdrawalBalance(partnerId);
        partnerController.activityFeed(partnerId, 1, 15);

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
        verify(operationsService).warnings(partnerId);
        verify(operationsService).warningDetail(partnerId, 71L);
        verify(operationsService).acknowledgeWarning(partnerId, 71L);
        verify(operationsService).disputeWarning(partnerId, 71L, "Please review this warning",
                java.util.List.of("https://cdn.lexho.test/evidence.jpg"));
        verify(operationsService).addWarningComment(partnerId, 71L, commentRequest);
        verify(operationsService).warningStream(partnerId, 71L);
        verify(settlementService).payout(partnerId, 91L);
        verify(profileService).bankDetails(partnerId);
        verify(profileService).updateBankDetails(partnerId, bank);
        verify(settlementService).requestWithdrawal(partnerId, withdrawal);
        verify(settlementService).withdrawals(partnerId, 4, 12);
        verify(settlementService).withdrawableBalance(partnerId);
        verify(activityFeedService).activity(partnerId, 1, 15);
        org.assertj.core.api.Assertions.assertThat(balance.getData())
                .containsEntry("availableBalance", BigDecimal.valueOf(1200))
                .doesNotContainKey("withdrawableAmount");
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
    void multipartOnboardingStoresFilesAndDelegatesAsOnboardingRequest() {
        Long partnerId = 12L;
        MockMultipartFile aadhaarFront = new MockMultipartFile(
                "aadhaar", "aadhaar-front.jpg", "image/jpeg", new byte[]{2});
        MockMultipartFile aadhaarBack = new MockMultipartFile(
                "aadhaarBack", "aadhaar-back.jpg", "image/jpeg", new byte[]{4});
        MockMultipartFile panCard = new MockMultipartFile(
                "pan", "pan.pdf", "application/pdf", new byte[]{3});
        MockMultipartFile selfie = new MockMultipartFile(
                "selfie", "selfie.jpg", "image/jpeg", new byte[]{5});
        MockMultipartFile certificate = new MockMultipartFile(
                "certificate", "certificate.pdf", "application/pdf", new byte[]{6});
        when(fileStorageUtil.storeDocument(aadhaarFront, "partner-onboarding/aadhaar-front"))
                .thenReturn("/uploads/partner-onboarding/aadhaar-front/aadhaar-front.jpg");
        when(fileStorageUtil.storeDocument(aadhaarBack, "partner-onboarding/aadhaar-back"))
                .thenReturn("/uploads/partner-onboarding/aadhaar-back/aadhaar-back.jpg");
        when(fileStorageUtil.storeDocument(panCard, "partner-onboarding/pan-card"))
                .thenReturn("/uploads/partner-onboarding/pan-card/pan.pdf");
        when(fileStorageUtil.storeImage(selfie, "partner-onboarding/selfie"))
                .thenReturn("/uploads/partner-onboarding/selfie/selfie.jpg");
        when(fileStorageUtil.storeDocument(certificate, "partner-onboarding/certificate"))
                .thenReturn("/uploads/partner-onboarding/certificate/certificate.pdf");

        MockMultipartHttpServletRequest multipartRequest = new MockMultipartHttpServletRequest();
        multipartRequest.addParameter("fullName", "Ravi Kumar");
        multipartRequest.addParameter("phoneNumber", "8888888888");
        multipartRequest.addParameter("dateOfBirth", "1995-03-12");
        multipartRequest.addParameter("gender", "MALE");
        multipartRequest.addParameter("address", "12 MG Road");
        multipartRequest.addParameter("city", "Bengaluru");
        multipartRequest.addParameter("state", "Karnataka");
        multipartRequest.addParameter("pincode", "560001");
        multipartRequest.addParameter("serviceTypes", "Cleaning, Deep Cleaning");
        multipartRequest.addParameter("accountNumber", "9876543210");
        multipartRequest.addParameter("ifscCode", "HDFC0009999");
        multipartRequest.addFile(aadhaarFront);
        multipartRequest.addFile(aadhaarBack);
        multipartRequest.addFile(panCard);
        multipartRequest.addFile(selfie);
        multipartRequest.addFile(certificate);

        partnerController.submitOnboardingMultipart(partnerId, "9999999999", multipartRequest);

        org.mockito.ArgumentCaptor<PartnerOnboardingRequest> requestCaptor =
                org.mockito.ArgumentCaptor.forClass(PartnerOnboardingRequest.class);
        verify(profileService).submitOnboarding(org.mockito.Mockito.eq(partnerId), requestCaptor.capture());
        PartnerOnboardingRequest capturedRequest = requestCaptor.getValue();
        assertThat(capturedRequest.phoneNumber()).isEqualTo("9999999999");
        assertThat(capturedRequest.serviceCategoryId()).isNull();
        assertThat(capturedRequest.aadhaarFrontUrl()).isEqualTo("/uploads/partner-onboarding/aadhaar-front/aadhaar-front.jpg");
        assertThat(capturedRequest.aadhaarBackUrl()).isEqualTo("/uploads/partner-onboarding/aadhaar-back/aadhaar-back.jpg");
        assertThat(capturedRequest.panCardUrl()).isEqualTo("/uploads/partner-onboarding/pan-card/pan.pdf");
        assertThat(capturedRequest.selfieUrl()).isEqualTo("/uploads/partner-onboarding/selfie/selfie.jpg");
        assertThat(capturedRequest.certificateUrl()).isEqualTo("/uploads/partner-onboarding/certificate/certificate.pdf");
        assertThat(capturedRequest.skills()).containsExactly("Cleaning", "Deep Cleaning");
    }

    @Test
    void multipartOnboardingRejectsMissingRequiredDocumentsBeforeService() {
        Long partnerId = 12L;
        MockMultipartFile aadhaarFront = new MockMultipartFile(
                "aadhaar", "aadhaar-front.jpg", "image/jpeg", new byte[]{2});
        MockMultipartFile panCard = new MockMultipartFile(
                "pan", "pan.pdf", "application/pdf", new byte[]{3});

        when(fileStorageUtil.storeDocument(aadhaarFront, "partner-onboarding/aadhaar-front"))
                .thenReturn("/uploads/partner-onboarding/aadhaar-front/aadhaar-front.jpg");

        MockMultipartHttpServletRequest multipartRequest = new MockMultipartHttpServletRequest();
        multipartRequest.addParameter("fullName", "Ravi Kumar");
        multipartRequest.addParameter("phoneNumber", "8888888888");
        multipartRequest.addParameter("dateOfBirth", "1995-03-12");
        multipartRequest.addParameter("gender", "MALE");
        multipartRequest.addParameter("address", "12 MG Road");
        multipartRequest.addParameter("city", "Bengaluru");
        multipartRequest.addParameter("state", "Karnataka");
        multipartRequest.addParameter("pincode", "560001");
        multipartRequest.addParameter("skills", "Cleaning");
        multipartRequest.addParameter("accountNumber", "9876543210");
        multipartRequest.addParameter("ifscCode", "HDFC0009999");
        multipartRequest.addFile(aadhaarFront);
        multipartRequest.addFile(panCard);

        assertThatThrownBy(() -> partnerController.submitOnboardingMultipart(partnerId, "9999999999", multipartRequest))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("aadhaar-back is required");

        verify(profileService, never()).submitOnboarding(org.mockito.Mockito.anyLong(), org.mockito.Mockito.any());
    }

    @Test
    void partnerOrderEndpointsDelegateAllLifecycleActions() {
        Long partnerId = 9L;
        MockMultipartFile image = new MockMultipartFile("image", "job.jpg", "image/jpeg", new byte[]{1});

        orderController.newOrders(partnerId, 1, 10, "createdAt", "asc");
        orderController.getOrder(partnerId, 101L);
        orderController.timeline(partnerId, 101L);
        orderController.accept(partnerId, 101L);
        orderController.reject(partnerId, 102L);
        orderController.arrive(partnerId, 103L);
        orderController.start(partnerId, 103L, image, "1234");
        orderController.verifyStartPin(partnerId, 103L, new com.lexho.user_service.lexhopartner.dto.ServicePinVerifyRequest("1234"));
        orderController.requestCompletePin(partnerId, 104L);
        orderController.complete(partnerId, 104L, image, "5678");
        orderController.verifyCompletePin(partnerId, 104L, new com.lexho.user_service.lexhopartner.dto.ServicePinVerifyRequest("5678"));
        orderController.history(partnerId, 2, 30, "updatedAt", "desc");
        orderController.dashboard(partnerId);

        verify(orderService).getNewOrders(partnerId, 1, 10, "createdAt", "asc");
        verify(orderService).getOrder(partnerId, 101L);
        verify(orderService).timeline(partnerId, 101L);
        verify(orderService).acceptOrder(partnerId, 101L);
        verify(orderService).rejectOrder(partnerId, 102L);
        verify(orderService).arriveOrder(partnerId, 103L);
        verify(orderService).startOrder(partnerId, 103L, image, "1234");
        verify(orderService).startOrder(partnerId, 103L, null, "1234");
        verify(orderService).requestCompletePin(partnerId, 104L);
        verify(orderService).completeOrder(partnerId, 104L, image, "5678");
        verify(orderService).completeOrder(partnerId, 104L, null, "5678");
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
        AdminPartnerControlController.WarningDisputeReviewRequest disputeReview =
                new AdminPartnerControlController.WarningDisputeReviewRequest();
        disputeReview.status = "APPROVED";
        disputeReview.reviewNote = "accepted";
        AdminPartnerControlController.OnboardingReviewRequest onboardingReview =
                new AdminPartnerControlController.OnboardingReviewRequest();
        onboardingReview.applicationStatus = "APPROVED";
        onboardingReview.documentStatus = "VERIFIED";

        adminController.dashboardStats(4L);
        adminController.updateAvailability(4L, availability);
        adminController.partnerBanners(4L, 1, 7);
        adminController.onboarding(4L);
        adminController.partner(4L);
        adminController.reviewOnboarding(4L, 99L, onboardingReview);
        adminController.settlements("name", "PENDING", 2, 9);
        adminController.updateSettlementStatus(11L, status);
        adminController.withdrawals(3, 8);
        adminController.updateWithdrawalStatus(12L, status);
        adminController.reviewBank(4L, verification);
        adminController.forceAvailability(4L, force);
        adminController.governSkills(4L, skills);
        adminController.riskAction(4L, risk);
        adminController.addPenalty(4L, 99L, penalty);
        adminController.penalties(4L);
        adminController.createWarning(4L, 99L, warning);
        adminController.warningDisputes("OPEN", 1, 10);
        adminController.reviewWarningDispute(4L, 44L, 99L, disputeReview);

        verify(orderService).dashboard(4L);
        verify(profileService).updateAvailability(4L, availability);
        verify(bannerService).partnerBanners(4L, 1, 7);
        verify(profileService, org.mockito.Mockito.times(2)).onboardingDetails(4L);
        verify(profileService).reviewOnboarding(4L, "APPROVED", "VERIFIED", null, 99L);
        verify(governanceService).settlements("name", "PENDING", 2, 9);
        verify(governanceService).updateSettlementStatus(11L, "PAID", "settled");
        verify(governanceService).withdrawals(3, 8);
        verify(governanceService).updateWithdrawalStatus(12L, "PAID", "settled");
        verify(governanceService).reviewBank(4L, "VERIFIED", "checked");
        verify(governanceService).forceAvailability(4L, true, 30, "support");
        verify(governanceService).governSkills(4L, skills);
        verify(governanceService).riskAction(4L, "REVIEW", "signal");
        verify(operationsService).addPenalty(4L, "WARNING", "late", 99L);
        verify(operationsService).penalties(4L);
        verify(operationsService).createWarning(4L, "documents missing", 99L, false);
        verify(governanceService).warningDisputes("OPEN", 1, 10);
        verify(governanceService).reviewWarningDispute(4L, 44L, "APPROVED", "accepted", 99L);
    }
}

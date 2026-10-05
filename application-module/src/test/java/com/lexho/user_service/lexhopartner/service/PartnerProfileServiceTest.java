package com.lexho.user_service.lexhopartner.service;

import com.lexho.user_service.admin.repository.AdminCategoryRepository;
import com.lexho.user_service.exception.BadRequestException;
import com.lexho.user_service.lexhopartner.dto.PartnerAvailabilityResponse;
import com.lexho.user_service.lexhopartner.dto.PartnerBankDetailsDto;
import com.lexho.user_service.lexhopartner.dto.PartnerOnboardingRequest;
import com.lexho.user_service.lexhopartner.dto.PartnerOnboardingResponse;
import com.lexho.user_service.lexhopartner.dto.UpdateAvailabilityRequest;
import com.lexho.user_service.lexhopartner.dto.UpdatePartnerBankDetailsRequest;
import com.lexho.user_service.lexhopartner.entity.PartnerAvailability;
import com.lexho.user_service.lexhopartner.entity.PartnerOperationalProfile;
import com.lexho.user_service.lexhopartner.repository.PartnerAvailabilityRepository;
import com.lexho.user_service.lexhopartner.repository.PartnerOperationalProfileRepository;
import com.lexho.user_service.lexhopartner.util.AuditLogUtil;
import com.lexho.user_service.notification.event.SystemNotificationEvent;
import com.lexho.user_service.notification.event.SystemNotificationEventListener;
import com.lexho.user_service.notification.service.NotificationTriggerService;
import com.lexho.notification.entity.Notification;
import com.lexho.notification.enums.ChannelType;
import com.lexho.notification.enums.NotificationStatus;
import com.lexho.notification.enums.NotificationUserType;
import com.lexho.notification.repository.NotificationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PartnerOperationalProfileServiceTest {

    @Mock
    private PartnerOperationalProfileRepository partnerProfileRepository;
    @Mock
    private PartnerAvailabilityRepository partnerAvailabilityRepository;
    @Mock
    private AdminCategoryRepository categoryRepository;
    @Mock
    private AuditLogUtil auditLogUtil;
    @Mock
    private NotificationTriggerService notificationTriggerService;

    @Mock
    private com.lexho.provider.repository.PartnerRepository dispatchPartnerRepository;

    @InjectMocks
    private PartnerOperationalProfileService service;

    @BeforeEach
    void setUpNotificationTrigger() {
        service.setNotificationTriggerService(notificationTriggerService);
    }

    @Test
    void bankDetailsMasksConfiguredAccountAndUpdateNormalizesInput() {
        PartnerOperationalProfile profile = profile(21L);
        profile.setAccountNumber("1234567890");
        profile.setIfscCode("HDFC0001234");
        profile.setBankVerificationStatus("REJECTED");
        profile.setBankRejectionReason("Mismatch");
        when(partnerProfileRepository.findById(21L)).thenReturn(Optional.of(profile));

        PartnerBankDetailsDto existing = service.bankDetails(21L);
        PartnerBankDetailsDto updated = service.updateBankDetails(
                21L, new UpdatePartnerBankDetailsRequest(" 9876543210 ", " hdfc0009999 "));

        assertEquals("******7890", existing.maskedAccountNumber());
        assertTrue(existing.configured());
        assertEquals("******3210", updated.maskedAccountNumber());
        assertEquals("HDFC0009999", updated.ifscCode());
        assertEquals("PENDING", updated.verificationStatus());
        assertNull(updated.rejectionReason());
        verify(auditLogUtil).audit(21L, "PARTNER_BANK_DETAILS_UPDATED", "bank details updated");
    }

    @Test
    void updateAvailabilityRejectsActiveAdminLock() {
        PartnerOperationalProfile profile = profile(25L);
        profile.setAvailabilityLockedUntil(LocalDateTime.now().plusHours(1));
        when(partnerProfileRepository.findById(25L)).thenReturn(Optional.of(profile));

        assertThrows(BadRequestException.class,
                () -> service.updateAvailability(25L, new UpdateAvailabilityRequest(true)));

        verify(partnerAvailabilityRepository, never()).save(any());
    }

    @Test
    void submitOnboardingStoresFinalPartnerFieldsAndResetsReviewState() {
        PartnerOperationalProfile profile = profile(31L);
        profile.setPartnerOperationalStatus(com.lexho.user_service.lexhopartner.model.PartnerOperationalStatus.REJECTED);
        profile.setDocumentStatus("REJECTED");
        profile.setRejectionReason("old reason");
        when(partnerProfileRepository.findById(31L)).thenReturn(Optional.of(profile));
        when(categoryRepository.findByDeletedFalseOrderByDisplayOrderAscNameAsc())
                .thenReturn(List.of(category(4L, "Cleaning"), category(5L, "Deep Cleaning")));
        when(partnerProfileRepository.save(any(PartnerOperationalProfile.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        PartnerOnboardingResponse response = service.submitOnboarding(31L, new PartnerOnboardingRequest(
                " Ravi Kumar ",
                "8888888888",
                LocalDate.of(1995, 3, 12),
                " MALE ",
                " 12 MG Road ",
                " Bengaluru ",
                " Karnataka ",
                "560001",
                4L,
                List.of("Cleaning", "Deep Cleaning", "Cleaning"),
                " 9876543210 ",
                " hdfc0009999 ",
                " https://cdn.lexho.test/aadhaar-front.jpg ",
                " https://cdn.lexho.test/aadhaar-back.jpg ",
                " https://cdn.lexho.test/pan.jpg ",
                " https://cdn.lexho.test/selfie.jpg ",
                " https://cdn.lexho.test/certificate.jpg "
        ));

        assertEquals("Ravi Kumar", response.fullName());
        assertEquals("8888888888", response.mobileNumber());
        assertEquals("Bengaluru", response.city());
        assertEquals("Karnataka", response.state());
        assertEquals("560001", response.pincode());
        assertEquals(4L, response.serviceCategoryId());
        assertEquals(List.of("Cleaning", "Deep Cleaning"), response.skills());
        assertEquals("******3210", response.maskedAccountNumber());
        assertEquals("9876543210", response.accountNumber());
        assertEquals("HDFC0009999", response.ifscCode());
        assertEquals("PENDING", response.applicationStatus());
        assertEquals("PENDING", response.documentStatus());
        assertNull(response.rejectionReason());
        assertTrue(response.submittedAt() != null);
        verify(auditLogUtil).audit(31L, "PARTNER_ONBOARDING_SUBMITTED", "partner onboarding submitted");
    }

    @Test
    void submitOnboardingRejectsMissingRequiredDocumentsBeforeSaving() {
        PartnerOperationalProfile profile = profile(33L);
        when(partnerProfileRepository.findById(33L)).thenReturn(Optional.of(profile));

        PartnerOnboardingRequest request = new PartnerOnboardingRequest(
                "Ravi Kumar",
                "8888888888",
                LocalDate.of(1995, 3, 12),
                "MALE",
                "12 MG Road",
                "Bengaluru",
                "Karnataka",
                "560001",
                4L,
                List.of("Cleaning"),
                "9876543210",
                "HDFC0009999",
                "https://cdn.lexho.test/aadhaar-front.jpg",
                null,
                "https://cdn.lexho.test/pan.jpg",
                "https://cdn.lexho.test/selfie.jpg",
                null
        );

        BadRequestException exception = assertThrows(BadRequestException.class,
                () -> service.submitOnboarding(33L, request));

        assertEquals("Missing required onboarding fields: aadhaarBack", exception.getMessage());
        verify(partnerProfileRepository, never()).save(any());
    }

    @Test
    void submitOnboardingRejectsMissingDocumentsEvenWhenProfileHasOldUploads() {
        PartnerOperationalProfile profile = profile(37L);
        profile.setAadhaarFrontUrl("/uploads/partner-onboarding/aadhaar-front/front.jpg");
        profile.setAadhaarBackUrl("/uploads/partner-onboarding/aadhaar-back/back.jpg");
        profile.setPanCardUrl("/uploads/partner-onboarding/pan-card/pan.jpg");
        profile.setSelfieUrl("/uploads/partner-onboarding/selfie/selfie.jpg");
        when(partnerProfileRepository.findById(37L)).thenReturn(Optional.of(profile));

        PartnerOnboardingRequest request = new PartnerOnboardingRequest(
                "Ravi Kumar",
                "8888888888",
                LocalDate.of(1995, 3, 12),
                "MALE",
                "12 MG Road",
                "Bengaluru",
                "Karnataka",
                "560001",
                4L,
                List.of("Cleaning"),
                "9876543210",
                "HDFC0009999",
                null,
                null,
                null,
                null,
                null
        );

        BadRequestException exception = assertThrows(BadRequestException.class,
                () -> service.submitOnboarding(37L, request));

        assertEquals("Missing required onboarding fields: aadhaarFront, aadhaarBack, panCard, selfie",
                exception.getMessage());
        verify(partnerProfileRepository, never()).save(any());
    }

    @Test
    void fullOnboardingFlowUsesOneMultipartSubmitContractAcrossFetchAndAdminReview() {
        PartnerOperationalProfile profile = profile(41L);
        when(partnerProfileRepository.findById(41L)).thenReturn(Optional.of(profile));
        when(categoryRepository.findByDeletedFalseOrderByDisplayOrderAscNameAsc())
                .thenReturn(List.of(category(4L, "Cleaning"), category(5L, "Deep Cleaning")));
        when(partnerProfileRepository.save(any(PartnerOperationalProfile.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        PartnerOnboardingResponse submitted = service.submitOnboarding(41L, new PartnerOnboardingRequest(
                "Ravi Kumar",
                "8888888888",
                LocalDate.of(1995, 3, 12),
                "MALE",
                "12 MG Road",
                "Bengaluru",
                "Karnataka",
                "560001",
                4L,
                List.of("Cleaning", "Deep Cleaning"),
                "9876543210",
                "HDFC0009999",
                "/uploads/partner-onboarding/aadhaar-front/front.jpg",
                "/uploads/partner-onboarding/aadhaar-back/back.jpg",
                "/uploads/partner-onboarding/pan-card/pan.jpg",
                "/uploads/partner-onboarding/selfie/selfie.jpg",
                null
        ));
        PartnerOnboardingResponse fetched = service.onboardingDetails(41L);
        PartnerOnboardingResponse reviewed = service.reviewOnboarding(41L, "APPROVED", "VERIFIED", null, 99L);

        assertEquals("PENDING", submitted.applicationStatus());
        assertEquals("PENDING", submitted.documentStatus());
        assertEquals("/uploads/partner-onboarding/aadhaar-front/front.jpg", fetched.aadhaarFrontUrl());
        assertEquals("/uploads/partner-onboarding/aadhaar-back/back.jpg", fetched.aadhaarBackUrl());
        assertEquals("/uploads/partner-onboarding/pan-card/pan.jpg", fetched.panCardUrl());
        assertEquals("/uploads/partner-onboarding/selfie/selfie.jpg", fetched.selfieUrl());
        assertEquals("9876543210", fetched.accountNumber());
        assertEquals(5, fetched.documents().size());
        assertTrue(fetched.documentMap().get("aadhaarFront").uploaded());
        assertEquals("/uploads/partner-onboarding/aadhaar-front/front.jpg",
                fetched.documentMap().get("aadhaarFront").previewUrl());
        assertEquals("/uploads/partner-onboarding/aadhaar-front/front.jpg",
                fetched.documentMap().get("aadhaarFront").downloadUrl());
        assertTrue(fetched.documents().stream()
                .anyMatch(document -> "PAN_CARD".equals(document.type())
                        && "panCard".equals(document.key())
                        && document.uploaded()));
        assertEquals("APPROVED", reviewed.applicationStatus());
        assertEquals("VERIFIED", reviewed.documentStatus());
        assertEquals(99L, reviewed.reviewedByAdminId());
        verify(notificationTriggerService).partnerOnboardingReviewed(41L, "APPROVED", "VERIFIED", null);
    }

    @Test
    void fullOnboardingReviewFlowCreatesPartnerNotificationsWhenAdminChangesStatus() {
        PartnerOperationalProfile profile = profile(51L);
        when(partnerProfileRepository.findById(51L)).thenReturn(Optional.of(profile));
        when(categoryRepository.findByDeletedFalseOrderByDisplayOrderAscNameAsc())
                .thenReturn(List.of(category(4L, "Cleaning")));
        when(partnerProfileRepository.save(any(PartnerOperationalProfile.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        NotificationRepository notificationRepository = org.mockito.Mockito.mock(NotificationRepository.class);
        ApplicationEventPublisher dispatchPublisher = org.mockito.Mockito.mock(ApplicationEventPublisher.class);
        SystemNotificationEventListener listener =
                new SystemNotificationEventListener(notificationRepository, dispatchPublisher);
        List<Notification> savedNotifications = new ArrayList<>();
        doAnswer(invocation -> {
            Notification notification = invocation.getArgument(0);
            notification.setId((long) (savedNotifications.size() + 1));
            savedNotifications.add(notification);
            return notification;
        }).when(notificationRepository).save(any(Notification.class));

        ApplicationEventPublisher immediateSystemPublisher = event -> {
            if (event instanceof SystemNotificationEvent systemNotificationEvent) {
                listener.onSystemEvent(systemNotificationEvent);
            }
        };
        PartnerOperationalProfileService onboardingService = new PartnerOperationalProfileService(
                partnerProfileRepository,
                partnerAvailabilityRepository,
                categoryRepository,
                auditLogUtil,
                dispatchPartnerRepository
        );
        onboardingService.setNotificationTriggerService(new NotificationTriggerService(immediateSystemPublisher));

        PartnerOnboardingResponse submitted = onboardingService.submitOnboarding(51L, new PartnerOnboardingRequest(
                "Ravi Kumar",
                "8888888888",
                LocalDate.of(1995, 3, 12),
                "MALE",
                "12 MG Road",
                "Bengaluru",
                "Karnataka",
                "560001",
                4L,
                List.of("Cleaning"),
                "9876543210",
                "HDFC0009999",
                "/uploads/partner-onboarding/aadhaar-front/front.jpg",
                "/uploads/partner-onboarding/aadhaar-back/back.jpg",
                "/uploads/partner-onboarding/pan-card/pan.jpg",
                "/uploads/partner-onboarding/selfie/selfie.jpg",
                null
        ));
        PartnerOnboardingResponse reviewed =
                onboardingService.reviewOnboarding(51L, "APPROVED", "VERIFIED", null, 99L);

        assertEquals("PENDING", submitted.applicationStatus());
        assertEquals("APPROVED", reviewed.applicationStatus());
        assertEquals("VERIFIED", reviewed.documentStatus());
        assertEquals(2, savedNotifications.size());
        assertTrue(savedNotifications.stream()
                .anyMatch(notification -> notification.getChannelType() == ChannelType.IN_APP));
        assertTrue(savedNotifications.stream()
                .anyMatch(notification -> notification.getChannelType() == ChannelType.PUSH));
        savedNotifications.forEach(notification -> {
            assertEquals(51L, notification.getUserId());
            assertEquals(NotificationUserType.PARTNER, notification.getUserType());
            assertEquals(NotificationStatus.PENDING, notification.getStatus());
            assertEquals("Onboarding approved", notification.getTitle());
            assertEquals("Your onboarding has been approved. You can now start using partner services.",
                    notification.getMessage());
            assertEquals("ONBOARDING", notification.getReferenceType());
            assertEquals(51L, notification.getReferenceId());
            assertEquals("ONBOARDING", notification.getRedirectType());
            assertEquals("51", notification.getRedirectValue());
            assertTrue(notification.getIdempotencyKey()
                    .startsWith("partner-onboarding-review:51:APPROVED:VERIFIED:"));
        });
        verify(dispatchPublisher, times(2)).publishEvent(any(Object.class));
    }

    @Test
    void submitOnboardingRejectsDuplicatePhoneNumberBeforeSaving() {
        PartnerOperationalProfile profile = profile(36L);
        when(partnerProfileRepository.findById(36L)).thenReturn(Optional.of(profile));
        when(categoryRepository.findByDeletedFalseOrderByDisplayOrderAscNameAsc())
                .thenReturn(List.of(category(4L, "Cleaning")));
        when(partnerProfileRepository.existsByPhoneNumberAndIdNot("8888888888", 36L)).thenReturn(true);

        PartnerOnboardingRequest request = new PartnerOnboardingRequest(
                "Ravi Kumar",
                "8888888888",
                LocalDate.of(1995, 3, 12),
                "MALE",
                "12 MG Road",
                "Bengaluru",
                "Karnataka",
                "560001",
                4L,
                List.of("Cleaning"),
                "9876543210",
                "HDFC0009999",
                "https://cdn.lexho.test/aadhaar-front.jpg",
                "https://cdn.lexho.test/aadhaar-back.jpg",
                "https://cdn.lexho.test/pan.jpg",
                "https://cdn.lexho.test/selfie.jpg",
                null
        );

        BadRequestException exception = assertThrows(BadRequestException.class,
                () -> service.submitOnboarding(36L, request));

        assertEquals("phoneNumber already exists", exception.getMessage());
        verify(partnerProfileRepository, never()).save(any());
    }

    @Test
    void submitOnboardingReportsAllMissingJsonUploadFieldsBeforeSaving() {
        PartnerOperationalProfile profile = profile(35L);
        when(partnerProfileRepository.findById(35L)).thenReturn(Optional.of(profile));

        PartnerOnboardingRequest request = new PartnerOnboardingRequest(
                "Drs",
                "7999700471",
                LocalDate.of(2000, 1, 1),
                "FEMALE",
                "varsova",
                "Mumbai",
                "Maharashtra",
                null,
                4L,
                List.of("Cook", "cleaning"),
                "12365897758",
                "SBIN0000300",
                null,
                null,
                null,
                null,
                null
        );

        BadRequestException exception = assertThrows(BadRequestException.class,
                () -> service.submitOnboarding(35L, request));

        assertEquals("Missing required onboarding fields: pincode, aadhaarFront, aadhaarBack, panCard, selfie",
                exception.getMessage());
        verify(partnerProfileRepository, never()).save(any());
    }

    @Test
    void submitOnboardingRejectsInvalidMultipartOnlyValuesBeforeSaving() {
        PartnerOperationalProfile profile = profile(34L);
        when(partnerProfileRepository.findById(34L)).thenReturn(Optional.of(profile));
        when(categoryRepository.findByDeletedFalseOrderByDisplayOrderAscNameAsc())
                .thenReturn(List.of(category(4L, "Cleaning")));

        PartnerOnboardingRequest request = new PartnerOnboardingRequest(
                "Ravi Kumar",
                "8888888888",
                LocalDate.of(1995, 3, 12),
                "MALE",
                "12 MG Road",
                "Bengaluru",
                "Karnataka",
                "560001",
                4L,
                List.of("Cleaning"),
                "******3210",
                "HDFC0009999",
                "https://cdn.lexho.test/aadhaar-front.jpg",
                "https://cdn.lexho.test/aadhaar-back.jpg",
                "https://cdn.lexho.test/pan.jpg",
                "https://cdn.lexho.test/selfie.jpg",
                null
        );

        BadRequestException exception = assertThrows(BadRequestException.class,
                () -> service.submitOnboarding(34L, request));

        assertEquals("accountNumber must contain 9 to 18 digits", exception.getMessage());
        verify(partnerProfileRepository, never()).save(any());
    }

    @Test
    void submitOnboardingRejectsMoreThanTwoSelectedServiceTypes() {
        PartnerOperationalProfile profile = profile(38L);
        when(partnerProfileRepository.findById(38L)).thenReturn(Optional.of(profile));

        PartnerOnboardingRequest request = new PartnerOnboardingRequest(
                "Ravi Kumar",
                "8888888888",
                LocalDate.of(1995, 3, 12),
                "MALE",
                "12 MG Road",
                "Bengaluru",
                "Karnataka",
                "560001",
                null,
                List.of("Cleaning", "Deep Cleaning", "Cooking"),
                "9876543210",
                "HDFC0009999",
                "https://cdn.lexho.test/aadhaar-front.jpg",
                "https://cdn.lexho.test/aadhaar-back.jpg",
                "https://cdn.lexho.test/pan.jpg",
                "https://cdn.lexho.test/selfie.jpg",
                null
        );

        BadRequestException exception = assertThrows(BadRequestException.class,
                () -> service.submitOnboarding(38L, request));

        assertEquals("serviceTypes can contain at most 2 selections", exception.getMessage());
        verify(partnerProfileRepository, never()).save(any());
    }

    @Test
    void reviewOnboardingSetsAdminManagedFieldsAndRequiresReasonOnlyForRejectedStatus() {
        PartnerOperationalProfile profile = profile(32L);
        when(partnerProfileRepository.findById(32L)).thenReturn(Optional.of(profile));
        when(partnerProfileRepository.save(any(PartnerOperationalProfile.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        PartnerOnboardingResponse approved =
                service.reviewOnboarding(32L, " approved ", " pending ", null, 99L);

        assertEquals("APPROVED", approved.applicationStatus());
        assertEquals("PENDING", approved.documentStatus());
        assertEquals(99L, approved.reviewedByAdminId());
        assertTrue(approved.reviewedAt() != null);
        assertNull(approved.rejectionReason());
        verify(notificationTriggerService).partnerOnboardingReviewed(32L, "APPROVED", "PENDING", null);

        PartnerOnboardingResponse blocked =
                service.reviewOnboarding(32L, "BLOCKED", "VERIFIED", null, 99L);

        assertEquals("BLOCKED", blocked.applicationStatus());
        assertEquals("VERIFIED", blocked.documentStatus());
        assertNull(blocked.rejectionReason());
        verify(notificationTriggerService).partnerOnboardingReviewed(32L, "BLOCKED", "VERIFIED", null);

        assertThrows(BadRequestException.class,
                () -> service.reviewOnboarding(32L, "REJECTED", "VERIFIED", null, 99L));
        assertThrows(BadRequestException.class,
                () -> service.reviewOnboarding(32L, "APPROVED", "REJECTED", " ", 99L));

        PartnerOnboardingResponse documentRejected =
                service.reviewOnboarding(32L, "APPROVED", "REJECTED", "Document mismatch", 99L);

        assertEquals("APPROVED", documentRejected.applicationStatus());
        assertEquals("REJECTED", documentRejected.documentStatus());
        assertEquals("Document mismatch", documentRejected.rejectionReason());
        verify(notificationTriggerService).partnerOnboardingReviewed(32L, "APPROVED", "REJECTED", "Document mismatch");

        verify(auditLogUtil).audit(32L, "PARTNER_ONBOARDING_REVIEWED",
                "applicationStatus=APPROVED,documentStatus=PENDING");
    }

    @Test
    void updateAvailabilityCreatesStateAndReturnsUtcTimestamp() {
        com.lexho.provider.entity.Partner dispatchPartner = new com.lexho.provider.entity.Partner();
        dispatchPartner.setAvailable(false);
        when(dispatchPartnerRepository.findById(26L)).thenReturn(Optional.of(dispatchPartner));
        PartnerOperationalProfile profile = profile(26L);
        when(partnerProfileRepository.findById(26L)).thenReturn(Optional.of(profile));
        when(partnerAvailabilityRepository.findById(26L)).thenReturn(Optional.empty());
        when(partnerAvailabilityRepository.save(any(PartnerAvailability.class))).thenAnswer(invocation -> {
            PartnerAvailability availability = invocation.getArgument(0);
            ReflectionTestUtils.setField(availability, "updatedAt", LocalDateTime.of(2026, 6, 11, 10, 0));
            return availability;
        });

        PartnerAvailabilityResponse result =
                service.updateAvailability(26L, new UpdateAvailabilityRequest(true));

        assertTrue(result.online());
        assertEquals("2026-06-11T10:00Z", result.updatedAt());
        assertTrue(profile.isOnline());
        assertTrue(dispatchPartner.getAvailable());
        verify(dispatchPartnerRepository).save(dispatchPartner);
        verify(auditLogUtil).audit(26L, "SERVICE_AVAILABLE", "serviceAvailability=SERVICE_AVAILABLE");
    }

    private PartnerOperationalProfile profile(Long id) {
        PartnerOperationalProfile profile = new PartnerOperationalProfile();
        profile.setId(id);
        profile.setName("Partner");
        profile.setMobileNumber("9999999999");
        profile.setBankVerificationStatus("PENDING");
        return profile;
    }

    private com.lexho.user_service.admin.entity.AdminCategoryEntity category(Long id, String name) {
        com.lexho.user_service.admin.entity.AdminCategoryEntity category =
                new com.lexho.user_service.admin.entity.AdminCategoryEntity();
        ReflectionTestUtils.setField(category, "id", id);
        category.setName(name);
        category.setActive(true);
        category.setDeleted(false);
        return category;
    }

}

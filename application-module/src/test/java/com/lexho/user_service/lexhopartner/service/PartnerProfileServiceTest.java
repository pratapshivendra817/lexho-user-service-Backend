package com.lexho.user_service.lexhopartner.service;

import com.lexho.catalog.entity.ServiceCategory;
import com.lexho.user_service.exception.BadRequestException;
import com.lexho.user_service.lexhopartner.dto.PartnerAvailabilityResponse;
import com.lexho.user_service.lexhopartner.dto.PartnerBankDetailsDto;
import com.lexho.user_service.lexhopartner.dto.PartnerDocumentDto;
import com.lexho.user_service.lexhopartner.dto.PartnerProfileDto;
import com.lexho.user_service.lexhopartner.dto.UpdateAvailabilityRequest;
import com.lexho.user_service.lexhopartner.dto.UpdatePartnerBankDetailsRequest;
import com.lexho.user_service.lexhopartner.dto.UpdatePartnerProfileRequest;
import com.lexho.user_service.lexhopartner.entity.DocumentType;
import com.lexho.user_service.lexhopartner.entity.PartnerAvailability;
import com.lexho.user_service.lexhopartner.entity.PartnerDocument;
import com.lexho.user_service.lexhopartner.entity.PartnerProfile;
import com.lexho.user_service.lexhopartner.repository.PartnerAvailabilityRepository;
import com.lexho.user_service.lexhopartner.repository.PartnerDocumentRepository;
import com.lexho.user_service.lexhopartner.repository.PartnerProfileRepository;
import com.lexho.user_service.lexhopartner.util.AuditLogUtil;
import com.lexho.user_service.lexhopartner.util.FileStorageUtil;
import com.lexho.user_service.review.repository.ServiceCategoryLookupRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PartnerProfileServiceTest {

    @Mock
    private PartnerProfileRepository partnerProfileRepository;
    @Mock
    private PartnerDocumentRepository partnerDocumentRepository;
    @Mock
    private PartnerAvailabilityRepository partnerAvailabilityRepository;
    @Mock
    private ServiceCategoryLookupRepository serviceCategoryLookupRepository;
    @Mock
    private FileStorageUtil fileStorageUtil;
    @Mock
    private AuditLogUtil auditLogUtil;

    @InjectMocks
    private PartnerProfileService service;

    @Test
    void getProfileCreatesDefaultsUsingExistingCategory() {
        ServiceCategory category = new ServiceCategory();
        category.setId(8L);
        when(partnerProfileRepository.findById(15L)).thenReturn(Optional.empty());
        when(serviceCategoryLookupRepository.findAll()).thenReturn(List.of(category));
        when(partnerProfileRepository.save(any(PartnerProfile.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        PartnerProfileDto result = service.getProfile(15L);

        assertEquals(15L, result.id());
        assertEquals("Partner 15", result.name());
        assertEquals("P15", result.mobileNumber());
        assertFalse(result.isOnline());
        ArgumentCaptor<PartnerProfile> profile = ArgumentCaptor.forClass(PartnerProfile.class);
        verify(partnerProfileRepository).save(profile.capture());
        assertEquals(8L, profile.getValue().getServiceCategoryId());
    }

    @Test
    void getProfileCreatesGeneralCategoryWhenCatalogIsEmpty() {
        when(partnerProfileRepository.findById(16L)).thenReturn(Optional.empty());
        when(serviceCategoryLookupRepository.findAll()).thenReturn(List.of());
        when(serviceCategoryLookupRepository.save(any(ServiceCategory.class))).thenAnswer(invocation -> {
            ServiceCategory category = invocation.getArgument(0);
            category.setId(99L);
            return category;
        });
        when(partnerProfileRepository.save(any(PartnerProfile.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        service.getProfile(16L);

        ArgumentCaptor<ServiceCategory> category = ArgumentCaptor.forClass(ServiceCategory.class);
        verify(serviceCategoryLookupRepository).save(category.capture());
        assertEquals("General", category.getValue().getName());
    }

    @Test
    void updateProfilePersistsFieldsAndAudits() {
        PartnerProfile profile = profile(20L);
        when(partnerProfileRepository.findById(20L)).thenReturn(Optional.of(profile));
        UpdatePartnerProfileRequest request =
                new UpdatePartnerProfileRequest("Neha Sharma", "9876543210", "n@example.com", "Delhi", "110001", "Sector 1", "Chef");

        PartnerProfileDto result = service.updateProfile(20L, request);

        assertEquals("Neha Sharma", result.fullName());
        assertEquals("Neha Sharma", result.name());
        assertEquals("Neha Sharma", profile.getFullName());
        assertEquals("Neha Sharma", profile.getName());
        assertEquals("9876543210", result.phone());
        assertEquals("9876543210", result.mobileNumber());
        assertEquals("9876543210", profile.getPhoneNumber());
        assertEquals("9876543210", profile.getMobileNumber());
        assertEquals("n@example.com", result.email());
        assertEquals("n@example.com", profile.getEmail());
        assertEquals("Delhi", result.city());
        assertEquals("Delhi", profile.getCity());
        assertEquals("110001", result.pincode());
        assertEquals("110001", profile.getPincode());
        assertEquals("Sector 1", result.address());
        assertEquals("Sector 1", profile.getAddress());
        assertEquals("Chef", result.serviceCategory());
        verify(partnerProfileRepository).save(profile);
        verify(auditLogUtil).audit(20L, "PARTNER_PROFILE_UPDATED", "profile core fields updated");
    }

    @Test
    void bankDetailsMasksConfiguredAccountAndUpdateNormalizesInput() {
        PartnerProfile profile = profile(21L);
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
    void profileImageStoresFileAndReturnsUrl() {
        PartnerProfile profile = profile(22L);
        MockMultipartFile image = new MockMultipartFile("image", "me.png", "image/png", new byte[]{1});
        when(partnerProfileRepository.findById(22L)).thenReturn(Optional.of(profile));
        when(fileStorageUtil.storeImage(image, "partner-profile")).thenReturn("/files/me.png");

        PartnerProfileDto result = service.updateProfileImage(22L, image);

        assertEquals("/files/me.png", result.profileImage());
        verify(partnerProfileRepository).save(profile);
    }

    @Test
    void uploadDocumentsRejectsMissingRequiredFileBeforePersistingLaterDocuments() {
        PartnerProfile profile = profile(23L);
        MockMultipartFile aadhaar = file("aadhaar");
        when(partnerProfileRepository.findById(23L)).thenReturn(Optional.of(profile));
        when(fileStorageUtil.storeImage(aadhaar, "partner-docs")).thenReturn("/aadhaar");

        assertThrows(BadRequestException.class,
                () -> service.uploadDocuments(23L, aadhaar, null, file("selfie"), null));

        verify(partnerDocumentRepository, times(1)).save(any(PartnerDocument.class));
        verify(partnerProfileRepository, never()).save(profile);
    }

    @Test
    void uploadDocumentsStoresRequiredAndOptionalFilesAndMapsResult() {
        PartnerProfile profile = profile(24L);
        MockMultipartFile aadhaar = file("aadhaar");
        MockMultipartFile pan = file("pan");
        MockMultipartFile selfie = file("selfie");
        MockMultipartFile certificate = file("certificate");
        when(partnerProfileRepository.findById(24L)).thenReturn(Optional.of(profile));
        when(fileStorageUtil.storeImage(any(), eq("partner-docs"))).thenReturn("/stored");
        PartnerDocument document = new PartnerDocument();
        document.setId(30L);
        document.setDocumentType(DocumentType.AADHAAR);
        document.setDocumentUrl("/stored");
        when(partnerDocumentRepository.findByPartnerId(24L)).thenReturn(List.of(document));

        List<PartnerDocumentDto> result =
                service.uploadDocuments(24L, aadhaar, pan, selfie, certificate);

        assertEquals(1, result.size());
        assertEquals(DocumentType.AADHAAR, result.get(0).type());
        assertEquals("UNDER_REVIEW", profile.getKycWorkflowStatus());
        verify(partnerDocumentRepository, times(4)).save(any(PartnerDocument.class));
        verify(auditLogUtil).audit(24L, "PARTNER_KYC_UPLOADED", "documents uploaded");
    }

    @Test
    void updateAvailabilityRejectsActiveAdminLock() {
        PartnerProfile profile = profile(25L);
        profile.setAvailabilityLockedUntil(LocalDateTime.now().plusHours(1));
        when(partnerProfileRepository.findById(25L)).thenReturn(Optional.of(profile));

        assertThrows(BadRequestException.class,
                () -> service.updateAvailability(25L, new UpdateAvailabilityRequest(true)));

        verify(partnerAvailabilityRepository, never()).save(any());
    }

    @Test
    void updateAvailabilityCreatesStateAndReturnsUtcTimestamp() {
        PartnerProfile profile = profile(26L);
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
        verify(auditLogUtil).audit(26L, "PARTNER_AVAILABILITY_UPDATED", "online=true");
    }

    private PartnerProfile profile(Long id) {
        PartnerProfile profile = new PartnerProfile();
        profile.setId(id);
        profile.setName("Partner");
        profile.setMobileNumber("9999999999");
        profile.setKycWorkflowStatus("PENDING");
        profile.setBankVerificationStatus("PENDING");
        return profile;
    }

    private MockMultipartFile file(String name) {
        return new MockMultipartFile(name, name + ".jpg", "image/jpeg", new byte[]{1});
    }
}

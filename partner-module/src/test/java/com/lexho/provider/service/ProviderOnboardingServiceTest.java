package com.lexho.provider.service;

import com.lexho.catalog.repository.CategoryRepository;
import com.lexho.catalog.entity.ServiceCategory;
import com.lexho.provider.dto.ProviderActionResponse;
import com.lexho.provider.dto.ProviderDetailsResponse;
import com.lexho.provider.dto.ProviderListItemResponse;
import com.lexho.provider.dto.ProviderOnboardingRequest;
import com.lexho.provider.entity.ProviderDocument;
import com.lexho.provider.entity.Provider;
import com.lexho.provider.enums.KycStatus;
import com.lexho.provider.enums.OnboardingStatus;
import com.lexho.provider.enums.ProviderDocumentType;
import com.lexho.provider.exception.ProviderOperationException;
import com.lexho.provider.exception.ProviderFileUploadException;
import com.lexho.provider.exception.ProviderNotFoundException;
import com.lexho.provider.exception.ProviderValidationException;
import com.lexho.provider.mapper.ProviderMapper;
import com.lexho.provider.repository.ProviderDocumentRepository;
import com.lexho.provider.repository.ProviderRepository;
import com.lexho.provider.service.storage.DocumentStorageService;
import com.lexho.provider.service.storage.StoredDocument;
import com.lexho.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.Set;
import java.util.List;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.mock.web.MockMultipartFile;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProviderOnboardingServiceTest {

    @Mock
    private ProviderRepository providerRepository;

    @Mock
    private ProviderDocumentRepository providerDocumentRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ProviderMapper providerMapper;

    @Mock
    private DocumentStorageService documentStorageService;

    private ProviderOnboardingService providerOnboardingService;

    @BeforeEach
    void setUp() {
        providerOnboardingService = new ProviderOnboardingService(
                providerRepository,
                providerDocumentRepository,
                categoryRepository,
                userRepository,
                providerMapper,
                documentStorageService
        );
    }

    @Test
    void submitOnboardingThrowsWhenDuplicateEmail() {
        ProviderOnboardingRequest request = validRequest();
        when(providerRepository.existsByEmailIgnoreCaseAndDeletedFalse("raju@gmail.com")).thenReturn(true);

        assertThrows(ProviderValidationException.class, () -> providerOnboardingService.submitOnboarding(request));
    }

    @Test
    void submitOnboardingThrowsWhenRequestIsNullOrPhoneIsDuplicate() {
        assertThrows(NullPointerException.class, () -> providerOnboardingService.submitOnboarding(null));

        ProviderOnboardingRequest request = validRequest();
        when(providerRepository.existsByEmailIgnoreCaseAndDeletedFalse("raju@gmail.com")).thenReturn(false);
        when(providerRepository.existsByPhoneNumberAndDeletedFalse("9876543210")).thenReturn(true);

        ProviderValidationException exception = assertThrows(ProviderValidationException.class,
                () -> providerOnboardingService.submitOnboarding(request));

        assertThat(exception.getMessage()).contains("phone number");
        verify(categoryRepository, never()).findById(any());
    }

    @Test
    void approveThrowsWhenProviderNotPending() {
        Provider partner = new Provider();
        partner.setId(1L);
        partner.setOnboardingStatus(OnboardingStatus.APPROVED);
        when(providerRepository.findByIdAndDeletedFalse(1L)).thenReturn(Optional.of(partner));

        assertThrows(ProviderOperationException.class, () -> providerOnboardingService.approveProvider(1L));
    }

    @Test
    void approveThrowsWhenMandatoryDocumentMissing() {
        Provider partner = new Provider();
        partner.setId(1L);
        partner.setOnboardingStatus(OnboardingStatus.PENDING);
        when(providerRepository.findByIdAndDeletedFalse(1L)).thenReturn(Optional.of(partner));

        assertThrows(ProviderOperationException.class, () -> providerOnboardingService.approveProvider(1L));
    }

    @Test
    void approveSuccessUpdatesStatusAndKyc() {
        Provider partner = new Provider();
        partner.setId(1L);
        partner.setProviderCode("PRV1001");
        partner.setOnboardingStatus(OnboardingStatus.PENDING);
        partner.setKycStatus(KycStatus.PENDING);

        when(providerRepository.findByIdAndDeletedFalse(1L)).thenReturn(Optional.of(partner));
        when(providerDocumentRepository.existsByProviderIdAndDocumentType(1L, ProviderDocumentType.AADHAAR_FRONT)).thenReturn(true);
        when(providerDocumentRepository.existsByProviderIdAndDocumentType(1L, ProviderDocumentType.AADHAAR_BACK)).thenReturn(true);
        when(providerDocumentRepository.existsByProviderIdAndDocumentType(1L, ProviderDocumentType.PAN)).thenReturn(true);
        when(providerDocumentRepository.existsByProviderIdAndDocumentType(1L, ProviderDocumentType.PROFILE_PHOTO)).thenReturn(true);
        when(providerRepository.save(any(Provider.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ProviderActionResponse response = providerOnboardingService.approveProvider(1L);

        assertEquals(OnboardingStatus.APPROVED, response.getOnboardingStatus());
        assertEquals(KycStatus.VERIFIED, partner.getKycStatus());
    }

    @Test
    void submitOnboardingValidatesCategoryExists() {
        ProviderOnboardingRequest request = validRequest();
        when(providerRepository.existsByEmailIgnoreCaseAndDeletedFalse("raju@gmail.com")).thenReturn(false);
        when(providerRepository.existsByPhoneNumberAndDeletedFalse("9876543210")).thenReturn(false);
        when(categoryRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(ProviderValidationException.class, () -> providerOnboardingService.submitOnboarding(request));
    }

    @Test
    void submitOnboardingShouldNormalizeFieldsGenerateCodeAndLinkUser() {
        ProviderOnboardingRequest request = validRequest();
        ServiceCategory category = category(1L, "Cleaning");
        com.lexho.user.entity.User user = new com.lexho.user.entity.User("9876543210", true);
        ProviderDetailsResponse mapped = new ProviderDetailsResponse();
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));
        when(userRepository.findByMobile("9876543210")).thenReturn(Optional.of(user));
        when(providerRepository.save(any())).thenAnswer(invocation -> {
            Provider provider = invocation.getArgument(0);
            provider.setId(4L);
            return provider;
        });
        when(providerDocumentRepository.findByProviderIdOrderByUploadedAtDesc(4L)).thenReturn(List.of());
        when(providerMapper.toDetails(any(), eq(List.of()))).thenReturn(mapped);

        assertEquals(mapped, providerOnboardingService.submitOnboarding(request));
        verify(providerRepository, org.mockito.Mockito.times(2)).save(org.mockito.ArgumentMatchers.argThat(provider ->
                provider.getEmail().equals("raju@gmail.com")
                        && provider.getProviderCode().equals("PRV1004")
                        && provider.getIfscCode().equals("SBIN0123456")
                        && provider.getUser() == user));
    }

    @Test
    void submitOnboardingShouldUseFallbackAddressDefaultExperienceAndExistingCode() {
        ProviderOnboardingRequest request = validRequest();
        request.setServiceCategoryId(null);
        request.setCity(null);
        request.setState(null);
        request.setFullAddress(null);
        request.setCurrentCity(" Gurugram ");
        request.setCurrentState(" Haryana ");
        request.setCurrentFullAddress(null);
        request.setExperienceYears(null);
        request.setServiceAreas(List.of());
        request.setServiceType(List.of());

        ServiceCategory category = category(11L, "Default");
        ProviderDetailsResponse mapped = new ProviderDetailsResponse();
        when(categoryRepository.findAll()).thenReturn(List.of(category));
        when(userRepository.findByMobile("9876543210")).thenReturn(Optional.empty());
        when(providerRepository.save(any())).thenAnswer(invocation -> {
            Provider provider = invocation.getArgument(0);
            provider.setId(9L);
            provider.setProviderCode("PRV9009");
            return provider;
        });
        when(providerDocumentRepository.findByProviderIdOrderByUploadedAtDesc(9L)).thenReturn(List.of());
        when(providerMapper.toDetails(any(), eq(List.of()))).thenReturn(mapped);

        ProviderDetailsResponse response = providerOnboardingService.submitOnboarding(request);

        assertThat(response).isSameAs(mapped);
        verify(providerRepository).save(org.mockito.ArgumentMatchers.argThat(provider ->
                provider.getExperienceYears().equals(0)
                        && provider.getCity().equals("Gurugram")
                        && provider.getState().equals("Haryana")
                        && provider.getFullAddress().equals("Gurugram")
                        && provider.getServiceAreas().contains("Gurugram")
                        && provider.getUser() == null));
        verify(providerRepository, org.mockito.Mockito.times(1)).save(any(Provider.class));
    }

    @Test
    void submitOnboardingShouldResolveCategoryFromServiceTypeFallbacks() {
        ProviderOnboardingRequest request = validRequest();
        request.setServiceCategoryId(null);
        request.setServiceType(List.of(" ", "CoOk"));
        request.setServiceAreas(List.of("Zone 1"));

        ServiceCategory cook = category(22L, "Cook");
        ProviderDetailsResponse mapped = new ProviderDetailsResponse();
        when(categoryRepository.findByName("CoOk")).thenReturn(Optional.empty());
        when(categoryRepository.findAll()).thenReturn(List.of(cook));
        when(userRepository.findByMobile("9876543210")).thenReturn(Optional.empty());
        when(providerRepository.save(any())).thenAnswer(invocation -> {
            Provider provider = invocation.getArgument(0);
            provider.setId(12L);
            provider.setProviderCode("PRV1012");
            return provider;
        });
        when(providerDocumentRepository.findByProviderIdOrderByUploadedAtDesc(12L)).thenReturn(List.of());
        when(providerMapper.toDetails(any(), eq(List.of()))).thenReturn(mapped);

        assertThat(providerOnboardingService.submitOnboarding(request)).isSameAs(mapped);
        verify(providerRepository).save(org.mockito.ArgumentMatchers.argThat(provider ->
                provider.getServiceCategory() == cook && provider.getServiceAreas().contains("Zone 1")));
    }

    @Test
    void submitOnboardingShouldRejectMissingDefaultCategoryAndBlankAreas() {
        ProviderOnboardingRequest noCategory = validRequest();
        noCategory.setServiceCategoryId(null);
        noCategory.setServiceType(List.of("ALL"));
        when(categoryRepository.findAll()).thenReturn(List.of());

        assertThrows(ProviderValidationException.class, () -> providerOnboardingService.submitOnboarding(noCategory));

        ProviderOnboardingRequest blankAreas = validRequest();
        blankAreas.setServiceAreas(List.of(" ", "\t"));
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(category(1L, "Cleaning")));

        ProviderValidationException exception = assertThrows(ProviderValidationException.class,
                () -> providerOnboardingService.submitOnboarding(blankAreas));
        assertThat(exception.getMessage()).isEqualTo("At least one service area is required");
    }

    @Test
    void statsShouldReturnRepositoryCounts() {
        when(providerRepository.countByOnboardingStatusAndDeletedFalse(OnboardingStatus.PENDING)).thenReturn(3L);
        when(providerRepository.countByOnboardingStatusAndDeletedFalse(OnboardingStatus.APPROVED)).thenReturn(4L);
        when(providerRepository.countByOnboardingStatusAndDeletedFalse(OnboardingStatus.REJECTED)).thenReturn(2L);
        when(providerRepository.countByOnboardingStatusAndDeletedFalse(OnboardingStatus.BLOCKED)).thenReturn(1L);

        var stats = providerOnboardingService.getStats();

        assertEquals(3L, stats.getPending());
        assertEquals(4L, stats.getApproved());
        assertEquals(2L, stats.getRejected());
        assertEquals(1L, stats.getBlocked());
    }

    @Test
    void getProvidersShouldClampPagingMapContentAndRejectInvalidStatus() {
        Provider provider = provider(1L, OnboardingStatus.PENDING);
        ProviderListItemResponse mapped = new ProviderListItemResponse();
        when(providerRepository.findAll(any(org.springframework.data.jpa.domain.Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(provider)));
        when(providerMapper.toListItem(provider)).thenReturn(mapped);

        var response = providerOnboardingService.getProviders(-1, 500, "badSort", "asc",
                "pending", "ALL", " noida ", " raju ");

        assertEquals(1, response.getContent().size());
        verify(providerRepository).findAll(any(org.springframework.data.jpa.domain.Specification.class),
                org.mockito.ArgumentMatchers.<Pageable>argThat(pageable -> pageable.getPageNumber() == 0
                        && pageable.getPageSize() == 100
                        && pageable.getSort().getOrderFor("createdAt") != null));
        assertThrows(ProviderValidationException.class,
                () -> providerOnboardingService.getProviders(0, 10, null, null, "bad", null, null, null));
    }

    @Test
    @SuppressWarnings({"unchecked", "rawtypes"})
    void getProvidersShouldExecuteSpecificationBranchesForFilters() {
        Provider provider = provider(1L, OnboardingStatus.PENDING);
        ProviderListItemResponse mapped = new ProviderListItemResponse();
        Root<Provider> root = mock(Root.class);
        CriteriaQuery<?> query = mock(CriteriaQuery.class);
        CriteriaBuilder cb = mock(CriteriaBuilder.class);
        Path stringPath = mock(Path.class);
        Join serviceCategoryJoin = mock(Join.class);
        Expression lowerExpression = mock(Expression.class);
        Predicate predicate = mock(Predicate.class);

        doReturn(stringPath).when(root).get(anyString());
        doReturn(serviceCategoryJoin).when(root).join(eq("serviceCategory"), any());
        doReturn(stringPath).when(serviceCategoryJoin).get("name");
        doReturn(predicate).when(cb).isFalse(any());
        lenient().doReturn(predicate).when(cb).equal(any(Expression.class), any());
        doReturn(lowerExpression).when(cb).lower(any());
        doReturn(predicate).when(cb).like(any(), anyString());
        doReturn(predicate).when(cb).or(any(Predicate.class), any(Predicate.class), any(Predicate.class), any(Predicate.class));
        doReturn(predicate).when(cb).and(any(Predicate[].class));
        when(providerRepository.findAll(any(org.springframework.data.jpa.domain.Specification.class), any(Pageable.class)))
                .thenAnswer(invocation -> {
                    org.springframework.data.jpa.domain.Specification<Provider> specification = invocation.getArgument(0);
                    assertThat(specification.toPredicate(root, query, cb)).isSameAs(predicate);
                    return new PageImpl<>(List.of(provider));
                });
        when(providerMapper.toListItem(provider)).thenReturn(mapped);

        var response = providerOnboardingService.getProviders(0, 10, "city", "desc",
                "APPROVED", "Cleaning", "Delhi", "PRV");

        assertThat(response.getContent()).containsExactly(mapped);
    }

    @Test
    void detailsShouldMapDocumentsAndMissingProviderShouldFail() {
        Provider provider = provider(1L, OnboardingStatus.PENDING);
        ProviderDocument document = new ProviderDocument();
        when(providerRepository.findByIdAndDeletedFalse(1L)).thenReturn(Optional.of(provider));
        when(providerDocumentRepository.findByProviderIdOrderByUploadedAtDesc(1L)).thenReturn(List.of(document));
        ProviderDetailsResponse mapped = new ProviderDetailsResponse();
        when(providerMapper.toDetails(provider, List.of(document))).thenReturn(mapped);

        assertEquals(mapped, providerOnboardingService.getProviderDetails(1L));
        when(providerRepository.findByIdAndDeletedFalse(99L)).thenReturn(Optional.empty());
        assertThrows(ProviderNotFoundException.class, () -> providerOnboardingService.getProviderDetails(99L));
    }

    @Test
    void rejectBlockUnblockAndDeleteShouldUpdateProviderState() {
        Provider provider = provider(1L, OnboardingStatus.PENDING);
        when(providerRepository.findByIdAndDeletedFalse(1L)).thenReturn(Optional.of(provider));
        when(providerRepository.save(provider)).thenReturn(provider);

        assertEquals(OnboardingStatus.REJECTED,
                providerOnboardingService.rejectProvider(1L, " missing PAN ").getOnboardingStatus());
        assertEquals("missing PAN", provider.getRejectionReason());
        assertEquals(KycStatus.FAILED, provider.getKycStatus());

        provider.setOnboardingStatus(OnboardingStatus.APPROVED);
        assertEquals(OnboardingStatus.BLOCKED,
                providerOnboardingService.blockProvider(1L, " policy violation ").getOnboardingStatus());
        assertEquals(OnboardingStatus.APPROVED,
                providerOnboardingService.unblockProvider(1L).getOnboardingStatus());
        assertNull(provider.getBlockedReason());

        providerOnboardingService.softDeleteProvider(1L);
        assertTrue(provider.isDeleted());
        assertThrows(ProviderValidationException.class, () -> providerOnboardingService.blockProvider(1L, " "));
    }

    @Test
    void rejectShouldRejectProviderThatIsNotPendingAndBlankReason() {
        Provider approved = provider(1L, OnboardingStatus.APPROVED);
        when(providerRepository.findByIdAndDeletedFalse(1L)).thenReturn(Optional.of(approved));

        assertThrows(ProviderOperationException.class, () -> providerOnboardingService.rejectProvider(1L, "duplicate"));

        Provider pending = provider(2L, OnboardingStatus.PENDING);
        when(providerRepository.findByIdAndDeletedFalse(2L)).thenReturn(Optional.of(pending));

        assertThrows(ProviderValidationException.class, () -> providerOnboardingService.rejectProvider(2L, " "));
    }

    @Test
    void unblockShouldRejectProviderThatIsNotBlocked() {
        Provider provider = provider(1L, OnboardingStatus.APPROVED);
        when(providerRepository.findByIdAndDeletedFalse(1L)).thenReturn(Optional.of(provider));
        assertThrows(ProviderOperationException.class, () -> providerOnboardingService.unblockProvider(1L));
    }

    @Test
    void uploadDocumentShouldStorePdfAndMapResponse() {
        Provider provider = provider(1L, OnboardingStatus.PENDING);
        MockMultipartFile file = new MockMultipartFile("file", "pan.pdf", "application/pdf", "pdf".getBytes());
        when(providerRepository.findByIdAndDeletedFalse(1L)).thenReturn(Optional.of(provider));
        when(documentStorageService.store(1L, ProviderDocumentType.PAN, file))
                .thenReturn(new StoredDocument("/docs/pan.pdf", "pan.pdf", "application/pdf"));
        when(providerDocumentRepository.save(any())).thenAnswer(invocation -> {
            ProviderDocument document = invocation.getArgument(0);
            document.setId(8L);
            return document;
        });

        var response = providerOnboardingService.uploadDocument(1L, ProviderDocumentType.PAN, file);

        assertEquals(8L, response.getDocumentId());
        assertEquals("/docs/pan.pdf", response.getFileUrl());
    }

    @Test
    void uploadDocumentShouldAcceptJpgProfilePhotoAndRejectNullContentType() {
        Provider provider = provider(1L, OnboardingStatus.PENDING);
        MockMultipartFile photo = new MockMultipartFile("file", "photo.jpg", "image/jpg", "jpg".getBytes());
        MockMultipartFile unknownPdf = new MockMultipartFile("file", "pan.pdf", null, "pdf".getBytes());
        when(providerRepository.findByIdAndDeletedFalse(1L)).thenReturn(Optional.of(provider));
        when(documentStorageService.store(1L, ProviderDocumentType.PROFILE_PHOTO, photo))
                .thenReturn(new StoredDocument("/docs/photo.jpg", "photo.jpg", "image/jpg"));
        when(providerDocumentRepository.save(any())).thenAnswer(invocation -> {
            ProviderDocument document = invocation.getArgument(0);
            document.setId(10L);
            return document;
        });

        var response = providerOnboardingService.uploadDocument(1L, ProviderDocumentType.PROFILE_PHOTO, photo);

        assertThat(response.getDocumentId()).isEqualTo(10L);
        assertThrows(ProviderFileUploadException.class,
                () -> providerOnboardingService.uploadDocument(1L, ProviderDocumentType.PAN, unknownPdf));
    }

    @Test
    void uploadDocumentShouldValidateEmptyAndContentTypes() {
        Provider provider = provider(1L, OnboardingStatus.PENDING);
        when(providerRepository.findByIdAndDeletedFalse(1L)).thenReturn(Optional.of(provider));
        MockMultipartFile empty = new MockMultipartFile("file", new byte[0]);
        assertThrows(ProviderValidationException.class,
                () -> providerOnboardingService.uploadDocument(1L, ProviderDocumentType.PAN, empty));
        MockMultipartFile imagePan = new MockMultipartFile("file", "pan.png", "image/png", "x".getBytes());
        assertThrows(ProviderFileUploadException.class,
                () -> providerOnboardingService.uploadDocument(1L, ProviderDocumentType.PAN, imagePan));
        MockMultipartFile pdfPhoto = new MockMultipartFile("file", "photo.pdf", "application/pdf", "x".getBytes());
        assertThrows(ProviderFileUploadException.class,
                () -> providerOnboardingService.uploadDocument(1L, ProviderDocumentType.PROFILE_PHOTO, pdfPhoto));
        verify(documentStorageService, never()).store(any(), any(), any());
    }

    @Test
    void documentLookupsShouldReturnOwnedDocumentsAndFailWhenMissing() {
        ProviderDocument document = new ProviderDocument();
        document.setId(8L);
        when(providerDocumentRepository.findById(8L)).thenReturn(Optional.of(document));
        when(providerDocumentRepository.findByIdAndProviderId(8L, 1L)).thenReturn(Optional.of(document));

        assertEquals(document, providerOnboardingService.getDocument(8L));
        assertEquals(document, providerOnboardingService.getProviderDocument(1L, 8L));
        assertThrows(com.lexho.provider.exception.DocumentNotFoundException.class,
                () -> providerOnboardingService.getDocument(9L));
        assertThrows(com.lexho.provider.exception.DocumentNotFoundException.class,
                () -> providerOnboardingService.getProviderDocument(1L, 10L));
    }

    @Test
    void getProviderForAdminShouldReturnProviderAndFailWhenMissing() {
        Provider provider = provider(7L, OnboardingStatus.APPROVED);
        when(providerRepository.findByIdAndDeletedFalse(7L)).thenReturn(Optional.of(provider));
        when(providerRepository.findByIdAndDeletedFalse(8L)).thenReturn(Optional.empty());

        assertThat(providerOnboardingService.getProviderForAdmin(7L)).isSameAs(provider);
        assertThrows(ProviderNotFoundException.class, () -> providerOnboardingService.getProviderForAdmin(8L));
    }

    private static Provider provider(Long id, OnboardingStatus status) {
        Provider provider = new Provider();
        provider.setId(id);
        provider.setProviderCode("PRV1001");
        provider.setOnboardingStatus(status);
        provider.setKycStatus(KycStatus.PENDING);
        return provider;
    }

    private static ServiceCategory category(Long id, String name) {
        ServiceCategory category = new ServiceCategory();
        category.setId(id);
        category.setName(name);
        return category;
    }

    private ProviderOnboardingRequest validRequest() {
        ProviderOnboardingRequest request = new ProviderOnboardingRequest();
        request.setFullName("Raju");
        request.setEmail("raju@gmail.com");
        request.setPhoneNumber("9876543210");
        request.setServiceCategoryId(1L);
        request.setExperienceYears(5);
        request.setCity("Noida");
        request.setState("UP");
        request.setFullAddress("Sector 62");
        request.setCurrentFullAddress("Sector 104");
        request.setServiceAreas(Set.of("Noida", "Delhi").stream().toList());
        request.setAccountNumber("123456789012");
        request.setIfscCode("SBIN0123456");
        return request;
    }
}

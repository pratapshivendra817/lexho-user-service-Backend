package com.lexho.user_service.admin;

import com.lexho.user_service.admin.dto.AdminCategoryUpsertRequest;
import com.lexho.user_service.admin.dto.AdminServiceUpsertRequest;
import com.lexho.user_service.admin.dto.AdminServiceDetailBulkRequest;
import com.lexho.user_service.admin.dto.AdminServiceDetailUpdateRequest;
import com.lexho.user_service.admin.entity.AdminCategoryEntity;
import com.lexho.user_service.admin.entity.AdminServiceEntity;
import com.lexho.user_service.admin.entity.DetailType;
import com.lexho.user_service.admin.entity.ServiceDetailEntity;
import com.lexho.user_service.banner.entity.BannerPromotion;
import com.lexho.user_service.banner.enums.BannerStatus;
import com.lexho.user_service.admin.repository.AdminCategoryRepository;
import com.lexho.user_service.admin.repository.AdminServiceEntityRepository;
import com.lexho.user_service.admin.repository.ServiceDetailRepository;
import com.lexho.user_service.banner.repository.BannerPromotionRepository;
import com.lexho.user_service.admin.service.AdminAuditService;
import com.lexho.user_service.admin.service.AdminCatalogService;
import com.lexho.user_service.exception.BadRequestException;
import com.lexho.user_service.exception.ResourceNotFoundException;
import com.lexho.user_service.security.SecurityUtils;
import com.lexho.user_service.util.MediaUrlResolver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.PageRequest;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminCatalogServiceTest {

    @Mock
    private AdminCategoryRepository adminCategoryRepository;
    @Mock
    private AdminServiceEntityRepository adminServiceEntityRepository;
    @Mock
    private ServiceDetailRepository serviceDetailRepository;
    @Mock
    private BannerPromotionRepository bannerPromotionRepository;
    @Mock
    private AdminAuditService adminAuditService;
    @Mock
    private SecurityUtils securityUtils;
    @Mock
    private MediaUrlResolver mediaUrlResolver;

    private AdminCatalogService adminCatalogService;

    @BeforeEach
    void setup() {
        lenient().when(securityUtils.getCurrentUserId()).thenReturn(Optional.of(1L));
        lenient().when(mediaUrlResolver.toStorablePath(any())).thenAnswer(invocation -> invocation.getArgument(0));
        lenient().when(mediaUrlResolver.toPublicUrl(any())).thenAnswer(invocation -> invocation.getArgument(0));
        adminCatalogService = new AdminCatalogService(
                adminCategoryRepository,
                adminServiceEntityRepository,
                serviceDetailRepository,
                bannerPromotionRepository,
                adminAuditService,
                securityUtils,
                mediaUrlResolver
        );
    }

    @Test
    void createCategoryShouldRejectDuplicateName() {
        AdminCategoryUpsertRequest request = new AdminCategoryUpsertRequest();
        request.setName("Cleaning");
        AdminCategoryEntity existing = new AdminCategoryEntity();
        ReflectionTestUtils.setField(existing, "id", 99L);
        existing.setDeleted(false);
        when(adminCategoryRepository.findByNameIgnoreCase("Cleaning")).thenReturn(Optional.of(existing));

        assertThrows(BadRequestException.class, () -> adminCatalogService.createCategory(request));
        verify(adminCategoryRepository, never()).save(any(AdminCategoryEntity.class));
    }

    @Test
    void createServiceShouldPersistMappedFields() {
        AdminCategoryEntity category = new AdminCategoryEntity();
        category.setName("Cleaning");
        when(adminCategoryRepository.findByIdAndDeletedFalse(7L)).thenReturn(Optional.of(category));

        when(adminServiceEntityRepository.save(any(AdminServiceEntity.class))).thenAnswer(invocation -> {
            AdminServiceEntity entity = invocation.getArgument(0);
            return entity;
        });

        AdminServiceUpsertRequest request = new AdminServiceUpsertRequest();
        request.setName("Bathroom Cleaning");
        request.setCategoryId(7L);
        request.setDescription("Deep cleaning");
        request.setBasePrice(new BigDecimal("499"));
        request.setDurationMinutes(90);
        request.setRating(4.7);
        request.setActive(true);

        adminCatalogService.createService(request);

        ArgumentCaptor<AdminServiceEntity> captor = ArgumentCaptor.forClass(AdminServiceEntity.class);
        verify(adminServiceEntityRepository).save(captor.capture());
        AdminServiceEntity saved = captor.getValue();
        assertEquals("Bathroom Cleaning", saved.getName());
        assertNotNull(saved.getCategory());
        assertEquals("Cleaning", saved.getCategory().getName());
        assertEquals(new BigDecimal("499"), saved.getBasePrice());
        assertEquals(new BigDecimal("499"), saved.getDiscountedPrice());
        assertEquals(90, saved.getDurationMinutes());
        assertEquals(4.7, saved.getAvgRating());
        assertTrue(saved.isActive());
        assertFalse(saved.isDeleted());
    }

    @Test
    void createCategoryShouldRestoreDeletedCategoryAndMapCounts() {
        AdminCategoryEntity category = category(9L, "Old", false);
        category.setDeleted(true);
        when(adminCategoryRepository.findByNameIgnoreCase("Cleaning")).thenReturn(Optional.of(category));
        when(adminCategoryRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(adminServiceEntityRepository.findByCategory_IdAndDeletedFalse(9L)).thenReturn(List.of(service(1L, 4L)));

        AdminCategoryUpsertRequest request = new AdminCategoryUpsertRequest();
        request.setName(" Cleaning ");
        request.setDescription(" Home services ");
        request.setActive(true);
        request.setIconUrl(" /clean.png ");

        var response = adminCatalogService.createCategory(request).getData();

        assertEquals("Cleaning", response.getName());
        assertEquals("Home services", response.getDescription());
        assertTrue(response.isActive());
        assertEquals(1, response.getTotalServicesCount());
        assertEquals(4, response.getTotalBookingsCount());
        assertFalse(category.isDeleted());
        verify(adminAuditService).log(1L, "CATEGORY_CREATE", "admin_categories", 9L, "name=Cleaning");
    }

    @Test
    void updateCategoryShouldRejectDuplicateAndMissingCategory() {
        AdminCategoryUpsertRequest request = new AdminCategoryUpsertRequest();
        request.setName("Cleaning");
        when(adminCategoryRepository.findByIdAndDeletedFalse(3L)).thenReturn(Optional.of(category(3L, "Old", true)));
        when(adminCategoryRepository.existsByNameIgnoreCaseAndDeletedFalseAndIdNot("Cleaning", 3L)).thenReturn(true);

        assertThrows(BadRequestException.class, () -> adminCatalogService.updateCategory(3L, request));
        assertThrows(ResourceNotFoundException.class, () -> adminCatalogService.updateCategory(99L, request));
    }

    @Test
    void deleteCategoryShouldSoftDeleteCategoryAndServices() {
        AdminCategoryEntity category = category(3L, "Cleaning", true);
        AdminServiceEntity first = service(10L, 2L);
        AdminServiceEntity second = service(11L, null);
        when(adminCategoryRepository.findByIdAndDeletedFalse(3L)).thenReturn(Optional.of(category));
        when(adminServiceEntityRepository.findByCategory_IdAndDeletedFalse(3L)).thenReturn(List.of(first, second));

        assertEquals("Category deleted successfully", adminCatalogService.deleteCategory(3L).getMessage());

        assertTrue(category.isDeleted());
        assertFalse(category.isActive());
        assertTrue(first.isDeleted());
        assertFalse(first.isActive());
        verify(adminServiceEntityRepository).saveAll(List.of(first, second));
    }

    @Test
    void listCategoriesShouldClampPagingAndMapResults() {
        AdminCategoryEntity category = category(4L, "Cooking", true);
        when(adminCategoryRepository.search(eq("cook"), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(category)));
        when(adminServiceEntityRepository.findByCategory_IdAndDeletedFalse(4L)).thenReturn(List.of());

        var response = adminCatalogService.listCategories(0, 500, " cook ");

        assertEquals(1, response.getTotal());
        assertEquals("Cooking", response.getData().get(0).getName());
        ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);
        verify(adminCategoryRepository).search(eq("cook"), pageable.capture());
        assertEquals(0, pageable.getValue().getPageNumber());
        assertEquals(100, pageable.getValue().getPageSize());
    }

    @Test
    void addServiceDetailsShouldTrimDeduplicateAndRejectEmptyItems() {
        AdminServiceEntity service = service(5L, 0L);
        when(adminServiceEntityRepository.findByIdAndDeletedFalse(5L)).thenReturn(Optional.of(service));
        when(serviceDetailRepository.saveAll(any())).thenAnswer(invocation -> invocation.getArgument(0));
        AdminServiceDetailBulkRequest request = new AdminServiceDetailBulkRequest();
        request.setType(DetailType.INCLUDED);
        request.setItems(List.of(" Mop floor ", "Mop floor"));

        var response = adminCatalogService.addServiceDetails(5L, request);

        assertEquals(1, response.getData().size());
        assertEquals("Mop floor", response.getData().get(0).getItemName());

        request.setItems(List.of("   "));
        assertThrows(BadRequestException.class, () -> adminCatalogService.addServiceDetails(5L, request));
    }

    @Test
    void updateAndDeleteServiceDetailShouldHandleSuccessAndMissingDetail() {
        AdminServiceEntity service = service(5L, 0L);
        ServiceDetailEntity detail = new ServiceDetailEntity();
        ReflectionTestUtils.setField(detail, "id", 8L);
        detail.setService(service);
        when(adminServiceEntityRepository.findByIdAndDeletedFalse(5L)).thenReturn(Optional.of(service));
        when(serviceDetailRepository.findByIdAndService_Id(8L, 5L)).thenReturn(Optional.of(detail));
        when(serviceDetailRepository.save(detail)).thenReturn(detail);
        AdminServiceDetailUpdateRequest request = new AdminServiceDetailUpdateRequest();
        request.setType(DetailType.EXCLUDED);
        request.setItemName("  Materials  ");

        assertEquals("Materials", adminCatalogService.updateServiceDetail(5L, 8L, request).getData().getItemName());
        assertEquals("Service detail deleted successfully", adminCatalogService.deleteServiceDetail(5L, 8L).getMessage());
        verify(serviceDetailRepository).delete(detail);

        when(serviceDetailRepository.findByIdAndService_Id(9L, 5L)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class,
                () -> adminCatalogService.updateServiceDetail(5L, 9L, request));
    }

    @Test
    void listServicesShouldParseStatusSortAndRejectInvalidStatus() {
        AdminServiceEntity service = service(2L, 7L);
        when(adminServiceEntityRepository.searchCatalog(eq("clean"), eq(true), eq("home"), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(service)));
        when(serviceDetailRepository.findByService_Id(2L)).thenReturn(List.of());

        var response = adminCatalogService.listServices(1, 10, " clean ", " home ", "active", "rating");

        assertEquals(1, response.getTotal());
        assertEquals(2L, response.getData().get(0).getId());
        assertThrows(BadRequestException.class,
                () -> adminCatalogService.listServices(1, 10, null, null, "unknown", null));
    }

    @Test
    void toggleAndDeleteServiceShouldPersistStateChanges() {
        AdminServiceEntity service = service(2L, 0L);
        service.setActive(true);
        when(adminServiceEntityRepository.findByIdAndDeletedFalse(2L)).thenReturn(Optional.of(service));
        when(adminServiceEntityRepository.save(service)).thenReturn(service);
        when(serviceDetailRepository.findByService_Id(2L)).thenReturn(List.of());

        assertFalse(adminCatalogService.toggleServiceStatus(2L).getData().isActive());
        assertEquals("Service deleted successfully", adminCatalogService.deleteService(2L).getMessage());
        assertTrue(service.isDeleted());
        assertFalse(service.isActive());
    }

    @Test
    void updateCategoryStatusAndMediaSnapshotsShouldMapResolvedUrlsAndLimits() {
        AdminCategoryEntity category = category(8L, "Cleaning", true);
        category.setIconUrl("/icon.png");
        ReflectionTestUtils.setField(category, "createdAt", LocalDateTime.of(2026, 6, 20, 10, 0));
        when(adminCategoryRepository.findByIdAndDeletedFalse(8L)).thenReturn(Optional.of(category));
        when(adminCategoryRepository.save(category)).thenReturn(category);
        when(adminServiceEntityRepository.findByCategory_IdAndDeletedFalse(8L)).thenReturn(List.of());
        when(mediaUrlResolver.toPublicUrl("/icon.png")).thenReturn("https://cdn/icon.png");

        var statusResponse = adminCatalogService.updateCategoryStatus(8L, false).getData();
        assertFalse(statusResponse.isActive());

        BannerPromotion banner = new BannerPromotion();
        ReflectionTestUtils.setField(banner, "id", 5L);
        banner.setTitle("Hero");
        banner.setBannerImageUrl("/banner.png");
        banner.setStatus(BannerStatus.ACTIVE);
        banner.setSlotNumber(1);
        banner.setDeleted(false);
        banner.setCreatedAt(LocalDateTime.of(2026, 6, 20, 11, 0));

        when(adminCategoryRepository.findAll(any(Pageable.class))).thenReturn(new PageImpl<>(List.of(category)));
        when(bannerPromotionRepository.findAll(any(Pageable.class))).thenReturn(new PageImpl<>(List.of(banner)));
        when(adminCategoryRepository.findAllByDeletedFalseOrderByCreatedAtDesc(any(PageRequest.class)))
                .thenReturn(List.of(category));
        when(bannerPromotionRepository.findByDeletedFalseOrderByCreatedAtDesc(any(PageRequest.class)))
                .thenReturn(List.of(banner));
        when(mediaUrlResolver.toPublicUrl("/banner.png")).thenReturn("https://cdn/banner.png");

        Map<String, Object> snapshot = adminCatalogService.mediaDebugSnapshot(500).getData();
        Map<String, Object> latest = adminCatalogService.mediaDebugLatestActive(0).getData();

        assertEquals("https://cdn/icon.png", ((List<Map<String, Object>>) snapshot.get("categories")).get(0).get("imageUrlResolved"));
        assertEquals("https://cdn/banner.png", ((List<Map<String, Object>>) snapshot.get("banners")).get(0).get("imageUrlResolved"));
        assertEquals(1, ((List<?>) latest.get("categories")).size());
        verify(adminAuditService).log(1L, "CATEGORY_STATUS_UPDATE", "admin_categories", 8L, "active=false");
    }

    @Test
    void updateServiceGetByIdAndTopServicesShouldUseFallbackCategoryAndImageMapping() {
        AdminCategoryEntity category = category(6L, "Home", true);
        AdminServiceEntity existing = service(4L, null);
        existing.setCategory(category);
        existing.setImageUrl("/service.png");
        when(adminServiceEntityRepository.findByIdAndDeletedFalse(4L)).thenReturn(Optional.of(existing));
        when(adminServiceEntityRepository.save(existing)).thenReturn(existing);
        when(serviceDetailRepository.findByService_Id(4L)).thenReturn(List.of());
        when(adminServiceEntityRepository.findTopBookedServices(any(PageRequest.class))).thenReturn(List.of(existing));
        when(mediaUrlResolver.toPublicUrl("/service.png")).thenReturn("https://cdn/service.png");

        AdminServiceUpsertRequest request = new AdminServiceUpsertRequest();
        request.setName("Updated");
        request.setDescription("  Deep clean  ");
        request.setServiceType("  HOME  ");
        request.setPrice(new BigDecimal("1499"));
        request.setDurationMinutes(null);
        request.setRating(null);
        request.setImageUrl(" /service.png ");
        request.setActive(null);

        var updated = adminCatalogService.updateService(4L, request).getData();
        var fetched = adminCatalogService.getServiceById(4L).getData();
        var top = adminCatalogService.topServices(99).getData();

        assertEquals("Updated", updated.getName());
        assertEquals(60, updated.getDurationMinutes());
        assertEquals(0.0, updated.getRating());
        assertTrue(updated.isActive());
        assertEquals("https://cdn/service.png", fetched.getImageUrl());
        assertEquals(1, top.size());
        verify(adminAuditService).log(1L, "SERVICE_UPDATE", "services", 4L, "name=Updated");
    }

    @Test
    void addServiceDetailsAndListServicesCoverValidationAndSortFallbacks() {
        AdminServiceEntity service = service(5L, 0L);
        when(adminServiceEntityRepository.findByIdAndDeletedFalse(5L)).thenReturn(Optional.of(service));

        AdminServiceDetailBulkRequest bulk = new AdminServiceDetailBulkRequest();
        bulk.setType(DetailType.INCLUDED);
        bulk.setItems(java.util.Collections.singletonList(null));
        assertThrows(BadRequestException.class, () -> adminCatalogService.addServiceDetails(5L, bulk));

        String tooLong = "x".repeat(256);
        ServiceDetailEntity detail = new ServiceDetailEntity();
        detail.setService(service);
        when(serviceDetailRepository.findByIdAndService_Id(8L, 5L)).thenReturn(Optional.of(detail));
        AdminServiceDetailUpdateRequest updateRequest = new AdminServiceDetailUpdateRequest();
        updateRequest.setType(DetailType.INCLUDED);
        updateRequest.setItemName(tooLong);
        assertThrows(BadRequestException.class, () -> adminCatalogService.updateServiceDetail(5L, 8L, updateRequest));

        when(adminServiceEntityRepository.searchCatalog(eq(null), eq(false), eq(null), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(service)));
        when(serviceDetailRepository.findByService_Id(5L)).thenReturn(List.of());
        var response = adminCatalogService.listServices(0, 0, " ", " ", "inactive", "unknown");
        assertEquals(1, response.getTotal());
    }

    @Test
    void summaryAndAnalyticsShouldConvertNullNumbersAndStrings() {
        when(adminServiceEntityRepository.serviceSummaryByCategory())
                .thenReturn(List.<Object[]>of(new Object[]{"Cleaning", "6", null}));
        when(adminServiceEntityRepository.ratingAnalyticsByCategory())
                .thenReturn(List.<Object[]>of(new Object[]{"Cleaning", "4.5", 5, null, "3"}));

        var summary = adminCatalogService.serviceSummary().getData().get(0);
        var analytics = adminCatalogService.serviceAnalytics().getData().get(0);

        assertEquals(6L, summary.getTotalServices());
        assertEquals(0L, summary.getTotalBookings());
        assertEquals(4.5, analytics.getAvgRating());
        assertEquals(0.0, analytics.getMinRating());
        assertEquals(3L, analytics.getServicesCount());
    }

    @Test
    void createServiceShouldRejectMissingPriceAndMissingDefaultCategory() {
        AdminServiceUpsertRequest request = new AdminServiceUpsertRequest();
        request.setName("Cleaning");
        when(adminCategoryRepository.findFirstByNameContainingIgnoreCaseAndDeletedFalse("All Services"))
                .thenReturn(Optional.empty());
        when(adminCategoryRepository.findAllByDeletedFalseOrderByCreatedAtDesc(any())).thenReturn(List.of());
        assertThrows(BadRequestException.class, () -> adminCatalogService.createService(request));

        request.setCategoryId(3L);
        when(adminCategoryRepository.findByIdAndDeletedFalse(3L)).thenReturn(Optional.of(category(3L, "Home", true)));
        assertThrows(BadRequestException.class, () -> adminCatalogService.createService(request));
    }

    private static AdminCategoryEntity category(Long id, String name, boolean active) {
        AdminCategoryEntity category = new AdminCategoryEntity();
        ReflectionTestUtils.setField(category, "id", id);
        category.setName(name);
        category.setActive(active);
        return category;
    }

    private static AdminServiceEntity service(Long id, Long bookings) {
        AdminServiceEntity service = new AdminServiceEntity();
        ReflectionTestUtils.setField(service, "id", id);
        service.setName("Service " + id);
        service.setBasePrice(BigDecimal.TEN);
        service.setTotalBookings(bookings);
        return service;
    }
}

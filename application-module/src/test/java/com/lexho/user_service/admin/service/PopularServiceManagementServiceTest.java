package com.lexho.user_service.admin.service;

import com.lexho.user_service.admin.dto.PopularServiceUpsertRequest;
import com.lexho.user_service.admin.entity.AdminCategoryEntity;
import com.lexho.user_service.admin.entity.AdminServiceEntity;
import com.lexho.user_service.admin.entity.PopularServiceEntity;
import com.lexho.user_service.admin.repository.AdminCategoryRepository;
import com.lexho.user_service.admin.repository.AdminServiceEntityRepository;
import com.lexho.user_service.admin.repository.PopularServiceRepository;
import com.lexho.user_service.admin.repository.PopularServiceView;
import com.lexho.user_service.exception.BadRequestException;
import com.lexho.user_service.exception.ResourceNotFoundException;
import com.lexho.user_service.security.SecurityUtils;
import com.lexho.user_service.util.MediaUrlResolver;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PopularServiceManagementServiceTest {

    private final PopularServiceRepository popularServiceRepository = mock(PopularServiceRepository.class);
    private final AdminServiceEntityRepository serviceRepository = mock(AdminServiceEntityRepository.class);
    private final AdminCategoryRepository categoryRepository = mock(AdminCategoryRepository.class);
    private final AdminAuditService adminAuditService = mock(AdminAuditService.class);
    private final SecurityUtils securityUtils = mock(SecurityUtils.class);
    private final MediaUrlResolver mediaUrlResolver = mock(MediaUrlResolver.class);
    private final PopularServiceManagementService service = new PopularServiceManagementService(
            popularServiceRepository, serviceRepository, categoryRepository, adminAuditService, securityUtils, mediaUrlResolver);

    @Test
    void addPopularServiceShouldCreateAndShiftPriorities() {
        AdminServiceEntity adminService = activeService(10L, "Cut", "/img.png");
        AdminCategoryEntity category = category(20L, "Salon");
        PopularServiceEntity shifted = popular(30L, adminService, category, 1, true);
        when(serviceRepository.findByIdAndDeletedFalse(10L)).thenReturn(Optional.of(adminService));
        when(categoryRepository.findByIdAndDeletedFalse(20L)).thenReturn(Optional.of(category));
        when(popularServiceRepository.findByServiceIdAndCategoryId(10L, 20L)).thenReturn(Optional.empty());
        when(popularServiceRepository.findByCategory_IdAndActiveTrueOrderByPriorityAsc(20L)).thenReturn(List.of(shifted));
        when(popularServiceRepository.save(any(PopularServiceEntity.class))).thenAnswer(invocation -> {
            PopularServiceEntity saved = invocation.getArgument(0);
            ReflectionTestUtils.setField(saved, "id", 31L);
            return saved;
        });
        when(mediaUrlResolver.toPublicUrl("/img.png")).thenReturn("https://cdn/img.png");
        when(securityUtils.getCurrentUserId()).thenReturn(Optional.of(99L));
        PopularServiceUpsertRequest request = request(10L, 20L, 1);

        var response = service.addPopularService(request);

        assertEquals(10L, response.getData().getId());
        assertEquals("https://cdn/img.png", response.getData().getImageUrl());
        assertEquals(2, shifted.getPriority());
        verify(popularServiceRepository).saveAll(List.of(shifted));
        verify(adminAuditService).log(99L, "POPULAR_SERVICE_CREATE", "popular_services", 31L,
                "serviceId=10,categoryId=20,priority=1");
    }

    @Test
    void addPopularServiceShouldReactivateExistingInactivePairAndRejectInvalidStates() {
        AdminServiceEntity adminService = activeService(10L, "Cut", "/img.png");
        AdminCategoryEntity category = category(20L, "Salon");
        PopularServiceEntity inactive = popular(40L, adminService, category, 4, false);
        inactive.setDeletedAt(LocalDateTime.now());
        when(serviceRepository.findByIdAndDeletedFalse(10L)).thenReturn(Optional.of(adminService));
        when(categoryRepository.findByIdAndDeletedFalse(20L)).thenReturn(Optional.of(category));
        when(popularServiceRepository.findByServiceIdAndCategoryId(10L, 20L)).thenReturn(Optional.of(inactive));
        when(popularServiceRepository.findByCategory_IdAndActiveTrueOrderByPriorityAsc(20L)).thenReturn(List.of());
        when(popularServiceRepository.save(any(PopularServiceEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(mediaUrlResolver.toPublicUrl("/img.png")).thenReturn("https://cdn/img.png");
        when(securityUtils.getCurrentUserId()).thenReturn(Optional.of(7L));

        assertThat(service.addPopularService(request(10L, 20L, 2)).getData().getPriority()).isEqualTo(2);
        assertThat(inactive.isActive()).isTrue();
        verify(adminAuditService).log(7L, "POPULAR_SERVICE_REACTIVATE", "popular_services", 40L,
                "serviceId=10,categoryId=20,priority=2");

        adminService.setActive(false);
        assertThrows(BadRequestException.class, () -> service.addPopularService(request(10L, 20L, 1)));
        when(serviceRepository.findByIdAndDeletedFalse(11L)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> service.addPopularService(request(11L, 20L, 1)));
    }

    @Test
    void getDeleteAndRestoreShouldCoverMappingAndValidationBranches() {
        PopularServiceView view = new PopularServiceView() {
            @Override public Long getId() { return 50L; }
            @Override public String getName() { return "Massage"; }
            @Override public String getImageUrl() { return "/m.png"; }
            @Override public BigDecimal getPrice() { return new BigDecimal("499.00"); }
            @Override public Double getRating() { return null; }
            @Override public Integer getPriority() { return 3; }
        };
        when(popularServiceRepository.findActiveServiceViewsByCategory(20L)).thenReturn(List.of(view));
        when(mediaUrlResolver.toPublicUrl("/m.png")).thenReturn("https://cdn/m.png");

        var listed = service.getPopularServicesByCategory(20L);
        assertEquals(0.0, listed.getData().get(0).getRating());
        assertThrows(BadRequestException.class, () -> service.getPopularServicesByCategory(null));

        AdminServiceEntity adminService = activeService(10L, "Cut", "/img.png");
        AdminCategoryEntity category = category(20L, "Salon");
        PopularServiceEntity active = popular(60L, adminService, category, 3, true);
        when(popularServiceRepository.findById(60L)).thenReturn(Optional.of(active));
        when(securityUtils.getCurrentUserId()).thenReturn(Optional.of(9L));
        assertEquals("Popular service deleted successfully", service.deletePopularService(60L, "  old  ").getMessage());
        assertThat(active.isActive()).isFalse();
        assertEquals("old", active.getDeleteReason());
        verify(adminAuditService).log(9L, "POPULAR_SERVICE_DELETE", "popular_services", 60L,
                "softDeactivate=true,deletedBy=9,deleteReason=old");

        PopularServiceEntity inactive = popular(61L, adminService, category, 5, false);
        when(popularServiceRepository.findByIdAndActiveFalse(61L)).thenReturn(Optional.of(inactive));
        when(popularServiceRepository.findByCategory_IdAndActiveTrueOrderByPriorityAsc(20L)).thenReturn(List.of());
        when(popularServiceRepository.save(any(PopularServiceEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(mediaUrlResolver.toPublicUrl("/img.png")).thenReturn("https://cdn/img.png");
        assertEquals(5, service.restorePopularService(61L, null).getData().getPriority());

        when(popularServiceRepository.findById(62L)).thenReturn(Optional.of(popular(62L, adminService, category, 1, false)));
        assertThrows(BadRequestException.class, () -> service.deletePopularService(62L, "x"));
        assertThrows(BadRequestException.class, () -> service.restorePopularService(61L, 0));
        when(popularServiceRepository.findByIdAndActiveFalse(99L)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> service.restorePopularService(99L, 1));
    }

    @Test
    void addShouldRejectAlreadyActivePairAndMissingCategory() {
        AdminServiceEntity adminService = activeService(10L, "Cut", "/img.png");
        AdminCategoryEntity category = category(20L, "Salon");
        PopularServiceEntity active = popular(70L, adminService, category, 1, true);
        when(serviceRepository.findByIdAndDeletedFalse(10L)).thenReturn(Optional.of(adminService));
        when(categoryRepository.findByIdAndDeletedFalse(20L)).thenReturn(Optional.of(category));
        when(popularServiceRepository.findByServiceIdAndCategoryId(10L, 20L)).thenReturn(Optional.of(active));
        when(popularServiceRepository.findByCategory_IdAndActiveTrueOrderByPriorityAsc(20L)).thenReturn(List.of());

        assertThrows(BadRequestException.class, () -> service.addPopularService(request(10L, 20L, 1)));

        when(categoryRepository.findByIdAndDeletedFalse(99L)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> service.addPopularService(request(10L, 99L, 1)));
        verify(popularServiceRepository, never()).save(any(PopularServiceEntity.class));
    }

    private PopularServiceUpsertRequest request(Long serviceId, Long categoryId, Integer priority) {
        PopularServiceUpsertRequest request = new PopularServiceUpsertRequest();
        request.setServiceId(serviceId);
        request.setCategoryId(categoryId);
        request.setPriority(priority);
        return request;
    }

    private AdminServiceEntity activeService(Long id, String name, String imageUrl) {
        AdminServiceEntity entity = new AdminServiceEntity();
        ReflectionTestUtils.setField(entity, "id", id);
        entity.setName(name);
        entity.setBasePrice(new BigDecimal("199.00"));
        entity.setAvgRating(4.5);
        entity.setImageUrl(imageUrl);
        entity.setActive(true);
        return entity;
    }

    private AdminCategoryEntity category(Long id, String name) {
        AdminCategoryEntity category = new AdminCategoryEntity();
        ReflectionTestUtils.setField(category, "id", id);
        category.setName(name);
        return category;
    }

    private PopularServiceEntity popular(Long id, AdminServiceEntity service, AdminCategoryEntity category, int priority, boolean active) {
        PopularServiceEntity entity = new PopularServiceEntity();
        ReflectionTestUtils.setField(entity, "id", id);
        entity.setService(service);
        entity.setCategory(category);
        entity.setPriority(priority);
        entity.setActive(active);
        return entity;
    }
}

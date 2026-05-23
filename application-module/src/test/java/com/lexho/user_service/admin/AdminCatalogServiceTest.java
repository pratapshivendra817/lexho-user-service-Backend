package com.lexho.user_service.admin;

import com.lexho.user_service.admin.dto.AdminCategoryUpsertRequest;
import com.lexho.user_service.admin.dto.AdminServiceUpsertRequest;
import com.lexho.user_service.admin.entity.AdminCategoryEntity;
import com.lexho.user_service.admin.entity.AdminServiceEntity;
import com.lexho.user_service.admin.repository.AdminCategoryRepository;
import com.lexho.user_service.admin.repository.AdminServiceEntityRepository;
import com.lexho.user_service.admin.repository.ServiceDetailRepository;
import com.lexho.user_service.banner.repository.BannerPromotionRepository;
import com.lexho.user_service.admin.service.AdminAuditService;
import com.lexho.user_service.admin.service.AdminCatalogService;
import com.lexho.user_service.exception.BadRequestException;
import com.lexho.user_service.security.SecurityUtils;
import com.lexho.user_service.util.MediaUrlResolver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.Optional;

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
        assertEquals(90, saved.getDurationMinutes());
        assertEquals(4.7, saved.getAvgRating());
        assertTrue(saved.isActive());
        assertFalse(saved.isDeleted());
    }
}

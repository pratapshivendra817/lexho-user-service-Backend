package com.lexho.user_service.admin;

import com.lexho.user_service.admin.dto.PopularServiceUpsertRequest;
import com.lexho.user_service.admin.entity.AdminCategoryEntity;
import com.lexho.user_service.admin.entity.AdminServiceEntity;
import com.lexho.user_service.admin.entity.PopularServiceEntity;
import com.lexho.user_service.admin.repository.AdminCategoryRepository;
import com.lexho.user_service.admin.repository.AdminServiceEntityRepository;
import com.lexho.user_service.admin.repository.PopularServiceRepository;
import com.lexho.user_service.admin.service.AdminAuditService;
import com.lexho.user_service.admin.service.PopularServiceManagementService;
import com.lexho.user_service.exception.BadRequestException;
import com.lexho.user_service.security.SecurityUtils;
import com.lexho.user_service.util.MediaUrlResolver;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PopularServiceManagementServiceTest {

    @Mock
    private PopularServiceRepository popularServiceRepository;
    @Mock
    private AdminServiceEntityRepository adminServiceEntityRepository;
    @Mock
    private AdminCategoryRepository adminCategoryRepository;
    @Mock
    private AdminAuditService adminAuditService;
    @Mock
    private SecurityUtils securityUtils;
    @Mock
    private MediaUrlResolver mediaUrlResolver;

    @InjectMocks
    private PopularServiceManagementService popularServiceManagementService;

    @Test
    void addPopularServiceShouldFailOnDuplicateServiceCategory() {
        PopularServiceUpsertRequest request = new PopularServiceUpsertRequest();
        request.setServiceId(12L);
        request.setCategoryId(2L);
        request.setPriority(1);

        AdminServiceEntity service = new AdminServiceEntity();
        ReflectionTestUtils.setField(service, "id", 12L);
        service.setName("Sofa Cleaning");
        service.setBasePrice(BigDecimal.valueOf(499));
        service.setAvgRating(4.7);
        service.setActive(true);
        AdminCategoryEntity category = new AdminCategoryEntity();
        ReflectionTestUtils.setField(category, "id", 2L);
        category.setName("Cleaning");

        when(adminServiceEntityRepository.findByIdAndDeletedFalse(12L)).thenReturn(Optional.of(service));
        when(adminCategoryRepository.findByIdAndDeletedFalse(2L)).thenReturn(Optional.of(category));
        PopularServiceEntity existing = new PopularServiceEntity();
        ReflectionTestUtils.setField(existing, "id", 100L);
        existing.setService(service);
        existing.setCategory(category);
        existing.setPriority(1);
        existing.setActive(true);
        when(popularServiceRepository.findByServiceIdAndCategoryId(12L, 2L)).thenReturn(Optional.of(existing));

        assertThrows(BadRequestException.class, () -> popularServiceManagementService.addPopularService(request));
        verify(popularServiceRepository, never()).save(any());
    }

    @Test
    void addPopularServiceShouldShiftExistingPriorities() {
        PopularServiceUpsertRequest request = new PopularServiceUpsertRequest();
        request.setServiceId(12L);
        request.setCategoryId(2L);
        request.setPriority(1);

        AdminServiceEntity service = new AdminServiceEntity();
        ReflectionTestUtils.setField(service, "id", 12L);
        service.setName("Sofa Cleaning");
        service.setBasePrice(BigDecimal.valueOf(499));
        service.setAvgRating(4.7);
        service.setActive(true);
        AdminCategoryEntity category = new AdminCategoryEntity();
        ReflectionTestUtils.setField(category, "id", 2L);
        category.setName("Cleaning");

        when(adminServiceEntityRepository.findByIdAndDeletedFalse(12L)).thenReturn(Optional.of(service));
        when(adminCategoryRepository.findByIdAndDeletedFalse(2L)).thenReturn(Optional.of(category));
        when(popularServiceRepository.findByServiceIdAndCategoryId(12L, 2L)).thenReturn(Optional.empty());
        PopularServiceEntity priorityOwner = new PopularServiceEntity();
        ReflectionTestUtils.setField(priorityOwner, "id", 101L);
        priorityOwner.setCategory(category);
        priorityOwner.setService(service);
        priorityOwner.setPriority(1);
        priorityOwner.setActive(true);
        when(popularServiceRepository.findByCategory_IdAndActiveTrueOrderByPriorityAsc(2L))
                .thenReturn(List.of(priorityOwner));
        when(popularServiceRepository.save(any(PopularServiceEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        var response = popularServiceManagementService.addPopularService(request);
        assertNotNull(response);
        assertEquals(2, priorityOwner.getPriority());
        verify(popularServiceRepository).saveAll(any());
    }

    @Test
    void getPopularServicesByCategoryShouldFailWithoutCategoryId() {
        BadRequestException ex = assertThrows(BadRequestException.class,
                () -> popularServiceManagementService.getPopularServicesByCategory(null));
        assertEquals("categoryId is required", ex.getMessage());
    }

    @Test
    void addPopularServiceShouldReactivateSoftDeletedRecord() {
        PopularServiceUpsertRequest request = new PopularServiceUpsertRequest();
        request.setServiceId(12L);
        request.setCategoryId(2L);
        request.setPriority(2);

        AdminServiceEntity service = new AdminServiceEntity();
        ReflectionTestUtils.setField(service, "id", 12L);
        service.setName("Sofa Cleaning");
        service.setBasePrice(BigDecimal.valueOf(499));
        service.setAvgRating(4.7);
        service.setActive(true);
        AdminCategoryEntity category = new AdminCategoryEntity();
        ReflectionTestUtils.setField(category, "id", 2L);
        category.setName("Cleaning");

        PopularServiceEntity inactive = new PopularServiceEntity();
        ReflectionTestUtils.setField(inactive, "id", 200L);
        inactive.setService(service);
        inactive.setCategory(category);
        inactive.setPriority(5);
        inactive.setActive(false);

        when(adminServiceEntityRepository.findByIdAndDeletedFalse(12L)).thenReturn(Optional.of(service));
        when(adminCategoryRepository.findByIdAndDeletedFalse(2L)).thenReturn(Optional.of(category));
        when(popularServiceRepository.findByServiceIdAndCategoryId(12L, 2L)).thenReturn(Optional.of(inactive));
        when(popularServiceRepository.findByCategory_IdAndActiveTrueOrderByPriorityAsc(2L)).thenReturn(List.of());
        when(popularServiceRepository.save(inactive)).thenReturn(inactive);

        popularServiceManagementService.addPopularService(request);

        assertEquals(2, inactive.getPriority());
        assertTrue(inactive.isActive());
        verify(popularServiceRepository).save(inactive);
    }
}

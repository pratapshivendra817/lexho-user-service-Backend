package com.lexho.user_service.admin;

import com.lexho.user_service.admin.dto.PopularServiceUpsertRequest;
import com.lexho.user_service.admin.entity.AdminCategoryEntity;
import com.lexho.user_service.admin.entity.AdminServiceEntity;
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
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
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
        when(popularServiceRepository.existsByService_IdAndCategory_IdAndActiveTrue(12L, 2L)).thenReturn(true);

        assertThrows(BadRequestException.class, () -> popularServiceManagementService.addPopularService(request));
    }

    @Test
    void addPopularServiceShouldFailOnDuplicatePriority() {
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
        when(popularServiceRepository.existsByService_IdAndCategory_IdAndActiveTrue(12L, 2L)).thenReturn(false);
        when(popularServiceRepository.existsByCategory_IdAndPriorityAndActiveTrue(2L, 1)).thenReturn(true);

        assertThrows(BadRequestException.class, () -> popularServiceManagementService.addPopularService(request));
    }

    @Test
    void getPopularServicesByCategoryShouldFailWithoutCategoryId() {
        BadRequestException ex = assertThrows(BadRequestException.class,
                () -> popularServiceManagementService.getPopularServicesByCategory(null));
        assertEquals("categoryId is required", ex.getMessage());
    }
}

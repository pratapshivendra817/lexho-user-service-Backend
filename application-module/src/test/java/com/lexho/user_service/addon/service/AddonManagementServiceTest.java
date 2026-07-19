package com.lexho.user_service.addon.service;

import com.lexho.user_service.addon.dto.AddonCategoryMappingRequest;
import com.lexho.user_service.addon.dto.AddonSelectionRequest;
import com.lexho.user_service.addon.dto.AddonServiceMappingRequest;
import com.lexho.user_service.addon.dto.AddonStatusRequest;
import com.lexho.user_service.addon.dto.AddonUpsertRequest;
import com.lexho.user_service.addon.entity.AddonEntity;
import com.lexho.user_service.addon.entity.CartItemAddonEntity;
import com.lexho.user_service.addon.entity.CategoryAddonEntity;
import com.lexho.user_service.addon.entity.ServiceAddonEntity;
import com.lexho.user_service.addon.repository.AddonRepository;
import com.lexho.user_service.addon.repository.CartItemAddonRepository;
import com.lexho.user_service.addon.repository.CategoryAddonRepository;
import com.lexho.user_service.addon.repository.ServiceAddonRepository;
import com.lexho.user_service.admin.entity.AdminCategoryEntity;
import com.lexho.user_service.admin.entity.AdminServiceEntity;
import com.lexho.user_service.admin.repository.AdminCategoryRepository;
import com.lexho.user_service.admin.repository.AdminServiceEntityRepository;
import com.lexho.user_service.exception.BadRequestException;
import com.lexho.user_service.exception.CodedValidationException;
import com.lexho.user_service.exception.ResourceNotFoundException;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AddonManagementServiceTest {

    private final AddonRepository addonRepository = mock(AddonRepository.class);
    private final ServiceAddonRepository serviceAddonRepository = mock(ServiceAddonRepository.class);
    private final CategoryAddonRepository categoryAddonRepository = mock(CategoryAddonRepository.class);
    private final CartItemAddonRepository cartItemAddonRepository = mock(CartItemAddonRepository.class);
    private final AdminServiceEntityRepository serviceRepository = mock(AdminServiceEntityRepository.class);
    private final AdminCategoryRepository categoryRepository = mock(AdminCategoryRepository.class);
    private final AddonManagementService service = new AddonManagementService(
            addonRepository, serviceAddonRepository, categoryAddonRepository, cartItemAddonRepository,
            serviceRepository, categoryRepository);

    @Test
    void createShouldNormalizeFieldsAndSyncInlineMappings() {
        when(categoryRepository.findByIdAndDeletedFalse(2L)).thenReturn(Optional.of(new AdminCategoryEntity()));
        when(serviceRepository.findByIdAndDeletedFalse(3L)).thenReturn(Optional.of(new AdminServiceEntity()));
        when(categoryAddonRepository.findByAddonId(10L)).thenReturn(List.of());
        when(serviceAddonRepository.findByAddonId(10L)).thenReturn(List.of());
        when(categoryAddonRepository.findByCategoryIdAndAddonId(2L, 10L)).thenReturn(Optional.empty());
        when(serviceAddonRepository.findByServiceIdAndAddonId(3L, 10L)).thenReturn(Optional.empty());
        when(addonRepository.save(any(AddonEntity.class))).thenAnswer(invocation -> {
            AddonEntity saved = invocation.getArgument(0);
            ReflectionTestUtils.setField(saved, "id", 10L);
            ReflectionTestUtils.setField(saved, "createdAt", LocalDateTime.now());
            ReflectionTestUtils.setField(saved, "updatedAt", LocalDateTime.now());
            return saved;
        });
        when(categoryAddonRepository.save(any(CategoryAddonEntity.class))).thenAnswer(invocation -> {
            CategoryAddonEntity saved = invocation.getArgument(0);
            ReflectionTestUtils.setField(saved, "id", 20L);
            return saved;
        });
        when(serviceAddonRepository.save(any(ServiceAddonEntity.class))).thenAnswer(invocation -> {
            ServiceAddonEntity saved = invocation.getArgument(0);
            ReflectionTestUtils.setField(saved, "id", 30L);
            return saved;
        });

        var response = service.create(upsert(" Addon ", " desc ", "25.50", List.of(2L), List.of(3L)));

        assertEquals(10L, response.getAddonId());
        assertEquals("Addon", response.getAddonName());
        assertEquals("desc", response.getDescription());
        verify(categoryAddonRepository).save(any(CategoryAddonEntity.class));
        verify(serviceAddonRepository).save(any(ServiceAddonEntity.class));
    }

    @Test
    void mapToServicesAndCategoriesShouldDistinctIdsAndValidateTargets() {
        AddonEntity addon = addon(10L, true, false, false, 1, 1);
        when(addonRepository.findById(10L)).thenReturn(Optional.of(addon));
        when(serviceRepository.findByIdAndDeletedFalse(3L)).thenReturn(Optional.of(new AdminServiceEntity()));
        when(categoryRepository.findByIdAndDeletedFalse(2L)).thenReturn(Optional.of(new AdminCategoryEntity()));
        when(serviceAddonRepository.findByServiceIdAndAddonId(3L, 10L)).thenReturn(Optional.empty());
        when(categoryAddonRepository.findByCategoryIdAndAddonId(2L, 10L)).thenReturn(Optional.empty());
        when(serviceAddonRepository.save(any(ServiceAddonEntity.class))).thenAnswer(invocation -> {
            ServiceAddonEntity saved = invocation.getArgument(0);
            ReflectionTestUtils.setField(saved, "id", 11L);
            return saved;
        });
        when(categoryAddonRepository.save(any(CategoryAddonEntity.class))).thenAnswer(invocation -> {
            CategoryAddonEntity saved = invocation.getArgument(0);
            ReflectionTestUtils.setField(saved, "id", 12L);
            return saved;
        });

        AddonServiceMappingRequest serviceRequest = new AddonServiceMappingRequest();
        serviceRequest.setAddonId(10L);
        serviceRequest.setServiceIds(List.of(3L, 3L));
        AddonCategoryMappingRequest categoryRequest = new AddonCategoryMappingRequest();
        categoryRequest.setAddonId(10L);
        categoryRequest.setCategoryIds(List.of(2L, 2L));

        assertEquals(1, service.mapToServices(serviceRequest).size());
        assertEquals(-12L, service.mapToCategories(categoryRequest).get(0).getMappingId());
        serviceRequest.setServiceIds(List.of());
        assertThrows(BadRequestException.class, () -> service.mapToServices(serviceRequest));
    }

    @Test
    void validateSelectionsShouldAutoIncludeRequiredAndRejectUnavailableOrExceededQuantity() {
        AdminServiceEntity adminService = new AdminServiceEntity();
        ReflectionTestUtils.setField(adminService, "id", 3L);
        ServiceAddonEntity mapping = new ServiceAddonEntity();
        mapping.setServiceId(3L);
        mapping.setAddonId(10L);
        AddonEntity required = addon(10L, true, true, true, 2, 3);
        when(serviceAddonRepository.findByServiceId(3L)).thenReturn(List.of(mapping));
        when(addonRepository.findByIdInAndActiveTrue(any())).thenReturn(List.of(required));
        when(addonRepository.findById(10L)).thenReturn(Optional.of(required));

        var selection = service.validateSelections(adminService, null);

        assertEquals(1, selection.getAddons().size());
        assertEquals(2, selection.getAddons().get(0).getQuantity());

        AddonSelectionRequest request = new AddonSelectionRequest();
        request.setAddonId(10L);
        request.setQuantity(4);
        assertThrows(CodedValidationException.class, () -> service.validateSelections(adminService, List.of(request)));
    }

    @Test
    void statusDeleteListAndRemoveMappingShouldUseRepositories() {
        AddonEntity addon = addon(10L, true, false, false, 1, 1);
        when(addonRepository.findById(10L)).thenReturn(Optional.of(addon));
        when(addonRepository.save(addon)).thenReturn(addon);
        when(addonRepository.findByAddonNameContainingIgnoreCase(any(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(addon)));
        when(addonRepository.findAll(any(Pageable.class))).thenReturn(new PageImpl<>(List.of(addon)));
        when(categoryAddonRepository.findByAddonId(10L)).thenReturn(List.of());
        when(serviceAddonRepository.findByAddonId(10L)).thenReturn(List.of());
        when(serviceAddonRepository.existsById(99L)).thenReturn(true);
        when(categoryAddonRepository.existsById(77L)).thenReturn(true);

        AddonStatusRequest statusRequest = new AddonStatusRequest();
        statusRequest.setActive(false);

        assertEquals(false, service.updateStatus(10L, statusRequest).isActive());
        assertEquals(1, service.list(-1, 1000, " add ").getContent().size());
        assertEquals(1, service.list(0, 5, " ").getContent().size());
        service.delete(10L);
        service.removeMapping(99L);
        service.removeMapping(-77L);

        verify(cartItemAddonRepository).deleteByAddonId(10L);
        verify(serviceAddonRepository).deleteById(99L);
        verify(categoryAddonRepository).deleteById(77L);
        assertThrows(ResourceNotFoundException.class, () -> service.details(404L));
    }

    @Test
    void updateShouldDeleteStaleMappingsAndValidateNullMappingIds() {
        AddonEntity addon = addon(10L, true, true, false, 1, 2);
        CategoryAddonEntity staleCategory = new CategoryAddonEntity();
        staleCategory.setCategoryId(4L);
        ServiceAddonEntity staleService = new ServiceAddonEntity();
        staleService.setServiceId(5L);

        when(addonRepository.findById(10L)).thenReturn(Optional.of(addon));
        when(addonRepository.save(any(AddonEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(categoryRepository.findByIdAndDeletedFalse(2L)).thenReturn(Optional.of(new AdminCategoryEntity()));
        when(serviceRepository.findByIdAndDeletedFalse(3L)).thenReturn(Optional.of(new AdminServiceEntity()));
        when(categoryAddonRepository.findByAddonId(10L)).thenReturn(List.of(staleCategory));
        when(serviceAddonRepository.findByAddonId(10L)).thenReturn(List.of(staleService));
        when(categoryAddonRepository.findByCategoryIdAndAddonId(2L, 10L)).thenReturn(Optional.empty());
        when(serviceAddonRepository.findByServiceIdAndAddonId(3L, 10L)).thenReturn(Optional.empty());

        service.update(10L, upsert("Updated", "desc", "9.99", List.of(2L), List.of(3L)));

        verify(categoryAddonRepository).delete(staleCategory);
        verify(serviceAddonRepository).delete(staleService);
        assertThrows(BadRequestException.class, () -> service.removeMapping(null));
        assertThrows(ResourceNotFoundException.class, () -> service.removeMapping(-999L));
    }

    @Test
    void availableAddonsForServiceAndValidationCoverCategoryAndErrorBranches() {
        AdminServiceEntity adminService = new AdminServiceEntity();
        ReflectionTestUtils.setField(adminService, "id", 3L);
        AdminCategoryEntity category = new AdminCategoryEntity();
        ReflectionTestUtils.setField(category, "id", 8L);
        category.setName("Home");
        adminService.setCategory(category);

        CategoryAddonEntity categoryMapping = new CategoryAddonEntity();
        categoryMapping.setCategoryId(8L);
        categoryMapping.setAddonId(10L);
        ServiceAddonEntity serviceMapping = new ServiceAddonEntity();
        serviceMapping.setServiceId(3L);
        serviceMapping.setAddonId(11L);

        AddonEntity inactive = addon(10L, false, false, false, 1, 1);
        AddonEntity active = addon(11L, true, false, false, 1, 1);
        when(categoryAddonRepository.findByCategoryId(8L)).thenReturn(List.of(categoryMapping));
        when(serviceAddonRepository.findByServiceId(3L)).thenReturn(List.of(serviceMapping));
        when(addonRepository.findByIdInAndActiveTrue(any())).thenReturn(List.of(active));
        when(addonRepository.findById(11L)).thenReturn(Optional.of(active));

        assertThat(service.availableAddonsForService(null)).isEmpty();
        assertThat(service.availableAddonsForService(adminService)).hasSize(1);

        AddonSelectionRequest missingId = new AddonSelectionRequest();
        assertThrows(BadRequestException.class, () -> service.validateSelections(adminService, List.of(missingId)));

        AddonSelectionRequest unavailable = new AddonSelectionRequest();
        unavailable.setAddonId(10L);
        assertThrows(CodedValidationException.class, () -> service.validateSelections(adminService, List.of(unavailable)));

        when(addonRepository.findById(11L)).thenReturn(Optional.of(inactive));
        AddonSelectionRequest selected = new AddonSelectionRequest();
        selected.setAddonId(11L);
        assertThrows(CodedValidationException.class, () -> service.validateSelections(adminService, List.of(selected)));
    }

    private AddonUpsertRequest upsert(String name, String description, String price,
                                      List<Long> categoryIds, List<Long> serviceIds) {
        AddonUpsertRequest request = new AddonUpsertRequest();
        request.setAddonName(name);
        request.setDescription(description);
        request.setPrice(new BigDecimal(price));
        request.setActive(true);
        request.setQuantityAllowed(true);
        request.setMinQuantity(1);
        request.setMaxQuantity(2);
        request.setRequired(false);
        request.setCategoryIds(categoryIds);
        request.setServiceIds(serviceIds);
        return request;
    }

    private AddonEntity addon(Long id, boolean active, boolean quantityAllowed, boolean required,
                              int minQuantity, int maxQuantity) {
        AddonEntity addon = new AddonEntity();
        ReflectionTestUtils.setField(addon, "id", id);
        addon.setAddonName("Addon " + id);
        addon.setDescription("Description");
        addon.setPrice(new BigDecimal("10.00"));
        addon.setActive(active);
        addon.setQuantityAllowed(quantityAllowed);
        addon.setRequired(required);
        addon.setMinQuantity(minQuantity);
        addon.setMaxQuantity(maxQuantity);
        assertTrue(addon.getId() != null);
        return addon;
    }
}

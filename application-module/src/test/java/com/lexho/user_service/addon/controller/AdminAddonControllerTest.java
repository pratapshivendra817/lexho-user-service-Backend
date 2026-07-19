package com.lexho.user_service.addon.controller;

import com.lexho.user_service.addon.dto.AddonCategoryMappingRequest;
import com.lexho.user_service.addon.dto.AddonMappingResponse;
import com.lexho.user_service.addon.dto.AddonResponse;
import com.lexho.user_service.addon.dto.AddonServiceMappingRequest;
import com.lexho.user_service.addon.dto.AddonStatusRequest;
import com.lexho.user_service.addon.dto.AddonUpsertRequest;
import com.lexho.user_service.addon.service.AddonManagementService;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AdminAddonControllerTest {

    private final AddonManagementService addonManagementService = mock(AddonManagementService.class);
    private final AdminAddonController controller = new AdminAddonController(addonManagementService);

    @Test
    void controllerShouldDelegateAllEndpoints() {
        AddonUpsertRequest upsertRequest = new AddonUpsertRequest();
        upsertRequest.setAddonName("Addon");
        upsertRequest.setPrice(BigDecimal.TEN);
        AddonResponse addon = new AddonResponse();
        addon.setAddonId(1L);
        addon.setAddonName("Addon");
        addon.setDescription("desc");
        addon.setPrice(BigDecimal.TEN);
        addon.setActive(true);
        AddonStatusRequest statusRequest = new AddonStatusRequest();
        statusRequest.setActive(Boolean.TRUE);
        AddonMappingResponse mapping = new AddonMappingResponse(9L, 1L, 2L, "SERVICE", LocalDateTime.now());
        AddonServiceMappingRequest serviceMappingRequest = new AddonServiceMappingRequest();
        serviceMappingRequest.setAddonId(1L);
        serviceMappingRequest.setServiceIds(List.of(2L, 3L));
        AddonCategoryMappingRequest categoryMappingRequest = new AddonCategoryMappingRequest();
        categoryMappingRequest.setAddonId(1L);
        categoryMappingRequest.setCategoryIds(List.of(4L));

        when(addonManagementService.create(upsertRequest)).thenReturn(addon);
        when(addonManagementService.update(1L, upsertRequest)).thenReturn(addon);
        when(addonManagementService.details(1L)).thenReturn(addon);
        when(addonManagementService.list(0, 20, "a")).thenReturn(new PageImpl<>(List.of(addon), PageRequest.of(0, 20), 1));
        when(addonManagementService.updateStatus(1L, statusRequest)).thenReturn(addon);
        when(addonManagementService.mapToServices(serviceMappingRequest)).thenReturn(List.of(mapping));
        when(addonManagementService.mapToCategories(categoryMappingRequest)).thenReturn(List.of(mapping));

        assertThat(controller.create(upsertRequest).getData()).isEqualTo(addon);
        assertThat(controller.update(1L, upsertRequest).getData()).isEqualTo(addon);
        assertThat(controller.details(1L).getData()).isEqualTo(addon);
        assertThat(controller.list(0, 20, "a").getData()).containsEntry("totalElements", 1L);
        assertThat(controller.status(1L, statusRequest).getData()).isEqualTo(addon);
        assertThat(controller.mapService(serviceMappingRequest).getData()).containsExactly(mapping);
        assertThat(controller.mapCategory(categoryMappingRequest).getData()).containsExactly(mapping);
        assertThat(controller.delete(1L).getMessage()).isEqualTo("Add-on deleted");
        assertThat(controller.removeMapping(9L).getMessage()).isEqualTo("Add-on mapping removed");

        verify(addonManagementService).delete(1L);
        verify(addonManagementService).removeMapping(9L);
    }
}

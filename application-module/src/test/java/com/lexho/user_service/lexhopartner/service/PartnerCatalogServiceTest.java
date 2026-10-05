package com.lexho.user_service.lexhopartner.service;

import com.lexho.user_service.admin.entity.AdminCategoryEntity;
import com.lexho.user_service.admin.entity.AdminServiceEntity;
import com.lexho.user_service.admin.entity.DetailType;
import com.lexho.user_service.admin.entity.ServiceDetailEntity;
import com.lexho.user_service.admin.repository.AdminCategoryRepository;
import com.lexho.user_service.admin.repository.AdminServiceEntityRepository;
import com.lexho.user_service.admin.repository.ServiceDetailRepository;
import com.lexho.user_service.localization.dynamic.DynamicContentTranslationService;
import com.lexho.user_service.util.MediaUrlResolver;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PartnerCatalogServiceTest {

    private final AdminCategoryRepository categoryRepository = mock(AdminCategoryRepository.class);
    private final AdminServiceEntityRepository serviceRepository = mock(AdminServiceEntityRepository.class);
    private final ServiceDetailRepository serviceDetailRepository = mock(ServiceDetailRepository.class);
    private final DynamicContentTranslationService translationService = mock(DynamicContentTranslationService.class);
    private final PartnerCatalogService service = new PartnerCatalogService(
            categoryRepository,
            serviceRepository,
            serviceDetailRepository,
            new MediaUrlResolver(),
            translationService
    );

    @Test
    void categoriesTranslatePartnerFacingName() {
        AdminCategoryEntity category = category(10L, "Cleaning");
        category.setDescription("Home service category");
        when(categoryRepository.findActiveForCustomer(any(Pageable.class))).thenReturn(List.of(category));
        when(translationService.translateForPartner(7L, null, "category", 10L, "name", "Cleaning"))
                .thenReturn("Safai");
        when(translationService.translateForPartner(7L, null, "category", 10L, "description", "Home service category"))
                .thenReturn("Ghar seva category");

        var response = service.categories(7L, 20);

        assertThat(response).hasSize(1);
        assertThat(response.get(0).getName()).isEqualTo("Safai");
        assertThat(response.get(0).getCategoryName()).isEqualTo("Safai");
        assertThat(response.get(0).getDisplayName()).isEqualTo("Safai");
        assertThat(response.get(0).getServiceName()).isEqualTo("Safai");
        assertThat(response.get(0).getId()).isEqualTo(10L);
        assertThat(response.get(0).getCategoryId()).isEqualTo(10L);
        assertThat(response.get(0).getCode()).isEqualTo("10");
        assertThat(response.get(0).getCategoryCode()).isEqualTo("10");
        assertThat(response.get(0).getServiceCode()).isEqualTo("10");
        assertThat(response.get(0).getEnabled()).isTrue();
        assertThat(response.get(0).getActive()).isTrue();
        assertThat(response.get(0).getIsActive()).isTrue();
        assertThat(response.get(0).getStatus()).isEqualTo("ACTIVE");
        assertThat(response.get(0).getDescription()).isEqualTo("Ghar seva category");
        verify(translationService).translateForPartner(7L, null, "category", 10L, "name", "Cleaning");
        verify(translationService).translateForPartner(7L, null, "category", 10L, "description", "Home service category");
    }

    @Test
    void categoryDropdownExposesSupportedArrayKeys() {
        AdminCategoryEntity category = category(10L, "Cleaning");
        when(categoryRepository.findActiveForCustomer(any(Pageable.class))).thenReturn(List.of(category));
        when(translationService.translateForPartner(any(), any(), any(), any(), any(), any()))
                .thenAnswer(invocation -> invocation.getArgument(5));

        var response = service.categoryDropdown(7L, 20);

        assertThat(response).containsKeys(
                "categories",
                "serviceTypes",
                "services",
                "results",
                "rows",
                "records",
                "items",
                "content"
        );
        assertThat(response.get("categories")).hasSize(1);
        assertThat(response.get("serviceTypes")).isSameAs(response.get("categories"));
        assertThat(response.get("content")).isSameAs(response.get("categories"));
    }

    @Test
    void serviceListTranslatesServiceAndCategoryDynamicText() {
        AdminCategoryEntity category = category(10L, "Cleaning");
        AdminServiceEntity cleaning = serviceEntity(20L, category, "Deep Cleaning", "Full home cleaning", "HOME_CLEANING");
        when(serviceRepository.searchActiveCatalogForPartner(eq(10L), eq("deep"), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(cleaning)));
        when(serviceDetailRepository.findByService_IdAndType(20L, DetailType.INCLUDED))
                .thenReturn(List.of(detail(101L, cleaning, DetailType.INCLUDED, "Floor cleaning")));
        when(serviceDetailRepository.findByService_IdAndType(20L, DetailType.EXCLUDED)).thenReturn(List.of());
        when(serviceDetailRepository.findByService_IdAndType(20L, DetailType.FAQ)).thenReturn(List.of());
        when(serviceDetailRepository.findByService_IdAndType(20L, DetailType.INSTRUCTION))
                .thenReturn(List.of(detail(102L, cleaning, DetailType.INSTRUCTION, "Keep water available")));
        when(serviceDetailRepository.findByService_IdAndType(20L, DetailType.REQUIREMENT))
                .thenReturn(List.of(detail(103L, cleaning, DetailType.REQUIREMENT, "Access to kitchen")));
        translated("category", 10L, "name", "Cleaning", "Safai");
        translated("service", 20L, "name", "Deep Cleaning", "Gehri Safai");
        translated("service", 20L, "description", "Full home cleaning", "Pure ghar ki safai");
        translated("service", 20L, "service_type", "HOME_CLEANING", "Ghar Safai");
        translated("service_detail", 101L, "included", "Floor cleaning", "Floor safai");
        translated("service_detail", 102L, "instruction", "Keep water available", "Pani rakhein");
        translated("service_detail", 103L, "requirement", "Access to kitchen", "Kitchen access");

        var page = service.services(7L, 10L, 0, 20, " deep ");

        assertThat(page.getItems()).hasSize(1);
        var item = page.getItems().get(0);
        assertThat(item.getName()).isEqualTo("Gehri Safai");
        assertThat(item.getServiceName()).isEqualTo("Gehri Safai");
        assertThat(item.getCategoryName()).isEqualTo("Safai");
        assertThat(item.getDescription()).isEqualTo("Pure ghar ki safai");
        assertThat(item.getServiceType()).isEqualTo("Ghar Safai");
        assertThat(item.getIncluded()).containsExactly("Floor safai");
        assertThat(item.getInstructions()).containsExactly("Pani rakhein");
        assertThat(item.getRequirements()).containsExactly("Kitchen access");
    }

    @Test
    void serviceDetailUsesExistingTranslationFallbackForEnglishLocale() {
        AdminCategoryEntity category = category(10L, "Cleaning");
        AdminServiceEntity cleaning = serviceEntity(20L, category, "Deep Cleaning", "Full home cleaning", "HOME_CLEANING");
        when(serviceRepository.findByIdAndDeletedFalse(20L)).thenReturn(Optional.of(cleaning));
        when(serviceDetailRepository.findByService_IdAndType(20L, DetailType.INCLUDED)).thenReturn(List.of());
        when(serviceDetailRepository.findByService_IdAndType(20L, DetailType.EXCLUDED)).thenReturn(List.of());
        when(serviceDetailRepository.findByService_IdAndType(20L, DetailType.FAQ)).thenReturn(List.of());
        when(serviceDetailRepository.findByService_IdAndType(20L, DetailType.INSTRUCTION)).thenReturn(List.of());
        when(serviceDetailRepository.findByService_IdAndType(20L, DetailType.REQUIREMENT)).thenReturn(List.of());
        when(translationService.translateForPartner(any(), any(), any(), any(), any(), any()))
                .thenAnswer(invocation -> invocation.getArgument(5));

        var response = service.service(7L, 20L);

        assertThat(response.getName()).isEqualTo("Deep Cleaning");
        assertThat(response.getDescription()).isEqualTo("Full home cleaning");
        assertThat(response.getCategoryName()).isEqualTo("Cleaning");
        assertThat(response.getServiceType()).isEqualTo("HOME_CLEANING");
    }

    private void translated(String entityType, Object entityId, String fieldName, String englishText, String translatedText) {
        when(translationService.translateForPartner(7L, null, entityType, entityId, fieldName, englishText))
                .thenReturn(translatedText);
    }

    private AdminCategoryEntity category(Long id, String name) {
        AdminCategoryEntity category = new AdminCategoryEntity();
        ReflectionTestUtils.setField(category, "id", id);
        category.setName(name);
        category.setActive(true);
        category.setDeleted(false);
        category.setIconUrl("/uploads/category.png");
        return category;
    }

    private AdminServiceEntity serviceEntity(Long id,
                                             AdminCategoryEntity category,
                                             String name,
                                             String description,
                                             String serviceType) {
        AdminServiceEntity service = new AdminServiceEntity();
        ReflectionTestUtils.setField(service, "id", id);
        service.setName(name);
        service.setCategory(category);
        service.setDescription(description);
        service.setServiceType(serviceType);
        service.setBasePrice(BigDecimal.valueOf(499));
        service.setDiscountedPrice(BigDecimal.valueOf(399));
        service.setDurationMinutes(60);
        service.setAvgRating(4.5);
        service.setTotalReviews(12L);
        service.setActive(true);
        service.setDeleted(false);
        service.setImageUrl("/uploads/service.png");
        return service;
    }

    private ServiceDetailEntity detail(Long id, AdminServiceEntity service, DetailType type, String itemName) {
        ServiceDetailEntity detail = new ServiceDetailEntity();
        ReflectionTestUtils.setField(detail, "id", id);
        detail.setService(service);
        detail.setType(type);
        detail.setItemName(itemName);
        return detail;
    }
}

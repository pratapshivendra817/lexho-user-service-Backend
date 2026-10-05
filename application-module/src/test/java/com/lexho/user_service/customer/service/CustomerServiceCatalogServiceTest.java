package com.lexho.user_service.customer.service;

import com.lexho.user_service.addon.dto.CustomerAddonResponse;
import com.lexho.user_service.addon.service.AddonManagementService;
import com.lexho.user_service.admin.entity.AdminServiceEntity;
import com.lexho.user_service.admin.entity.DetailType;
import com.lexho.user_service.admin.entity.ServiceDetailEntity;
import com.lexho.user_service.admin.repository.AdminServiceEntityRepository;
import com.lexho.user_service.admin.repository.ServiceDetailRepository;
import com.lexho.user_service.customer.dto.CustomerServiceCatalogPageResponse;
import com.lexho.user_service.customer.dto.CustomerServiceCatalogResponse;
import com.lexho.user_service.exception.ResourceNotFoundException;
import com.lexho.user_service.util.MediaUrlResolver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CustomerServiceCatalogServiceTest {

    @Mock AdminServiceEntityRepository adminServiceEntityRepository;
    @Mock ServiceDetailRepository serviceDetailRepository;
    @Mock AddonManagementService addonManagementService;
    @Mock MediaUrlResolver mediaUrlResolver;

    private CustomerServiceCatalogService service;

    @BeforeEach
    void setUp() {
        service = new CustomerServiceCatalogService(
                adminServiceEntityRepository,
                serviceDetailRepository,
                addonManagementService,
                mediaUrlResolver);
    }

    @Test
    void listServicesNormalizesPagingSearchAndMapsCatalogDetails() {
        AdminServiceEntity entity = serviceEntity(9L, "Window Cleaning", "images/service.png");
        CustomerAddonResponse addon = new CustomerAddonResponse();
        addon.setAddonId(1L);
        addon.setAddonName("Deep Clean");

        when(adminServiceEntityRepository.searchActiveCatalogForCustomer(eq("clean"), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(entity)));
        when(addonManagementService.availableAddonsForService(entity)).thenReturn(List.of(addon));
        when(serviceDetailRepository.findByService_IdAndType(9L, DetailType.INCLUDED))
                .thenReturn(List.of(detail("Bucket included")));
        when(serviceDetailRepository.findByService_IdAndType(9L, DetailType.EXCLUDED))
                .thenReturn(List.of(detail("Balcony excluded")));
        when(serviceDetailRepository.findByService_IdAndType(9L, DetailType.FAQ))
                .thenReturn(List.of(detail("How long does it take?")));
        when(serviceDetailRepository.findByService_IdAndType(9L, DetailType.INSTRUCTION))
                .thenReturn(List.of(detail("Keep area accessible")));
        when(mediaUrlResolver.toPublicUrl("images/service.png")).thenReturn("https://api.lexho.test/images/service.png");

        CustomerServiceCatalogPageResponse response = service.listServices(-1, 500, "  clean ");

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(adminServiceEntityRepository).searchActiveCatalogForCustomer(eq("clean"), pageableCaptor.capture());
        assertThat(pageableCaptor.getValue().getPageNumber()).isZero();
        assertThat(pageableCaptor.getValue().getPageSize()).isEqualTo(100);

        assertThat(response.getTotal()).isEqualTo(1);
        assertThat(response.getPage()).isZero();
        assertThat(response.getSize()).isEqualTo(100);
        assertThat(response.getItems()).hasSize(1);
        assertThat(response.getItems().get(0).getPrice()).isEqualTo(new BigDecimal("249.00"));
        assertThat(response.getItems().get(0).getImageUrl()).isEqualTo("https://api.lexho.test/images/service.png");
        assertThat(response.getItems().get(0).getAddons()).hasSize(1);
        assertThat(response.getItems().get(0).getIncluded()).containsExactly("Bucket included");
        assertThat(response.getItems().get(0).getExcluded()).containsExactly("Balcony excluded");
        assertThat(response.getItems().get(0).getFaqs()).containsExactly("How long does it take?");
        assertThat(response.getItems().get(0).getInstructions()).containsExactly("Keep area accessible");
    }

    @Test
    void getServiceByIdRejectsMissingOrInactiveServicesAndNormalizesImages() {
        when(adminServiceEntityRepository.findByIdAndDeletedFalse(10L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getServiceById(10L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Service detail not found");

        AdminServiceEntity inactive = serviceEntity(11L, "Cook", null);
        inactive.setActive(false);
        when(adminServiceEntityRepository.findByIdAndDeletedFalse(11L)).thenReturn(Optional.of(inactive));
        assertThatThrownBy(() -> service.getServiceById(11L)).isInstanceOf(ResourceNotFoundException.class);

        AdminServiceEntity active = serviceEntity(12L, "Cook", "http://cdn.lexho.com/cook.png");
        when(adminServiceEntityRepository.findByIdAndDeletedFalse(12L)).thenReturn(Optional.of(active));
        when(addonManagementService.availableAddonsForService(active)).thenReturn(List.of());
        when(serviceDetailRepository.findByService_IdAndType(eq(12L), any())).thenReturn(List.of());
        when(mediaUrlResolver.toPublicUrl("http://cdn.lexho.com/cook.png")).thenReturn("http://cdn.lexho.com/cook.png");

        CustomerServiceCatalogResponse response = service.getServiceById(12L);

        assertThat(response.getImageUrl()).isEqualTo("http://cdn.lexho.com/cook.png");
        assertThat(response.getServiceName()).isEqualTo("Cook");
        assertThat(response.getPrice()).isEqualTo(new BigDecimal("249.00"));
    }

    @Test
    void dtoClassesExposeAllFields() {
        CustomerServiceCatalogResponse item = new CustomerServiceCatalogResponse();
        item.setId(1L);
        item.setServiceId(2L);
        item.setName("Name");
        item.setServiceName("Service Name");
        item.setDescription("Description");
        item.setImageUrl("/path.png");
        item.setPrice(new BigDecimal("199.00"));
        item.setServiceType("CLEANING");
        item.setAddons(List.of());
        item.setIncluded(List.of("Included"));
        item.setExcluded(List.of("Excluded"));
        item.setFaqs(List.of("Faq"));
        item.setInstructions(List.of("Instruction"));

        CustomerServiceCatalogPageResponse page = new CustomerServiceCatalogPageResponse();
        page.setItems(List.of(item));
        page.setTotal(1);
        page.setPage(2);
        page.setSize(20);

        assertThat(item.getId()).isEqualTo(1L);
        assertThat(item.getServiceId()).isEqualTo(2L);
        assertThat(item.getName()).isEqualTo("Name");
        assertThat(item.getServiceName()).isEqualTo("Service Name");
        assertThat(item.getDescription()).isEqualTo("Description");
        assertThat(item.getImageUrl()).isEqualTo("/path.png");
        assertThat(item.getPrice()).isEqualTo(new BigDecimal("199.00"));
        assertThat(item.getServiceType()).isEqualTo("CLEANING");
        assertThat(item.getIncluded()).containsExactly("Included");
        assertThat(item.getExcluded()).containsExactly("Excluded");
        assertThat(item.getFaqs()).containsExactly("Faq");
        assertThat(item.getInstructions()).containsExactly("Instruction");
        assertThat(page.getItems()).containsExactly(item);
        assertThat(page.getTotal()).isEqualTo(1);
        assertThat(page.getPage()).isEqualTo(2);
        assertThat(page.getSize()).isEqualTo(20);
    }

    private static AdminServiceEntity serviceEntity(Long id, String name, String imageUrl) {
        AdminServiceEntity entity = new AdminServiceEntity();
        entity.setName(name);
        entity.setDescription("Description");
        entity.setBasePrice(new BigDecimal("299.00"));
        entity.setDiscountedPrice(new BigDecimal("249.00"));
        entity.setServiceType("HOME_SERVICE");
        entity.setImageUrl(imageUrl);
        entity.setActive(true);
        org.springframework.test.util.ReflectionTestUtils.setField(entity, "id", id);
        return entity;
    }

    private static ServiceDetailEntity detail(String itemName) {
        ServiceDetailEntity detail = new ServiceDetailEntity();
        detail.setItemName(itemName);
        return detail;
    }
}

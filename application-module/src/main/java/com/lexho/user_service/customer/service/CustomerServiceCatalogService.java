package com.lexho.user_service.customer.service;

import com.lexho.user_service.admin.entity.AdminServiceEntity;
import com.lexho.user_service.admin.entity.DetailType;
import com.lexho.user_service.admin.repository.AdminServiceEntityRepository;
import com.lexho.user_service.admin.repository.ServiceDetailRepository;
import com.lexho.user_service.addon.service.AddonManagementService;
import com.lexho.user_service.customer.dto.CustomerServiceCatalogPageResponse;
import com.lexho.user_service.customer.dto.CustomerServiceCatalogResponse;
import com.lexho.user_service.exception.ResourceNotFoundException;
import com.lexho.user_service.util.MediaUrlResolver;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CustomerServiceCatalogService {

    private final AdminServiceEntityRepository adminServiceEntityRepository;
    private final ServiceDetailRepository serviceDetailRepository;
    private final AddonManagementService addonManagementService;
    private final MediaUrlResolver mediaUrlResolver;

    public CustomerServiceCatalogPageResponse listServices(int page, int size, String search) {
        int safePage = Math.max(0, page);
        int safeSize = Math.min(Math.max(1, size), 100);
        String normalizedSearch = search == null ? null : search.trim();
        Page<AdminServiceEntity> result = adminServiceEntityRepository.searchActiveCatalogForCustomer(
                normalizedSearch, PageRequest.of(safePage, safeSize)
        );
        return new CustomerServiceCatalogPageResponse(
                result.getContent()
                .stream()
                .map(this::map)
                .toList(),
                result.getTotalElements(),
                safePage,
                safeSize
        );
    }

    public CustomerServiceCatalogResponse getServiceById(Long id) {
        AdminServiceEntity service = adminServiceEntityRepository.findByIdAndDeletedFalse(id)
                .filter(AdminServiceEntity::isActive)
                .orElseThrow(() -> new ResourceNotFoundException("Service detail not found"));
        return map(service);
    }

    private CustomerServiceCatalogResponse map(AdminServiceEntity service) {
        CustomerServiceCatalogResponse response = new CustomerServiceCatalogResponse();
        response.setId(service.getId());
        response.setServiceId(service.getId());
        response.setName(service.getName());
        response.setServiceName(service.getName());
        response.setDescription(service.getDescription());
        String imageUrl = mediaUrlResolver.toPublicUrl(service.getImageUrl());
        response.setImageUrl(imageUrl == null ? "" : imageUrl);
        response.setPrice(effectivePrice(service));
        response.setServiceType(service.getServiceType());
        response.setAddons(addonManagementService.availableAddonsForService(service));
        response.setIncluded(serviceDetailRepository.findByService_IdAndType(service.getId(), DetailType.INCLUDED)
                .stream()
                .map(detail -> detail.getItemName())
                .toList());
        response.setExcluded(serviceDetailRepository.findByService_IdAndType(service.getId(), DetailType.EXCLUDED)
                .stream()
                .map(detail -> detail.getItemName())
                .toList());
        response.setFaqs(serviceDetailRepository.findByService_IdAndType(service.getId(), DetailType.FAQ)
                .stream()
                .map(detail -> detail.getItemName())
                .toList());
        response.setInstructions(serviceDetailRepository.findByService_IdAndType(service.getId(), DetailType.INSTRUCTION)
                .stream()
                .map(detail -> detail.getItemName())
                .toList());
        return response;
    }

    private java.math.BigDecimal effectivePrice(AdminServiceEntity service) {
        if (service.getDiscountedPrice() != null && service.getDiscountedPrice().signum() > 0) {
            return service.getDiscountedPrice();
        }
        return service.getBasePrice();
    }

}

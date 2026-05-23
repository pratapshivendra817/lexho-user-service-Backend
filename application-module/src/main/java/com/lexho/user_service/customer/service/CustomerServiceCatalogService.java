package com.lexho.user_service.customer.service;

import com.lexho.user_service.admin.entity.AdminServiceEntity;
import com.lexho.user_service.admin.entity.DetailType;
import com.lexho.user_service.admin.repository.AdminServiceEntityRepository;
import com.lexho.user_service.admin.repository.ServiceDetailRepository;
import com.lexho.user_service.customer.dto.CustomerServiceCatalogPageResponse;
import com.lexho.user_service.customer.dto.CustomerServiceCatalogResponse;
import com.lexho.user_service.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CustomerServiceCatalogService {

    private final AdminServiceEntityRepository adminServiceEntityRepository;
    private final ServiceDetailRepository serviceDetailRepository;

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
        response.setName(service.getName());
        response.setDescription(service.getDescription());
        response.setImageUrl(toAbsoluteImageUrl(service.getImageUrl()));
        response.setPrice(service.getBasePrice());
        response.setServiceType(service.getServiceType());
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

    private String toAbsoluteImageUrl(String raw) {
        if (raw == null || raw.isBlank()) {
            return "https://cdn.lexho.com/defaults/service.png";
        }
        if (raw.startsWith("http://") || raw.startsWith("https://")) {
            return raw.replace("http://", "https://");
        }
        String normalized = raw.startsWith("/") ? raw.substring(1) : raw;
        return "https://cdn.lexho.com/" + normalized;
    }
}

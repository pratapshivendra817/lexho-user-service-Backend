package com.lexho.user_service.lexhopartner.service;

import com.lexho.user_service.admin.entity.AdminCategoryEntity;
import com.lexho.user_service.admin.entity.AdminServiceEntity;
import com.lexho.user_service.admin.entity.DetailType;
import com.lexho.user_service.admin.entity.ServiceDetailEntity;
import com.lexho.user_service.admin.repository.AdminCategoryRepository;
import com.lexho.user_service.admin.repository.AdminServiceEntityRepository;
import com.lexho.user_service.admin.repository.ServiceDetailRepository;
import com.lexho.user_service.exception.ResourceNotFoundException;
import com.lexho.user_service.lexhopartner.dto.PartnerCatalogCategoryResponse;
import com.lexho.user_service.lexhopartner.dto.PartnerCatalogServicePageResponse;
import com.lexho.user_service.lexhopartner.dto.PartnerCatalogServiceResponse;
import com.lexho.user_service.localization.dynamic.DynamicContentTranslationService;
import com.lexho.user_service.util.MediaUrlResolver;
import com.lexho.user_service.util.StringSanitizers;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class PartnerCatalogService {
    private static final String SOURCE_CATEGORY = "category";
    private static final String SOURCE_SERVICE = "service";

    private final AdminCategoryRepository categoryRepository;
    private final AdminServiceEntityRepository serviceRepository;
    private final ServiceDetailRepository serviceDetailRepository;
    private final MediaUrlResolver mediaUrlResolver;
    private final DynamicContentTranslationService dynamicTranslationService;

    public PartnerCatalogService(AdminCategoryRepository categoryRepository,
                                 AdminServiceEntityRepository serviceRepository,
                                 ServiceDetailRepository serviceDetailRepository,
                                 MediaUrlResolver mediaUrlResolver,
                                 DynamicContentTranslationService dynamicTranslationService) {
        this.categoryRepository = categoryRepository;
        this.serviceRepository = serviceRepository;
        this.serviceDetailRepository = serviceDetailRepository;
        this.mediaUrlResolver = mediaUrlResolver;
        this.dynamicTranslationService = dynamicTranslationService;
    }

    @Transactional
    public List<PartnerCatalogCategoryResponse> categories(Long partnerId, int limit) {
        return categoryResponses(partnerId, limit);
    }

    private List<PartnerCatalogCategoryResponse> categoryResponses(Long partnerId, int limit) {
        int safeLimit = Math.min(Math.max(limit, 1), 100);
        return categoryRepository.findActiveForCustomer(PageRequest.of(0, safeLimit))
                .stream()
                .map(category -> mapCategory(partnerId, category))
                .toList();
    }

    @Transactional
    public Map<String, List<PartnerCatalogCategoryResponse>> categoryDropdown(Long partnerId, int limit) {
        List<PartnerCatalogCategoryResponse> categories = categoryResponses(partnerId, limit);
        Map<String, List<PartnerCatalogCategoryResponse>> response = new LinkedHashMap<>();
        response.put("categories", categories);
        response.put("serviceTypes", categories);
        response.put("services", categories);
        response.put("results", categories);
        response.put("rows", categories);
        response.put("records", categories);
        response.put("items", categories);
        response.put("content", categories);
        return response;
    }

    @Transactional
    public PartnerCatalogServicePageResponse services(Long partnerId, Long categoryId, int page, int size, String search) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), 100);
        Page<AdminServiceEntity> result = serviceRepository.searchActiveCatalogForPartner(
                categoryId,
                normalize(search),
                PageRequest.of(safePage, safeSize)
        );
        return new PartnerCatalogServicePageResponse(
                result.getContent().stream().map(service -> mapService(partnerId, service)).toList(),
                result.getTotalElements(),
                safePage,
                safeSize
        );
    }

    @Transactional
    public PartnerCatalogServiceResponse service(Long partnerId, Long serviceId) {
        AdminServiceEntity service = serviceRepository.findByIdAndDeletedFalse(serviceId)
                .filter(AdminServiceEntity::isActive)
                .orElseThrow(() -> new ResourceNotFoundException(PartnerServiceText.MSG_SERVICE_NOT_FOUND));
        return mapService(partnerId, service);
    }

    private PartnerCatalogCategoryResponse mapCategory(Long partnerId, AdminCategoryEntity category) {
        PartnerCatalogCategoryResponse response = new PartnerCatalogCategoryResponse();
        response.setId(category.getId());
        response.setCategoryId(category.getId());
        String code = category.getId() == null ? null : String.valueOf(category.getId());
        response.setCode(code);
        response.setCategoryCode(code);
        response.setServiceCode(code);
        String translatedName = translate(partnerId, SOURCE_CATEGORY, category.getId(), "name", category.getName());
        response.setName(translatedName);
        response.setCategoryName(translatedName);
        response.setDisplayName(translatedName);
        response.setServiceName(translatedName);
        response.setDescription(translate(partnerId, SOURCE_CATEGORY, category.getId(), "description", category.getDescription()));
        String imageUrl = mediaUrlResolver.toPublicUrl(category.getIconUrl());
        response.setIconUrl(imageUrl);
        response.setImageUrl(imageUrl);
        response.setEnabled(category.isActive());
        response.setActive(category.isActive());
        response.setIsActive(category.isActive());
        response.setStatus(category.isActive() ? PartnerServiceText.STATUS_ACTIVE : PartnerServiceText.STATUS_INACTIVE);
        response.setDisplayOrder(category.getDisplayOrder());
        return response;
    }

    private PartnerCatalogServiceResponse mapService(Long partnerId, AdminServiceEntity service) {
        PartnerCatalogServiceResponse response = new PartnerCatalogServiceResponse();
        response.setId(service.getId());
        response.setServiceId(service.getId());
        String translatedName = translate(partnerId, SOURCE_SERVICE, service.getId(), "name", service.getName());
        response.setName(translatedName);
        response.setServiceName(translatedName);
        response.setCategoryId(service.getCategory() == null ? null : service.getCategory().getId());
        response.setCategoryName(translatedCategoryName(partnerId, service));
        response.setDescription(translate(partnerId, SOURCE_SERVICE, service.getId(), "description", service.getDescription()));
        response.setImageUrl(mediaUrlResolver.toPublicUrl(service.getImageUrl()));
        response.setPrice(effectivePrice(service));
        response.setDiscountedPrice(service.getDiscountedPrice());
        response.setServiceType(translate(partnerId, SOURCE_SERVICE, service.getId(), "service_type", service.getServiceType()));
        response.setDurationMinutes(service.getDurationMinutes());
        response.setRating(service.getAvgRating() == null ? 0.0 : service.getAvgRating());
        response.setTotalReviews(service.getTotalReviews() == null ? 0L : service.getTotalReviews());
        response.setIncluded(details(partnerId, service, DetailType.INCLUDED));
        response.setExcluded(details(partnerId, service, DetailType.EXCLUDED));
        response.setFaqs(details(partnerId, service, DetailType.FAQ));
        response.setInstructions(details(partnerId, service, DetailType.INSTRUCTION));
        response.setRequirements(details(partnerId, service, DetailType.REQUIREMENT));
        return response;
    }

    private String translatedCategoryName(Long partnerId, AdminServiceEntity service) {
        if (service.getCategory() != null) {
            return translate(partnerId, SOURCE_CATEGORY, service.getCategory().getId(), "name", service.getCategory().getName());
        }
        return translate(partnerId, SOURCE_CATEGORY, "service-" + service.getId(), "name", service.getCategoryName());
    }

    private List<String> details(Long partnerId, AdminServiceEntity service, DetailType type) {
        return serviceDetailRepository.findByService_IdAndType(service.getId(), type)
                .stream()
                .map(detail -> translate(partnerId, "service_detail", detail.getId(), fieldName(type), detail.getItemName()))
                .toList();
    }

    private String fieldName(DetailType type) {
        return switch (type) {
            case INCLUDED -> "included";
            case EXCLUDED -> "excluded";
            case FAQ -> "faq";
            case INSTRUCTION -> "instruction";
            case REQUIREMENT -> "requirement";
        };
    }

    private BigDecimal effectivePrice(AdminServiceEntity service) {
        if (service.getDiscountedPrice() != null && service.getDiscountedPrice().signum() > 0) {
            return service.getDiscountedPrice();
        }
        return service.getBasePrice();
    }

    private String translate(Long partnerId, String entityType, Object entityId, String fieldName, String englishText) {
        if (englishText == null || englishText.isBlank()) {
            return englishText;
        }
        return dynamicTranslationService.translateForPartner(partnerId, null, entityType, entityId, fieldName, englishText);
    }

    private String normalize(String value) {
        return StringSanitizers.trimToNull(value);
    }
}

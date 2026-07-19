package com.lexho.user_service.admin.service;

import com.lexho.user_service.admin.dto.*;
import com.lexho.user_service.admin.entity.AdminCategoryEntity;
import com.lexho.user_service.admin.entity.AdminServiceEntity;
import com.lexho.user_service.admin.entity.ServiceDetailEntity;
import com.lexho.user_service.admin.repository.AdminCategoryRepository;
import com.lexho.user_service.admin.repository.AdminServiceEntityRepository;
import com.lexho.user_service.admin.repository.ServiceDetailRepository;
import com.lexho.user_service.banner.entity.BannerPromotion;
import com.lexho.user_service.banner.repository.BannerPromotionRepository;
import com.lexho.user_service.exception.BadRequestException;
import com.lexho.user_service.exception.ResourceNotFoundException;
import com.lexho.user_service.security.SecurityUtils;
import com.lexho.user_service.util.MediaUrlResolver;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

@Service
public class AdminCatalogService {

    private final AdminCategoryRepository adminCategoryRepository;
    private final AdminServiceEntityRepository adminServiceEntityRepository;
    private final ServiceDetailRepository serviceDetailRepository;
    private final BannerPromotionRepository bannerPromotionRepository;
    private final AdminAuditService adminAuditService;
    private final SecurityUtils securityUtils;
    private final MediaUrlResolver mediaUrlResolver;

    public AdminCatalogService(AdminCategoryRepository adminCategoryRepository,
                               AdminServiceEntityRepository adminServiceEntityRepository,
                               ServiceDetailRepository serviceDetailRepository,
                               BannerPromotionRepository bannerPromotionRepository,
                               AdminAuditService adminAuditService,
                               SecurityUtils securityUtils,
                               MediaUrlResolver mediaUrlResolver) {
        this.adminCategoryRepository = adminCategoryRepository;
        this.adminServiceEntityRepository = adminServiceEntityRepository;
        this.serviceDetailRepository = serviceDetailRepository;
        this.bannerPromotionRepository = bannerPromotionRepository;
        this.adminAuditService = adminAuditService;
        this.securityUtils = securityUtils;
        this.mediaUrlResolver = mediaUrlResolver;
    }

    @Transactional
    public DataResponse<AdminCategoryResponse> createCategory(AdminCategoryUpsertRequest request) {
        String normalizedName = normalizeRequired(request.getName(), "Category name is required");
        AdminCategoryEntity category = adminCategoryRepository.findByNameIgnoreCase(normalizedName)
                .orElseGet(AdminCategoryEntity::new);

        if (category.getId() != null && !category.isDeleted()) {
            throw new BadRequestException("Category with this name already exists");
        }

        category.setName(normalizedName);
        category.setIconUrl(mediaUrlResolver.toStorablePath(normalize(request.getIconUrl())));
        category.setActive(request.getActive() == null || request.getActive());
        category.setDisplayOrder(request.getDisplayOrder());
        category.setDeleted(false);

        AdminCategoryEntity saved = adminCategoryRepository.save(category);
        audit("CATEGORY_CREATE", "admin_categories", saved.getId(), "name=" + saved.getName());
        return new DataResponse<>(mapCategory(saved));
    }

    @Transactional
    public DataResponse<AdminCategoryResponse> updateCategory(Long id, AdminCategoryUpsertRequest request) {
        AdminCategoryEntity category = getCategory(id);
        String normalizedName = normalizeRequired(request.getName(), "Category name is required");
        if (adminCategoryRepository.existsByNameIgnoreCaseAndDeletedFalseAndIdNot(normalizedName, id)) {
            throw new BadRequestException("Category with this name already exists");
        }

        category.setName(normalizedName);
        category.setIconUrl(mediaUrlResolver.toStorablePath(normalize(request.getIconUrl())));
        category.setDisplayOrder(request.getDisplayOrder());
        if (request.getActive() != null) {
            category.setActive(request.getActive());
        }
        AdminCategoryEntity saved = adminCategoryRepository.save(category);
        audit("CATEGORY_UPDATE", "admin_categories", saved.getId(), "name=" + saved.getName());
        return new DataResponse<>(mapCategory(saved));
    }

    @Transactional
    public AdminMessageResponse deleteCategory(Long id) {
        AdminCategoryEntity category = getCategory(id);
        category.setDeleted(true);
        category.setActive(false);
        adminCategoryRepository.save(category);

        List<AdminServiceEntity> services = adminServiceEntityRepository.findByCategory_IdAndDeletedFalse(id);
        for (AdminServiceEntity service : services) {
            service.setDeleted(true);
            service.setActive(false);
        }
        adminServiceEntityRepository.saveAll(services);

        audit("CATEGORY_DELETE", "admin_categories", id, "softDelete=true");
        return new AdminMessageResponse("Category deleted successfully");
    }

    @Transactional
    public DataResponse<AdminCategoryResponse> updateCategoryStatus(Long id, boolean active) {
        AdminCategoryEntity category = getCategory(id);
        category.setActive(active);
        AdminCategoryEntity saved = adminCategoryRepository.save(category);
        audit("CATEGORY_STATUS_UPDATE", "admin_categories", id, "active=" + active);
        return new DataResponse<>(mapCategory(saved));
    }

    public PagedDataResponse<AdminCategoryResponse> listCategories(int page, int limit, String search) {
        Pageable pageable = page(page, limit, Sort.by(Sort.Direction.ASC, "displayOrder").and(Sort.by(Sort.Direction.ASC, "name")));
        Page<AdminCategoryEntity> result = adminCategoryRepository.search(normalize(search), pageable);
        return new PagedDataResponse<>(
                result.getContent().stream().map(this::mapCategory).collect(Collectors.toList()),
                result.getTotalElements()
        );
    }

    @Transactional
    public DataResponse<AdminServiceResponse> createService(AdminServiceUpsertRequest request) {
        AdminCategoryEntity category = resolveCategory(request.getCategoryId());
        AdminServiceEntity service = new AdminServiceEntity();
        applyServiceFields(service, request, category);
        service.setDeleted(false);
        AdminServiceEntity saved = adminServiceEntityRepository.save(service);
        audit("SERVICE_CREATE", "services", saved.getId(), "name=" + saved.getName());
        return new DataResponse<>(mapService(saved));
    }

    @Transactional
    public DataResponse<AdminServiceResponse> updateService(Long id, AdminServiceUpsertRequest request) {
        AdminServiceEntity service = getService(id);
        AdminCategoryEntity category = resolveCategoryForUpdate(service, request.getCategoryId());
        applyServiceFields(service, request, category);
        AdminServiceEntity saved = adminServiceEntityRepository.save(service);
        audit("SERVICE_UPDATE", "services", saved.getId(), "name=" + saved.getName());
        return new DataResponse<>(mapService(saved));
    }

    public DataResponse<AdminServiceResponse> getServiceById(Long id) {
        return new DataResponse<>(mapService(getService(id)));
    }

    @Transactional
    public AdminServiceDetailBulkCreateResponse addServiceDetails(Long serviceId, AdminServiceDetailBulkRequest request) {
        AdminServiceEntity service = getService(serviceId);
        List<ServiceDetailEntity> entities = request.getItems().stream()
                .map(this::normalizeDetailItem)
                .distinct()
                .map(item -> {
                    ServiceDetailEntity detail = new ServiceDetailEntity();
                    detail.setService(service);
                    detail.setType(request.getType());
                    detail.setItemName(item);
                    return detail;
                })
                .toList();

        if (entities.isEmpty()) {
            throw new BadRequestException("At least one non-empty item is required");
        }

        List<ServiceDetailEntity> saved = serviceDetailRepository.saveAll(entities);
        audit("SERVICE_DETAIL_CREATE_BULK", "service_details", serviceId, "count=" + saved.size());
        return new AdminServiceDetailBulkCreateResponse(
                "Details added successfully",
                saved.stream().map(this::mapDetail).toList()
        );
    }

    @Transactional
    public DataResponse<AdminServiceDetailResponse> updateServiceDetail(Long serviceId,
                                                                        Long detailId,
                                                                        AdminServiceDetailUpdateRequest request) {
        getService(serviceId);
        ServiceDetailEntity detail = serviceDetailRepository.findByIdAndService_Id(detailId, serviceId)
                .orElseThrow(() -> new ResourceNotFoundException("Service detail not found"));
        detail.setType(request.getType());
        detail.setItemName(normalizeDetailItem(request.getItemName()));
        ServiceDetailEntity saved = serviceDetailRepository.save(detail);
        audit("SERVICE_DETAIL_UPDATE", "service_details", detailId, "serviceId=" + serviceId);
        return new DataResponse<>(mapDetail(saved));
    }

    @Transactional
    public AdminMessageResponse deleteServiceDetail(Long serviceId, Long detailId) {
        getService(serviceId);
        ServiceDetailEntity detail = serviceDetailRepository.findByIdAndService_Id(detailId, serviceId)
                .orElseThrow(() -> new ResourceNotFoundException("Service detail not found"));
        serviceDetailRepository.delete(detail);
        audit("SERVICE_DETAIL_DELETE", "service_details", detailId, "serviceId=" + serviceId);
        return new AdminMessageResponse("Service detail deleted successfully");
    }

    public PagedDataResponse<AdminServiceResponse> listServices(int page,
                                                                int limit,
                                                                String search,
                                                                String category,
                                                                String status,
                                                                String sort) {
        Pageable pageable = page(page, limit, serviceSort(sort));
        Boolean active = parseStatus(status);
        Page<AdminServiceEntity> result = adminServiceEntityRepository.searchCatalog(
                normalize(search),
                active,
                normalize(category),
                pageable
        );
        return new PagedDataResponse<>(
                result.getContent().stream().map(this::mapService).collect(Collectors.toList()),
                result.getTotalElements()
        );
    }

    @Transactional
    public DataResponse<AdminServiceResponse> toggleServiceStatus(Long id) {
        AdminServiceEntity service = getService(id);
        service.setActive(!service.isActive());
        AdminServiceEntity saved = adminServiceEntityRepository.save(service);
        audit("SERVICE_TOGGLE_STATUS", "services", id, "active=" + saved.isActive());
        return new DataResponse<>(mapService(saved));
    }

    @Transactional
    public AdminMessageResponse deleteService(Long id) {
        AdminServiceEntity service = getService(id);
        service.setDeleted(true);
        service.setActive(false);
        adminServiceEntityRepository.save(service);
        audit("SERVICE_DELETE", "services", id, "softDelete=true");
        return new AdminMessageResponse("Service deleted successfully");
    }

    public DataResponse<List<AdminServiceSummaryResponse>> serviceSummary() {
        List<AdminServiceSummaryResponse> data = adminServiceEntityRepository.serviceSummaryByCategory()
                .stream()
                .map(row -> {
                    AdminServiceSummaryResponse response = new AdminServiceSummaryResponse();
                    response.setCategory(String.valueOf(row[0]));
                    response.setTotalServices(asLong(row[1]));
                    response.setTotalBookings(asLong(row[2]));
                    return response;
                })
                .collect(Collectors.toList());
        return new DataResponse<>(data);
    }

    public DataResponse<List<AdminServiceResponse>> topServices(int limit) {
        int safeLimit = Math.max(1, Math.min(limit, 50));
        List<AdminServiceResponse> data = adminServiceEntityRepository.findTopBookedServices(PageRequest.of(0, safeLimit))
                .stream()
                .map(this::mapService)
                .collect(Collectors.toList());
        return new DataResponse<>(data);
    }

    public DataResponse<List<AdminServiceRatingAnalyticsResponse>> serviceAnalytics() {
        List<AdminServiceRatingAnalyticsResponse> data = adminServiceEntityRepository.ratingAnalyticsByCategory()
                .stream()
                .map(row -> {
                    AdminServiceRatingAnalyticsResponse response = new AdminServiceRatingAnalyticsResponse();
                    response.setCategory(String.valueOf(row[0]));
                    response.setAvgRating(asDouble(row[1]));
                    response.setMaxRating(asDouble(row[2]));
                    response.setMinRating(asDouble(row[3]));
                    response.setServicesCount(asLong(row[4]));
                    return response;
                })
                .collect(Collectors.toList());
        return new DataResponse<>(data);
    }

    public DataResponse<java.util.Map<String, Object>> mediaDebugSnapshot(int limit) {
        int safeLimit = Math.min(Math.max(limit, 1), 100);
        Pageable pageable = PageRequest.of(0, safeLimit, Sort.by(Sort.Direction.DESC, "createdAt"));

        List<java.util.Map<String, Object>> categories = adminCategoryRepository.findAll(pageable).getContent().stream()
                .map(category -> java.util.Map.<String, Object>of(
                        "id", category.getId(),
                        "name", category.getName() == null ? "" : category.getName(),
                        "iconUrlStored", category.getIconUrl() == null ? "" : category.getIconUrl(),
                        "imageUrlResolved", mediaUrlResolver.toPublicUrl(category.getIconUrl()) == null ? "" : mediaUrlResolver.toPublicUrl(category.getIconUrl()),
                        "active", category.isActive(),
                        "deleted", category.isDeleted(),
                        "createdAt", category.getCreatedAt() == null ? "" : category.getCreatedAt().toString()
                ))
                .collect(Collectors.toList());

        List<java.util.Map<String, Object>> banners = bannerPromotionRepository.findAll(pageable).getContent().stream()
                .map(banner -> java.util.Map.<String, Object>of(
                        "id", banner.getId(),
                        "title", banner.getTitle() == null ? "" : banner.getTitle(),
                        "bannerImageStored", banner.getBannerImageUrl() == null ? "" : banner.getBannerImageUrl(),
                        "imageUrlResolved", mediaUrlResolver.toPublicUrl(banner.getBannerImageUrl()) == null ? "" : mediaUrlResolver.toPublicUrl(banner.getBannerImageUrl()),
                        "status", banner.getStatus() == null ? "" : banner.getStatus().name(),
                        "slotNumber", banner.getSlotNumber() == null ? "" : banner.getSlotNumber(),
                        "deleted", banner.isDeleted(),
                        "createdAt", banner.getCreatedAt() == null ? "" : banner.getCreatedAt().toString()
                ))
                .collect(Collectors.toList());

        return new DataResponse<>(java.util.Map.of(
                "categories", categories,
                "banners", banners
        ));
    }

    public DataResponse<java.util.Map<String, Object>> mediaDebugLatestActive(int limit) {
        int safeLimit = Math.min(Math.max(limit, 1), 100);

        List<java.util.Map<String, Object>> categories = adminCategoryRepository
                .findAllByDeletedFalseOrderByCreatedAtDesc(PageRequest.of(0, safeLimit))
                .stream()
                .map(category -> java.util.Map.<String, Object>of(
                        "id", category.getId(),
                        "name", category.getName() == null ? "" : category.getName(),
                        "iconUrlStored", category.getIconUrl() == null ? "" : category.getIconUrl(),
                        "imageUrlResolved", mediaUrlResolver.toPublicUrl(category.getIconUrl()) == null ? "" : mediaUrlResolver.toPublicUrl(category.getIconUrl()),
                        "active", category.isActive(),
                        "deleted", category.isDeleted(),
                        "createdAt", category.getCreatedAt() == null ? "" : category.getCreatedAt().toString()
                ))
                .collect(Collectors.toList());

        List<java.util.Map<String, Object>> banners = bannerPromotionRepository
                .findByDeletedFalseOrderByCreatedAtDesc(PageRequest.of(0, safeLimit))
                .stream()
                .map(banner -> java.util.Map.<String, Object>of(
                        "id", banner.getId(),
                        "title", banner.getTitle() == null ? "" : banner.getTitle(),
                        "bannerImageStored", banner.getBannerImageUrl() == null ? "" : banner.getBannerImageUrl(),
                        "imageUrlResolved", mediaUrlResolver.toPublicUrl(banner.getBannerImageUrl()) == null ? "" : mediaUrlResolver.toPublicUrl(banner.getBannerImageUrl()),
                        "status", banner.getStatus() == null ? "" : banner.getStatus().name(),
                        "slotNumber", banner.getSlotNumber() == null ? "" : banner.getSlotNumber(),
                        "deleted", banner.isDeleted(),
                        "createdAt", banner.getCreatedAt() == null ? "" : banner.getCreatedAt().toString()
                ))
                .collect(Collectors.toList());

        return new DataResponse<>(java.util.Map.of(
                "categories", categories,
                "banners", banners
        ));
    }

    private void applyServiceFields(AdminServiceEntity service, AdminServiceUpsertRequest request, AdminCategoryEntity category) {
        service.setName(normalizeRequired(request.getName(), "Service name is required"));
        service.setCategory(category);
        service.setDescription(normalize(request.getDescription()));
        service.setServiceType(normalize(request.getServiceType()));
        java.math.BigDecimal effectivePrice = request.getBasePrice() != null ? request.getBasePrice() : request.getPrice();
        if (effectivePrice == null || effectivePrice.signum() <= 0) {
            throw new BadRequestException("Price is required and must be greater than 0");
        }
        service.setBasePrice(effectivePrice);
        service.setDiscountedPrice(effectivePrice);
        service.setDurationMinutes(request.getDurationMinutes() == null ? 60 : request.getDurationMinutes());
        service.setAvgRating(request.getRating() == null ? 0.0 : request.getRating());
        service.setImageUrl(mediaUrlResolver.toStorablePath(normalize(request.getImageUrl())));
        service.setActive(request.getActive() == null || request.getActive());
        if (service.getTotalBookings() == null) {
            service.setTotalBookings(0L);
        }
    }

    private AdminCategoryEntity resolveCategory(Long categoryId) {
        if (categoryId != null) {
            return getCategory(categoryId);
        }
        return adminCategoryRepository.findFirstByNameContainingIgnoreCaseAndDeletedFalse("All Services")
                .or(() -> adminCategoryRepository.findAllByDeletedFalseOrderByCreatedAtDesc(org.springframework.data.domain.PageRequest.of(0, 1))
                        .stream().findFirst())
                .orElseThrow(() -> new BadRequestException("Category is required. No default category found."));
    }

    private AdminCategoryEntity resolveCategoryForUpdate(AdminServiceEntity existing, Long categoryId) {
        if (categoryId != null) {
            return getCategory(categoryId);
        }
        if (existing.getCategory() != null) {
            return existing.getCategory();
        }
        return resolveCategory(null);
    }

    private AdminCategoryEntity getCategory(Long id) {
        return adminCategoryRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found"));
    }

    private AdminServiceEntity getService(Long id) {
        return adminServiceEntityRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new ResourceNotFoundException("Service not found"));
    }

    private AdminCategoryResponse mapCategory(AdminCategoryEntity category) {
        AdminCategoryResponse response = new AdminCategoryResponse();
        response.setId(category.getId());
        response.setName(category.getName());
        String imageUrl = mediaUrlResolver.toPublicUrl(category.getIconUrl());
        response.setIconUrl(imageUrl);
        response.setImageUrl(imageUrl);
        response.setDisplayOrder(category.getDisplayOrder());
        response.setActive(category.isActive());
        response.setCreatedAt(category.getCreatedAt());
        response.setUpdatedAt(category.getUpdatedAt());

        List<AdminServiceEntity> services = adminServiceEntityRepository.findByCategory_IdAndDeletedFalse(category.getId());
        response.setTotalServicesCount(services.size());
        long bookings = services.stream().mapToLong(service -> service.getTotalBookings() == null ? 0L : service.getTotalBookings()).sum();
        response.setTotalBookingsCount(bookings);
        return response;
    }

    private AdminServiceResponse mapService(AdminServiceEntity service) {
        AdminServiceResponse response = new AdminServiceResponse();
        response.setId(service.getId());
        response.setName(service.getName());
        response.setCategoryId(service.getCategory() == null ? null : service.getCategory().getId());
        response.setCategoryName(service.getCategory() == null ? service.getCategoryName() : service.getCategory().getName());
        response.setDescription(service.getDescription());
        response.setServiceType(service.getServiceType());
        response.setBasePrice(service.getBasePrice());
        response.setDurationMinutes(service.getDurationMinutes());
        response.setRating(service.getAvgRating() == null ? 0.0 : service.getAvgRating());
        response.setActive(service.isActive());
        response.setTotalBookings(service.getTotalBookings() == null ? 0L : service.getTotalBookings());
        response.setImageUrl(mediaUrlResolver.toPublicUrl(service.getImageUrl()));
        response.setDetails(serviceDetailRepository.findByService_Id(service.getId()).stream()
                .map(this::mapDetail)
                .toList());
        response.setCreatedAt(service.getCreatedAt());
        response.setUpdatedAt(service.getUpdatedAt());
        return response;
    }

    private AdminServiceDetailResponse mapDetail(ServiceDetailEntity detail) {
        AdminServiceDetailResponse response = new AdminServiceDetailResponse();
        response.setId(detail.getId());
        response.setServiceId(detail.getService() == null ? null : detail.getService().getId());
        response.setType(detail.getType());
        response.setItemName(detail.getItemName());
        return response;
    }

    private String normalizeDetailItem(String item) {
        if (item == null) {
            throw new BadRequestException("item must be between 1 and 255 characters");
        }
        String trimmed = item.trim();
        if (trimmed.isEmpty() || trimmed.length() > 255) {
            throw new BadRequestException("item must be between 1 and 255 characters");
        }
        return trimmed;
    }

    private Pageable page(int page, int limit, Sort sort) {
        int safePage = Math.max(page, 1) - 1;
        int safeLimit = Math.min(Math.max(limit, 1), 100);
        return PageRequest.of(safePage, safeLimit, sort);
    }

    private Sort serviceSort(String sort) {
        String normalized = normalize(sort);
        if (normalized == null || "latest".equalsIgnoreCase(normalized)) {
            return Sort.by(Sort.Direction.DESC, "createdAt");
        }
        if ("topBookings".equalsIgnoreCase(normalized)) {
            return Sort.by(Sort.Direction.DESC, "totalBookings").and(Sort.by(Sort.Direction.DESC, "createdAt"));
        }
        if ("rating".equalsIgnoreCase(normalized)) {
            return Sort.by(Sort.Direction.DESC, "avgRating").and(Sort.by(Sort.Direction.DESC, "createdAt"));
        }
        return Sort.by(Sort.Direction.DESC, "createdAt");
    }

    private Boolean parseStatus(String status) {
        String normalized = normalize(status);
        if (normalized == null) {
            return null;
        }
        if ("active".equalsIgnoreCase(normalized)) {
            return true;
        }
        if ("inactive".equalsIgnoreCase(normalized)) {
            return false;
        }
        throw new BadRequestException("Invalid status filter");
    }

    private String normalize(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String normalizeRequired(String value, String message) {
        String normalized = normalize(value);
        if (normalized == null) {
            throw new BadRequestException(message);
        }
        return normalized;
    }

    private void audit(String action, String entity, Long entityId, String metadata) {
        adminAuditService.log(securityUtils.getCurrentUserId().orElse(0L), action, entity, entityId, metadata);
    }

    private long asLong(Object value) {
        if (value == null) {
            return 0L;
        }
        if (value instanceof Number number) {
            return number.longValue();
        }
        return Long.parseLong(String.valueOf(value));
    }

    private double asDouble(Object value) {
        if (value == null) {
            return 0.0;
        }
        if (value instanceof Number number) {
            return number.doubleValue();
        }
        return Double.parseDouble(String.valueOf(value));
    }
}

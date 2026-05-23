package com.lexho.user_service.admin.service;

import com.lexho.user_service.admin.dto.*;
import com.lexho.user_service.admin.entity.AdminCategoryEntity;
import com.lexho.user_service.admin.entity.AdminServiceEntity;
import com.lexho.user_service.admin.entity.PopularServiceEntity;
import com.lexho.user_service.admin.repository.AdminCategoryRepository;
import com.lexho.user_service.admin.repository.AdminServiceEntityRepository;
import com.lexho.user_service.admin.repository.PopularServiceRepository;
import com.lexho.user_service.admin.repository.PopularServiceView;
import com.lexho.user_service.exception.BadRequestException;
import com.lexho.user_service.exception.ResourceNotFoundException;
import com.lexho.user_service.security.SecurityUtils;
import com.lexho.user_service.util.MediaUrlResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class PopularServiceManagementService {

    private static final Logger log = LoggerFactory.getLogger(PopularServiceManagementService.class);

    private final PopularServiceRepository popularServiceRepository;
    private final AdminServiceEntityRepository adminServiceEntityRepository;
    private final AdminCategoryRepository adminCategoryRepository;
    private final AdminAuditService adminAuditService;
    private final SecurityUtils securityUtils;
    private final MediaUrlResolver mediaUrlResolver;

    public PopularServiceManagementService(PopularServiceRepository popularServiceRepository,
                                           AdminServiceEntityRepository adminServiceEntityRepository,
                                           AdminCategoryRepository adminCategoryRepository,
                                           AdminAuditService adminAuditService,
                                           SecurityUtils securityUtils,
                                           MediaUrlResolver mediaUrlResolver) {
        this.popularServiceRepository = popularServiceRepository;
        this.adminServiceEntityRepository = adminServiceEntityRepository;
        this.adminCategoryRepository = adminCategoryRepository;
        this.adminAuditService = adminAuditService;
        this.securityUtils = securityUtils;
        this.mediaUrlResolver = mediaUrlResolver;
    }

    @Transactional
    public DataResponse<PopularServiceResponse> addPopularService(PopularServiceUpsertRequest request) {
        AdminServiceEntity service = adminServiceEntityRepository.findByIdAndDeletedFalse(request.getServiceId())
                .orElseThrow(() -> new ResourceNotFoundException("Service not found"));
        AdminCategoryEntity category = adminCategoryRepository.findByIdAndDeletedFalse(request.getCategoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Category not found"));

        if (!service.isActive()) {
            throw new BadRequestException("Only active services can be marked as popular");
        }

        if (popularServiceRepository.existsByService_IdAndCategory_IdAndActiveTrue(service.getId(), category.getId())) {
            throw new BadRequestException("Service is already configured as popular for this category");
        }

        if (popularServiceRepository.existsByCategory_IdAndPriorityAndActiveTrue(category.getId(), request.getPriority())) {
            throw new BadRequestException("Priority already exists for this category");
        }

        PopularServiceEntity entity = new PopularServiceEntity();
        entity.setService(service);
        entity.setCategory(category);
        entity.setPriority(request.getPriority());
        entity.setActive(true);

        PopularServiceEntity saved = popularServiceRepository.save(entity);
        audit("POPULAR_SERVICE_CREATE", "popular_services", saved.getId(),
                "serviceId=" + service.getId() + ",categoryId=" + category.getId() + ",priority=" + request.getPriority());
        log.info("Popular service added: popularServiceId={}, serviceId={}, categoryId={}, priority={}",
                saved.getId(), service.getId(), category.getId(), saved.getPriority());

        return new DataResponse<>(toResponse(saved));
    }

    @Transactional(readOnly = true)
    public DataResponse<List<PopularServiceResponse>> getPopularServicesByCategory(Long categoryId) {
        if (categoryId == null) {
            throw new BadRequestException("categoryId is required");
        }

        List<PopularServiceView> rows = popularServiceRepository.findActiveServiceViewsByCategory(categoryId);
        List<PopularServiceResponse> response = rows.stream().map(this::toResponse).toList();
        return new DataResponse<>(response);
    }

    @Transactional
    public AdminMessageResponse deletePopularService(Long id) {
        PopularServiceEntity entity = popularServiceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Popular service not found"));

        entity.setActive(false);
        popularServiceRepository.save(entity);
        audit("POPULAR_SERVICE_DELETE", "popular_services", id, "softDeactivate=true");
        log.info("Popular service deactivated: popularServiceId={}", id);
        return new AdminMessageResponse("Popular service deleted successfully");
    }

    private PopularServiceResponse toResponse(PopularServiceEntity entity) {
        PopularServiceResponse response = new PopularServiceResponse();
        response.setId(entity.getService().getId());
        response.setName(entity.getService().getName());
        response.setImageUrl(mediaUrlResolver.toPublicUrl(entity.getService().getImageUrl()));
        response.setPrice(entity.getService().getBasePrice());
        response.setRating(entity.getService().getAvgRating() == null ? 0.0 : entity.getService().getAvgRating());
        response.setPriority(entity.getPriority());
        return response;
    }

    private PopularServiceResponse toResponse(PopularServiceView view) {
        PopularServiceResponse response = new PopularServiceResponse();
        response.setId(view.getId());
        response.setName(view.getName());
        response.setImageUrl(mediaUrlResolver.toPublicUrl(view.getImageUrl()));
        response.setPrice(view.getPrice());
        response.setRating(view.getRating() == null ? 0.0 : view.getRating());
        response.setPriority(view.getPriority());
        return response;
    }

    private void audit(String action, String entity, Long entityId, String metadata) {
        adminAuditService.log(securityUtils.getCurrentUserId().orElse(0L), action, entity, entityId, metadata);
    }
}

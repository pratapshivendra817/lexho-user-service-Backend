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

import java.util.ArrayList;
import java.time.LocalDateTime;
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

        PopularServiceEntity existingPair = popularServiceRepository
                .findByServiceIdAndCategoryId(service.getId(), category.getId())
                .orElse(null);

        shiftPrioritiesForInsert(category.getId(), request.getPriority(), existingPair == null ? null : existingPair.getId());

        PopularServiceEntity saved;
        if (existingPair != null) {
            if (existingPair.isActive()) {
                throw new BadRequestException("Service is already configured as popular for this category");
            }
            saved = reactivateExisting(existingPair, request.getPriority(), "POPULAR_SERVICE_REACTIVATE");
            log.info("Popular service reactivated from add flow: popularServiceId={}, serviceId={}, categoryId={}, priority={}",
                    saved.getId(), service.getId(), category.getId(), saved.getPriority());
        } else {
            PopularServiceEntity entity = new PopularServiceEntity();
            entity.setService(service);
            entity.setCategory(category);
            entity.setPriority(request.getPriority());
            entity.setActive(true);
            entity.setDeletedAt(null);
            entity.setDeletedBy(null);
            entity.setDeleteReason(null);
            saved = popularServiceRepository.save(entity);
            audit("POPULAR_SERVICE_CREATE", "popular_services", saved.getId(),
                    "serviceId=" + service.getId() + ",categoryId=" + category.getId() + ",priority=" + request.getPriority());
            log.info("Popular service added: popularServiceId={}, serviceId={}, categoryId={}, priority={}",
                    saved.getId(), service.getId(), category.getId(), saved.getPriority());
        }

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
    public AdminMessageResponse deletePopularService(Long id, String deleteReason) {
        PopularServiceEntity entity = popularServiceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Popular service not found"));

        if (!entity.isActive()) {
            throw new BadRequestException("Popular service is already inactive");
        }

        Long actorId = securityUtils.getCurrentUserId().orElse(0L);
        entity.setActive(false);
        entity.setDeletedAt(LocalDateTime.now());
        entity.setDeletedBy(actorId);
        entity.setDeleteReason(normalizeDeleteReason(deleteReason));
        popularServiceRepository.save(entity);
        audit("POPULAR_SERVICE_DELETE", "popular_services", id,
                "softDeactivate=true,deletedBy=" + actorId + ",deleteReason=" + normalizeDeleteReason(deleteReason));
        log.info("Popular service deactivated: popularServiceId={}, deletedBy={}", id, actorId);
        return new AdminMessageResponse("Popular service deleted successfully");
    }

    @Transactional
    public DataResponse<PopularServiceResponse> restorePopularService(Long id, Integer requestedPriority) {
        PopularServiceEntity entity = popularServiceRepository.findByIdAndActiveFalse(id)
                .orElseThrow(() -> new ResourceNotFoundException("Inactive popular service not found"));

        int priority = requestedPriority == null ? entity.getPriority() : requestedPriority;
        if (priority < 1) {
            throw new BadRequestException("priority must be at least 1");
        }

        shiftPrioritiesForInsert(entity.getCategory().getId(), priority, entity.getId());

        PopularServiceEntity restored = reactivateExisting(entity, priority, "POPULAR_SERVICE_RESTORE");
        log.info("Popular service restored via endpoint: popularServiceId={}, serviceId={}, categoryId={}, priority={}",
                restored.getId(), restored.getService().getId(), restored.getCategory().getId(), restored.getPriority());
        return new DataResponse<>(toResponse(restored));
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

    private PopularServiceEntity reactivateExisting(PopularServiceEntity existingPair, Integer priority, String auditAction) {
        existingPair.setPriority(priority);
        existingPair.setActive(true);
        existingPair.setDeletedAt(null);
        existingPair.setDeletedBy(null);
        existingPair.setDeleteReason(null);
        PopularServiceEntity saved = popularServiceRepository.save(existingPair);
        audit(auditAction, "popular_services", saved.getId(),
                "serviceId=" + saved.getService().getId() + ",categoryId=" + saved.getCategory().getId() + ",priority=" + saved.getPriority());
        return saved;
    }

    private void shiftPrioritiesForInsert(Long categoryId, Integer requestedPriority, Long excludedPopularServiceId) {
        if (requestedPriority == null || requestedPriority < 1) {
            throw new BadRequestException("priority must be at least 1");
        }

        List<PopularServiceEntity> activeItems = popularServiceRepository
                .findByCategory_IdAndActiveTrueOrderByPriorityAsc(categoryId);
        if (activeItems.isEmpty()) {
            return;
        }

        List<PopularServiceEntity> toShift = new ArrayList<>();
        for (PopularServiceEntity item : activeItems) {
            if (excludedPopularServiceId != null && excludedPopularServiceId.equals(item.getId())) {
                continue;
            }
            if (item.getPriority() >= requestedPriority) {
                toShift.add(item);
            }
        }

        for (int i = toShift.size() - 1; i >= 0; i--) {
            PopularServiceEntity item = toShift.get(i);
            item.setPriority(item.getPriority() + 1);
        }

        if (!toShift.isEmpty()) {
            popularServiceRepository.saveAll(toShift);
        }
    }

    private String normalizeDeleteReason(String deleteReason) {
        if (deleteReason == null) {
            return null;
        }
        String trimmed = deleteReason.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}

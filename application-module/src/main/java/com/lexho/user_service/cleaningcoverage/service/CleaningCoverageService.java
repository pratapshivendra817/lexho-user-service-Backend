package com.lexho.user_service.cleaningcoverage.service;

import com.lexho.user_service.admin.dto.AdminMessageResponse;
import com.lexho.user_service.admin.dto.DataResponse;
import com.lexho.user_service.cleaningcoverage.dto.AdminCleaningCoverageRequest;
import com.lexho.user_service.cleaningcoverage.dto.AdminCleaningCoverageResponse;
import com.lexho.user_service.cleaningcoverage.dto.AdminCleaningCoverageTaskRequest;
import com.lexho.user_service.cleaningcoverage.dto.AdminCleaningCoverageTaskResponse;
import com.lexho.user_service.cleaningcoverage.dto.CleaningCoverageResponse;
import com.lexho.user_service.cleaningcoverage.dto.CleaningCoverageTaskResponse;
import com.lexho.user_service.cleaningcoverage.entity.CleaningCoverageEntity;
import com.lexho.user_service.cleaningcoverage.entity.CleaningCoverageTaskEntity;
import com.lexho.user_service.cleaningcoverage.repository.CleaningCoverageRepository;
import com.lexho.user_service.exception.ResourceNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class CleaningCoverageService {

    private final CleaningCoverageRepository cleaningCoverageRepository;

    public CleaningCoverageService(CleaningCoverageRepository cleaningCoverageRepository) {
        this.cleaningCoverageRepository = cleaningCoverageRepository;
    }

    @Transactional(readOnly = true)
    public List<CleaningCoverageResponse> list(Optional<BigDecimal> durationHours) {
        List<CleaningCoverageEntity> rows = durationHours
                .map(cleaningCoverageRepository::findByDurationHoursAndActiveTrueOrderBySortOrderAsc)
                .orElseGet(cleaningCoverageRepository::findByActiveTrueOrderBySortOrderAscDurationHoursAsc);
        return rows.stream().map(this::toCustomerResponse).toList();
    }

    @Transactional(readOnly = true)
    public DataResponse<List<AdminCleaningCoverageResponse>> adminList() {
        return new DataResponse<>(cleaningCoverageRepository.findAllByOrderBySortOrderAscDurationHoursAsc()
                .stream()
                .map(this::toAdminResponse)
                .toList());
    }

    @Transactional
    public DataResponse<AdminCleaningCoverageResponse> create(AdminCleaningCoverageRequest request) {
        CleaningCoverageEntity entity = new CleaningCoverageEntity();
        applyRequest(entity, request);
        CleaningCoverageEntity saved = cleaningCoverageRepository.save(entity);
        return new DataResponse<>(toAdminResponse(saved));
    }

    @Transactional
    public DataResponse<AdminCleaningCoverageResponse> update(Long id, AdminCleaningCoverageRequest request) {
        CleaningCoverageEntity entity = cleaningCoverageRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Cleaning coverage not found"));
        applyRequest(entity, request);
        CleaningCoverageEntity saved = cleaningCoverageRepository.save(entity);
        return new DataResponse<>(toAdminResponse(saved));
    }

    @Transactional
    public AdminMessageResponse delete(Long id) {
        CleaningCoverageEntity entity = cleaningCoverageRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Cleaning coverage not found"));
        entity.setActive(false);
        cleaningCoverageRepository.save(entity);
        return new AdminMessageResponse("Cleaning coverage deleted successfully");
    }

    private void applyRequest(CleaningCoverageEntity entity, AdminCleaningCoverageRequest request) {
        entity.setDurationHours(request.durationHours());
        entity.setReference(request.reference().trim());
        entity.setSortOrder(request.sortOrder() == null ? request.durationHours().multiply(BigDecimal.valueOf(100)).intValue()
                : request.sortOrder());
        entity.setActive(request.active() == null || request.active());
        entity.setTasks(toTaskEntities(request.tasks()));
    }

    private List<CleaningCoverageTaskEntity> toTaskEntities(List<AdminCleaningCoverageTaskRequest> tasks) {
        List<CleaningCoverageTaskEntity> entities = new ArrayList<>();
        for (int i = 0; i < tasks.size(); i++) {
            AdminCleaningCoverageTaskRequest request = tasks.get(i);
            CleaningCoverageTaskEntity entity = new CleaningCoverageTaskEntity();
            entity.setName(request.name().trim());
            entity.setEstimatedMinutes(request.estimatedMinutes());
            entity.setIcon(request.icon().trim());
            entity.setSortOrder(request.sortOrder() == null ? i + 1 : request.sortOrder());
            entities.add(entity);
        }
        return entities;
    }

    private CleaningCoverageResponse toCustomerResponse(CleaningCoverageEntity entity) {
        return new CleaningCoverageResponse(strip(entity.getDurationHours()), entity.getReference(), entity.getTasks()
                .stream()
                .map(task -> new CleaningCoverageTaskResponse(task.getName(), task.getEstimatedMinutes(), task.getIcon()))
                .toList());
    }

    private AdminCleaningCoverageResponse toAdminResponse(CleaningCoverageEntity entity) {
        return new AdminCleaningCoverageResponse(entity.getId(), strip(entity.getDurationHours()), entity.getReference(),
                entity.getSortOrder(), entity.isActive(), entity.getTasks()
                .stream()
                .map(task -> new AdminCleaningCoverageTaskResponse(task.getId(), task.getName(),
                        task.getEstimatedMinutes(), task.getIcon(), task.getSortOrder()))
                .toList());
    }

    private BigDecimal strip(BigDecimal value) {
        return value == null ? null : value.stripTrailingZeros();
    }
}

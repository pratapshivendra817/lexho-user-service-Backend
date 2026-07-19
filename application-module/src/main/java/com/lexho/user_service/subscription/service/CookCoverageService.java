package com.lexho.user_service.subscription.service;

import com.lexho.user_service.admin.dto.AdminMessageResponse;
import com.lexho.user_service.admin.dto.DataResponse;
import com.lexho.user_service.admin.entity.AdminServiceEntity;
import com.lexho.user_service.admin.repository.AdminServiceEntityRepository;
import com.lexho.user_service.exception.BadRequestException;
import com.lexho.user_service.exception.ResourceNotFoundException;
import com.lexho.user_service.subscription.dto.CookCoverageItemRequest;
import com.lexho.user_service.subscription.dto.CookCoverageItemResponse;
import com.lexho.user_service.subscription.dto.CookCoverageRequest;
import com.lexho.user_service.subscription.dto.CookCoverageResponse;
import com.lexho.user_service.subscription.entity.CookCoverage;
import com.lexho.user_service.subscription.entity.CookCoverageItem;
import com.lexho.user_service.subscription.enums.FamilySize;
import com.lexho.user_service.subscription.repository.CookCoverageRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class CookCoverageService {

    private static final String DEFAULT_REFERENCE = "Standard home cooking";

    private final CookCoverageRepository cookCoverageRepository;
    private final AdminServiceEntityRepository serviceRepository;

    public CookCoverageService(CookCoverageRepository cookCoverageRepository,
                               AdminServiceEntityRepository serviceRepository) {
        this.cookCoverageRepository = cookCoverageRepository;
        this.serviceRepository = serviceRepository;
    }

    @Transactional(readOnly = true)
    public List<CookCoverageResponse> customerList(Long serviceId, FamilySize familySize) {
        List<CookCoverage> allActiveRows = activeRows(serviceId, null);
        if (allActiveRows.isEmpty()) {
            if (serviceId != null) {
                return List.of();
            }
            return fallback(familySize);
        }
        List<CookCoverage> rows = familySize == null
                ? allActiveRows
                : allActiveRows.stream()
                .filter(row -> row.getFamilySize() == familySize)
                .toList();
        if (rows.isEmpty()) {
            if (serviceId != null) {
                return List.of();
            }
            return fallback(familySize);
        }
        Map<Long, String> serviceNames = serviceNames(allActiveRows);
        return rows.stream().map(row -> toCustomerResponse(row, serviceNames)).toList();
    }

    @Transactional(readOnly = true)
    public DataResponse<List<CookCoverageResponse>> adminList() {
        List<CookCoverage> rows = cookCoverageRepository.findAllByOrderBySortOrderAscFamilySizeAsc();
        Map<Long, String> serviceNames = serviceNames(rows);
        return new DataResponse<>(rows.stream().map(entity -> toResponse(entity, serviceNames)).toList());
    }

    @Transactional
    public DataResponse<CookCoverageResponse> create(CookCoverageRequest request) {
        CookCoverage entity = new CookCoverage();
        applyRequest(entity, request);
        CookCoverage saved = cookCoverageRepository.save(entity);
        return new DataResponse<>(toResponse(saved, serviceNames(List.of(saved))));
    }

    @Transactional
    public DataResponse<CookCoverageResponse> update(Long id, CookCoverageRequest request) {
        CookCoverage entity = cookCoverageRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Cook coverage not found"));
        applyRequest(entity, request);
        CookCoverage saved = cookCoverageRepository.save(entity);
        return new DataResponse<>(toResponse(saved, serviceNames(List.of(saved))));
    }

    @Transactional
    public AdminMessageResponse delete(Long id) {
        CookCoverage entity = cookCoverageRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Cook coverage not found"));
        entity.setActive(false);
        cookCoverageRepository.save(entity);
        return new AdminMessageResponse("Cook coverage deleted successfully");
    }

    private void applyRequest(CookCoverage entity, CookCoverageRequest request) {
        validateService(request.serviceId());
        ensureFamilySizeSlotAvailable(entity.getId(), request.serviceId(), request.familySize());
        entity.setServiceId(request.serviceId());
        entity.setFamilySize(request.familySize());
        entity.setMembers(request.members().trim());
        entity.setFamilySizeLabel(request.familySizeLabel().trim());
        entity.setReference(request.reference() == null || request.reference().isBlank()
                ? DEFAULT_REFERENCE
                : request.reference().trim());
        entity.setSortOrder(request.sortOrder() == null ? defaultSortOrder(request.familySize()) : request.sortOrder());
        entity.setActive(request.active() == null || request.active());
        entity.setItems(toItemEntities(request.items()));
    }

    private List<CookCoverageItem> toItemEntities(List<CookCoverageItemRequest> requests) {
        List<CookCoverageItem> items = new ArrayList<>();
        for (int i = 0; i < requests.size(); i++) {
            CookCoverageItemRequest request = requests.get(i);
            CookCoverageItem item = new CookCoverageItem();
            item.setName(request.name().trim());
            item.setIcon(request.icon().trim());
            item.setSortOrder(request.sortOrder() == null ? i + 1 : request.sortOrder());
            item.setQuantity(request.quantity() == null ? defaultQuantity(request.name()) : request.quantity());
            items.add(item);
        }
        return items;
    }

    private CookCoverageResponse toResponse(CookCoverage entity, Map<Long, String> serviceNames) {
        return new CookCoverageResponse(
                entity.getId(),
                entity.getServiceId(),
                serviceName(serviceNames, entity.getServiceId()),
                entity.getFamilySize().name(),
                entity.getMembers(),
                entity.getFamilySizeLabel(),
                entity.getReference(),
                entity.getSortOrder(),
                entity.isActive(),
                entity.getItems().stream().map(this::toItemResponse).toList()
        );
    }

    private CookCoverageResponse toCustomerResponse(CookCoverage entity, Map<Long, String> serviceNames) {
        return new CookCoverageResponse(
                null,
                entity.getServiceId(),
                serviceName(serviceNames, entity.getServiceId()),
                entity.getFamilySize().name(),
                entity.getMembers(),
                entity.getFamilySizeLabel(),
                entity.getReference(),
                null,
                null,
                entity.getItems().stream().map(this::toItemResponse).toList()
        );
    }

    private CookCoverageItemResponse toItemResponse(CookCoverageItem item) {
        return new CookCoverageItemResponse(
                item.getId(),
                item.getName(),
                item.getIcon(),
                item.getSortOrder(),
                item.getQuantity()
        );
    }

    private List<CookCoverageResponse> fallback(FamilySize requestedSize) {
        List<FamilySize> sizes = requestedSize == null ? List.of(FamilySize.values()) : List.of(requestedSize);
        return sizes.stream().map(size -> new CookCoverageResponse(
                null,
                null,
                null,
                size.name(),
                members(size),
                familySizeLabel(size),
                DEFAULT_REFERENCE,
                defaultSortOrder(size),
                true,
                fallbackItems(size)
        )).toList();
    }

    private List<CookCoverageItemResponse> fallbackItems(FamilySize familySize) {
        List<CookCoverageItemResponse> items = new ArrayList<>();
        items.add(new CookCoverageItemResponse(null, "Dal", "dal", 1, 1));
        items.add(new CookCoverageItemResponse(null, "Rice", "rice", 2, 1));
        items.add(new CookCoverageItemResponse(null, "Sabzi", "sabzi", 3, 1));
        items.add(new CookCoverageItemResponse(null, "Roti", "roti", 4, rotiQuantity(familySize)));
        items.add(new CookCoverageItemResponse(null, "Salad", "salad", 5, 1));
        return items;
    }

    private int defaultQuantity(String itemName) {
        if (itemName != null && "roti".equalsIgnoreCase(itemName.trim())) {
            return 6;
        }
        return 1;
    }

    private int rotiQuantity(FamilySize familySize) {
        return switch (familySize) {
            case SMALL -> 6;
            case MEDIUM -> 10;
            case LARGE -> 14;
            case XLARGE -> 18;
        };
    }

    private String members(FamilySize familySize) {
        return switch (familySize) {
            case SMALL -> "1-2";
            case MEDIUM -> "3-4";
            case LARGE -> "5-6";
            case XLARGE -> "6-8";
        };
    }

    private String familySizeLabel(FamilySize familySize) {
        return members(familySize) + " Members";
    }

    private Integer defaultSortOrder(FamilySize familySize) {
        return switch (familySize) {
            case SMALL -> 10;
            case MEDIUM -> 20;
            case LARGE -> 30;
            case XLARGE -> 40;
        };
    }

    private List<CookCoverage> activeRows(Long serviceId, FamilySize familySize) {
        if (serviceId != null) {
            return familySize == null
                    ? cookCoverageRepository.findByServiceIdAndActiveTrueOrderBySortOrderAscFamilySizeAsc(serviceId)
                    : cookCoverageRepository.findByServiceIdAndFamilySizeAndActiveTrueOrderBySortOrderAsc(serviceId, familySize);
        }
        return familySize == null
                ? cookCoverageRepository.findByServiceIdIsNullAndActiveTrueOrderBySortOrderAscFamilySizeAsc()
                : cookCoverageRepository.findByServiceIdIsNullAndFamilySizeAndActiveTrueOrderBySortOrderAsc(familySize);
    }

    private void validateService(Long serviceId) {
        if (serviceId != null && serviceRepository.findByIdAndDeletedFalse(serviceId).isEmpty()) {
            throw new ResourceNotFoundException("Service not found");
        }
    }

    private void ensureFamilySizeSlotAvailable(Long currentId, Long serviceId, FamilySize familySize) {
        CookCoverage existing = serviceId == null
                ? cookCoverageRepository.findByServiceIdIsNullAndFamilySizeAndActiveTrue(familySize).orElse(null)
                : cookCoverageRepository.findByServiceIdAndFamilySizeAndActiveTrue(serviceId, familySize).orElse(null);
        if (existing != null && (currentId == null || !existing.getId().equals(currentId))) {
            throw new BadRequestException("Cook coverage already exists for this service and family size");
        }
    }

    private Map<Long, String> serviceNames(List<CookCoverage> rows) {
        List<Long> serviceIds = rows.stream()
                .map(CookCoverage::getServiceId)
                .filter(id -> id != null)
                .distinct()
                .toList();
        if (serviceIds.isEmpty()) {
            return Map.of();
        }
        return serviceRepository.findAllById(serviceIds).stream()
                .collect(Collectors.toMap(AdminServiceEntity::getId, AdminServiceEntity::getName, (left, right) -> left));
    }

    private String serviceName(Map<Long, String> serviceNames, Long serviceId) {
        if (serviceId == null) {
            return null;
        }
        return serviceNames.get(serviceId);
    }
}

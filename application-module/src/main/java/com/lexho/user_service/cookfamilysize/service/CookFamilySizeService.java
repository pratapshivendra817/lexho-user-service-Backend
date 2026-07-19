package com.lexho.user_service.cookfamilysize.service;

import com.lexho.user_service.cookfamilysize.dto.CookFamilySizeAdminResponse;
import com.lexho.user_service.cookfamilysize.dto.CookFamilySizeRequest;
import com.lexho.user_service.cookfamilysize.dto.CookFamilySizeStatusRequest;
import com.lexho.user_service.cookfamilysize.entity.CookFamilySizeEntity;
import com.lexho.user_service.cookfamilysize.repository.CookFamilySizeRepository;
import com.lexho.user_service.customer.dto.CookFamilySizeResponse;
import com.lexho.user_service.exception.BadRequestException;
import com.lexho.user_service.exception.CodedValidationException;
import com.lexho.user_service.exception.ResourceNotFoundException;
import com.lexho.user_service.subscription.enums.FamilySize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;

@Service
public class CookFamilySizeService {
    private final CookFamilySizeRepository repository;

    public CookFamilySizeService(CookFamilySizeRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public List<CookFamilySizeResponse> customerList() {
        return repository.findByActiveTrueOrderByDisplayOrderAscIdAsc().stream()
                .map(entity -> new CookFamilySizeResponse(entity.getId(), entity.getFamilySize(),
                        entity.getFamilySizeLabel()))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<CookFamilySizeAdminResponse> adminList() {
        return repository.findAllByOrderByDisplayOrderAscIdAsc().stream()
                .map(this::toAdminResponse)
                .toList();
    }

    @Transactional
    public CookFamilySizeAdminResponse create(CookFamilySizeRequest request) {
        String familySize = normalizeFamilySize(request.getFamilySize());
        if (repository.findByFamilySize(familySize).isPresent()) {
            throw new BadRequestException("familySize already exists");
        }
        CookFamilySizeEntity entity = new CookFamilySizeEntity();
        applyRequest(entity, request, familySize);
        return toAdminResponse(repository.save(entity));
    }

    @Transactional
    public CookFamilySizeAdminResponse update(Long id, CookFamilySizeRequest request) {
        CookFamilySizeEntity entity = find(id);
        String familySize = normalizeFamilySize(request.getFamilySize());
        if (repository.existsByFamilySizeAndIdNot(familySize, id)) {
            throw new BadRequestException("familySize already exists");
        }
        applyRequest(entity, request, familySize);
        return toAdminResponse(repository.save(entity));
    }

    @Transactional
    public CookFamilySizeAdminResponse updateStatus(Long id, CookFamilySizeStatusRequest request) {
        CookFamilySizeEntity entity = find(id);
        entity.setActive(Boolean.TRUE.equals(request.getActive()));
        return toAdminResponse(repository.save(entity));
    }

    @Transactional
    public CookFamilySizeAdminResponse deactivate(Long id) {
        CookFamilySizeEntity entity = find(id);
        entity.setActive(false);
        return toAdminResponse(repository.save(entity));
    }

    @Transactional(readOnly = true)
    public CookFamilySizeEntity requireActive(Long id, String familySize) {
        if (familySize != null && !familySize.isBlank()) {
            CookFamilySizeEntity byCode = repository.findByFamilySizeAndActiveTrue(normalizeFamilySize(familySize))
                    .orElseThrow(() -> new CodedValidationException("SERVICE_UNAVAILABLE",
                            "Selected family size is unavailable"));
            if (id != null && !byCode.getId().equals(id)) {
                throw new CodedValidationException("SERVICE_UNAVAILABLE",
                        "familySizeId does not match familySize");
            }
            return byCode;
        }
        if (id == null) {
            throw new CodedValidationException("SERVICE_UNAVAILABLE", "familySize is required");
        }
        return repository.findByIdAndActiveTrue(id)
                .orElseThrow(() -> new CodedValidationException("SERVICE_UNAVAILABLE",
                        "Selected family size is unavailable"));
    }

    @Transactional(readOnly = true)
    public String label(FamilySize familySize) {
        if (familySize == null) {
            return null;
        }
        return repository.findByFamilySizeAndActiveTrue(familySize.name())
                .map(CookFamilySizeEntity::getFamilySizeLabel)
                .orElse(defaultLabel(familySize));
    }

    private void applyRequest(CookFamilySizeEntity entity, CookFamilySizeRequest request, String familySize) {
        entity.setFamilySize(familySize);
        entity.setFamilySizeLabel(request.getFamilySizeLabel().trim());
        entity.setActive(Boolean.TRUE.equals(request.getActive()));
        entity.setDisplayOrder(request.getDisplayOrder());
    }

    private CookFamilySizeEntity find(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Cook family size not found"));
    }

    private CookFamilySizeAdminResponse toAdminResponse(CookFamilySizeEntity entity) {
        return new CookFamilySizeAdminResponse(entity.getId(), entity.getFamilySize(),
                entity.getFamilySizeLabel(), entity.isActive(), entity.getDisplayOrder());
    }

    private String normalizeFamilySize(String value) {
        String normalized = value == null ? "" : value.trim().toUpperCase(Locale.ROOT);
        try {
            FamilySize.valueOf(normalized);
            return normalized;
        } catch (IllegalArgumentException ex) {
            throw new BadRequestException("Invalid familySize");
        }
    }

    private String defaultLabel(FamilySize familySize) {
        return switch (familySize) {
            case SMALL -> "1-2 Members";
            case MEDIUM -> "3-5 Members";
            case LARGE -> "6-8 Members";
            case XLARGE -> "9+ Members";
        };
    }
}

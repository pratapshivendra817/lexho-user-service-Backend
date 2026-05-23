package com.lexho.user_service.admin.settings.service;

import com.lexho.user_service.admin.settings.entity.AppSettingEntity;
import com.lexho.user_service.admin.settings.repository.AppSettingRepository;
import com.lexho.user_service.exception.BadRequestException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class AdminSettingsService {

    public static final String DISPATCH_RADIUS_KM = "dispatch.radius.km";
    public static final String PARTNER_SERVICE_RADIUS_KM = "partner.service.radius.km";

    private final AppSettingRepository appSettingRepository;

    public AdminSettingsService(AppSettingRepository appSettingRepository) {
        this.appSettingRepository = appSettingRepository;
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> listAll() {
        return appSettingRepository.findAll().stream().map(this::toMap).toList();
    }

    @Transactional(readOnly = true)
    public Map<String, Object> get(String key) {
        AppSettingEntity entity = appSettingRepository.findById(key)
                .orElseThrow(() -> new BadRequestException("Setting not found"));
        return toMap(entity);
    }

    @Transactional
    public Map<String, Object> upsert(String key, String value, String description, String updatedBy) {
        if (key == null || key.isBlank()) {
            throw new BadRequestException("key is required");
        }
        if (value == null || value.isBlank()) {
            throw new BadRequestException("value is required");
        }
        AppSettingEntity entity = appSettingRepository.findById(key.trim()).orElseGet(AppSettingEntity::new);
        entity.setSettingKey(key.trim());
        entity.setSettingValue(value.trim());
        entity.setDescription(description == null ? null : description.trim());
        entity.setUpdatedBy(updatedBy);
        return toMap(appSettingRepository.save(entity));
    }

    @Transactional
    public Map<String, Object> updateDispatchRadius(Double radiusKm, String updatedBy) {
        if (radiusKm == null || radiusKm <= 0d || radiusKm > 100d) {
            throw new BadRequestException("radiusKm must be > 0 and <= 100");
        }
        return upsert(DISPATCH_RADIUS_KM, String.valueOf(radiusKm), "Dispatch radius in km", updatedBy);
    }

    @Transactional
    public Map<String, Object> updatePartnerServiceRadius(Double radiusKm, String updatedBy) {
        if (radiusKm == null || radiusKm <= 0d || radiusKm > 100d) {
            throw new BadRequestException("radiusKm must be > 0 and <= 100");
        }
        return upsert(PARTNER_SERVICE_RADIUS_KM, String.valueOf(radiusKm), "Partner service radius in km", updatedBy);
    }

    private Map<String, Object> toMap(AppSettingEntity entity) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("key", entity.getSettingKey());
        row.put("value", entity.getSettingValue());
        row.put("description", entity.getDescription());
        row.put("updatedBy", entity.getUpdatedBy());
        row.put("createdAt", entity.getCreatedAt());
        row.put("updatedAt", entity.getUpdatedAt());
        return row;
    }
}


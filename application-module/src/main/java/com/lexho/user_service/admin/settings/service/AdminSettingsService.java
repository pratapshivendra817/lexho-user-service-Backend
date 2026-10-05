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
    public static final String DISPATCH_RETRY_COUNT = "dispatch.retry.count";
    public static final String DISPATCH_TIMEOUT_SEC = "dispatch.timeout.sec";
    public static final String MAP_PROVIDER = "map.provider";
    public static final String MAP_DEFAULT_CITY = "map.default.city";
    public static final String MAP_DEFAULT_LAT = "map.default.lat";
    public static final String MAP_DEFAULT_LNG = "map.default.lng";
    private static final String VALUE_KEY = "value";
    private static final String SERVICE_RADIUS_KM_KEY = "serviceRadiusKm";
    private static final String DISPATCH_RETRY_COUNT_KEY = "dispatchRetryCount";
    private static final String DISPATCH_TIMEOUT_SEC_KEY = "dispatchTimeoutSec";
    private static final String MAP_PROVIDER_KEY = "mapProvider";
    private static final String MAP_DEFAULT_CITY_KEY = "mapDefaultCity";
    private static final String MAP_DEFAULT_LAT_KEY = "mapDefaultLat";
    private static final String MAP_DEFAULT_LNG_KEY = "mapDefaultLng";
    private static final String DESCRIPTION_PARTNER_SERVICE_RADIUS = "Partner service radius in km";
    private static final double MAX_RADIUS_KM = 100d;
    private static final int MAX_DISPATCH_RETRY_COUNT = 20;
    private static final int MAX_DISPATCH_TIMEOUT_SEC = 3600;

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
        return saveSetting(key, value, description, updatedBy);
    }

    @Transactional
    public Map<String, Object> updateDispatchRadius(Double radiusKm, String updatedBy) {
        validateRadius(radiusKm, "radiusKm must be > 0 and <= 100");
        return saveSetting(DISPATCH_RADIUS_KM, String.valueOf(radiusKm), "Dispatch radius in km", updatedBy);
    }

    @Transactional
    public Map<String, Object> updatePartnerServiceRadius(Double radiusKm, String updatedBy) {
        validateRadius(radiusKm, "radiusKm must be > 0 and <= 100");
        return saveSetting(PARTNER_SERVICE_RADIUS_KM, String.valueOf(radiusKm), DESCRIPTION_PARTNER_SERVICE_RADIUS,
                updatedBy);
    }

    @Transactional
    public Map<String, Object> updateDashboardSettings(Double serviceRadiusKm,
                                                        Integer dispatchRetryCount,
                                                        Integer dispatchTimeoutSec,
                                                        String mapProvider,
                                                        String mapDefaultCity,
                                                        Double mapDefaultLat,
                                                        Double mapDefaultLng,
                                                        String updatedBy) {
        if (serviceRadiusKm == null || serviceRadiusKm <= 0d || serviceRadiusKm > MAX_RADIUS_KM) {
            throw new BadRequestException("serviceRadiusKm must be > 0 and <= 100");
        }
        if (dispatchRetryCount == null || dispatchRetryCount < 0 || dispatchRetryCount > MAX_DISPATCH_RETRY_COUNT) {
            throw new BadRequestException("dispatchRetryCount must be >= 0 and <= 20");
        }
        if (dispatchTimeoutSec == null || dispatchTimeoutSec <= 0 || dispatchTimeoutSec > MAX_DISPATCH_TIMEOUT_SEC) {
            throw new BadRequestException("dispatchTimeoutSec must be > 0 and <= 3600");
        }
        if (mapProvider == null || mapProvider.isBlank()) {
            throw new BadRequestException("mapProvider is required");
        }
        if (mapDefaultCity == null || mapDefaultCity.isBlank()) {
            throw new BadRequestException("mapDefaultCity is required");
        }
        if (mapDefaultLat == null || mapDefaultLat < -90d || mapDefaultLat > 90d) {
            throw new BadRequestException("mapDefaultLat must be >= -90 and <= 90");
        }
        if (mapDefaultLng == null || mapDefaultLng < -180d || mapDefaultLng > 180d) {
            throw new BadRequestException("mapDefaultLng must be >= -180 and <= 180");
        }

        Map<String, Object> settings = new LinkedHashMap<>();
        settings.put(SERVICE_RADIUS_KM_KEY, value(saveSetting(PARTNER_SERVICE_RADIUS_KM, String.valueOf(serviceRadiusKm),
                DESCRIPTION_PARTNER_SERVICE_RADIUS, updatedBy)));
        settings.put(DISPATCH_RETRY_COUNT_KEY, value(saveSetting(DISPATCH_RETRY_COUNT, String.valueOf(dispatchRetryCount),
                "Dispatch retry count", updatedBy)));
        settings.put(DISPATCH_TIMEOUT_SEC_KEY, value(saveSetting(DISPATCH_TIMEOUT_SEC, String.valueOf(dispatchTimeoutSec),
                "Dispatch timeout in seconds", updatedBy)));
        settings.put(MAP_PROVIDER_KEY, value(saveSetting(MAP_PROVIDER, mapProvider, "Map provider", updatedBy)));
        settings.put(MAP_DEFAULT_CITY_KEY, value(saveSetting(MAP_DEFAULT_CITY, mapDefaultCity, "Default map city",
                updatedBy)));
        settings.put(MAP_DEFAULT_LAT_KEY, value(saveSetting(MAP_DEFAULT_LAT, String.valueOf(mapDefaultLat),
                "Default map latitude", updatedBy)));
        settings.put(MAP_DEFAULT_LNG_KEY, value(saveSetting(MAP_DEFAULT_LNG, String.valueOf(mapDefaultLng),
                "Default map longitude", updatedBy)));
        return settings;
    }

    private Map<String, Object> saveSetting(String key, String value, String description, String updatedBy) {
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

    private Object value(Map<String, Object> setting) {
        return setting.get(VALUE_KEY);
    }

    private void validateRadius(Double radiusKm, String message) {
        if (radiusKm == null || radiusKm <= 0d || radiusKm > MAX_RADIUS_KM) {
            throw new BadRequestException(message);
        }
    }

    private Map<String, Object> toMap(AppSettingEntity entity) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("key", entity.getSettingKey());
        row.put(VALUE_KEY, entity.getSettingValue());
        row.put("description", entity.getDescription());
        row.put("updatedBy", entity.getUpdatedBy());
        row.put("createdAt", entity.getCreatedAt());
        row.put("updatedAt", entity.getUpdatedAt());
        return row;
    }
}

package com.lexho.user_service.website.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lexho.user_service.admin.dto.AdminMessageResponse;
import com.lexho.user_service.admin.entity.AdminCategoryEntity;
import com.lexho.user_service.admin.repository.AdminCategoryRepository;
import com.lexho.user_service.admin.repository.AdminServiceEntityRepository;
import com.lexho.user_service.admin.service.AdminAuditService;
import com.lexho.user_service.exception.BadRequestException;
import com.lexho.user_service.exception.ResourceNotFoundException;
import com.lexho.user_service.security.SecurityUtils;
import com.lexho.user_service.website.dto.*;
import com.lexho.user_service.website.entity.WebsiteAreaEntity;
import com.lexho.user_service.website.entity.WebsiteCategoryConfig;
import com.lexho.user_service.website.entity.WebsiteCityEntity;
import com.lexho.user_service.website.repository.WebsiteAreaRepository;
import com.lexho.user_service.website.repository.WebsiteCategoryConfigRepository;
import com.lexho.user_service.website.repository.WebsiteCityRepository;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.Normalizer;
import java.time.Duration;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Pattern;

@Service
public class WebsiteHeaderService {
    private static final String WEBSITE_CITIES = "website_cities";
    private static final String WEBSITE_AREAS = "website_areas";
    private static final String ACTIVE_AUDIT_PREFIX = "active=";
    private static final String SLUG_AUDIT_PREFIX = "slug=";
    private static final String CITY_ID_AUDIT_PREFIX = "cityId=";
    private static final String AREA_NOT_FOUND = "Area not found";
    public static final String CATEGORY_CACHE_KEY = "lexho:website:header:categories";
    public static final String CATEGORY_SERVICE_CACHE_PREFIX = "lexho:website:header:category:";
    public static final String CITY_CACHE_KEY = "lexho:website:header:cities";
    public static final String CITY_AREA_CACHE_PREFIX = "lexho:website:header:city:";

    private static final Pattern NON_SLUG_CHAR = Pattern.compile("[^a-z0-9]+");
    private static final Duration CACHE_TTL = Duration.ofMinutes(10);

    private final AdminCategoryRepository categoryRepository;
    private final AdminServiceEntityRepository serviceRepository;
    private final WebsiteCategoryConfigRepository categoryConfigRepository;
    private final WebsiteCityRepository cityRepository;
    private final WebsiteAreaRepository areaRepository;
    private final AdminAuditService auditService;
    private final SecurityUtils securityUtils;
    private final ObjectProvider<StringRedisTemplate> redisProvider;
    private final ObjectMapper objectMapper;

    public WebsiteHeaderService(AdminCategoryRepository categoryRepository,
                                AdminServiceEntityRepository serviceRepository,
                                WebsiteCategoryConfigRepository categoryConfigRepository,
                                WebsiteCityRepository cityRepository,
                                WebsiteAreaRepository areaRepository,
                                AdminAuditService auditService,
                                SecurityUtils securityUtils,
                                ObjectProvider<StringRedisTemplate> redisProvider,
                                ObjectMapper objectMapper) {
        this.categoryRepository = categoryRepository;
        this.serviceRepository = serviceRepository;
        this.categoryConfigRepository = categoryConfigRepository;
        this.cityRepository = cityRepository;
        this.areaRepository = areaRepository;
        this.auditService = auditService;
        this.securityUtils = securityUtils;
        this.redisProvider = redisProvider;
        this.objectMapper = objectMapper;
    }

    @Transactional(readOnly = true)
    public List<WebsiteHeaderItemResponse> publicCategories() {
        return cached(CATEGORY_CACHE_KEY, new TypeReference<>() {}, () ->
                categoryConfigRepository
                        .findByShowOnWebsiteTrueAndCategory_ActiveTrueAndCategory_DeletedFalseOrderByDisplayOrderAscCategory_NameAsc()
                        .stream()
                        .map(config -> categoryItem(config.getCategory()))
                        .toList()
        );
    }

    @Transactional(readOnly = true)
    public List<WebsiteHeaderServiceResponse> publicServicesByCategorySlug(String categorySlug) {
        String normalizedSlug = requireSlug(categorySlug);
        String cacheKey = CATEGORY_SERVICE_CACHE_PREFIX + normalizedSlug + ":services";
        return cached(cacheKey, new TypeReference<>() {}, () -> {
            AdminCategoryEntity category = categoryConfigRepository
                    .findByShowOnWebsiteTrueAndCategory_ActiveTrueAndCategory_DeletedFalseOrderByDisplayOrderAscCategory_NameAsc()
                    .stream()
                    .map(WebsiteCategoryConfig::getCategory)
                    .filter(item -> slug(item.getName()).equals(normalizedSlug))
                    .findFirst()
                    .orElseThrow(() -> new ResourceNotFoundException("Website category not found"));
            return serviceRepository.findByCategory_IdAndDeletedFalseAndActiveTrueOrderByNameAsc(category.getId())
                    .stream()
                    .map(service -> new WebsiteHeaderServiceResponse(service.getId(), service.getName(), slug(service.getName())))
                    .toList();
        });
    }

    @Transactional(readOnly = true)
    public List<WebsiteHeaderItemResponse> publicCities() {
        return cached(CITY_CACHE_KEY, new TypeReference<>() {}, () ->
                cityRepository
                        .findByActiveTrueAndDeletedFalseOrderByDisplayOrderAscNameAsc()
                        .stream()
                        .map(this::cityItem)
                        .toList()
        );
    }

    @Transactional(readOnly = true)
    public List<WebsiteHeaderItemResponse> publicAreasByCitySlug(String citySlug) {
        String normalizedSlug = requireSlug(citySlug);
        String cacheKey = CITY_AREA_CACHE_PREFIX + normalizedSlug + ":areas";
        return cached(cacheKey, new TypeReference<>() {}, () -> {
            WebsiteCityEntity city = cityRepository
                    .findBySlugIgnoreCaseAndActiveTrueAndDeletedFalse(normalizedSlug)
                    .orElseThrow(() -> new ResourceNotFoundException("Website city not found"));
            return areaRepository.findByCity_IdAndActiveTrueAndDeletedFalseOrderByDisplayOrderAscAreaNameAsc(city.getId())
                    .stream()
                    .map(this::areaItem)
                    .toList();
        });
    }

    @Transactional(readOnly = true)
    public List<AdminWebsiteCategoryResponse> adminCategories() {
        return categoryRepository.findByDeletedFalseOrderByDisplayOrderAscNameAsc()
                .stream()
                .map(category -> {
                    Optional<WebsiteCategoryConfig> config = categoryConfigRepository.findByCategory_Id(category.getId());
                    return new AdminWebsiteCategoryResponse(
                            category.getId(),
                            category.getName(),
                            slug(category.getName()),
                            category.isActive(),
                            config.map(WebsiteCategoryConfig::isShowOnWebsite).orElse(false),
                            config.map(WebsiteCategoryConfig::getDisplayOrder).orElse(category.getDisplayOrder())
                    );
                })
                .toList();
    }

    @Transactional
    public AdminWebsiteCategoryResponse updateCategoryVisibility(Long categoryId, WebsiteVisibilityRequest request) {
        AdminCategoryEntity category = categoryRepository.findByIdAndDeletedFalse(categoryId)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found"));
        WebsiteCategoryConfig config = categoryConfigRepository.findByCategory_Id(categoryId)
                .orElseGet(() -> newCategoryConfig(category));
        config.setShowOnWebsite(Boolean.TRUE.equals(request.getShowOnWebsite()));
        if (request.getDisplayOrder() != null) {
            config.setDisplayOrder(request.getDisplayOrder());
        }
        config.setUpdatedBy(currentAdminId());
        WebsiteCategoryConfig saved = categoryConfigRepository.save(config);
        audit("WEBSITE_CATEGORY_VISIBILITY_CHANGED", "website_category_config", categoryId,
                "showOnWebsite=" + saved.isShowOnWebsite() + ",displayOrder=" + saved.getDisplayOrder());
        evictCategoryCaches();
        return new AdminWebsiteCategoryResponse(category.getId(), category.getName(), slug(category.getName()),
                category.isActive(), saved.isShowOnWebsite(), saved.getDisplayOrder());
    }

    @Transactional
    public AdminMessageResponse reorderCategories(WebsiteReorderRequest request) {
        for (WebsiteReorderRequest.Item item : request.getItems()) {
            AdminCategoryEntity category = categoryRepository.findByIdAndDeletedFalse(item.getId())
                    .orElseThrow(() -> new ResourceNotFoundException("Category not found: " + item.getId()));
            WebsiteCategoryConfig config = categoryConfigRepository.findByCategory_Id(item.getId())
                    .orElseGet(() -> newCategoryConfig(category));
            config.setDisplayOrder(item.getDisplayOrder());
            config.setUpdatedBy(currentAdminId());
            categoryConfigRepository.save(config);
        }
        audit("WEBSITE_CATEGORY_ORDER_CHANGED", "website_category_config", null,
                "count=" + request.getItems().size());
        evictCategoryCaches();
        return new AdminMessageResponse("Category order updated successfully");
    }

    @Transactional(readOnly = true)
    public List<AdminWebsiteCityResponse> adminCities() {
        return cityRepository.findByDeletedFalseOrderByDisplayOrderAscNameAsc()
                .stream()
                .map(this::cityResponse)
                .toList();
    }

    @Transactional
    public AdminWebsiteCityResponse updateCityVisibility(Long cityId, WebsiteVisibilityRequest request) {
        WebsiteCityEntity city = activeOrInactiveCity(cityId);
        city.setActive(Boolean.TRUE.equals(request.getShowOnWebsite()));
        if (request.getDisplayOrder() != null) {
            city.setDisplayOrder(request.getDisplayOrder());
        }
        city.setUpdatedBy(currentAdminId());
        WebsiteCityEntity saved = cityRepository.save(city);
        audit("WEBSITE_CITY_VISIBILITY_CHANGED", WEBSITE_CITIES, cityId,
                ACTIVE_AUDIT_PREFIX + saved.isActive() + ",displayOrder=" + saved.getDisplayOrder());
        evictCityCaches();
        return cityResponse(saved);
    }

    @Transactional
    public AdminMessageResponse reorderCities(WebsiteReorderRequest request) {
        for (WebsiteReorderRequest.Item item : request.getItems()) {
            WebsiteCityEntity city = activeOrInactiveCity(item.getId());
            city.setDisplayOrder(item.getDisplayOrder());
            city.setUpdatedBy(currentAdminId());
            cityRepository.save(city);
        }
        audit("WEBSITE_CITY_ORDER_CHANGED", WEBSITE_CITIES, null,
                "count=" + request.getItems().size());
        evictCityCaches();
        return new AdminMessageResponse("City order updated successfully");
    }

    @Transactional
    public AdminWebsiteCityResponse createCity(WebsiteCityUpsertRequest request) {
        String cityName = requireCityName(request.getName());
        String citySlug = request.getSlug() == null || request.getSlug().isBlank()
                ? slug(cityName)
                : requireSlug(request.getSlug());
        if (cityRepository.existsBySlugIgnoreCaseAndDeletedFalse(citySlug)) {
            throw new BadRequestException("City slug already exists");
        }
        WebsiteCityEntity city = new WebsiteCityEntity();
        city.setName(cityName);
        city.setSlug(citySlug);
        city.setDisplayOrder(request.getDisplayOrder());
        city.setActive(request.getActive() == null || request.getActive());
        city.setCreatedBy(currentAdminId());
        city.setUpdatedBy(currentAdminId());
        WebsiteCityEntity saved = cityRepository.save(city);
        audit("WEBSITE_CITY_CREATED", WEBSITE_CITIES, saved.getId(), SLUG_AUDIT_PREFIX + saved.getSlug());
        evictCityCaches();
        return cityResponse(saved);
    }

    @Transactional
    public AdminWebsiteCityResponse updateCity(Long cityId, WebsiteCityUpsertRequest request) {
        WebsiteCityEntity city = activeOrInactiveCity(cityId);
        String cityName = requireCityName(request.getName());
        String citySlug = request.getSlug() == null || request.getSlug().isBlank()
                ? slug(cityName)
                : requireSlug(request.getSlug());
        if (cityRepository.existsBySlugIgnoreCaseAndDeletedFalseAndIdNot(citySlug, cityId)) {
            throw new BadRequestException("City slug already exists");
        }
        city.setName(cityName);
        city.setSlug(citySlug);
        if (request.getDisplayOrder() != null) {
            city.setDisplayOrder(request.getDisplayOrder());
        }
        if (request.getActive() != null) {
            city.setActive(request.getActive());
        }
        city.setUpdatedBy(currentAdminId());
        WebsiteCityEntity saved = cityRepository.save(city);
        audit("WEBSITE_CITY_UPDATED", WEBSITE_CITIES, saved.getId(), SLUG_AUDIT_PREFIX + saved.getSlug());
        evictCityCaches();
        return cityResponse(saved);
    }

    @Transactional
    public AdminWebsiteCityResponse updateCityStatus(Long cityId, WebsiteStatusRequest request) {
        WebsiteCityEntity city = activeOrInactiveCity(cityId);
        city.setActive(Boolean.TRUE.equals(request.getActive()));
        city.setUpdatedBy(currentAdminId());
        WebsiteCityEntity saved = cityRepository.save(city);
        audit("WEBSITE_CITY_STATUS_CHANGED", WEBSITE_CITIES, saved.getId(), ACTIVE_AUDIT_PREFIX + saved.isActive());
        evictCityCaches();
        return cityResponse(saved);
    }

    @Transactional
    public AdminMessageResponse deleteCity(Long cityId) {
        WebsiteCityEntity city = activeOrInactiveCity(cityId);
        city.setDeleted(true);
        city.setActive(false);
        city.setUpdatedBy(currentAdminId());
        cityRepository.save(city);
        audit("WEBSITE_CITY_DELETED", WEBSITE_CITIES, cityId, SLUG_AUDIT_PREFIX + city.getSlug());
        evictCityCaches();
        return new AdminMessageResponse("City deleted successfully");
    }

    @Transactional(readOnly = true)
    public List<WebsiteAreaResponse> adminAreas(Long cityId) {
        activeOrInactiveCity(cityId);
        return areaRepository.findByCity_IdAndDeletedFalseOrderByDisplayOrderAscAreaNameAsc(cityId)
                .stream()
                .map(this::areaResponse)
                .toList();
    }

    @Transactional
    public WebsiteAreaResponse createArea(Long cityId, WebsiteAreaUpsertRequest request) {
        WebsiteCityEntity city = activeOrInactiveCity(cityId);
        String areaName = requireName(request.getName());
        String areaSlug = request.getSlug() == null || request.getSlug().isBlank()
                ? slug(areaName)
                : requireSlug(request.getSlug());
        if (areaRepository.existsByCity_IdAndSlugIgnoreCaseAndDeletedFalse(cityId, areaSlug)) {
            throw new BadRequestException("Area slug already exists for this city");
        }
        WebsiteAreaEntity area = new WebsiteAreaEntity();
        area.setCity(city);
        area.setAreaName(areaName);
        area.setSlug(areaSlug);
        area.setDisplayOrder(request.getDisplayOrder());
        area.setActive(request.getActive() == null || request.getActive());
        area.setCreatedBy(currentAdminId());
        area.setUpdatedBy(currentAdminId());
        WebsiteAreaEntity saved = areaRepository.save(area);
        audit("WEBSITE_AREA_CREATED", WEBSITE_AREAS, saved.getId(), CITY_ID_AUDIT_PREFIX + cityId + ",slug=" + areaSlug);
        evictCityCaches();
        return areaResponse(saved);
    }

    @Transactional
    public WebsiteAreaResponse updateArea(Long areaId, WebsiteAreaUpsertRequest request) {
        WebsiteAreaEntity area = areaRepository.findByIdAndDeletedFalse(areaId)
                .orElseThrow(() -> new ResourceNotFoundException(AREA_NOT_FOUND));
        String areaName = requireName(request.getName());
        String areaSlug = request.getSlug() == null || request.getSlug().isBlank()
                ? slug(areaName)
                : requireSlug(request.getSlug());
        Long cityId = area.getCity().getId();
        if (areaRepository.existsByCity_IdAndSlugIgnoreCaseAndDeletedFalseAndIdNot(cityId, areaSlug, areaId)) {
            throw new BadRequestException("Area slug already exists for this city");
        }
        area.setAreaName(areaName);
        area.setSlug(areaSlug);
        if (request.getDisplayOrder() != null) {
            area.setDisplayOrder(request.getDisplayOrder());
        }
        if (request.getActive() != null) {
            area.setActive(request.getActive());
        }
        area.setUpdatedBy(currentAdminId());
        WebsiteAreaEntity saved = areaRepository.save(area);
        audit("WEBSITE_AREA_UPDATED", WEBSITE_AREAS, saved.getId(), CITY_ID_AUDIT_PREFIX + cityId + ",slug=" + areaSlug);
        evictCityCaches();
        return areaResponse(saved);
    }

    @Transactional
    public WebsiteAreaResponse updateAreaStatus(Long areaId, WebsiteStatusRequest request) {
        WebsiteAreaEntity area = areaRepository.findByIdAndDeletedFalse(areaId)
                .orElseThrow(() -> new ResourceNotFoundException(AREA_NOT_FOUND));
        area.setActive(Boolean.TRUE.equals(request.getActive()));
        area.setUpdatedBy(currentAdminId());
        WebsiteAreaEntity saved = areaRepository.save(area);
        audit("WEBSITE_AREA_STATUS_CHANGED", WEBSITE_AREAS, saved.getId(), ACTIVE_AUDIT_PREFIX + saved.isActive());
        evictCityCaches();
        return areaResponse(saved);
    }

    @Transactional
    public AdminMessageResponse deleteArea(Long areaId) {
        WebsiteAreaEntity area = areaRepository.findByIdAndDeletedFalse(areaId)
                .orElseThrow(() -> new ResourceNotFoundException(AREA_NOT_FOUND));
        area.setDeleted(true);
        area.setActive(false);
        area.setUpdatedBy(currentAdminId());
        areaRepository.save(area);
        audit("WEBSITE_AREA_DELETED", WEBSITE_AREAS, areaId, CITY_ID_AUDIT_PREFIX + area.getCity().getId());
        evictCityCaches();
        return new AdminMessageResponse("Area deleted successfully");
    }

    @Transactional
    public AdminMessageResponse reorderAreas(Long cityId, WebsiteReorderRequest request) {
        activeOrInactiveCity(cityId);
        for (WebsiteReorderRequest.Item item : request.getItems()) {
            WebsiteAreaEntity area = areaRepository.findByIdAndDeletedFalse(item.getId())
                    .filter(existing -> existing.getCity().getId().equals(cityId))
                    .orElseThrow(() -> new ResourceNotFoundException("Area not found: " + item.getId()));
            area.setDisplayOrder(item.getDisplayOrder());
            area.setUpdatedBy(currentAdminId());
            areaRepository.save(area);
        }
        audit("WEBSITE_AREA_ORDER_CHANGED", WEBSITE_AREAS, null,
                CITY_ID_AUDIT_PREFIX + cityId + ",count=" + request.getItems().size());
        evictCityCaches();
        return new AdminMessageResponse("Area order updated successfully");
    }

    private WebsiteCategoryConfig newCategoryConfig(AdminCategoryEntity category) {
        WebsiteCategoryConfig config = new WebsiteCategoryConfig();
        config.setCategory(category);
        config.setCreatedBy(currentAdminId());
        config.setUpdatedBy(currentAdminId());
        return config;
    }

    private WebsiteCityEntity activeOrInactiveCity(Long cityId) {
        return cityRepository.findByIdAndDeletedFalse(cityId)
                .orElseThrow(() -> new ResourceNotFoundException("City not found"));
    }

    private WebsiteHeaderItemResponse categoryItem(AdminCategoryEntity category) {
        return new WebsiteHeaderItemResponse(category.getId(), category.getName(), slug(category.getName()));
    }

    private WebsiteHeaderItemResponse cityItem(WebsiteCityEntity city) {
        return new WebsiteHeaderItemResponse(city.getId(), city.getName(), city.getSlug());
    }

    private AdminWebsiteCityResponse cityResponse(WebsiteCityEntity city) {
        return new AdminWebsiteCityResponse(city.getId(), city.getName(), city.getSlug(),
                city.isActive(), city.getDisplayOrder());
    }

    private WebsiteHeaderItemResponse areaItem(WebsiteAreaEntity area) {
        return new WebsiteHeaderItemResponse(area.getId(), area.getAreaName(), area.getSlug());
    }

    private WebsiteAreaResponse areaResponse(WebsiteAreaEntity area) {
        return new WebsiteAreaResponse(area.getId(), area.getAreaName(), area.getSlug(),
                area.isActive(), area.getDisplayOrder());
    }

    private String requireName(String value) {
        if (value == null || value.trim().isEmpty()) {
            throw new BadRequestException("Area name is required");
        }
        return value.trim();
    }

    private String requireCityName(String value) {
        if (value == null || value.trim().isEmpty()) {
            throw new BadRequestException("City name is required");
        }
        return value.trim();
    }

    private String requireSlug(String value) {
        String normalized = slug(value);
        if (normalized.isBlank()) {
            throw new BadRequestException("Slug is required");
        }
        return normalized;
    }

    public static String slug(String value) {
        if (value == null) {
            return "";
        }
        String normalized = Normalizer.normalize(value, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT);
        normalized = NON_SLUG_CHAR.matcher(normalized).replaceAll("-");
        return trimBoundaryCharacter(normalized, '-');
    }

    private static String trimBoundaryCharacter(String value, char boundaryCharacter) {
        int start = 0;
        int end = value.length();
        while (start < end && value.charAt(start) == boundaryCharacter) {
            start++;
        }
        while (end > start && value.charAt(end - 1) == boundaryCharacter) {
            end--;
        }
        return value.substring(start, end);
    }

    private Long currentAdminId() {
        return securityUtils.getCurrentUserId().orElse(0L);
    }

    private void audit(String action, String entity, Long entityId, String metadata) {
        auditService.log(currentAdminId(), action, entity, entityId, metadata);
    }

    private void evictCategoryCaches() {
        delete(CATEGORY_CACHE_KEY);
        deletePattern(CATEGORY_SERVICE_CACHE_PREFIX + "*");
    }

    private void evictCityCaches() {
        delete(CITY_CACHE_KEY);
        deletePattern(CITY_AREA_CACHE_PREFIX + "*");
    }

    private void delete(String key) {
        StringRedisTemplate redis = redisProvider.getIfAvailable();
        if (redis != null) {
            try {
                redis.delete(key);
            } catch (RuntimeException ignored) {
                // Redis is an optimization for website navigation, not a write dependency.
            }
        }
    }

    private void deletePattern(String pattern) {
        StringRedisTemplate redis = redisProvider.getIfAvailable();
        if (redis != null) {
            try {
                var keys = redis.keys(pattern);
                if (keys != null && !keys.isEmpty()) {
                    redis.delete(keys);
                }
            } catch (RuntimeException ignored) {
                // Redis is an optimization for website navigation, not a write dependency.
            }
        }
    }

    private <T> List<T> cached(String key, TypeReference<List<T>> type, java.util.function.Supplier<List<T>> loader) {
        StringRedisTemplate redis = redisProvider.getIfAvailable();
        if (redis != null) {
            try {
                String cached = redis.opsForValue().get(key);
                if (cached != null && !cached.isBlank()) {
                    return objectMapper.readValue(cached, type);
                }
            } catch (Exception ignored) {
                // Ignore corrupt or unavailable cache entries and rebuild from database.
            }
        }
        List<T> data = loader.get();
        if (redis != null) {
            try {
                redis.opsForValue().set(key, objectMapper.writeValueAsString(data), CACHE_TTL);
            } catch (Exception ignored) {
                // Ignore cache write failures.
            }
        }
        return data;
    }
}

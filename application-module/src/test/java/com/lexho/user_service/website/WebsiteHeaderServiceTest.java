package com.lexho.user_service.website;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lexho.user_service.admin.entity.AdminCategoryEntity;
import com.lexho.user_service.admin.entity.AdminServiceEntity;
import com.lexho.user_service.admin.repository.AdminCategoryRepository;
import com.lexho.user_service.admin.repository.AdminServiceEntityRepository;
import com.lexho.user_service.admin.service.AdminAuditService;
import com.lexho.user_service.exception.BadRequestException;
import com.lexho.user_service.exception.ResourceNotFoundException;
import com.lexho.user_service.security.SecurityUtils;
import com.lexho.user_service.website.dto.WebsiteAreaUpsertRequest;
import com.lexho.user_service.website.dto.WebsiteCityUpsertRequest;
import com.lexho.user_service.website.dto.WebsiteReorderRequest;
import com.lexho.user_service.website.dto.WebsiteStatusRequest;
import com.lexho.user_service.website.dto.WebsiteVisibilityRequest;
import com.lexho.user_service.website.entity.WebsiteAreaEntity;
import com.lexho.user_service.website.entity.WebsiteCategoryConfig;
import com.lexho.user_service.website.entity.WebsiteCityEntity;
import com.lexho.user_service.website.repository.WebsiteAreaRepository;
import com.lexho.user_service.website.repository.WebsiteCategoryConfigRepository;
import com.lexho.user_service.website.repository.WebsiteCityRepository;
import com.lexho.user_service.website.service.WebsiteHeaderService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WebsiteHeaderServiceTest {
    @Mock
    private AdminCategoryRepository categoryRepository;
    @Mock
    private AdminServiceEntityRepository serviceRepository;
    @Mock
    private WebsiteCategoryConfigRepository categoryConfigRepository;
    @Mock
    private WebsiteCityRepository cityRepository;
    @Mock
    private WebsiteAreaRepository areaRepository;
    @Mock
    private AdminAuditService auditService;
    @Mock
    private SecurityUtils securityUtils;
    @Mock
    private ObjectProvider<StringRedisTemplate> redisProvider;

    private WebsiteHeaderService service;

    @BeforeEach
    void setup() {
        lenient().when(securityUtils.getCurrentUserId()).thenReturn(Optional.of(44L));
        lenient().when(redisProvider.getIfAvailable()).thenReturn(null);
        service = new WebsiteHeaderService(
                categoryRepository,
                serviceRepository,
                categoryConfigRepository,
                cityRepository,
                areaRepository,
                auditService,
                securityUtils,
                redisProvider,
                new ObjectMapper()
        );
    }

    @Test
    void publicCategoriesReturnOnlyWebsiteConfiguredExistingCategories() {
        WebsiteCategoryConfig config = categoryConfig(category(1L, "Cook At Home", true), true, 2);
        when(categoryConfigRepository
                .findByShowOnWebsiteTrueAndCategory_ActiveTrueAndCategory_DeletedFalseOrderByDisplayOrderAscCategory_NameAsc())
                .thenReturn(List.of(config));

        var response = service.publicCategories();

        assertEquals(1, response.size());
        assertEquals(1L, response.get(0).id());
        assertEquals("Cook At Home", response.get(0).name());
        assertEquals("cook-at-home", response.get(0).slug());
    }

    @Test
    void publicServicesUseExistingCategoryServiceRelationshipAndOnlyReturnListFields() {
        AdminCategoryEntity category = category(7L, "Cleaning", true);
        AdminServiceEntity first = serviceEntity(20L, "Home Deep Cleaning", category, true);
        AdminServiceEntity second = serviceEntity(21L, "Bathroom Cleaning", category, true);
        when(categoryConfigRepository
                .findByShowOnWebsiteTrueAndCategory_ActiveTrueAndCategory_DeletedFalseOrderByDisplayOrderAscCategory_NameAsc())
                .thenReturn(List.of(categoryConfig(category, true, 1)));
        when(serviceRepository.findByCategory_IdAndDeletedFalseAndActiveTrueOrderByNameAsc(7L))
                .thenReturn(List.of(second, first));

        var response = service.publicServicesByCategorySlug("cleaning");

        assertEquals(List.of("Bathroom Cleaning", "Home Deep Cleaning"),
                response.stream().map(item -> item.name()).toList());
        assertEquals("bathroom-cleaning", response.get(0).slug());
        verify(serviceRepository).findByCategory_IdAndDeletedFalseAndActiveTrueOrderByNameAsc(7L);
    }

    @Test
    void publicServicesRejectHiddenOrUnknownCategorySlug() {
        when(categoryConfigRepository
                .findByShowOnWebsiteTrueAndCategory_ActiveTrueAndCategory_DeletedFalseOrderByDisplayOrderAscCategory_NameAsc())
                .thenReturn(List.of());

        assertThrows(ResourceNotFoundException.class,
                () -> service.publicServicesByCategorySlug("cleaning"));
        verifyNoInteractions(serviceRepository);
    }

    @Test
    void adminCategoryVisibilityCreatesConfigWithoutDuplicatingCategory() {
        AdminCategoryEntity category = category(3L, "Party", true);
        when(categoryRepository.findByIdAndDeletedFalse(3L)).thenReturn(Optional.of(category));
        when(categoryConfigRepository.findByCategory_Id(3L)).thenReturn(Optional.empty());
        when(categoryConfigRepository.save(any(WebsiteCategoryConfig.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        WebsiteVisibilityRequest request = new WebsiteVisibilityRequest();
        request.setShowOnWebsite(true);
        request.setDisplayOrder(5);

        var response = service.updateCategoryVisibility(3L, request);

        assertTrue(response.showOnWebsite());
        assertEquals(5, response.displayOrder());
        verify(categoryConfigRepository).save(argThat(config ->
                config.getCategory() == category && config.isShowOnWebsite() && config.getDisplayOrder() == 5));
        verify(auditService).log(44L, "WEBSITE_CATEGORY_VISIBILITY_CHANGED",
                "website_category_config", 3L, "showOnWebsite=true,displayOrder=5");
    }

    @Test
    void publicCitiesAndAreasReturnOnlyHeaderFields() {
        WebsiteCityEntity city = city(9L, "Mumbai", "mumbai", true);
        WebsiteAreaEntity andheri = area(30L, city, "Andheri", "andheri", true, 1);
        when(cityRepository.findByActiveTrueAndDeletedFalseOrderByDisplayOrderAscNameAsc())
                .thenReturn(List.of(city));
        when(cityRepository.findBySlugIgnoreCaseAndActiveTrueAndDeletedFalse("mumbai"))
                .thenReturn(Optional.of(city));
        when(areaRepository.findByCity_IdAndActiveTrueAndDeletedFalseOrderByDisplayOrderAscAreaNameAsc(9L))
                .thenReturn(List.of(andheri));

        assertEquals("mumbai", service.publicCities().get(0).slug());
        var areas = service.publicAreasByCitySlug("mumbai");
        assertEquals(1, areas.size());
        assertEquals("Andheri", areas.get(0).name());
        assertEquals("andheri", areas.get(0).slug());
    }

    @Test
    void createAreaValidatesCityAndUniqueSlugWithinCity() {
        WebsiteCityEntity city = city(4L, "Delhi", "delhi", true);
        when(cityRepository.findByIdAndDeletedFalse(4L)).thenReturn(Optional.of(city));
        when(areaRepository.existsByCity_IdAndSlugIgnoreCaseAndDeletedFalse(4L, "south-delhi"))
                .thenReturn(true);
        WebsiteAreaUpsertRequest request = new WebsiteAreaUpsertRequest();
        request.setName("South Delhi");

        assertThrows(BadRequestException.class, () -> service.createArea(4L, request));

        when(areaRepository.existsByCity_IdAndSlugIgnoreCaseAndDeletedFalse(4L, "south-delhi"))
                .thenReturn(false);
        when(areaRepository.save(any(WebsiteAreaEntity.class))).thenAnswer(invocation -> {
            WebsiteAreaEntity area = invocation.getArgument(0);
            ReflectionTestUtils.setField(area, "id", 88L);
            return area;
        });

        var response = service.createArea(4L, request);

        assertEquals(88L, response.id());
        assertEquals("south-delhi", response.slug());
        assertTrue(response.active());
        verify(auditService).log(44L, "WEBSITE_AREA_CREATED", "website_areas",
                88L, "cityId=4,slug=south-delhi");
    }

    @Test
    void areaStatusDeleteAndReorderStayWithinSelectedCity() {
        WebsiteCityEntity city = city(2L, "Noida", "noida", true);
        WebsiteAreaEntity area = area(12L, city, "Sector 18", "sector-18", true, 2);
        when(areaRepository.findByIdAndDeletedFalse(12L)).thenReturn(Optional.of(area));
        when(areaRepository.save(any(WebsiteAreaEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        WebsiteStatusRequest statusRequest = new WebsiteStatusRequest();
        statusRequest.setActive(false);
        assertFalse(service.updateAreaStatus(12L, statusRequest).active());
        assertEquals("Area deleted successfully", service.deleteArea(12L).getMessage());

        when(cityRepository.findByIdAndDeletedFalse(2L)).thenReturn(Optional.of(city));
        WebsiteReorderRequest reorder = new WebsiteReorderRequest();
        WebsiteReorderRequest.Item item = new WebsiteReorderRequest.Item();
        item.setId(12L);
        item.setDisplayOrder(1);
        reorder.setItems(List.of(item));
        assertEquals("Area order updated successfully", service.reorderAreas(2L, reorder).getMessage());
        assertEquals(1, area.getDisplayOrder());
    }

    @Test
    void createCityUsesWebsiteCityMasterAndRejectsDuplicateSlug() {
        WebsiteCityUpsertRequest request = new WebsiteCityUpsertRequest();
        request.setName("Greater Noida");
        request.setDisplayOrder(3);
        when(cityRepository.existsBySlugIgnoreCaseAndDeletedFalse("greater-noida")).thenReturn(true);

        assertThrows(BadRequestException.class, () -> service.createCity(request));

        when(cityRepository.existsBySlugIgnoreCaseAndDeletedFalse("greater-noida")).thenReturn(false);
        when(cityRepository.save(any(WebsiteCityEntity.class))).thenAnswer(invocation -> {
            WebsiteCityEntity city = invocation.getArgument(0);
            ReflectionTestUtils.setField(city, "id", 55L);
            return city;
        });

        var response = service.createCity(request);

        assertEquals(55L, response.id());
        assertEquals("Greater Noida", response.name());
        assertEquals("greater-noida", response.slug());
        assertTrue(response.active());
        assertEquals(3, response.displayOrder());
        verify(auditService).log(44L, "WEBSITE_CITY_CREATED", "website_cities", 55L, "slug=greater-noida");
    }

    @Test
    void adminCategoriesUseConfigWhenPresentAndCategoryDefaultsOtherwise() {
        AdminCategoryEntity configured = category(1L, "Cooking", true);
        configured.setDisplayOrder(9);
        AdminCategoryEntity fallback = category(2L, "Cleaning", false);
        fallback.setDisplayOrder(4);
        when(categoryRepository.findByDeletedFalseOrderByDisplayOrderAscNameAsc())
                .thenReturn(List.of(configured, fallback));
        when(categoryConfigRepository.findByCategory_Id(1L))
                .thenReturn(Optional.of(categoryConfig(configured, true, 3)));
        when(categoryConfigRepository.findByCategory_Id(2L)).thenReturn(Optional.empty());

        var response = service.adminCategories();

        assertEquals(2, response.size());
        assertTrue(response.get(0).showOnWebsite());
        assertEquals(3, response.get(0).displayOrder());
        assertFalse(response.get(1).showOnWebsite());
        assertEquals(4, response.get(1).displayOrder());
    }

    @Test
    void reorderCategoriesCreatesMissingConfigsAndAuditsCount() {
        AdminCategoryEntity category = category(7L, "Cleaning", true);
        when(categoryRepository.findByIdAndDeletedFalse(7L)).thenReturn(Optional.of(category));
        when(categoryConfigRepository.findByCategory_Id(7L)).thenReturn(Optional.empty());
        when(categoryConfigRepository.save(any(WebsiteCategoryConfig.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        WebsiteReorderRequest reorder = new WebsiteReorderRequest();
        WebsiteReorderRequest.Item item = new WebsiteReorderRequest.Item();
        item.setId(7L);
        item.setDisplayOrder(1);
        reorder.setItems(List.of(item));

        var response = service.reorderCategories(reorder);

        assertEquals("Category order updated successfully", response.getMessage());
        verify(categoryConfigRepository).save(argThat(config ->
                config.getCategory() == category && config.getDisplayOrder() == 1
                        && Long.valueOf(44L).equals(config.getCreatedBy())
                        && Long.valueOf(44L).equals(config.getUpdatedBy())));
        verify(auditService).log(44L, "WEBSITE_CATEGORY_ORDER_CHANGED", "website_category_config", null, "count=1");
    }

    @Test
    void updateCategoryVisibilityRejectsMissingCategoryAndDefaultsFalseVisibility() {
        when(categoryRepository.findByIdAndDeletedFalse(404L)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class,
                () -> service.updateCategoryVisibility(404L, new WebsiteVisibilityRequest()));

        AdminCategoryEntity category = category(8L, "Repairs", true);
        WebsiteCategoryConfig config = categoryConfig(category, true, 5);
        when(categoryRepository.findByIdAndDeletedFalse(8L)).thenReturn(Optional.of(category));
        when(categoryConfigRepository.findByCategory_Id(8L)).thenReturn(Optional.of(config));
        when(categoryConfigRepository.save(config)).thenReturn(config);

        var response = service.updateCategoryVisibility(8L, new WebsiteVisibilityRequest());

        assertFalse(response.showOnWebsite());
        assertEquals(5, response.displayOrder());
    }

    @Test
    void cityVisibilityUpdateStatusDeleteReorderAndAdminListUseCityMaster() {
        WebsiteCityEntity city = city(20L, "Pune", "pune", true);
        city.setDisplayOrder(7);
        when(cityRepository.findByDeletedFalseOrderByDisplayOrderAscNameAsc()).thenReturn(List.of(city));
        when(cityRepository.findByIdAndDeletedFalse(20L)).thenReturn(Optional.of(city));
        when(cityRepository.save(city)).thenReturn(city);

        assertEquals("pune", service.adminCities().get(0).slug());

        WebsiteVisibilityRequest visibility = new WebsiteVisibilityRequest();
        visibility.setShowOnWebsite(false);
        visibility.setDisplayOrder(2);
        var hidden = service.updateCityVisibility(20L, visibility);
        assertFalse(hidden.active());
        assertEquals(2, hidden.displayOrder());

        WebsiteStatusRequest status = new WebsiteStatusRequest();
        status.setActive(true);
        assertTrue(service.updateCityStatus(20L, status).active());

        WebsiteReorderRequest reorder = new WebsiteReorderRequest();
        WebsiteReorderRequest.Item item = new WebsiteReorderRequest.Item();
        item.setId(20L);
        item.setDisplayOrder(1);
        reorder.setItems(List.of(item));
        assertEquals("City order updated successfully", service.reorderCities(reorder).getMessage());

        assertEquals("City deleted successfully", service.deleteCity(20L).getMessage());
        assertTrue(city.isDeleted());
        assertFalse(city.isActive());
    }

    @Test
    void updateCityValidatesNameSlugUniquenessAndOptionalFields() {
        WebsiteCityEntity city = city(21L, "Old Name", "old-name", true);
        when(cityRepository.findByIdAndDeletedFalse(21L)).thenReturn(Optional.of(city));
        WebsiteCityUpsertRequest blankName = new WebsiteCityUpsertRequest();
        blankName.setName(" ");
        assertThrows(BadRequestException.class, () -> service.updateCity(21L, blankName));

        WebsiteCityUpsertRequest duplicate = new WebsiteCityUpsertRequest();
        duplicate.setName("New Name");
        duplicate.setSlug("new-name");
        when(cityRepository.existsBySlugIgnoreCaseAndDeletedFalseAndIdNot("new-name", 21L)).thenReturn(true);
        assertThrows(BadRequestException.class, () -> service.updateCity(21L, duplicate));

        duplicate.setDisplayOrder(6);
        duplicate.setActive(false);
        when(cityRepository.existsBySlugIgnoreCaseAndDeletedFalseAndIdNot("new-name", 21L)).thenReturn(false);
        when(cityRepository.save(city)).thenReturn(city);
        var response = service.updateCity(21L, duplicate);

        assertEquals("New Name", response.name());
        assertEquals("new-name", response.slug());
        assertFalse(response.active());
        assertEquals(6, response.displayOrder());
    }

    @Test
    void adminAreasAndUpdateAreaValidateCityOwnershipAndSlugUniqueness() {
        WebsiteCityEntity city = city(30L, "Mumbai", "mumbai", true);
        WebsiteCityEntity otherCity = city(31L, "Pune", "pune", true);
        WebsiteAreaEntity area = area(40L, city, "Andheri", "andheri", true, 4);
        WebsiteAreaEntity otherArea = area(41L, otherCity, "Kothrud", "kothrud", true, 5);
        when(cityRepository.findByIdAndDeletedFalse(30L)).thenReturn(Optional.of(city));
        when(areaRepository.findByCity_IdAndDeletedFalseOrderByDisplayOrderAscAreaNameAsc(30L))
                .thenReturn(List.of(area));

        assertEquals("andheri", service.adminAreas(30L).get(0).slug());

        when(areaRepository.findByIdAndDeletedFalse(40L)).thenReturn(Optional.of(area));
        WebsiteAreaUpsertRequest duplicate = new WebsiteAreaUpsertRequest();
        duplicate.setName("West");
        duplicate.setSlug("west");
        when(areaRepository.existsByCity_IdAndSlugIgnoreCaseAndDeletedFalseAndIdNot(30L, "west", 40L))
                .thenReturn(true);
        assertThrows(BadRequestException.class, () -> service.updateArea(40L, duplicate));

        duplicate.setDisplayOrder(1);
        duplicate.setActive(false);
        when(areaRepository.existsByCity_IdAndSlugIgnoreCaseAndDeletedFalseAndIdNot(30L, "west", 40L))
                .thenReturn(false);
        when(areaRepository.save(area)).thenReturn(area);
        var response = service.updateArea(40L, duplicate);
        assertEquals("West", response.name());
        assertEquals("west", response.slug());
        assertFalse(response.active());

        when(areaRepository.findByIdAndDeletedFalse(41L)).thenReturn(Optional.of(otherArea));
        WebsiteReorderRequest reorder = new WebsiteReorderRequest();
        WebsiteReorderRequest.Item item = new WebsiteReorderRequest.Item();
        item.setId(41L);
        item.setDisplayOrder(1);
        reorder.setItems(List.of(item));
        assertThrows(ResourceNotFoundException.class, () -> service.reorderAreas(30L, reorder));
    }

    @Test
    void publicLookupsRejectBlankSlugsAndCreateCityRejectsBlankNames() {
        assertThrows(BadRequestException.class, () -> service.publicServicesByCategorySlug("!!!"));
        assertThrows(BadRequestException.class, () -> service.publicAreasByCitySlug(" "));
        WebsiteCityUpsertRequest request = new WebsiteCityUpsertRequest();
        request.setName("");
        assertThrows(BadRequestException.class, () -> service.createCity(request));
    }

    @Test
    void slugTrimsSeparatorHeavyInputWithoutRegexBoundaryBacktracking() {
        assertEquals("cleaning-services", WebsiteHeaderService.slug("---Cleaning Services!!!"));
        assertEquals("", WebsiteHeaderService.slug("------"));
    }

    private static AdminCategoryEntity category(Long id, String name, boolean active) {
        AdminCategoryEntity category = new AdminCategoryEntity();
        ReflectionTestUtils.setField(category, "id", id);
        category.setName(name);
        category.setActive(active);
        category.setDeleted(false);
        return category;
    }

    private static AdminServiceEntity serviceEntity(Long id, String name, AdminCategoryEntity category, boolean active) {
        AdminServiceEntity service = new AdminServiceEntity();
        ReflectionTestUtils.setField(service, "id", id);
        service.setName(name);
        service.setCategory(category);
        service.setActive(active);
        service.setDeleted(false);
        return service;
    }

    private static WebsiteCityEntity city(Long id, String name, String slug, boolean active) {
        WebsiteCityEntity city = new WebsiteCityEntity();
        ReflectionTestUtils.setField(city, "id", id);
        city.setName(name);
        city.setSlug(slug);
        city.setActive(active);
        city.setDeleted(false);
        return city;
    }

    private static WebsiteCategoryConfig categoryConfig(AdminCategoryEntity category, boolean show, int order) {
        WebsiteCategoryConfig config = new WebsiteCategoryConfig();
        config.setCategory(category);
        config.setShowOnWebsite(show);
        config.setDisplayOrder(order);
        return config;
    }

    private static WebsiteAreaEntity area(Long id, WebsiteCityEntity city, String name, String slug, boolean active, int order) {
        WebsiteAreaEntity area = new WebsiteAreaEntity();
        ReflectionTestUtils.setField(area, "id", id);
        area.setCity(city);
        area.setAreaName(name);
        area.setSlug(slug);
        area.setActive(active);
        area.setDisplayOrder(order);
        return area;
    }
}

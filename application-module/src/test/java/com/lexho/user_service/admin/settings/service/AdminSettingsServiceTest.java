package com.lexho.user_service.admin.settings.service;

import com.lexho.user_service.admin.settings.entity.AppSettingEntity;
import com.lexho.user_service.admin.settings.repository.AppSettingRepository;
import com.lexho.user_service.exception.BadRequestException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminSettingsServiceTest {
    @Mock AppSettingRepository repository;
    @InjectMocks AdminSettingsService service;

    @Test
    void listAndGetMapStoredSettings() {
        AppSettingEntity entity = setting("key", "value");
        when(repository.findAll()).thenReturn(List.of(entity));
        assertEquals("value", service.listAll().get(0).get("value"));
        when(repository.findById("key")).thenReturn(Optional.of(entity));
        assertEquals("key", service.get("key").get("key"));
        when(repository.findById("missing")).thenReturn(Optional.empty());
        assertThrows(BadRequestException.class, () -> service.get("missing"));
    }

    @Test
    void upsertValidatesAndTrimsValues() {
        assertThrows(BadRequestException.class, () -> service.upsert(" ", "v", null, "admin"));
        assertThrows(BadRequestException.class, () -> service.upsert("k", " ", null, "admin"));
        when(repository.findById("key")).thenReturn(Optional.empty());
        when(repository.save(any())).thenAnswer(i -> i.getArgument(0));
        var result = service.upsert(" key ", " value ", " desc ", "admin");
        assertEquals("value", result.get("value"));
        assertEquals("desc", result.get("description"));
    }

    @Test
    void radiusUpdatesEnforceBounds() {
        assertThrows(BadRequestException.class, () -> service.updateDispatchRadius(0d, "a"));
        assertThrows(BadRequestException.class, () -> service.updatePartnerServiceRadius(101d, "a"));
        when(repository.findById(anyString())).thenReturn(Optional.empty());
        when(repository.save(any())).thenAnswer(i -> i.getArgument(0));
        assertEquals("15.0", service.updateDispatchRadius(15d, "a").get("value"));
        assertEquals("20.0", service.updatePartnerServiceRadius(20d, "a").get("value"));
    }

    @Test
    void dashboardSettingsValidateEveryBoundaryAndPersistSevenKeys() {
        assertInvalid(null, 1, 30, "mapbox", "Delhi", 1d, 1d);
        assertInvalid(10d, 21, 30, "mapbox", "Delhi", 1d, 1d);
        assertInvalid(10d, 1, 0, "mapbox", "Delhi", 1d, 1d);
        assertInvalid(10d, 1, 30, " ", "Delhi", 1d, 1d);
        assertInvalid(10d, 1, 30, "mapbox", " ", 1d, 1d);
        assertInvalid(10d, 1, 30, "mapbox", "Delhi", 91d, 1d);
        assertInvalid(10d, 1, 30, "mapbox", "Delhi", 1d, 181d);

        when(repository.findById(anyString())).thenReturn(Optional.empty());
        when(repository.save(any())).thenAnswer(i -> i.getArgument(0));
        var result = service.updateDashboardSettings(12d, 3, 45, "mapbox", "Delhi", 28.6, 77.2, "admin");
        assertEquals("12.0", result.get("serviceRadiusKm"));
        assertEquals("Delhi", result.get("mapDefaultCity"));
        verify(repository, times(7)).save(any());
    }

    private void assertInvalid(Double radius, Integer retry, Integer timeout, String provider,
                               String city, Double lat, Double lng) {
        assertThrows(BadRequestException.class,
                () -> service.updateDashboardSettings(radius, retry, timeout, provider, city, lat, lng, "admin"));
    }

    private AppSettingEntity setting(String key, String value) {
        AppSettingEntity entity = new AppSettingEntity();
        entity.setSettingKey(key); entity.setSettingValue(value); entity.setDescription("desc");
        return entity;
    }
}

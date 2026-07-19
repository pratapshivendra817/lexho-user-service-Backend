package com.lexho.user_service.admin.geo.service;

import com.lexho.user_service.admin.geo.dto.CityUpsertRequest;
import com.lexho.user_service.admin.geo.dto.ZoneUpsertRequest;
import com.lexho.user_service.admin.geo.entity.CityEntity;
import com.lexho.user_service.admin.geo.entity.ZoneEntity;
import com.lexho.user_service.admin.geo.repository.CityRepository;
import com.lexho.user_service.admin.geo.repository.ZoneRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.times;

@ExtendWith(MockitoExtension.class)
class AdminGeoServiceTest {

    @Mock private CityRepository cityRepository;
    @Mock private ZoneRepository zoneRepository;

    private AdminGeoService service;

    @BeforeEach
    void setUp() {
        service = new AdminGeoService(cityRepository, zoneRepository);
    }

    @Test
    void cityCrudCleansValuesMapsResponsesAndSoftDeletes() {
        CityEntity city = city(1L, "Delhi");
        when(cityRepository.save(any())).thenAnswer(invocation -> {
            CityEntity saved = invocation.getArgument(0);
            saved.setId(1L);
            return saved;
        });
        when(cityRepository.findByDeletedFalseOrderByNameAsc()).thenReturn(List.of(city));
        when(cityRepository.findById(1L)).thenReturn(Optional.of(city));
        CityUpsertRequest request = new CityUpsertRequest();
        request.name = "  New Delhi ";
        request.state = " ";
        request.country = null;
        request.active = false;

        assertThat(service.createCity(request).name).isEqualTo("New Delhi");
        assertThat(service.updateCity(1L, request).active).isFalse();
        assertThat(service.listCities()).singleElement().satisfies(response -> {
            assertThat(response.id).isEqualTo(1L);
            assertThat(response.name).isEqualTo("New Delhi");
        });
        service.deleteCity(1L);

        assertThat(city.isDeleted()).isTrue();
        verify(cityRepository, times(2)).save(city);
    }

    @Test
    void cityUpdateAndDeleteRejectMissingOrDeletedRecords() {
        CityEntity deleted = city(2L, "Old");
        deleted.setDeleted(true);
        when(cityRepository.findById(1L)).thenReturn(Optional.empty());
        when(cityRepository.findById(2L)).thenReturn(Optional.of(deleted));

        assertThatThrownBy(() -> service.updateCity(1L, new CityUpsertRequest()))
                .isInstanceOf(IllegalArgumentException.class).hasMessage("City not found");
        assertThatThrownBy(() -> service.deleteCity(2L))
                .isInstanceOf(IllegalArgumentException.class).hasMessage("City not found");
    }

    @Test
    void zoneCrudSupportsAllAndCityFilteredLists() {
        CityEntity city = city(1L, "Delhi");
        ZoneEntity zone = zone(4L, city, "North");
        when(cityRepository.findById(1L)).thenReturn(Optional.of(city));
        when(zoneRepository.save(any())).thenAnswer(invocation -> {
            ZoneEntity saved = invocation.getArgument(0);
            saved.setId(4L);
            return saved;
        });
        when(zoneRepository.findById(4L)).thenReturn(Optional.of(zone));
        when(zoneRepository.findByDeletedFalseOrderByNameAsc()).thenReturn(List.of(zone));
        when(zoneRepository.findByCityIdAndDeletedFalseOrderByNameAsc(1L)).thenReturn(List.of(zone));
        ZoneUpsertRequest request = new ZoneUpsertRequest();
        request.cityId = 1L;
        request.name = " Central ";
        request.polygonJson = " ";
        request.active = false;

        assertThat(service.createZone(request).name).isEqualTo("Central");
        assertThat(service.updateZone(4L, request).polygonJson).isNull();
        assertThat(service.listZones(null)).hasSize(1);
        assertThat(service.listZones(1L)).singleElement().satisfies(response -> {
            assertThat(response.cityId).isEqualTo(1L);
            assertThat(response.cityName).isEqualTo("Delhi");
        });
        service.deleteZone(4L);

        assertThat(zone.isDeleted()).isTrue();
    }

    @Test
    void zoneOperationsRejectMissingCityAndZone() {
        ZoneUpsertRequest request = new ZoneUpsertRequest();
        request.cityId = 99L;
        when(cityRepository.findById(99L)).thenReturn(Optional.empty());
        when(zoneRepository.findById(8L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.createZone(request))
                .isInstanceOf(IllegalArgumentException.class).hasMessage("City not found");
        assertThatThrownBy(() -> service.updateZone(8L, request))
                .isInstanceOf(IllegalArgumentException.class).hasMessage("Zone not found");
        assertThatThrownBy(() -> service.deleteZone(8L))
                .isInstanceOf(IllegalArgumentException.class).hasMessage("Zone not found");
    }

    private CityEntity city(Long id, String name) {
        CityEntity city = new CityEntity();
        city.setId(id);
        city.setName(name);
        city.setActive(true);
        return city;
    }

    private ZoneEntity zone(Long id, CityEntity city, String name) {
        ZoneEntity zone = new ZoneEntity();
        zone.setId(id);
        zone.setCity(city);
        zone.setName(name);
        zone.setActive(true);
        return zone;
    }
}

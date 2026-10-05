package com.lexho.user_service.admin.geo.service;

import com.lexho.user_service.admin.geo.dto.*;
import com.lexho.user_service.admin.geo.entity.CityEntity;
import com.lexho.user_service.admin.geo.entity.ZoneEntity;
import com.lexho.user_service.admin.geo.repository.CityRepository;
import com.lexho.user_service.admin.geo.repository.ZoneRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class AdminGeoService {
    private static final String CITY_NOT_FOUND = "City not found";

    private final CityRepository cityRepository;
    private final ZoneRepository zoneRepository;

    public AdminGeoService(CityRepository cityRepository, ZoneRepository zoneRepository) {
        this.cityRepository = cityRepository;
        this.zoneRepository = zoneRepository;
    }

    @Transactional(readOnly = true)
    public List<CityResponse> listCities() {
        return cityRepository.findByDeletedFalseOrderByNameAsc().stream().map(this::toCityResponse).toList();
    }

    @Transactional
    public CityResponse createCity(CityUpsertRequest request) {
        CityEntity city = new CityEntity();
        applyCity(city, request);
        return toCityResponse(cityRepository.save(city));
    }

    @Transactional
    public CityResponse updateCity(Long id, CityUpsertRequest request) {
        CityEntity city = cityRepository.findById(id)
                .filter(existing -> !existing.isDeleted())
                .orElseThrow(() -> new IllegalArgumentException(CITY_NOT_FOUND));
        applyCity(city, request);
        return toCityResponse(cityRepository.save(city));
    }

    @Transactional
    public void deleteCity(Long id) {
        CityEntity city = cityRepository.findById(id)
                .filter(existing -> !existing.isDeleted())
                .orElseThrow(() -> new IllegalArgumentException(CITY_NOT_FOUND));
        city.setDeleted(true);
        cityRepository.save(city);
    }

    @Transactional(readOnly = true)
    public List<ZoneResponse> listZones(Long cityId) {
        List<ZoneEntity> zones = cityId == null
                ? zoneRepository.findByDeletedFalseOrderByNameAsc()
                : zoneRepository.findByCityIdAndDeletedFalseOrderByNameAsc(cityId);
        return zones.stream().map(this::toZoneResponse).toList();
    }

    @Transactional
    public ZoneResponse createZone(ZoneUpsertRequest request) {
        ZoneEntity zone = new ZoneEntity();
        applyZone(zone, request);
        return toZoneResponse(zoneRepository.save(zone));
    }

    @Transactional
    public ZoneResponse updateZone(Long id, ZoneUpsertRequest request) {
        ZoneEntity zone = zoneRepository.findById(id)
                .filter(existing -> !existing.isDeleted())
                .orElseThrow(() -> new IllegalArgumentException("Zone not found"));
        applyZone(zone, request);
        return toZoneResponse(zoneRepository.save(zone));
    }

    @Transactional
    public void deleteZone(Long id) {
        ZoneEntity zone = zoneRepository.findById(id)
                .filter(existing -> !existing.isDeleted())
                .orElseThrow(() -> new IllegalArgumentException("Zone not found"));
        zone.setDeleted(true);
        zoneRepository.save(zone);
    }

    private void applyCity(CityEntity city, CityUpsertRequest request) {
        city.setName(clean(request.name));
        city.setState(clean(request.state));
        city.setCountry(clean(request.country));
        if (request.active != null) {
            city.setActive(request.active);
        }
    }

    private void applyZone(ZoneEntity zone, ZoneUpsertRequest request) {
        CityEntity city = cityRepository.findById(request.cityId)
                .filter(existing -> !existing.isDeleted())
                .orElseThrow(() -> new IllegalArgumentException(CITY_NOT_FOUND));
        zone.setCity(city);
        zone.setName(clean(request.name));
        zone.setPolygonJson(clean(request.polygonJson));
        if (request.active != null) {
            zone.setActive(request.active);
        }
    }

    private CityResponse toCityResponse(CityEntity city) {
        CityResponse response = new CityResponse();
        response.id = city.getId();
        response.name = city.getName();
        response.state = city.getState();
        response.country = city.getCountry();
        response.active = city.isActive();
        return response;
    }

    private ZoneResponse toZoneResponse(ZoneEntity zone) {
        ZoneResponse response = new ZoneResponse();
        response.id = zone.getId();
        response.cityId = zone.getCity().getId();
        response.cityName = zone.getCity().getName();
        response.name = zone.getName();
        response.polygonJson = zone.getPolygonJson();
        response.active = zone.isActive();
        return response;
    }

    private String clean(String value) {
        return AdminGeoText.clean(value);
    }
}

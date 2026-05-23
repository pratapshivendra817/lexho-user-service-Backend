package com.lexho.user_service.admin.geo.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lexho.user_service.admin.geo.repository.ZoneRepository;
import com.lexho.user_service.admin.serviceablearea.entity.ServiceableAreaEntity;
import com.lexho.user_service.admin.serviceablearea.repository.ServiceableAreaRepository;
import com.lexho.user_service.booking.exception.BookingValidationException;
import com.lexho.user_service.lexho.entity.LexhoAddress;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

class GeoFenceValidationServiceTest {

    private ServiceableAreaRepository serviceableAreaRepository;
    private ZoneRepository zoneRepository;
    private GeoFenceValidationService geoFenceValidationService;

    @BeforeEach
    void setup() {
        serviceableAreaRepository = Mockito.mock(ServiceableAreaRepository.class);
        zoneRepository = Mockito.mock(ZoneRepository.class);
        geoFenceValidationService = new GeoFenceValidationService(
                serviceableAreaRepository,
                zoneRepository,
                new ObjectMapper()
        );
    }

    @Test
    void shouldAllowBookingWhenPointInsidePolygon() {
        ServiceableAreaEntity area = new ServiceableAreaEntity();
        area.setPincode("110001");
        area.setCity("Delhi");
        area.setPolygonJson("""
                {"type":"Polygon","coordinates":[[[77.10,28.50],[77.30,28.50],[77.30,28.70],[77.10,28.70],[77.10,28.50]]]}
                """);

        when(serviceableAreaRepository.findByPincodeAndActiveTrueAndDeletedFalse("110001"))
                .thenReturn(List.of(area));
        when(serviceableAreaRepository.findByCityIgnoreCaseAndActiveTrueAndDeletedFalse(anyString()))
                .thenReturn(List.of());

        LexhoAddress address = new LexhoAddress();
        address.setCity("Delhi");
        address.setPincode("110001");
        address.setLatitude(BigDecimal.valueOf(28.60));
        address.setLongitude(BigDecimal.valueOf(77.20));

        assertDoesNotThrow(() -> geoFenceValidationService.validateBookingAddress(address));
    }

    @Test
    void shouldRejectBookingWhenPointOutsidePolygon() {
        ServiceableAreaEntity area = new ServiceableAreaEntity();
        area.setPincode("110001");
        area.setCity("Delhi");
        area.setPolygonJson("""
                {"type":"Polygon","coordinates":[[[77.10,28.50],[77.30,28.50],[77.30,28.70],[77.10,28.70],[77.10,28.50]]]}
                """);

        when(serviceableAreaRepository.findByPincodeAndActiveTrueAndDeletedFalse("110001"))
                .thenReturn(List.of(area));
        when(serviceableAreaRepository.findByCityIgnoreCaseAndActiveTrueAndDeletedFalse(anyString()))
                .thenReturn(List.of());

        LexhoAddress address = new LexhoAddress();
        address.setCity("Delhi");
        address.setPincode("110001");
        address.setLatitude(BigDecimal.valueOf(28.90));
        address.setLongitude(BigDecimal.valueOf(77.90));

        assertThrows(BookingValidationException.class,
                () -> geoFenceValidationService.validateBookingAddress(address));
    }
}

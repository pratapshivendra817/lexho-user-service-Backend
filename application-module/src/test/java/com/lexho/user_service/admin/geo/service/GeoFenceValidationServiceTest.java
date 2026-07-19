package com.lexho.user_service.admin.geo.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lexho.user_service.admin.geo.entity.CityEntity;
import com.lexho.user_service.admin.geo.entity.ZoneEntity;
import com.lexho.user_service.admin.geo.repository.ZoneRepository;
import com.lexho.user_service.admin.serviceablearea.entity.ServiceabilityRuleEntity;
import com.lexho.user_service.admin.serviceablearea.entity.ServiceableAreaEntity;
import com.lexho.user_service.admin.serviceablearea.repository.ServiceabilityRuleRepository;
import com.lexho.user_service.admin.serviceablearea.repository.ServiceableAreaRepository;
import com.lexho.user_service.booking.exception.BookingValidationException;
import com.lexho.user_service.lexho.entity.LexhoAddress;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

class GeoFenceValidationServiceTest {

    private ServiceableAreaRepository serviceableAreaRepository;
    private ZoneRepository zoneRepository;
    private ServiceabilityRuleRepository serviceabilityRuleRepository;
    private GeoFenceValidationService geoFenceValidationService;

    @BeforeEach
    void setup() {
        serviceableAreaRepository = Mockito.mock(ServiceableAreaRepository.class);
        zoneRepository = Mockito.mock(ZoneRepository.class);
        serviceabilityRuleRepository = Mockito.mock(ServiceabilityRuleRepository.class);
        geoFenceValidationService = new GeoFenceValidationService(
                serviceableAreaRepository,
                serviceabilityRuleRepository,
                zoneRepository,
                new ObjectMapper()
        );
    }

    @Test
    void shouldAllowBookingWhenPointInsidePolygon() {
        ServiceableAreaEntity area = area("110001", "Delhi", polygonJson());
        when(serviceableAreaRepository.findByPincodeAndActiveTrueAndDeletedFalse("110001"))
                .thenReturn(List.of(area));
        when(serviceableAreaRepository.findByCityIgnoreCaseAndActiveTrueAndDeletedFalse(anyString()))
                .thenReturn(List.of());

        LexhoAddress address = address("Delhi", "110001", 28.60, 77.20);

        assertDoesNotThrow(() -> geoFenceValidationService.validateBookingAddress(address));
    }

    @Test
    void shouldRejectBookingWhenPointOutsidePolygon() {
        ServiceableAreaEntity area = area("110001", "Delhi", polygonJson());
        when(serviceableAreaRepository.findByPincodeAndActiveTrueAndDeletedFalse("110001"))
                .thenReturn(List.of(area));
        when(serviceableAreaRepository.findByCityIgnoreCaseAndActiveTrueAndDeletedFalse(anyString()))
                .thenReturn(List.of());

        LexhoAddress address = address("Delhi", "110001", 28.90, 77.90);

        BookingValidationException ex = assertThrows(BookingValidationException.class,
                () -> geoFenceValidationService.validateBookingAddress(address));

        assertEquals("BOOKING_OUTSIDE_GEOFENCE", ex.getErrorCode());
    }

    @Test
    void resolveServiceabilityShouldSupportCircleRadiusFence() {
        ServiceableAreaEntity area = area("560001", "Bengaluru",
                "{\"type\":\"Circle\",\"center\":[77.5946,12.9716],\"radiusMeters\":3000}");
        when(serviceableAreaRepository.findByPincodeAndActiveTrueAndDeletedFalse("560001"))
                .thenReturn(List.of(area));

        GeoFenceValidationService.GeoResolution result =
                geoFenceValidationService.resolveServiceability("560001", 12.9721, 77.5951, null);

        assertTrue(result.isServiceable());
        assertEquals("560001", result.getMatchedPincode());
    }

    @Test
    void resolveServiceabilityShouldReturnPartialAreaUnsupported() {
        ServiceableAreaEntity area = area("110001", "Delhi", polygonJson());
        when(serviceableAreaRepository.findByPincodeAndActiveTrueAndDeletedFalse("110001"))
                .thenReturn(List.of(area));

        GeoFenceValidationService.GeoResolution result =
                geoFenceValidationService.resolveServiceability("110001", 28.90, 77.90, null);

        assertFalse(result.isServiceable());
        assertEquals("PARTIAL_AREA_UNSUPPORTED", result.getReason());
    }

    @Test
    void resolveServiceabilityShouldRejectMissingInputs() {
        GeoFenceValidationService.GeoResolution result =
                geoFenceValidationService.resolveServiceability("  ", null, null, " ");

        assertFalse(result.isServiceable());
        assertEquals("MISSING_LOCATION_INPUT", result.getReason());
    }

    @Test
    void resolveServiceabilityShouldUseCityRuleAndZoneMetadata() {
        ServiceabilityRuleEntity rule = new ServiceabilityRuleEntity();
        ReflectionTestUtils.setField(rule, "city", "Delhi");
        ReflectionTestUtils.setField(rule, "cityId", null);
        ReflectionTestUtils.setField(rule, "zoneId", 77L);

        ZoneEntity zone = new ZoneEntity();
        zone.setId(77L);
        zone.setActive(true);
        zone.setDeleted(false);
        zone.setPolygonJson(polygonJson());
        CityEntity city = new CityEntity();
        city.setId(31L);
        zone.setCity(city);

        when(serviceabilityRuleRepository.findByCityIgnoreCaseAndActiveTrueAndDeletedFalse("Delhi"))
                .thenReturn(List.of(rule));
        when(zoneRepository.findById(77L)).thenReturn(Optional.of(zone));

        GeoFenceValidationService.GeoResolution result =
                geoFenceValidationService.resolveServiceability(null, 28.60, 77.20, " Delhi ");

        assertTrue(result.isServiceable());
        assertEquals(77L, result.getMatchedZoneId());
        assertEquals(31L, result.getMatchedCityId());
    }

    @Test
    void resolveServiceabilityShouldRequireCoordinatesWhenFenceExists() {
        ServiceableAreaEntity area = area("110001", "Delhi", polygonJson());
        when(serviceableAreaRepository.findByCityIgnoreCaseAndActiveTrueAndDeletedFalse("Delhi"))
                .thenReturn(List.of(area));

        GeoFenceValidationService.GeoResolution result =
                geoFenceValidationService.resolveServiceability(null, null, null, "Delhi");

        assertFalse(result.isServiceable());
        assertEquals("COORDINATES_REQUIRED_FOR_GEOFENCE", result.getReason());
    }

    @Test
    void resolveServiceabilityShouldFallbackToAllAreasAndReturnOutsideGeofence() {
        ServiceableAreaEntity area = area("110001", "Delhi", polygonJson());
        when(serviceabilityRuleRepository.findByActiveTrueAndDeletedFalse()).thenReturn(List.of());
        when(serviceableAreaRepository.findByActiveTrueAndDeletedFalse()).thenReturn(List.of(area));

        GeoFenceValidationService.GeoResolution result =
                geoFenceValidationService.resolveServiceability(null, 29.50, 78.50, null);

        assertFalse(result.isServiceable());
        assertEquals("OUTSIDE_GEOFENCE", result.getReason());
    }

    @Test
    void resolveServiceabilityShouldTreatInvalidPolygonJsonAsServiceableAreaMatch() {
        ServiceableAreaEntity area = area("110001", "Delhi", "{invalid-json");
        when(serviceableAreaRepository.findByPincodeAndActiveTrueAndDeletedFalse("110001"))
                .thenReturn(List.of(area));

        GeoFenceValidationService.GeoResolution result =
                geoFenceValidationService.resolveServiceability("110001", 99.0, 99.0, null);

        assertTrue(result.isServiceable());
        assertEquals("110001", result.getMatchedPincode());
    }

    @Test
    void resolveServiceabilityShouldReturnNoMatchingAreaWhenRepositoriesAreEmpty() {
        when(serviceabilityRuleRepository.findByPincodeAndActiveTrueAndDeletedFalse("110001")).thenReturn(List.of());
        when(serviceableAreaRepository.findByPincodeAndActiveTrueAndDeletedFalse("110001")).thenReturn(List.of());
        when(serviceabilityRuleRepository.findByCityIgnoreCaseAndActiveTrueAndDeletedFalse("Delhi")).thenReturn(List.of());
        when(serviceableAreaRepository.findByCityIgnoreCaseAndActiveTrueAndDeletedFalse("Delhi")).thenReturn(List.of());

        GeoFenceValidationService.GeoResolution result =
                geoFenceValidationService.resolveServiceability("110001", null, null, "Delhi");

        assertFalse(result.isServiceable());
        assertEquals("NO_MATCHING_SERVICEABLE_AREA", result.getReason());
    }

    @Test
    void resolveServiceabilityShouldParseFeatureCollectionAndRawCoordinatePolygons() {
        ServiceabilityRuleEntity rule = new ServiceabilityRuleEntity();
        ReflectionTestUtils.setField(rule, "pincode", "220001");
        ReflectionTestUtils.setField(rule, "polygonJson", """
                {"type":"FeatureCollection","features":[
                  {"type":"Feature","geometry":{"type":"Circle","center":[77.5946,12.9716],"radiusMeters":"2500"}},
                  {"type":"Feature","geometry":{"type":"Polygon","coordinates":[[[77.10,28.50],[77.30,28.50],[77.30,28.70],[77.10,28.70],[77.10,28.50]]]}}
                ]}
                """);
        ServiceableAreaEntity rawArea = area("330001", "Jaipur",
                "[[75.70,26.80],[75.95,26.80],[75.95,27.05],[75.70,27.05],[75.70,26.80]]");

        when(serviceabilityRuleRepository.findByPincodeAndActiveTrueAndDeletedFalse("220001"))
                .thenReturn(List.of(rule));
        when(serviceableAreaRepository.findByPincodeAndActiveTrueAndDeletedFalse("330001"))
                .thenReturn(List.of(rawArea));

        GeoFenceValidationService.GeoResolution fromFeatureCollection =
                geoFenceValidationService.resolveServiceability("220001", 12.9720, 77.5950, null);
        GeoFenceValidationService.GeoResolution fromRawArea =
                geoFenceValidationService.resolveServiceability("330001", 26.90, 75.80, null);

        assertTrue(fromFeatureCollection.isServiceable());
        assertEquals("220001", fromFeatureCollection.getMatchedPincode());
        assertTrue(fromRawArea.isServiceable());
        assertEquals("330001", fromRawArea.getMatchedPincode());
    }

    @Test
    void validateBookingAddressShouldRejectNullAddressAndNoAreaAndMissingCoordinates() {
        BookingValidationException missingAddress = assertThrows(BookingValidationException.class,
                () -> geoFenceValidationService.validateBookingAddress(null));
        assertEquals("ADDRESS_NOT_FOUND", missingAddress.getErrorCode());

        LexhoAddress outsideAddress = address("Delhi", "110001", 28.60, 77.20);
        when(serviceableAreaRepository.findByPincodeAndActiveTrueAndDeletedFalse("110001")).thenReturn(List.of());
        when(serviceableAreaRepository.findByCityIgnoreCaseAndActiveTrueAndDeletedFalse("Delhi")).thenReturn(List.of());

        BookingValidationException noArea = assertThrows(BookingValidationException.class,
                () -> geoFenceValidationService.validateBookingAddress(outsideAddress));
        assertEquals("BOOKING_OUTSIDE_SERVICE_ZONE", noArea.getErrorCode());

        ServiceableAreaEntity area = area("110001", "Delhi", polygonJson());
        when(serviceableAreaRepository.findByPincodeAndActiveTrueAndDeletedFalse("110001")).thenReturn(List.of(area));
        LexhoAddress missingCoordinates = address("Delhi", "110001", 0.0, 77.20);

        BookingValidationException coordinates = assertThrows(BookingValidationException.class,
                () -> geoFenceValidationService.validateBookingAddress(missingCoordinates));
        assertEquals("SAVED_ADDRESS_COORDINATES_MISSING", coordinates.getErrorCode());
    }

    @Test
    void validateBookingAddressShouldAllowAreaWithoutPolygonsAndUseZonePolygonWhenPresent() {
        ServiceableAreaEntity plainArea = area("110001", "Delhi", null);
        when(serviceableAreaRepository.findByPincodeAndActiveTrueAndDeletedFalse("110001"))
                .thenReturn(List.of(plainArea));
        assertDoesNotThrow(() -> geoFenceValidationService.validateBookingAddress(address("Delhi", "110001", 28.80, 77.80)));

        ServiceableAreaEntity zonedArea = area("110001", "Delhi", null);
        zonedArea.setZoneId(41L);
        ZoneEntity zone = new ZoneEntity();
        zone.setId(41L);
        zone.setActive(true);
        zone.setDeleted(false);
        zone.setPolygonJson(polygonJson());
        when(serviceableAreaRepository.findByPincodeAndActiveTrueAndDeletedFalse("110001"))
                .thenReturn(List.of(zonedArea));
        when(zoneRepository.findById(41L)).thenReturn(Optional.of(zone));

        assertDoesNotThrow(() -> geoFenceValidationService.validateBookingAddress(address("Delhi", "110001", 28.60, 77.20)));
    }

    private static LexhoAddress address(String city, String pincode, double latitude, double longitude) {
        LexhoAddress address = new LexhoAddress();
        address.setCity(city);
        address.setPincode(pincode);
        address.setLatitude(BigDecimal.valueOf(latitude));
        address.setLongitude(BigDecimal.valueOf(longitude));
        return address;
    }

    private static ServiceableAreaEntity area(String pincode, String city, String polygonJson) {
        ServiceableAreaEntity area = new ServiceableAreaEntity();
        area.setPincode(pincode);
        area.setCity(city);
        area.setPolygonJson(polygonJson);
        return area;
    }

    private static String polygonJson() {
        return """
                {"type":"Polygon","coordinates":[[[77.10,28.50],[77.30,28.50],[77.30,28.70],[77.10,28.70],[77.10,28.50]]]}
                """;
    }
}

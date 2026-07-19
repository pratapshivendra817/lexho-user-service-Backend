package com.lexho.user_service.customer.location.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lexho.user_service.admin.geo.service.GeoFenceValidationService;
import com.lexho.user_service.admin.service.RestrictedAreaOpsService;
import com.lexho.user_service.customer.location.dto.ServiceabilityCheckResponse;
import com.lexho.user_service.customer.location.entity.CustomerCurrentLocation;
import com.lexho.user_service.customer.location.repository.CustomerCurrentLocationRepository;
import com.lexho.user_service.customer.location.repository.ServiceablePincodeRepository;
import com.lexho.user_service.customer.location.service.impl.ServiceabilityServiceImpl;
import com.lexho.user_service.dispatch.entity.PartnerShiftEntity;
import com.lexho.user_service.dispatch.repository.CurrentPartnerLocationRepository;
import com.lexho.user_service.dispatch.repository.PartnerShiftRepository;
import com.lexho.user_service.geo.events.GeoEventPublisher;
import com.lexho.user_service.geo.events.GeoEventType;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ServiceabilityServiceImplAdditionalTest {

    @Mock ServiceablePincodeRepository repository;
    @Mock CustomerCurrentLocationRepository customerLocationRepository;
    @Mock GeoFenceValidationService geoFenceValidationService;
    @Mock CurrentPartnerLocationRepository currentPartnerLocationRepository;
    @Mock PartnerShiftRepository partnerShiftRepository;
    @Mock GeoEventPublisher geoEventPublisher;
    @Mock StringRedisTemplate redisTemplate;
    @Mock ValueOperations<String, String> valueOperations;
    @Mock RestrictedAreaOpsService restrictedAreaOpsService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    }

    @Test
    void isServiceableShouldTrimPincodeAndHandleNull() {
        ServiceabilityServiceImpl service = service(false, 1);
        when(repository.existsByPincodeAndActiveTrue("110001")).thenReturn(true);

        assertTrue(service.isServiceable(" 110001 "));
        assertFalse(service.isServiceable(null));
        verify(repository).existsByPincodeAndActiveTrue("");
    }

    @Test
    void resolveShouldReturnRedisCachedResponseWithoutGeofenceLookup() throws Exception {
        ServiceabilityServiceImpl service = service(false, 1);
        ServiceabilityCheckResponse cached = response(true, "CACHED");
        when(valueOperations.get(anyString())).thenReturn(objectMapper.writeValueAsString(cached));

        ServiceabilityCheckResponse result = service.resolve("110001", 28.6, 77.2);

        assertTrue(result.isServiceable());
        assertEquals("CACHED", result.getReason());
        verifyNoInteractions(geoFenceValidationService);
    }

    @Test
    void resolveShouldMapZoneAndUseLocalCacheOnSecondCall() {
        ServiceabilityServiceImpl service = service(false, 1);
        when(valueOperations.get(anyString())).thenReturn(null);
        when(geoFenceValidationService.resolveServiceability("110001", 28.6, 77.2, null))
                .thenReturn(GeoFenceValidationService.GeoResolution.serviceable(1L, 2L, "110001"));

        ServiceabilityCheckResponse first = service.resolve("PIN-110001", 28.6, 77.2);
        ServiceabilityCheckResponse second = service.resolve("PIN-110001", 28.6, 77.2);

        assertTrue(first.isServiceable());
        assertEquals(List.of("zone:2"), first.getServiceZones());
        assertEquals(List.of("STANDARD"), first.getSupportedServices());
        assertSame(first, second);
        verify(geoFenceValidationService, times(1)).resolveServiceability(any(), any(), any(), any());
        verify(valueOperations).set(anyString(), anyString(), any(Duration.class));
    }

    @Test
    void resolveShouldPublishEventForUnsupportedArea() {
        ServiceabilityServiceImpl service = service(false, 1);
        when(valueOperations.get(anyString())).thenReturn(null);
        when(geoFenceValidationService.resolveServiceability("", null, null, null))
                .thenReturn(GeoFenceValidationService.GeoResolution.notServiceable("OUTSIDE_SERVICE_AREA"));

        ServiceabilityCheckResponse result = service.resolve("invalid", null, null);

        assertFalse(result.isServiceable());
        assertEquals("OUTSIDE_SERVICE_AREA", result.getReason());
        verify(geoEventPublisher).publish(eq(GeoEventType.SERVICEABILITY_CHANGED), isNull(),
                argThat(payload -> Boolean.FALSE.equals(payload.get("serviceable"))));
    }

    @Test
    void resolveForCustomerShouldHandleMissingContextAndLocation() {
        ServiceabilityServiceImpl service = service(false, 1);
        assertEquals("MISSING_CUSTOMER_CONTEXT", service.resolveForCustomer(null).getReason());
        when(customerLocationRepository.findByCustomerId(7L)).thenReturn(Optional.empty());
        assertEquals("CURRENT_LOCATION_NOT_FOUND", service.resolveForCustomer(7L).getReason());
    }

    @Test
    void resolveForCustomerShouldUseStoredLocationAndMapPincodeZone() {
        ServiceabilityServiceImpl service = service(false, 1);
        CustomerCurrentLocation location = new CustomerCurrentLocation();
        location.setPincode("110001");
        location.setLatitude(BigDecimal.valueOf(28.6));
        location.setLongitude(BigDecimal.valueOf(77.2));
        location.setCity("Delhi");
        when(customerLocationRepository.findByCustomerId(7L)).thenReturn(Optional.of(location));
        when(geoFenceValidationService.resolveServiceability("110001", 28.6, 77.2, "Delhi"))
                .thenReturn(GeoFenceValidationService.GeoResolution.serviceable(null, null, "110001"));

        ServiceabilityCheckResponse result = service.resolveForCustomer(7L);

        assertTrue(result.isServiceable());
        assertEquals(List.of("pincode:110001"), result.getServiceZones());
    }

    @Test
    void dynamicCapacityShouldRejectTemporarilyRestrictedArea() {
        ServiceabilityServiceImpl service = service(true, 1);
        serviceableGeofence();
        when(restrictedAreaOpsService.isRestricted(28.6, 77.2)).thenReturn(true);

        ServiceabilityCheckResponse result = service.resolve("110001", 28.6, 77.2);

        assertFalse(result.isServiceable());
        assertEquals("AREA_TEMPORARILY_UNAVAILABLE", result.getReason());
        verifyNoInteractions(currentPartnerLocationRepository);
    }

    @Test
    void dynamicCapacityShouldRejectWhenNoNearbyPartnerExists() {
        ServiceabilityServiceImpl service = service(true, 1);
        serviceableGeofence();
        when(restrictedAreaOpsService.isRestricted(28.6, 77.2)).thenReturn(false);
        when(currentPartnerLocationRepository.findNearbyPartnerIds(28.6, 77.2, 5.0, 200)).thenReturn(List.of());

        ServiceabilityCheckResponse result = service.resolve("110001", 28.6, 77.2);

        assertFalse(result.isServiceable());
        assertEquals("NO_NEARBY_PARTNER_AVAILABLE", result.getReason());
    }

    @Test
    void dynamicCapacityShouldDetectOverloadAndAllowEnoughCapacity() {
        ServiceabilityServiceImpl overloaded = service(true, 2);
        serviceableGeofence();
        when(restrictedAreaOpsService.isRestricted(28.6, 77.2)).thenReturn(false);
        when(currentPartnerLocationRepository.findNearbyPartnerIds(28.6, 77.2, 5.0, 200))
                .thenReturn(List.of(1L, 2L));
        PartnerShiftEntity available = shift(true, 0, 3);
        PartnerShiftEntity busy = shift(true, 3, 3);
        when(partnerShiftRepository.findByPartnerIdInAndDeletedFalse(List.of(1L, 2L)))
                .thenReturn(List.of(available, busy));

        ServiceabilityCheckResponse result = overloaded.resolve("110001", 28.6, 77.2);
        assertFalse(result.isServiceable());
        assertEquals("PEAK_HOUR_OVERLOAD", result.getReason());

        ServiceabilityServiceImpl enough = service(true, 1);
        ServiceabilityCheckResponse accepted = enough.resolve("110001", 28.6, 77.2);
        assertTrue(accepted.isServiceable());
    }

    private ServiceabilityServiceImpl service(boolean dynamic, int minimumPartners) {
        return new ServiceabilityServiceImpl(repository, customerLocationRepository, geoFenceValidationService,
                currentPartnerLocationRepository, partnerShiftRepository, geoEventPublisher, 10, 5.0,
                minimumPartners, 0, 0, dynamic, new SimpleMeterRegistry(), redisTemplate,
                restrictedAreaOpsService, objectMapper);
    }

    private void serviceableGeofence() {
        when(valueOperations.get(anyString())).thenReturn(null);
        when(geoFenceValidationService.resolveServiceability("110001", 28.6, 77.2, null))
                .thenReturn(GeoFenceValidationService.GeoResolution.serviceable(1L, 2L, "110001"));
    }

    private static ServiceabilityCheckResponse response(boolean serviceable, String reason) {
        ServiceabilityCheckResponse response = new ServiceabilityCheckResponse();
        response.setServiceable(serviceable);
        response.setReason(reason);
        return response;
    }

    private static PartnerShiftEntity shift(boolean online, int activeJobs, int maxJobs) {
        PartnerShiftEntity shift = new PartnerShiftEntity();
        shift.setOnline(online);
        shift.setActiveJobsCount(activeJobs);
        shift.setMaxActiveJobs(maxJobs);
        return shift;
    }
}

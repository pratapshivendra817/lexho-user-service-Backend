package com.lexho.user_service.customer.location.service;

import com.lexho.user_service.customer.location.dto.LocationUpdateRequest;
import com.lexho.user_service.customer.location.dto.LocationUpdateResponse;
import com.lexho.user_service.customer.location.dto.ReverseGeocodeResponse;
import com.lexho.user_service.customer.location.dto.ServiceabilityCheckResponse;
import com.lexho.user_service.customer.location.entity.CustomerCurrentLocation;
import com.lexho.user_service.customer.location.exception.AddressNotFoundException;
import com.lexho.user_service.customer.location.exception.InvalidCoordinatesException;
import com.lexho.user_service.customer.location.repository.CustomerCurrentLocationRepository;
import com.lexho.user_service.customer.location.repository.LocationAuditLogRepository;
import com.lexho.user_service.customer.location.service.impl.LocationServiceImpl;
import com.lexho.user_service.geo.events.GeoEventType;
import com.lexho.user_service.geo.events.GeoEventPublisher;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class LocationServiceImplTest {

    @Test
    void updateCurrentLocation_shouldResolveServiceability() {
        CustomerCurrentLocationRepository locationRepo = Mockito.mock(CustomerCurrentLocationRepository.class);
        ReverseGeocodeService reverse = Mockito.mock(ReverseGeocodeService.class);
        ReverseGeocodePipelineService pipelineService = Mockito.mock(ReverseGeocodePipelineService.class);
        ServiceabilityService serviceability = Mockito.mock(ServiceabilityService.class);
        LocationAuditLogRepository auditLogRepository = Mockito.mock(LocationAuditLogRepository.class);
        GeoEventPublisher geoEventPublisher = Mockito.mock(GeoEventPublisher.class);
        when(locationRepo.findByCustomerId(99L)).thenReturn(Optional.empty());
        when(locationRepo.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        ReverseGeocodeResponse geo = new ReverseGeocodeResponse();
        geo.pincode = "201304";
        geo.city = "Noida";
        geo.state = "UP";
        when(reverse.reverse(28.6123, 77.2290)).thenReturn(geo);
        when(pipelineService.reverseAsync(28.6123, 77.2290)).thenReturn(CompletableFuture.completedFuture(geo));
        ServiceabilityCheckResponse serviceabilityCheckResponse = new ServiceabilityCheckResponse();
        serviceabilityCheckResponse.setServiceable(true);
        serviceabilityCheckResponse.setMatchedPincode("201304");
        when(serviceability.resolve("201304", 28.6123, 77.2290)).thenReturn(serviceabilityCheckResponse);

        LocationServiceImpl service = new LocationServiceImpl(locationRepo, reverse, pipelineService, serviceability, auditLogRepository, geoEventPublisher, new SimpleMeterRegistry(), 300.0d, 300L, 500L);
        LocationUpdateRequest request = new LocationUpdateRequest();
        request.latitude = 28.6123;
        request.longitude = 77.2290;

        LocationUpdateResponse response = service.updateCurrentLocation(99L, request);
        assertTrue(response.serviceAvailable);
        assertEquals("201304", response.pincode);
    }

    @Test
    void updateCurrentLocation_shouldBeIdempotentForSameKeyAndCoordinates() {
        CustomerCurrentLocationRepository locationRepo = Mockito.mock(CustomerCurrentLocationRepository.class);
        ReverseGeocodeService reverse = Mockito.mock(ReverseGeocodeService.class);
        ReverseGeocodePipelineService pipelineService = Mockito.mock(ReverseGeocodePipelineService.class);
        ServiceabilityService serviceability = Mockito.mock(ServiceabilityService.class);
        LocationAuditLogRepository auditLogRepository = Mockito.mock(LocationAuditLogRepository.class);
        GeoEventPublisher geoEventPublisher = Mockito.mock(GeoEventPublisher.class);

        CustomerCurrentLocation existing = new CustomerCurrentLocation();
        existing.setCustomerId(99L);
        existing.setPincode("110001");
        existing.setServiceAvailable(true);
        existing.setLastIdempotencyKey("idem-1");
        existing.setLastLocationFingerprint("2861230:7722900:idem-1");
        when(locationRepo.findByCustomerId(99L)).thenReturn(Optional.of(existing));

        LocationServiceImpl service = new LocationServiceImpl(locationRepo, reverse, pipelineService, serviceability, auditLogRepository, geoEventPublisher, new SimpleMeterRegistry(), 300.0d, 300L, 500L);
        LocationUpdateRequest request = new LocationUpdateRequest();
        request.latitude = 28.6123;
        request.longitude = 77.2290;
        request.idempotencyKey = "idem-1";

        LocationUpdateResponse response = service.updateCurrentLocation(99L, request);

        assertTrue(response.serviceAvailable);
        assertEquals("110001", response.pincode);
        verify(reverse, never()).reverse(any(Double.class), any(Double.class));
        verify(serviceability, never()).resolve(any(), any(), any());
        verify(locationRepo, never()).save(any());
    }

    @Test
    void updateCurrentLocation_shouldPreserveManualConfirmedAddressOnAutoGps() {
        CustomerCurrentLocationRepository locationRepo = Mockito.mock(CustomerCurrentLocationRepository.class);
        ReverseGeocodeService reverse = Mockito.mock(ReverseGeocodeService.class);
        ReverseGeocodePipelineService pipelineService = Mockito.mock(ReverseGeocodePipelineService.class);
        ServiceabilityService serviceability = Mockito.mock(ServiceabilityService.class);
        LocationAuditLogRepository auditLogRepository = Mockito.mock(LocationAuditLogRepository.class);
        GeoEventPublisher geoEventPublisher = Mockito.mock(GeoEventPublisher.class);

        CustomerCurrentLocation existing = new CustomerCurrentLocation();
        existing.setCustomerId(99L);
        existing.setManualConfirmed(true);
        existing.setFormattedAddress("Manual Address");
        existing.setCity("Manual City");
        existing.setState("Manual State");
        existing.setPincode("110001");
        existing.setLatitude(BigDecimal.valueOf(28.6122));
        existing.setLongitude(BigDecimal.valueOf(77.2291));
        existing.setSpoofAttemptCount(0);
        when(locationRepo.findByCustomerId(99L)).thenReturn(Optional.of(existing));
        when(locationRepo.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        ServiceabilityCheckResponse serviceabilityCheckResponse = new ServiceabilityCheckResponse();
        serviceabilityCheckResponse.setServiceable(true);
        when(serviceability.resolve("110001", 28.6123, 77.2290)).thenReturn(serviceabilityCheckResponse);

        LocationServiceImpl service = new LocationServiceImpl(locationRepo, reverse, pipelineService, serviceability, auditLogRepository, geoEventPublisher, new SimpleMeterRegistry(), 300.0d, 300L, 500L);
        LocationUpdateRequest request = new LocationUpdateRequest();
        request.latitude = 28.6123;
        request.longitude = 77.2290;
        request.detectionSource = "GPS_AUTO";

        service.updateCurrentLocation(99L, request);

        verify(reverse, never()).reverse(any(Double.class), any(Double.class));
        ArgumentCaptor<CustomerCurrentLocation> captor = ArgumentCaptor.forClass(CustomerCurrentLocation.class);
        verify(locationRepo).save(captor.capture());
        CustomerCurrentLocation saved = captor.getValue();
        assertEquals("Manual Address", saved.getFormattedAddress());
        assertEquals("Manual City", saved.getCity());
        assertEquals("Manual State", saved.getState());
        assertEquals("110001", saved.getPincode());
    }

    @Test
    void getCurrentLocationShouldReturnSavedLocationOrThrowWhenMissing() {
        CustomerCurrentLocationRepository locationRepo = Mockito.mock(CustomerCurrentLocationRepository.class);
        ReverseGeocodeService reverse = Mockito.mock(ReverseGeocodeService.class);
        ReverseGeocodePipelineService pipelineService = Mockito.mock(ReverseGeocodePipelineService.class);
        ServiceabilityService serviceability = Mockito.mock(ServiceabilityService.class);
        LocationAuditLogRepository auditLogRepository = Mockito.mock(LocationAuditLogRepository.class);
        GeoEventPublisher geoEventPublisher = Mockito.mock(GeoEventPublisher.class);

        CustomerCurrentLocation existing = new CustomerCurrentLocation();
        existing.setCustomerId(99L);
        existing.setLatitude(BigDecimal.valueOf(28.6));
        existing.setLongitude(BigDecimal.valueOf(77.2));
        existing.setPincode("110001");
        existing.setServiceAvailable(true);
        when(locationRepo.findByCustomerId(99L)).thenReturn(Optional.of(existing));
        when(locationRepo.findByCustomerId(100L)).thenReturn(Optional.empty());

        LocationServiceImpl service = new LocationServiceImpl(locationRepo, reverse, pipelineService, serviceability, auditLogRepository, geoEventPublisher, new SimpleMeterRegistry(), 300.0d, 300L, 500L);

        assertEquals("110001", service.getCurrentLocation(99L).pincode);
        assertThrows(AddressNotFoundException.class, () -> service.getCurrentLocation(100L));
    }

    @Test
    void updateCurrentLocationShouldRejectInvalidCoordinatesAndStaleEvents() {
        CustomerCurrentLocationRepository locationRepo = Mockito.mock(CustomerCurrentLocationRepository.class);
        ReverseGeocodeService reverse = Mockito.mock(ReverseGeocodeService.class);
        ReverseGeocodePipelineService pipelineService = Mockito.mock(ReverseGeocodePipelineService.class);
        ServiceabilityService serviceability = Mockito.mock(ServiceabilityService.class);
        LocationAuditLogRepository auditLogRepository = Mockito.mock(LocationAuditLogRepository.class);
        GeoEventPublisher geoEventPublisher = Mockito.mock(GeoEventPublisher.class);
        LocationServiceImpl service = new LocationServiceImpl(locationRepo, reverse, pipelineService, serviceability, auditLogRepository, geoEventPublisher, new SimpleMeterRegistry(), 300.0d, 300L, 500L);

        LocationUpdateRequest invalid = new LocationUpdateRequest();
        invalid.latitude = 100.0;
        invalid.longitude = 77.0;
        assertThrows(InvalidCoordinatesException.class, () -> service.updateCurrentLocation(1L, invalid));

        LocationUpdateRequest stale = new LocationUpdateRequest();
        stale.latitude = 28.6;
        stale.longitude = 77.2;
        stale.eventTimestampMs = Instant.now().minusSeconds(600).toEpochMilli();
        assertThrows(InvalidCoordinatesException.class, () -> service.updateCurrentLocation(1L, stale));
    }

    @Test
    void updateCurrentLocationShouldHandleTimeoutsRiskSignalsAndManualConfirmEvents() {
        CustomerCurrentLocationRepository locationRepo = Mockito.mock(CustomerCurrentLocationRepository.class);
        ReverseGeocodeService reverse = Mockito.mock(ReverseGeocodeService.class);
        ReverseGeocodePipelineService pipelineService = Mockito.mock(ReverseGeocodePipelineService.class);
        ServiceabilityService serviceability = Mockito.mock(ServiceabilityService.class);
        LocationAuditLogRepository auditLogRepository = Mockito.mock(LocationAuditLogRepository.class);
        GeoEventPublisher geoEventPublisher = Mockito.mock(GeoEventPublisher.class);

        CustomerCurrentLocation existing = new CustomerCurrentLocation();
        existing.setCustomerId(99L);
        existing.setLatitude(BigDecimal.valueOf(12.0));
        existing.setLongitude(BigDecimal.valueOf(77.0));
        existing.setSpoofAttemptCount(1);
        when(locationRepo.findByCustomerId(99L)).thenReturn(Optional.of(existing));
        when(locationRepo.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(pipelineService.reverseAsync(13.0, 78.0)).thenReturn(CompletableFuture.failedFuture(new RuntimeException("boom")));
        ServiceabilityCheckResponse serviceabilityCheckResponse = new ServiceabilityCheckResponse();
        serviceabilityCheckResponse.setServiceable(false);
        serviceabilityCheckResponse.setMatchedPincode("560001");
        when(serviceability.resolve("", 13.0, 78.0)).thenReturn(serviceabilityCheckResponse);

        LocationServiceImpl service = new LocationServiceImpl(locationRepo, reverse, pipelineService, serviceability, auditLogRepository, geoEventPublisher, new SimpleMeterRegistry(), 100.0d, 300L, 100L);
        LocationUpdateRequest request = new LocationUpdateRequest();
        request.latitude = 13.0;
        request.longitude = 78.0;
        request.mockLocation = true;
        request.confidenceScore = 0.4;
        request.manualConfirmed = true;

        LocationUpdateResponse response = service.updateCurrentLocation(99L, request);

        assertEquals("560001", response.pincode);
        verify(auditLogRepository).save(any());
        verify(geoEventPublisher).publish(eq(GeoEventType.LOCATION_UPDATED), eq(99L), any());
        verify(geoEventPublisher).publish(eq(GeoEventType.ADDRESS_CONFIRMED), eq(99L), any());
        verify(geoEventPublisher).publish(eq(GeoEventType.SPOOF_DETECTED), eq(99L), any());
    }

    @Test
    void updateCurrentLocationShouldFallbackWhenServiceabilityOrAuditFails() {
        CustomerCurrentLocationRepository locationRepo = Mockito.mock(CustomerCurrentLocationRepository.class);
        ReverseGeocodeService reverse = Mockito.mock(ReverseGeocodeService.class);
        ReverseGeocodePipelineService pipelineService = Mockito.mock(ReverseGeocodePipelineService.class);
        ServiceabilityService serviceability = Mockito.mock(ServiceabilityService.class);
        LocationAuditLogRepository auditLogRepository = Mockito.mock(LocationAuditLogRepository.class);
        GeoEventPublisher geoEventPublisher = Mockito.mock(GeoEventPublisher.class);

        when(locationRepo.findByCustomerId(99L)).thenReturn(Optional.empty());
        when(locationRepo.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        ReverseGeocodeResponse geo = new ReverseGeocodeResponse();
        geo.formattedAddress = "  Sector 18 ";
        geo.area = "Noida";
        geo.city = "Noida";
        geo.state = "UP";
        geo.country = "India";
        geo.landmark = "Mall";
        when(pipelineService.reverseAsync(28.6123, 77.2290)).thenReturn(CompletableFuture.completedFuture(geo));
        when(serviceability.resolve("", 28.6123, 77.2290)).thenReturn(null);
        Mockito.doThrow(new RuntimeException("audit down")).when(auditLogRepository).save(any());

        LocationServiceImpl service = new LocationServiceImpl(locationRepo, reverse, pipelineService, serviceability, auditLogRepository, geoEventPublisher, new SimpleMeterRegistry(), 300.0d, 300L, 500L);
        LocationUpdateRequest request = new LocationUpdateRequest();
        request.latitude = 28.6123;
        request.longitude = 77.2290;
        request.mockLocation = true;

        LocationUpdateResponse response = service.updateCurrentLocation(99L, request);

        assertTrue(!response.serviceAvailable);
        assertEquals("Sector 18", response.formattedAddress);
        assertEquals("India", response.country);
        assertNotNull(response.city);
    }
}

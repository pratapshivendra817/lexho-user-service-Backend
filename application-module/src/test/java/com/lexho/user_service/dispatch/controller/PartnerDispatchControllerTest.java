package com.lexho.user_service.dispatch.controller;

import com.lexho.booking.repository.OrderRepository;
import com.lexho.user_service.customer.location.service.ReverseGeocodingService;
import com.lexho.user_service.dispatch.dto.PartnerAvailabilityUpdateRequest;
import com.lexho.user_service.dispatch.dto.PartnerLocationUpdateRequest;
import com.lexho.user_service.dispatch.dto.PartnerShiftEndRequest;
import com.lexho.user_service.dispatch.dto.PartnerShiftStartRequest;
import com.lexho.user_service.dispatch.dto.PartnerShiftUpdateRequest;
import com.lexho.user_service.dispatch.dto.PartnerSkillsUpdateRequest;
import com.lexho.user_service.dispatch.entity.CurrentPartnerLocationEntity;
import com.lexho.user_service.dispatch.entity.PartnerShiftEntity;
import com.lexho.user_service.dispatch.repository.CurrentPartnerLocationRepository;
import com.lexho.user_service.dispatch.repository.PartnerShiftRepository;
import com.lexho.user_service.dispatch.service.GeoBatchingService;
import com.lexho.user_service.dispatch.service.PartnerGeoIndexService;
import com.lexho.user_service.dispatch.service.PartnerLocationOptimizationService;
import com.lexho.user_service.dispatch.service.PartnerSkillService;
import com.lexho.user_service.dispatch.service.TrackingLifecycleService;
import com.lexho.user_service.dispatch.service.TrackingLifecycleState;
import com.lexho.user_service.exception.BadRequestException;
import com.lexho.user_service.customer.location.dto.ReverseGeocodeResponse;
import com.lexho.user_service.geo.events.GeoEventPublisher;
import com.lexho.user_service.geo.events.GeoEventType;
import com.lexho.user_service.lexhopartner.entity.PartnerOperationalProfile;
import com.lexho.user_service.lexhopartner.repository.PartnerOperationalProfileRepository;
import com.lexho.user_service.service.DeprecatedApiUsageService;
import com.lexho.user_service.service.DeprecationControlService;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PartnerDispatchControllerTest {

    @Mock PartnerShiftRepository shiftRepository;
    @Mock CurrentPartnerLocationRepository locationRepository;
    @Mock ReverseGeocodingService reverseGeocodingService;
    @Mock DeprecationControlService deprecationControlService;
    @Mock DeprecatedApiUsageService deprecatedApiUsageService;
    @Mock PartnerSkillService partnerSkillService;
    @Mock PartnerOperationalProfileRepository partnerProfileRepository;
    @Mock PartnerGeoIndexService partnerGeoIndexService;
    @Mock GeoEventPublisher geoEventPublisher;
    @Mock TrackingLifecycleService trackingLifecycleService;
    @Mock OrderRepository orderRepository;
    @Mock PartnerLocationOptimizationService locationOptimizationService;
    @Mock GeoBatchingService geoBatchingService;

    private PartnerDispatchController controller;

    @BeforeEach
    void setUp() {
        controller = new PartnerDispatchController(shiftRepository, locationRepository, reverseGeocodingService,
                deprecationControlService, deprecatedApiUsageService, partnerSkillService, partnerProfileRepository,
                partnerGeoIndexService, geoEventPublisher, trackingLifecycleService, orderRepository,
                locationOptimizationService, geoBatchingService, new SimpleMeterRegistry(), 12, 15, 30);
    }

    @Test
    void configShouldExposeTrackingIntervals() {
        Map<String, Object> data = controller.config().getData();
        assertEquals(15L, data.get("locationUpdateIntervalSeconds"));
        assertEquals(30L, data.get("backgroundIntervalSeconds"));
    }

    @Test
    void startShiftShouldCreateOnlineShiftUsingCompatibilityTimesAndLocation() {
        PartnerShiftStartRequest request = new PartnerShiftStartRequest();
        request.shiftDate = "2026-07-01";
        request.startTime = "09:00";
        request.endTime = "17:00";
        CurrentPartnerLocationEntity location = new CurrentPartnerLocationEntity();
        location.setPincode("110001");
        when(shiftRepository.findByPartnerId(7L)).thenReturn(Optional.empty());
        when(locationRepository.findByPartnerId(7L)).thenReturn(Optional.of(location));

        Map<String, Object> data = controller.startShift(7L, request).getData();

        assertEquals(true, data.get("online"));
        assertEquals("110001", data.get("currentPincode"));
        verify(shiftRepository).save(argThat(shift -> shift.isOnline()
                && shift.getShiftStart().equals(LocalDateTime.of(2026, 7, 1, 9, 0))
                && "PARTNER:7".equals(shift.getCreatedBy())));
        verify(geoEventPublisher).publish(GeoEventType.PARTNER_ONLINE, 7L, Map.of("online", true));
    }

    @Test
    void endShiftShouldPersistOfflineAndRemoveGeoIndex() {
        PartnerShiftEntity shift = shift(7L, true);
        when(shiftRepository.findByPartnerId(7L)).thenReturn(Optional.of(shift));
        PartnerShiftEndRequest request = new PartnerShiftEndRequest();
        request.shiftEnd = LocalDateTime.of(2026, 7, 1, 18, 0);

        Map<String, Object> data = controller.endShift(7L, request).getData();

        assertEquals(false, data.get("online"));
        assertEquals(request.shiftEnd, shift.getShiftEnd());
        verify(partnerGeoIndexService).removePartner(7L);
        verify(geoEventPublisher).publish(GeoEventType.PARTNER_OFFLINE, 7L, Map.of("online", false));
    }

    @Test
    void availabilityPatchAndPutShouldResolveAliasesAndUpdateCapacity() {
        PartnerShiftEntity shift = shift(7L, false);
        when(shiftRepository.findByPartnerId(7L)).thenReturn(Optional.of(shift));
        PartnerAvailabilityUpdateRequest patch = new PartnerAvailabilityUpdateRequest();
        patch.available = true;
        patch.maxActiveJobs = 5;
        assertEquals(true, controller.updateAvailability(7L, patch).getData().get("online"));
        assertEquals(5, shift.getMaxActiveJobs());

        PartnerAvailabilityUpdateRequest put = new PartnerAvailabilityUpdateRequest();
        put.isOnline = false;
        assertEquals(false, controller.updateAvailabilityPut(7L, put).getData().get("online"));
        verify(partnerGeoIndexService).removePartner(7L);
    }

    @Test
    void historyAliasesShouldMapShiftFields() {
        PartnerShiftEntity shift = shift(7L, true);
        shift.setCurrentPincode("110001");
        shift.setActiveJobsCount(2);
        when(shiftRepository.findShiftHistoryByPartnerId(7L)).thenReturn(List.of(shift));

        assertEquals(1, controller.shiftHistory(7L).getData().size());
        assertEquals("110001", controller.history(7L).getData().get(0).get("currentPincode"));
        verify(shiftRepository, times(2)).findShiftHistoryByPartnerId(7L);
    }

    @Test
    void deprecatedShiftShouldRecordUsageAndReturnReplacementHeaders() {
        PartnerShiftUpdateRequest request = new PartnerShiftUpdateRequest();
        request.isOnline = true;
        request.maxActiveJobs = 4;
        when(shiftRepository.findByPartnerId(7L)).thenReturn(Optional.empty());
        when(locationRepository.findByPartnerId(7L)).thenReturn(Optional.empty());

        var response = controller.updateShift(7L, request);

        assertEquals("true", response.getHeaders().getFirst("X-API-Deprecated"));
        assertTrue(response.getHeaders().getFirst("X-API-Replacement").contains("shift/start"));
        verify(deprecationControlService).ensureLegacyPartnerShiftEnabled();
        verify(deprecatedApiUsageService).record("partner.dispatch.shift.legacy");
    }

    @Test
    void availabilityShouldRejectAdminLockedPartner() {
        PartnerOperationalProfile profile = new PartnerOperationalProfile();
        profile.setId(7L);
        profile.setAvailabilityLockedUntil(LocalDateTime.now().plusHours(1));
        when(partnerProfileRepository.findById(7L)).thenReturn(Optional.of(profile));
        PartnerAvailabilityUpdateRequest request = new PartnerAvailabilityUpdateRequest();
        request.online = true;

        assertThrows(BadRequestException.class, () -> controller.updateAvailability(7L, request));
        verifyNoInteractions(shiftRepository);
    }

    @Test
    void skillsEndpointsShouldDelegate() {
        PartnerSkillsUpdateRequest request = new PartnerSkillsUpdateRequest();
        request.setCategoryIds(List.of(1L));
        when(partnerSkillService.upsertSkills(7L, request)).thenReturn(Map.of("updated", true));
        when(partnerSkillService.getSkills(7L)).thenReturn(Map.of("categories", List.of(1L)));

        assertEquals(true, controller.upsertSkills(7L, request).getData().get("updated"));
        assertEquals(List.of(1L), controller.getSkills(7L).getData().get("categories"));
    }

    @Test
    void updateLocationShouldRejectOfflinePartnerAndHandleGpsDisabled() {
        when(shiftRepository.findByPartnerId(7L)).thenReturn(Optional.empty());
        PartnerLocationUpdateRequest request = new PartnerLocationUpdateRequest();
        request.latitude = 28.61;
        request.longitude = 77.20;

        assertThrows(BadRequestException.class, () -> controller.updateLocation(7L, request));

        PartnerShiftEntity shift = shift(7L, true);
        when(shiftRepository.findByPartnerId(7L)).thenReturn(Optional.of(shift));
        request.gpsEnabled = false;
        Map<String, Object> data = controller.updateLocation(7L, request).getData();

        assertEquals(false, data.get("online"));
        assertEquals("GPS_DISABLED", data.get("reason"));
        verify(partnerGeoIndexService).removePartner(7L);
        verify(geoEventPublisher).publish(GeoEventType.PARTNER_OFFLINE, 7L, Map.of("online", false, "reason", "GPS_DISABLED"));
    }

    @Test
    void updateLocationShouldThrottleTerminalAndPersistAcceptedMovement() {
        PartnerShiftEntity shift = shift(7L, true);
        shift.setActiveJobsCount(1);
        when(shiftRepository.findByPartnerId(7L)).thenReturn(Optional.of(shift));

        CurrentPartnerLocationEntity existing = new CurrentPartnerLocationEntity();
        ReflectionTestUtils.setField(existing, "id", 5L);
        existing.setPartnerId(7L);
        ReflectionTestUtils.setField(existing, "updatedAt", LocalDateTime.now());
        when(locationRepository.findAnyByPartnerId(7L)).thenReturn(Optional.of(existing));
        when(trackingLifecycleService.resolveState(any(), anyDouble(), anyDouble(), any(), any()))
                .thenReturn(TrackingLifecycleState.EN_ROUTE, TrackingLifecycleState.CANCELLED, TrackingLifecycleState.ARRIVED);
        when(trackingLifecycleService.ingestMinIntervalMs(TrackingLifecycleState.EN_ROUTE)).thenReturn(60_000L, 0L);
        when(trackingLifecycleService.isTerminal(TrackingLifecycleState.CANCELLED)).thenReturn(true);
        when(trackingLifecycleService.isTerminal(TrackingLifecycleState.ARRIVED)).thenReturn(false);
        when(trackingLifecycleService.sseIntervalMs(TrackingLifecycleState.ARRIVED)).thenReturn(2000L);

        PartnerLocationUpdateRequest throttled = new PartnerLocationUpdateRequest();
        throttled.latitude = 28.61;
        throttled.longitude = 77.20;
        throttled.trackingState = "EN_ROUTE";
        Map<String, Object> throttleData = controller.updateLocation(7L, throttled).getData();
        assertEquals(false, throttleData.get("accepted"));
        assertEquals("EN_ROUTE", throttleData.get("trackingState"));

        ReflectionTestUtils.setField(existing, "updatedAt", null);
        PartnerLocationUpdateRequest terminal = new PartnerLocationUpdateRequest();
        terminal.latitude = 28.62;
        terminal.longitude = 77.21;
        terminal.trackingState = "CANCELLED";
        Map<String, Object> terminalData = controller.updateLocation(7L, terminal).getData();
        assertEquals(true, terminalData.get("shutdown"));
        verify(partnerGeoIndexService).removePartner(7L);

        CurrentPartnerLocationEntity fresh = new CurrentPartnerLocationEntity();
        fresh.setLocationSeq(0L);
        shift.setOnline(true);
        when(locationRepository.findAnyByPartnerId(7L)).thenReturn(Optional.of(fresh));
        ReverseGeocodeResponse geo = new ReverseGeocodeResponse();
        geo.pincode = "110001";
        when(reverseGeocodingService.reverse(28.63, 77.22)).thenReturn(geo);
        when(locationOptimizationService.evaluate(any(), any()))
                .thenReturn(new PartnerLocationOptimizationService.Decision(true, true, true, "IMPOSSIBLE_SPEED"));
        when(locationOptimizationService.computeFraudScore(any(), any(), eq(true))).thenReturn(65.0d);

        PartnerLocationUpdateRequest accepted = new PartnerLocationUpdateRequest();
        accepted.latitude = 28.63;
        accepted.longitude = 77.22;
        accepted.trackingState = "ARRIVED";
        accepted.source = " APP ";
        accepted.accuracyMeters = 5.0;
        accepted.headingDegrees = 120.0;
        accepted.speedKmh = 12.0;
        Map<String, Object> acceptedData = controller.updateLocation(7L, accepted).getData();

        assertEquals("110001", acceptedData.get("pincode"));
        assertEquals("ARRIVED", acceptedData.get("trackingState"));
        assertEquals(true, acceptedData.get("movementAccepted"));
        verify(geoBatchingService).publishImmediate(eq(GeoEventType.PARTNER_MOVED), eq(7L), any(HashMap.class));
        verify(geoEventPublisher).publish(eq(GeoEventType.SPOOF_DETECTED), eq(7L), any(Map.class));
    }

    private static PartnerShiftEntity shift(Long partnerId, boolean online) {
        PartnerShiftEntity shift = new PartnerShiftEntity();
        ReflectionTestUtils.setField(shift, "id", 1L);
        shift.setPartnerId(partnerId);
        shift.setOnline(online);
        return shift;
    }
}

package com.lexho.user_service.dispatch.controller;

import com.lexho.user_service.dispatch.entity.CurrentPartnerLocationEntity;
import com.lexho.user_service.dispatch.entity.PartnerShiftEntity;
import com.lexho.user_service.dispatch.repository.CurrentPartnerLocationRepository;
import com.lexho.user_service.dispatch.repository.PartnerShiftRepository;
import com.lexho.user_service.dispatch.service.TrackingLifecycleService;
import com.lexho.user_service.dispatch.service.TrackingLifecycleState;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PartnerLiveTrackingControllerTest {

    @Mock
    private CurrentPartnerLocationRepository locationRepository;
    @Mock
    private PartnerShiftRepository shiftRepository;
    @Mock
    private TrackingLifecycleService lifecycleService;

    private PartnerLiveTrackingController controller;

    @BeforeEach
    void setUp() {
        controller = new PartnerLiveTrackingController(
                locationRepository, shiftRepository, lifecycleService,
                new SimpleMeterRegistry(), 1L, 99_000L, 1L, 100.0d);
    }

    @Test
    void snapshotReturnsOfflinePayloadWithoutLocation() {
        when(locationRepository.findByPartnerIdAndDeletedFalse(10L)).thenReturn(Optional.empty());
        when(shiftRepository.findByPartnerIdAndDeletedFalse(10L)).thenReturn(Optional.empty());

        Map<String, Object> payload = invoke("snapshot", 10L);

        assertThat(payload)
                .containsEntry("partnerId", 10L)
                .containsEntry("online", false)
                .containsEntry("trackingState", "OFFLINE")
                .doesNotContainKey("lat");
    }

    @Test
    void snapshotUsesPersistedLifecycleAndInitialMotionDefaults() {
        CurrentPartnerLocationEntity location = location(12.9716, 77.5946, 4L,
                LocalDateTime.of(2026, 6, 12, 10, 0));
        PartnerShiftEntity shift = shift(true, 0);
        when(locationRepository.findByPartnerIdAndDeletedFalse(11L)).thenReturn(Optional.of(location));
        when(shiftRepository.findByPartnerIdAndDeletedFalse(11L)).thenReturn(Optional.of(shift));
        when(lifecycleService.fetchState(11L)).thenReturn(TrackingLifecycleState.ARRIVED);

        Map<String, Object> payload = invoke("snapshot", 11L);

        assertThat(payload)
                .containsEntry("online", true)
                .containsEntry("trackingState", "ARRIVED")
                .containsEntry("lat", 12.9716)
                .containsEntry("lng", 77.5946)
                .containsEntry("seq", 4L)
                .containsEntry("speedKmh", 0.0d)
                .containsEntry("headingDegrees", 0.0d);
        assertThat(payload.get("lastSeenAt")).isEqualTo("2026-06-12T10:00Z");
    }

    @Test
    void consecutiveSnapshotsCalculateSpeedAndHeading() {
        PartnerShiftEntity shift = shift(true, 0);
        CurrentPartnerLocationEntity first = location(12.9716, 77.5946, 1L,
                LocalDateTime.of(2026, 6, 12, 10, 0));
        CurrentPartnerLocationEntity second = location(12.9726, 77.5956, 2L,
                LocalDateTime.of(2026, 6, 12, 10, 1));
        when(shiftRepository.findByPartnerIdAndDeletedFalse(12L)).thenReturn(Optional.of(shift));
        when(locationRepository.findByPartnerIdAndDeletedFalse(12L))
                .thenReturn(Optional.of(first), Optional.of(second));
        when(lifecycleService.fetchState(12L)).thenReturn(TrackingLifecycleState.OFFLINE);

        invoke("snapshot", 12L);
        Map<String, Object> payload = invoke("snapshot", 12L);

        assertThat((Double) payload.get("speedKmh")).isPositive();
        assertThat((Double) payload.get("headingDegrees")).isBetween(0.0d, 360.0d);
        assertThat(payload.get("trackingState")).isEqualTo("EN_ROUTE");
    }

    @Test
    void trackingStateCoversActiveJobMissingLocationAndStationaryBranches() {
        PartnerShiftEntity active = shift(true, 1);
        when(lifecycleService.fetchState(20L)).thenReturn(TrackingLifecycleState.OFFLINE);
        assertThat((String) invoke("resolveTrackingState", active, null, 20L)).isEqualTo("EN_ROUTE");

        PartnerShiftEntity idle = shift(true, 0);
        when(lifecycleService.fetchState(21L)).thenReturn(TrackingLifecycleState.OFFLINE);
        assertThat((String) invoke("resolveTrackingState", idle, null, 21L)).isEqualTo("ASSIGNED");

        CurrentPartnerLocationEntity missingCoordinates = new CurrentPartnerLocationEntity();
        when(lifecycleService.fetchState(22L)).thenReturn(TrackingLifecycleState.OFFLINE);
        assertThat((String) invoke("resolveTrackingState", idle, missingCoordinates, 22L)).isEqualTo("ASSIGNED");
    }

    @Test
    void intervalSelectionClampsEveryLifecycleGroup() {
        when(lifecycleService.sseIntervalMs(TrackingLifecycleState.EN_ROUTE)).thenReturn(1_000L);
        when(lifecycleService.sseIntervalMs(TrackingLifecycleState.ARRIVED)).thenReturn(99_000L);
        when(lifecycleService.sseIntervalMs(TrackingLifecycleState.JOB_STARTED)).thenReturn(8_000L);
        when(lifecycleService.sseIntervalMs(TrackingLifecycleState.ASSIGNED)).thenReturn(2_000L);
        when(lifecycleService.sseIntervalMs(TrackingLifecycleState.OFFLINE)).thenReturn(2_000L);

        assertThat((Long) invoke("resolveSseIntervalMs", Map.of("trackingState", "EN_ROUTE"))).isEqualTo(5_000L);
        assertThat((Long) invoke("resolveSseIntervalMs", Map.of("trackingState", "ARRIVED"))).isEqualTo(15_000L);
        assertThat((Long) invoke("resolveSseIntervalMs", Map.of("trackingState", "JOB_STARTED"))).isEqualTo(8_000L);
        assertThat((Long) invoke("resolveSseIntervalMs", Map.of("trackingState", "ASSIGNED"))).isEqualTo(15_000L);
        assertThat((Long) invoke("resolveSseIntervalMs", Map.of("trackingState", "OFFLINE"))).isEqualTo(10_000L);
    }

    @Test
    void numericConversionHandlesNullNumbersTextAndInvalidText() {
        assertThat((Long) invoke("toLong", (Object) null)).isZero();
        assertThat((Long) invoke("toLong", 7)).isEqualTo(7L);
        assertThat((Long) invoke("toLong", "19")).isEqualTo(19L);
        assertThat((Long) invoke("toLong", "bad")).isZero();
    }

    @SuppressWarnings("unchecked")
    private <T> T invoke(String method, Object... args) {
        return (T) ReflectionTestUtils.invokeMethod(controller, method, args);
    }

    private CurrentPartnerLocationEntity location(double lat, double lng, long seq, LocalDateTime updatedAt) {
        CurrentPartnerLocationEntity location = new CurrentPartnerLocationEntity();
        location.setLatitude(BigDecimal.valueOf(lat));
        location.setLongitude(BigDecimal.valueOf(lng));
        location.setGeohash("tdr1");
        location.setPincode("560001");
        location.setLocationSeq(seq);
        ReflectionTestUtils.setField(location, "updatedAt", updatedAt);
        return location;
    }

    private PartnerShiftEntity shift(boolean online, int activeJobs) {
        PartnerShiftEntity shift = new PartnerShiftEntity();
        shift.setOnline(online);
        shift.setActiveJobsCount(activeJobs);
        return shift;
    }
}

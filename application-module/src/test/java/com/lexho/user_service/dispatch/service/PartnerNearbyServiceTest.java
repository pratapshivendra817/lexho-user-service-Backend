package com.lexho.user_service.dispatch.service;

import com.lexho.user_service.dispatch.dto.PartnerNearbyRequest;
import com.lexho.user_service.dispatch.dto.PartnerNearbyResponse;
import com.lexho.user_service.dispatch.entity.CurrentPartnerLocationEntity;
import com.lexho.user_service.dispatch.entity.PartnerShiftEntity;
import com.lexho.user_service.dispatch.repository.CurrentPartnerLocationRepository;
import com.lexho.user_service.dispatch.repository.PartnerShiftRepository;
import com.lexho.user_service.lexhopartner.entity.PartnerProfile;
import com.lexho.user_service.lexhopartner.repository.PartnerProfileRepository;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.geo.Distance;
import org.springframework.data.geo.GeoResult;
import org.springframework.data.geo.Metrics;
import org.springframework.data.geo.Point;
import org.springframework.data.redis.connection.RedisGeoCommands;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PartnerNearbyServiceTest {

    @Mock
    private PartnerGeoIndexService partnerGeoIndexService;
    @Mock
    private PartnerShiftRepository partnerShiftRepository;
    @Mock
    private PartnerProfileRepository partnerProfileRepository;
    @Mock
    private CurrentPartnerLocationRepository currentPartnerLocationRepository;

    private PartnerNearbyService service;

    @BeforeEach
    void setUp() {
        service = new PartnerNearbyService(
                partnerGeoIndexService,
                partnerShiftRepository,
                partnerProfileRepository,
                currentPartnerLocationRepository,
                new SimpleMeterRegistry(),
                10L
        );
    }

    @Test
    void returnsEmptyWhenRedisAndDatabaseHaveNoCandidates() {
        PartnerNearbyRequest request = request("Cook");
        when(partnerGeoIndexService.findNearby(28.61, 77.20, 5.0, 300)).thenReturn(List.of());
        when(currentPartnerLocationRepository.findNearbyPartnerIds(28.61, 77.20, 5.0, 300))
                .thenReturn(List.of());

        PartnerNearbyResponse response = service.findNearby(request);

        assertTrue(response.partners.isEmpty());
        verify(partnerShiftRepository, never()).findByPartnerIdInAndDeletedFalse(anySet());
    }

    @Test
    void mapsEligibleRedisCandidatesAndSortsByDistanceThenRating() {
        PartnerNearbyRequest request = request(" cook ");
        when(partnerGeoIndexService.findNearby(28.61, 77.20, 5.0, 300)).thenReturn(List.of(
                geo("bad-id", 0.1), geo("2", 2.34567), geo("1", 1.23456)));
        PartnerShiftEntity shift1 = shift(1L, 1, 3, true);
        PartnerShiftEntity shift2 = shift(2L, 0, 3, true);
        PartnerProfile profile1 = profile(1L, "Home Cook", 4.5, true, false);
        PartnerProfile profile2 = profile(2L, "Cook", null, true, false);
        CurrentPartnerLocationEntity location1 = location(1L, 28.61, 77.20, LocalDateTime.now());
        CurrentPartnerLocationEntity location2 = location(2L, 28.62, 77.21, LocalDateTime.now());
        when(partnerShiftRepository.findByPartnerIdInAndDeletedFalse(Set.of(1L, 2L)))
                .thenReturn(List.of(shift1, shift2));
        when(partnerProfileRepository.findAllById(Set.of(1L, 2L))).thenReturn(List.of(profile1, profile2));
        when(currentPartnerLocationRepository.findByPartnerIdInAndDeletedFalse(Set.of(1L, 2L)))
                .thenReturn(List.of(location1, location2));

        PartnerNearbyResponse response = service.findNearby(request);

        assertEquals(2, response.partners.size());
        assertEquals(1L, response.partners.get(0).partnerId);
        assertEquals(1.235, response.partners.get(0).distanceKm);
        assertEquals(4.5, response.partners.get(0).rating);
        assertNotNull(response.partners.get(0).geohash);
        assertNotNull(response.partners.get(0).lastSeenAt);
        assertEquals(0.0, response.partners.get(1).rating);
    }

    @Test
    void databaseFallbackCalculatesDistanceAndFiltersIneligiblePartners() {
        PartnerNearbyRequest request = request("cleaning");
        when(partnerGeoIndexService.findNearby(28.61, 77.20, 5.0, 300)).thenReturn(List.of());
        when(currentPartnerLocationRepository.findNearbyPartnerIds(28.61, 77.20, 5.0, 300))
                .thenReturn(Arrays.asList(null, 3L, 4L, 5L, 6L));
        CurrentPartnerLocationEntity fallback3 = location(3L, 28.611, 77.201, LocalDateTime.now());
        CurrentPartnerLocationEntity fallback4 = location(4L, 28.612, 77.202, LocalDateTime.now());
        CurrentPartnerLocationEntity fallback5 = location(5L, 28.613, 77.203, LocalDateTime.now().minusHours(1));
        CurrentPartnerLocationEntity fallback6 = new CurrentPartnerLocationEntity();
        fallback6.setPartnerId(6L);
        when(currentPartnerLocationRepository.findByPartnerIdAndDeletedFalse(3L)).thenReturn(Optional.of(fallback3));
        when(currentPartnerLocationRepository.findByPartnerIdAndDeletedFalse(4L)).thenReturn(Optional.of(fallback4));
        when(currentPartnerLocationRepository.findByPartnerIdAndDeletedFalse(5L)).thenReturn(Optional.of(fallback5));
        when(currentPartnerLocationRepository.findByPartnerIdAndDeletedFalse(6L)).thenReturn(Optional.of(fallback6));
        when(partnerShiftRepository.findByPartnerIdInAndDeletedFalse(Set.of(3L, 4L, 5L)))
                .thenReturn(List.of(shift(3L, 0, 3, true), shift(4L, 3, 3, true), shift(5L, 0, 3, true)));
        when(partnerProfileRepository.findAllById(Set.of(3L, 4L, 5L))).thenReturn(List.of(
                profile(3L, "Cleaning", 4.8, true, false),
                profile(4L, "Cleaning", 4.9, true, false),
                profile(5L, "Cleaning", 4.7, true, false)));
        when(currentPartnerLocationRepository.findByPartnerIdInAndDeletedFalse(Set.of(3L, 4L, 5L)))
                .thenReturn(List.of(fallback3, fallback4, fallback5));

        PartnerNearbyResponse response = service.findNearby(request);

        assertEquals(1, response.partners.size());
        assertEquals(3L, response.partners.get(0).partnerId);
        assertTrue(response.partners.get(0).distanceKm < 1.0);
        verify(partnerGeoIndexService).removePartner(5L);
    }

    private PartnerNearbyRequest request(String serviceType) {
        PartnerNearbyRequest request = new PartnerNearbyRequest();
        request.lat = 28.61;
        request.lng = 77.20;
        request.radiusKm = 5.0;
        request.serviceType = serviceType;
        return request;
    }

    private GeoResult<RedisGeoCommands.GeoLocation<String>> geo(String id, double distance) {
        RedisGeoCommands.GeoLocation<String> location =
                new RedisGeoCommands.GeoLocation<>(id, new Point(77.20, 28.61));
        return new GeoResult<>(location, new Distance(distance, Metrics.KILOMETERS));
    }

    private PartnerShiftEntity shift(Long id, int active, int max, boolean online) {
        PartnerShiftEntity shift = new PartnerShiftEntity();
        shift.setPartnerId(id);
        shift.setActiveJobsCount(active);
        shift.setMaxActiveJobs(max);
        shift.setOnline(online);
        return shift;
    }

    private PartnerProfile profile(Long id, String category, Double rating, boolean online, boolean deleted) {
        PartnerProfile profile = new PartnerProfile();
        profile.setId(id);
        profile.setServiceCategory(category);
        profile.setRating(rating);
        profile.setOnline(online);
        profile.setDeleted(deleted);
        return profile;
    }

    private CurrentPartnerLocationEntity location(Long id, double lat, double lng, LocalDateTime updatedAt) {
        CurrentPartnerLocationEntity location = new CurrentPartnerLocationEntity();
        location.setPartnerId(id);
        location.setLatitude(BigDecimal.valueOf(lat));
        location.setLongitude(BigDecimal.valueOf(lng));
        ReflectionTestUtils.setField(location, "updatedAt", updatedAt);
        return location;
    }
}

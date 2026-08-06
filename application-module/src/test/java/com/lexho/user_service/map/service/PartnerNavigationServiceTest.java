package com.lexho.user_service.map.service;

import com.lexho.booking.entity.Order;
import com.lexho.booking.repository.OrderRepository;
import com.lexho.user_service.customer.location.entity.CustomerCurrentLocation;
import com.lexho.user_service.customer.location.repository.CustomerAddressRepository;
import com.lexho.user_service.customer.location.repository.CustomerCurrentLocationRepository;
import com.lexho.user_service.dispatch.entity.CurrentPartnerLocationEntity;
import com.lexho.user_service.dispatch.repository.CurrentPartnerLocationRepository;
import com.lexho.user_service.exception.BadRequestException;
import com.lexho.user_service.lexho.repository.LexhoAddressRepository;
import com.lexho.user_service.map.client.MapboxDirectionsClient;
import com.lexho.user_service.map.config.MapboxProperties;
import com.lexho.user_service.map.dto.NavigationRouteRequest;
import com.lexho.user_service.map.dto.NavigationRouteResponse;
import com.lexho.user_service.partnerrequest.entity.PartnerServiceRequestEntity;
import com.lexho.user_service.partnerrequest.repository.PartnerServiceRequestRepository;
import com.lexho.user_service.security.SecurityUtils;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PartnerNavigationServiceTest {

    private final MapboxDirectionsClient directionsClient = mock(MapboxDirectionsClient.class);
    private final NavigationRouteCacheService cacheService = mock(NavigationRouteCacheService.class);
    private final MapboxProperties mapboxProperties = new MapboxProperties();
    private final OrderRepository orderRepository = mock(OrderRepository.class);
    private final CurrentPartnerLocationRepository partnerLocationRepository = mock(CurrentPartnerLocationRepository.class);
    private final PartnerServiceRequestRepository partnerServiceRequestRepository = mock(PartnerServiceRequestRepository.class);
    private final CustomerCurrentLocationRepository customerCurrentLocationRepository = mock(CustomerCurrentLocationRepository.class);
    private final CustomerAddressRepository customerAddressRepository = mock(CustomerAddressRepository.class);
    private final LexhoAddressRepository lexhoAddressRepository = mock(LexhoAddressRepository.class);
    private final NavigationEtaTrackingRegistry navigationEtaTrackingRegistry = mock(NavigationEtaTrackingRegistry.class);
    private final SecurityUtils securityUtils = mock(SecurityUtils.class);

    private final PartnerNavigationService service = new PartnerNavigationService(
            directionsClient,
            cacheService,
            mapboxProperties,
            orderRepository,
            partnerLocationRepository,
            partnerServiceRequestRepository,
            customerCurrentLocationRepository,
            customerAddressRepository,
            lexhoAddressRepository,
            navigationEtaTrackingRegistry,
            securityUtils
    );

    @Test
    void routeUsesCacheBeforeCallingMapbox() {
        when(cacheService.get("navigation:route:2861000:7720000:2862000:7721000:driving-traffic"))
                .thenReturn(Optional.of(new NavigationRouteCacheService.CachedRoute(4.2, 12, "cached-polyline", "MAPBOX", 28.61, 77.20, 28.62, 77.21, true, 1, LocalDateTime.now())));

        NavigationRouteResponse response = service.route(new NavigationRouteRequest(28.61, 77.20, 28.62, 77.21));

        assertThat(response.distanceKm()).isEqualTo(4.2);
        assertThat(response.durationMinutes()).isEqualTo(12);
        assertThat(response.bookingId()).isNull();
        assertThat(response.polyline()).isEqualTo("cached-polyline");
        assertThat(response.cacheHit()).isTrue();
        verify(directionsClient, never()).fetchBestRoute(any(Double.class), any(Double.class), any(Double.class), any(Double.class));
    }

    @Test
    void routeFallsBackToMapboxAndCachesResponse() {
        when(cacheService.get(any())).thenReturn(Optional.empty());
        when(directionsClient.fetchBestRoute(28.61, 77.20, 28.62, 77.21))
                .thenReturn(new MapboxDirectionsClient.RouteResult(4.2, 12, "route-polyline", "MAPBOX", true, 2));

        NavigationRouteResponse response = service.route(new NavigationRouteRequest(28.61, 77.20, 28.62, 77.21));

        assertThat(response.durationMinutes()).isEqualTo(12);
        verify(cacheService).put(
                eq("navigation:route:2861000:7720000:2862000:7721000:driving-traffic"),
                eq(new NavigationRouteCacheService.CachedRoute(4.2, 12, "route-polyline", "MAPBOX", 28.61, 77.20, 28.62, 77.21, true, 2, response.routeComputedAt())),
                eq(Duration.ofSeconds(30))
        );
        assertThat(response.routeRecalculated()).isTrue();
        assertThat(response.alternativesConsidered()).isEqualTo(2);
    }

    @Test
    void etaForBookingUsesPartnerRequestCoordinates() {
        mockCustomer(17L);
        Order order = new Order();
        order.setId(88L);
        order.setUserId(17L);
        order.setAssignedPartnerId(23L);
        when(orderRepository.findById(88L)).thenReturn(Optional.of(order));

        CurrentPartnerLocationEntity partnerLocation = new CurrentPartnerLocationEntity();
        partnerLocation.setPartnerId(23L);
        partnerLocation.setLatitude(BigDecimal.valueOf(28.61));
        partnerLocation.setLongitude(BigDecimal.valueOf(77.20));
        when(partnerLocationRepository.findByPartnerIdAndDeletedFalse(23L)).thenReturn(Optional.of(partnerLocation));

        PartnerServiceRequestEntity requestEntity = new PartnerServiceRequestEntity();
        requestEntity.setCartId(88L);
        requestEntity.setLatitude(BigDecimal.valueOf(28.62));
        requestEntity.setLongitude(BigDecimal.valueOf(77.21));
        ReflectionTestUtils.setField(requestEntity, "createdAt", LocalDateTime.now());
        when(partnerServiceRequestRepository.findByCartId(88L)).thenReturn(List.of(requestEntity));

        when(cacheService.get(any())).thenReturn(Optional.empty());
        when(directionsClient.fetchBestRoute(28.61, 77.20, 28.62, 77.21))
                .thenReturn(new MapboxDirectionsClient.RouteResult(4.2, 12, "polyline", "MAPBOX", true, 2));

        NavigationRouteResponse response = service.etaForBooking(88L);

        assertThat(response.bookingId()).isEqualTo(88L);
        assertThat(response.distanceKm()).isEqualTo(4.2);
        assertThat(response.liveTrackingEnabled()).isTrue();
        assertThat(response.partnerLatitude()).isEqualTo(28.61);
        verify(navigationEtaTrackingRegistry).register(eq(88L), any(Duration.class));
        verify(customerCurrentLocationRepository, never()).findByCustomerId(any());
    }

    @Test
    void etaForBookingFallsBackToCustomerCurrentLocation() {
        mockCustomer(19L);
        Order order = new Order();
        order.setId(90L);
        order.setUserId(19L);
        order.setAssignedPartnerId(24L);
        when(orderRepository.findById(90L)).thenReturn(Optional.of(order));

        CurrentPartnerLocationEntity partnerLocation = new CurrentPartnerLocationEntity();
        partnerLocation.setPartnerId(24L);
        partnerLocation.setLatitude(BigDecimal.valueOf(28.60));
        partnerLocation.setLongitude(BigDecimal.valueOf(77.18));
        when(partnerLocationRepository.findByPartnerIdAndDeletedFalse(24L)).thenReturn(Optional.of(partnerLocation));
        when(partnerServiceRequestRepository.findByCartId(90L)).thenReturn(List.of());

        CustomerCurrentLocation customerLocation = new CustomerCurrentLocation();
        customerLocation.setCustomerId(19L);
        customerLocation.setLatitude(BigDecimal.valueOf(28.65));
        customerLocation.setLongitude(BigDecimal.valueOf(77.25));
        when(customerCurrentLocationRepository.findByCustomerId(19L)).thenReturn(Optional.of(customerLocation));
        when(cacheService.get(any())).thenReturn(Optional.empty());
        when(directionsClient.fetchBestRoute(28.60, 77.18, 28.65, 77.25))
                .thenReturn(new MapboxDirectionsClient.RouteResult(8.8, 20, "polyline-2", "MAPBOX", true, 1));

        NavigationRouteResponse response = service.etaForBooking(90L);

        assertThat(response.durationMinutes()).isEqualTo(20);
    }

    @Test
    void etaForBookingRecalculatesWhenPartnerHasDeviatedFromCachedPath() {
        mockPartner(25L);
        Order order = new Order();
        order.setId(92L);
        order.setUserId(20L);
        order.setAssignedPartnerId(25L);
        when(orderRepository.findById(92L)).thenReturn(Optional.of(order));

        CurrentPartnerLocationEntity partnerLocation = new CurrentPartnerLocationEntity();
        partnerLocation.setPartnerId(25L);
        partnerLocation.setLatitude(BigDecimal.valueOf(28.70));
        partnerLocation.setLongitude(BigDecimal.valueOf(77.30));
        when(partnerLocationRepository.findByPartnerIdAndDeletedFalse(25L)).thenReturn(Optional.of(partnerLocation));
        when(partnerServiceRequestRepository.findByCartId(92L)).thenReturn(List.of());

        CustomerCurrentLocation customerLocation = new CustomerCurrentLocation();
        customerLocation.setCustomerId(20L);
        customerLocation.setLatitude(BigDecimal.valueOf(28.62));
        customerLocation.setLongitude(BigDecimal.valueOf(77.21));
        when(customerCurrentLocationRepository.findByCustomerId(20L)).thenReturn(Optional.of(customerLocation));
        when(cacheService.get("navigation:route:booking:92:driving-traffic"))
                .thenReturn(Optional.of(new NavigationRouteCacheService.CachedRoute(
                        4.2, 12, "old-polyline", "MAPBOX",
                        28.61, 77.20, 28.62, 77.21, true, 1, LocalDateTime.now().minusMinutes(1)
                )));
        when(cacheService.get("navigation:route:2870000:7730000:2862000:7721000:driving-traffic"))
                .thenReturn(Optional.empty());
        when(directionsClient.fetchBestRoute(28.70, 77.30, 28.62, 77.21))
                .thenReturn(new MapboxDirectionsClient.RouteResult(14.4, 31, "new-polyline", "MAPBOX", true, 3));

        NavigationRouteResponse response = service.etaForBooking(92L);

        assertThat(response.routeDeviationDetected()).isTrue();
        assertThat(response.routeRecalculated()).isTrue();
        assertThat(response.polyline()).isEqualTo("new-polyline");
    }

    @Test
    void etaForBookingRejectsUnassignedPartner() {
        mockCustomer(19L);
        Order order = new Order();
        order.setId(91L);
        order.setUserId(19L);
        when(orderRepository.findById(91L)).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> service.etaForBooking(91L))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("No partner assigned");
    }

    @Test
    void etaForBookingRejectsForeignCustomer() {
        mockCustomer(99L);
        Order order = new Order();
        order.setId(93L);
        order.setUserId(19L);
        order.setAssignedPartnerId(24L);
        when(orderRepository.findById(93L)).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> service.etaForBooking(93L))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("not allowed");
    }

    @Test
    void etaForBookingRejectsForeignPartner() {
        mockPartner(77L);
        Order order = new Order();
        order.setId(94L);
        order.setUserId(19L);
        order.setAssignedPartnerId(24L);
        when(orderRepository.findById(94L)).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> service.etaForBooking(94L))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("not allowed");
    }

    @Test
    void etaForBookingAllowsAdmin() {
        when(securityUtils.getCurrentRole()).thenReturn(Optional.of("ADMIN"));
        Order order = new Order();
        order.setId(95L);
        order.setUserId(19L);
        order.setAssignedPartnerId(24L);
        when(orderRepository.findById(95L)).thenReturn(Optional.of(order));

        CurrentPartnerLocationEntity partnerLocation = new CurrentPartnerLocationEntity();
        partnerLocation.setPartnerId(24L);
        partnerLocation.setLatitude(BigDecimal.valueOf(28.60));
        partnerLocation.setLongitude(BigDecimal.valueOf(77.18));
        when(partnerLocationRepository.findByPartnerIdAndDeletedFalse(24L)).thenReturn(Optional.of(partnerLocation));
        when(partnerServiceRequestRepository.findByCartId(95L)).thenReturn(List.of());

        CustomerCurrentLocation customerLocation = new CustomerCurrentLocation();
        customerLocation.setCustomerId(19L);
        customerLocation.setLatitude(BigDecimal.valueOf(28.65));
        customerLocation.setLongitude(BigDecimal.valueOf(77.25));
        when(customerCurrentLocationRepository.findByCustomerId(19L)).thenReturn(Optional.of(customerLocation));
        when(cacheService.get(any())).thenReturn(Optional.empty());
        when(directionsClient.fetchBestRoute(28.60, 77.18, 28.65, 77.25))
                .thenReturn(new MapboxDirectionsClient.RouteResult(8.8, 20, "polyline-2", "MAPBOX", true, 1));

        NavigationRouteResponse response = service.etaForBooking(95L);

        assertThat(response.bookingId()).isEqualTo(95L);
    }

    private void mockCustomer(Long userId) {
        when(securityUtils.getCurrentRole()).thenReturn(Optional.of("CUSTOMER"));
        when(securityUtils.getCurrentUserId()).thenReturn(Optional.of(userId));
    }

    private void mockPartner(Long userId) {
        when(securityUtils.getCurrentRole()).thenReturn(Optional.of("PARTNER"));
        when(securityUtils.getCurrentUserId()).thenReturn(Optional.of(userId));
    }
}

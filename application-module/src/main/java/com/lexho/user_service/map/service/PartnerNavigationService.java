package com.lexho.user_service.map.service;

import com.lexho.booking.entity.Order;
import com.lexho.booking.repository.OrderRepository;
import com.lexho.user_service.customer.location.entity.CustomerCurrentLocation;
import com.lexho.user_service.customer.location.repository.CustomerAddressRepository;
import com.lexho.user_service.customer.location.repository.CustomerCurrentLocationRepository;
import com.lexho.user_service.dispatch.entity.CurrentPartnerLocationEntity;
import com.lexho.user_service.dispatch.repository.CurrentPartnerLocationRepository;
import com.lexho.user_service.exception.BadRequestException;
import com.lexho.user_service.exception.ResourceNotFoundException;
import com.lexho.user_service.lexho.repository.LexhoAddressRepository;
import com.lexho.user_service.map.client.MapboxDirectionsClient;
import com.lexho.user_service.map.config.MapboxProperties;
import com.lexho.user_service.map.dto.NavigationRouteRequest;
import com.lexho.user_service.map.dto.NavigationRouteResponse;
import com.lexho.user_service.partnerrequest.entity.PartnerServiceRequestEntity;
import com.lexho.user_service.partnerrequest.repository.PartnerServiceRequestRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@Service
public class PartnerNavigationService {

    private final MapboxDirectionsClient mapboxDirectionsClient;
    private final NavigationRouteCacheService navigationRouteCacheService;
    private final MapboxProperties mapboxProperties;
    private final OrderRepository orderRepository;
    private final CurrentPartnerLocationRepository currentPartnerLocationRepository;
    private final PartnerServiceRequestRepository partnerServiceRequestRepository;
    private final CustomerCurrentLocationRepository customerCurrentLocationRepository;
    private final CustomerAddressRepository customerAddressRepository;
    private final LexhoAddressRepository lexhoAddressRepository;
    private final NavigationEtaTrackingRegistry navigationEtaTrackingRegistry;

    public PartnerNavigationService(MapboxDirectionsClient mapboxDirectionsClient,
                                    NavigationRouteCacheService navigationRouteCacheService,
                                    MapboxProperties mapboxProperties,
                                    OrderRepository orderRepository,
                                    CurrentPartnerLocationRepository currentPartnerLocationRepository,
                                    PartnerServiceRequestRepository partnerServiceRequestRepository,
                                    CustomerCurrentLocationRepository customerCurrentLocationRepository,
                                    CustomerAddressRepository customerAddressRepository,
                                    LexhoAddressRepository lexhoAddressRepository,
                                    NavigationEtaTrackingRegistry navigationEtaTrackingRegistry) {
        this.mapboxDirectionsClient = mapboxDirectionsClient;
        this.navigationRouteCacheService = navigationRouteCacheService;
        this.mapboxProperties = mapboxProperties;
        this.orderRepository = orderRepository;
        this.currentPartnerLocationRepository = currentPartnerLocationRepository;
        this.partnerServiceRequestRepository = partnerServiceRequestRepository;
        this.customerCurrentLocationRepository = customerCurrentLocationRepository;
        this.customerAddressRepository = customerAddressRepository;
        this.lexhoAddressRepository = lexhoAddressRepository;
        this.navigationEtaTrackingRegistry = navigationEtaTrackingRegistry;
    }

    public NavigationRouteResponse route(NavigationRouteRequest request) {
        return buildRoute(RouteContext.manual(
                request.partnerLatitude(),
                request.partnerLongitude(),
                request.customerLatitude(),
                request.customerLongitude())
        );
    }

    public NavigationRouteResponse etaForBooking(Long bookingId) {
        navigationEtaTrackingRegistry.register(
                bookingId,
                Duration.ofSeconds(mapboxProperties.getLiveEtaTrackingTtlSeconds())
        );
        return etaForBookingSnapshot(bookingId);
    }

    public NavigationRouteResponse etaForBookingSnapshot(Long bookingId) {
        Order order = orderRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found"));
        if (order.getAssignedPartnerId() == null) {
            throw new BadRequestException("No partner assigned for this booking");
        }

        CurrentPartnerLocationEntity partnerLocation = currentPartnerLocationRepository
                .findByPartnerIdAndDeletedFalse(order.getAssignedPartnerId())
                .orElseThrow(() -> new ResourceNotFoundException("Partner current location not found"));

        Coordinates destination = resolveCustomerCoordinates(order)
                .orElseThrow(() -> new ResourceNotFoundException("Customer destination location not found"));

        return buildRoute(RouteContext.booking(
                bookingId,
                toDouble(partnerLocation.getLatitude()),
                toDouble(partnerLocation.getLongitude()),
                destination.latitude(),
                destination.longitude(),
                partnerLocation.getUpdatedAt()
        ));
    }

    private NavigationRouteResponse buildRoute(RouteContext context) {
        String coordinateCacheKey = coordinateCacheKey(
                context.originLatitude(),
                context.originLongitude(),
                context.destinationLatitude(),
                context.destinationLongitude());
        String bookingCacheKey = context.bookingId() == null ? null : bookingCacheKey(context.bookingId());

        NavigationRouteCacheService.CachedRoute coordinateCachedRoute =
                navigationRouteCacheService.get(coordinateCacheKey).orElse(null);
        NavigationRouteCacheService.CachedRoute bookingCachedRoute =
                bookingCacheKey == null ? null : navigationRouteCacheService.get(bookingCacheKey).orElse(null);

        NavigationRouteCacheService.CachedRoute cachedRoute = bookingCachedRoute != null ? bookingCachedRoute : coordinateCachedRoute;
        RouteDiagnostics diagnostics = computeDiagnostics(context, cachedRoute);
        boolean shouldRecalculate = cachedRoute == null
                || diagnostics.deviationDetected()
                || diagnostics.significantMovement()
                || diagnostics.etaWorsened();
        if (!shouldRecalculate && cachedRoute != null) {
            return toResponse(cachedRoute, context, true, false, diagnostics);
        }

        MapboxDirectionsClient.RouteResult routeResult = mapboxDirectionsClient.fetchBestRoute(
                context.originLatitude(),
                context.originLongitude(),
                context.destinationLatitude(),
                context.destinationLongitude());
        if (routeResult == null) {
            routeResult = haversineFallback(
                    context.originLatitude(),
                    context.originLongitude(),
                    context.destinationLatitude(),
                    context.destinationLongitude());
        }

        LocalDateTime computedAt = LocalDateTime.now();
        NavigationRouteCacheService.CachedRoute routeToCache = new NavigationRouteCacheService.CachedRoute(
                routeResult.distanceKm(),
                routeResult.durationMinutes(),
                routeResult.polyline(),
                routeResult.provider(),
                context.originLatitude(),
                context.originLongitude(),
                context.destinationLatitude(),
                context.destinationLongitude(),
                routeResult.trafficAware(),
                routeResult.alternativesConsidered(),
                computedAt
        );
        navigationRouteCacheService.put(
                coordinateCacheKey,
                routeToCache,
                Duration.ofSeconds(mapboxProperties.getRouteCacheTtlSeconds())
        );
        if (bookingCacheKey != null) {
            navigationRouteCacheService.put(
                    bookingCacheKey,
                    routeToCache,
                    Duration.ofSeconds(mapboxProperties.getRouteCacheTtlSeconds())
            );
        }
        return toResponse(routeToCache, context, false, true, diagnostics);
    }

    private Optional<Coordinates> resolveCustomerCoordinates(Order order) {
        Optional<Coordinates> fromPartnerRequest = partnerServiceRequestRepository.findByCartId(order.getId()).stream()
                .filter(request -> request.getLatitude() != null && request.getLongitude() != null)
                .max(Comparator.comparing(PartnerServiceRequestEntity::getCreatedAt))
                .map(request -> new Coordinates(
                        toDouble(request.getLatitude()),
                        toDouble(request.getLongitude())
                ));
        if (fromPartnerRequest.isPresent()) {
            return fromPartnerRequest;
        }

        Optional<Coordinates> fromCurrentLocation = customerCurrentLocationRepository.findByCustomerId(order.getUserId())
                .map(location -> new Coordinates(
                        toDouble(location.getLatitude()),
                        toDouble(location.getLongitude())
                ));
        if (fromCurrentLocation.isPresent()) {
            return fromCurrentLocation;
        }

        Optional<Coordinates> fromCustomerAddress = customerAddressRepository
                .findFirstByCustomerIdAndIsDeletedFalseOrderByIsDefaultDescCreatedAtAsc(order.getUserId())
                .filter(address -> address.getLatitude() != null && address.getLongitude() != null)
                .map(address -> new Coordinates(
                        toDouble(address.getLatitude()),
                        toDouble(address.getLongitude())
                ));
        if (fromCustomerAddress.isPresent()) {
            return fromCustomerAddress;
        }

        return lexhoAddressRepository.findByUserIdAndIsDeletedFalse(order.getUserId()).stream()
                .filter(address -> address.getLatitude() != null && address.getLongitude() != null)
                .findFirst()
                .map(address -> new Coordinates(
                        toDouble(address.getLatitude()),
                        toDouble(address.getLongitude())
                ));
    }

    private NavigationRouteResponse toResponse(NavigationRouteCacheService.CachedRoute route,
                                               RouteContext context,
                                               boolean cacheHit,
                                               boolean routeRecalculated,
                                               RouteDiagnostics diagnostics) {
        Integer durationMinutes = route.durationMinutes();
        return new NavigationRouteResponse(
                context.bookingId(),
                route.distanceKm(),
                durationMinutes,
                route.polyline(),
                durationMinutes == null ? null : LocalDateTime.now().plusMinutes(durationMinutes),
                route.trafficAware(),
                route.provider(),
                cacheHit,
                routeRecalculated,
                diagnostics.deviationDetected(),
                diagnostics.deviationMeters(),
                route.alternativesConsidered(),
                context.bookingId() != null,
                context.originLatitude(),
                context.originLongitude(),
                context.partnerLocationUpdatedAt(),
                context.destinationLatitude(),
                context.destinationLongitude(),
                route.computedAt()
        );
    }

    private MapboxDirectionsClient.RouteResult haversineFallback(Double originLatitude,
                                                                 Double originLongitude,
                                                                 Double destinationLatitude,
                                                                 Double destinationLongitude) {
        double distanceKm = haversineKm(originLatitude, originLongitude, destinationLatitude, destinationLongitude);
        int durationMinutes = Math.max(1, (int) Math.ceil((distanceKm / 28d) * 60d));
        return new MapboxDirectionsClient.RouteResult(
                round2(distanceKm),
                durationMinutes,
                null,
                "HAVERSINE_FALLBACK",
                false,
                1
        );
    }

    private String coordinateCacheKey(Double originLatitude,
                                      Double originLongitude,
                                      Double destinationLatitude,
                                      Double destinationLongitude) {
        return "navigation:route:"
                + round(originLatitude) + ':'
                + round(originLongitude) + ':'
                + round(destinationLatitude) + ':'
                + round(destinationLongitude) + ':'
                + mapboxProperties.getNavigationProfile();
    }

    private String bookingCacheKey(Long bookingId) {
        return "navigation:route:booking:" + bookingId + ':' + mapboxProperties.getNavigationProfile();
    }

    private RouteDiagnostics computeDiagnostics(RouteContext context, NavigationRouteCacheService.CachedRoute cachedRoute) {
        if (cachedRoute == null
                || cachedRoute.originLatitude() == null
                || cachedRoute.originLongitude() == null
                || cachedRoute.destinationLatitude() == null
                || cachedRoute.destinationLongitude() == null) {
            return RouteDiagnostics.empty();
        }

        double movementMeters = haversineMeters(
                cachedRoute.originLatitude(),
                cachedRoute.originLongitude(),
                context.originLatitude(),
                context.originLongitude()
        );
        double deviationMeters = distanceToSegmentMeters(
                context.originLatitude(),
                context.originLongitude(),
                cachedRoute.originLatitude(),
                cachedRoute.originLongitude(),
                cachedRoute.destinationLatitude(),
                cachedRoute.destinationLongitude()
        );
        double cachedDistanceToDestination = haversineMeters(
                cachedRoute.originLatitude(),
                cachedRoute.originLongitude(),
                cachedRoute.destinationLatitude(),
                cachedRoute.destinationLongitude()
        );
        double currentDistanceToDestination = haversineMeters(
                context.originLatitude(),
                context.originLongitude(),
                context.destinationLatitude(),
                context.destinationLongitude()
        );
        boolean movedAwayFromDestination = currentDistanceToDestination > cachedDistanceToDestination + 100.0d;
        boolean deviationDetected = deviationMeters >= mapboxProperties.getRouteDeviationThresholdMeters()
                && movedAwayFromDestination;
        boolean significantMovement = movementMeters >= mapboxProperties.getRouteRecalculationMovementMeters();
        boolean etaWorsened = cachedRoute.durationMinutes() != null
                && currentDistanceToDestination > cachedDistanceToDestination
                && movementMeters >= mapboxProperties.getRouteRecalculationMovementMeters()
                && cachedRoute.durationMinutes() >= mapboxProperties.getEtaWorseningThresholdMinutes();
        return new RouteDiagnostics(round2(deviationMeters), deviationDetected, significantMovement, etaWorsened);
    }

    private long round(Double value) {
        return Math.round(value * 100000d);
    }

    private double haversineKm(double lat1, double lon1, double lat2, double lon2) {
        double earthRadiusKm = 6371.0;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return earthRadiusKm * c;
    }

    private double haversineMeters(double lat1, double lon1, double lat2, double lon2) {
        return haversineKm(lat1, lon1, lat2, lon2) * 1000d;
    }

    private double distanceToSegmentMeters(double pointLat,
                                           double pointLon,
                                           double startLat,
                                           double startLon,
                                           double endLat,
                                           double endLon) {
        double meanLatRadians = Math.toRadians((startLat + endLat + pointLat) / 3.0d);
        double metersPerDegLat = 111_320.0d;
        double metersPerDegLon = Math.cos(meanLatRadians) * 111_320.0d;

        double px = pointLon * metersPerDegLon;
        double py = pointLat * metersPerDegLat;
        double sx = startLon * metersPerDegLon;
        double sy = startLat * metersPerDegLat;
        double ex = endLon * metersPerDegLon;
        double ey = endLat * metersPerDegLat;

        double dx = ex - sx;
        double dy = ey - sy;
        if (dx == 0.0d && dy == 0.0d) {
            return Math.hypot(px - sx, py - sy);
        }
        double t = ((px - sx) * dx + (py - sy) * dy) / (dx * dx + dy * dy);
        double clampedT = Math.max(0.0d, Math.min(1.0d, t));
        double closestX = sx + clampedT * dx;
        double closestY = sy + clampedT * dy;
        return Math.hypot(px - closestX, py - closestY);
    }

    private Double round2(double value) {
        return Math.round(value * 100d) / 100d;
    }

    private Double toDouble(BigDecimal value) {
        return value == null ? null : value.doubleValue();
    }

    private record Coordinates(Double latitude, Double longitude) {
    }

    private record RouteContext(Long bookingId,
                                Double originLatitude,
                                Double originLongitude,
                                Double destinationLatitude,
                                Double destinationLongitude,
                                LocalDateTime partnerLocationUpdatedAt) {

        private static RouteContext manual(Double originLatitude,
                                           Double originLongitude,
                                           Double destinationLatitude,
                                           Double destinationLongitude) {
            return new RouteContext(null, originLatitude, originLongitude, destinationLatitude, destinationLongitude, null);
        }

        private static RouteContext booking(Long bookingId,
                                            Double originLatitude,
                                            Double originLongitude,
                                            Double destinationLatitude,
                                            Double destinationLongitude,
                                            LocalDateTime partnerLocationUpdatedAt) {
            return new RouteContext(
                    bookingId,
                    originLatitude,
                    originLongitude,
                    destinationLatitude,
                    destinationLongitude,
                    partnerLocationUpdatedAt
            );
        }
    }

    private record RouteDiagnostics(Double deviationMeters,
                                    boolean deviationDetected,
                                    boolean significantMovement,
                                    boolean etaWorsened) {
        private static RouteDiagnostics empty() {
            return new RouteDiagnostics(null, false, false, false);
        }
    }
}

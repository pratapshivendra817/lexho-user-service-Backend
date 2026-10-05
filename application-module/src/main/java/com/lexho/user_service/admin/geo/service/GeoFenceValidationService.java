package com.lexho.user_service.admin.geo.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lexho.user_service.admin.geo.entity.ZoneEntity;
import com.lexho.user_service.admin.geo.repository.ZoneRepository;
import com.lexho.user_service.admin.serviceablearea.entity.ServiceableAreaEntity;
import com.lexho.user_service.admin.serviceablearea.entity.ServiceabilityRuleEntity;
import com.lexho.user_service.admin.serviceablearea.repository.ServiceableAreaRepository;
import com.lexho.user_service.admin.serviceablearea.repository.ServiceabilityRuleRepository;
import com.lexho.user_service.booking.exception.BookingValidationException;
import com.lexho.user_service.lexho.entity.LexhoAddress;
import com.lexho.user_service.util.GeoDistanceCalculator;
import com.lexho.user_service.util.LexhoConstants;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Service
public class GeoFenceValidationService {

    private final ServiceableAreaRepository serviceableAreaRepository;
    private final ServiceabilityRuleRepository serviceabilityRuleRepository;
    private final ZoneRepository zoneRepository;
    private final ObjectMapper objectMapper;

    public GeoFenceValidationService(ServiceableAreaRepository serviceableAreaRepository,
                                     ServiceabilityRuleRepository serviceabilityRuleRepository,
                                     ZoneRepository zoneRepository,
                                     ObjectMapper objectMapper) {
        this.serviceableAreaRepository = serviceableAreaRepository;
        this.serviceabilityRuleRepository = serviceabilityRuleRepository;
        this.zoneRepository = zoneRepository;
        this.objectMapper = objectMapper;
    }

    public GeoResolution resolveServiceability(String pincode, Double lat, Double lng, String city) {
        String normalizedPincode = clean(pincode);
        String normalizedCity = clean(city);

        if (normalizedPincode == null && normalizedCity == null && (lat == null || lng == null)) {
            return GeoResolution.notServiceable("MISSING_LOCATION_INPUT");
        }

        List<RuleArea> candidateAreas = candidateAreas(normalizedPincode, normalizedCity, lat, lng);
        if (candidateAreas.isEmpty()) {
            return GeoResolution.notServiceable("NO_MATCHING_SERVICEABLE_AREA");
        }

        AreaMatch match = findMatchingArea(candidateAreas, normalizedPincode, lat, lng);

        if (!match.matched()) {
            if (match.pincodeMatchedAreaExists()) {
                return GeoResolution.notServiceable("PARTIAL_AREA_UNSUPPORTED");
            }
            return GeoResolution.notServiceable(match.rejectReason() == null ? "OUTSIDE_GEOFENCE" : match.rejectReason());
        }

        return GeoResolution.serviceable(
                match.cityId(),
                match.zoneId(),
                clean(match.area().pincode())
        );
    }

    private List<RuleArea> candidateAreas(String normalizedPincode, String normalizedCity, Double lat, Double lng) {
        List<RuleArea> candidateAreas = candidateAreasByPincode(normalizedPincode);
        if (candidateAreas.isEmpty()) {
            candidateAreas.addAll(candidateAreasByCity(normalizedCity));
        }
        if (candidateAreas.isEmpty() && lat != null && lng != null) {
            candidateAreas.addAll(candidateAreasByAnyActiveRuleOrArea());
        }
        return candidateAreas;
    }

    private List<RuleArea> candidateAreasByPincode(String normalizedPincode) {
        if (normalizedPincode == null) {
            return new ArrayList<>();
        }
        List<RuleArea> candidateAreas = fromRules(
                serviceabilityRuleRepository.findByPincodeAndActiveTrueAndDeletedFalse(normalizedPincode));
        return candidateAreas.isEmpty()
                ? fromAreas(serviceableAreaRepository.findByPincodeAndActiveTrueAndDeletedFalse(normalizedPincode))
                : candidateAreas;
    }

    private List<RuleArea> candidateAreasByCity(String normalizedCity) {
        if (normalizedCity == null) {
            return new ArrayList<>();
        }
        List<RuleArea> candidateAreas = fromRules(
                serviceabilityRuleRepository.findByCityIgnoreCaseAndActiveTrueAndDeletedFalse(normalizedCity));
        return candidateAreas.isEmpty()
                ? fromAreas(serviceableAreaRepository.findByCityIgnoreCaseAndActiveTrueAndDeletedFalse(normalizedCity))
                : candidateAreas;
    }

    private List<RuleArea> candidateAreasByAnyActiveRuleOrArea() {
        List<RuleArea> candidateAreas = fromRules(serviceabilityRuleRepository.findByActiveTrueAndDeletedFalse());
        return candidateAreas.isEmpty()
                ? fromAreas(serviceableAreaRepository.findByActiveTrueAndDeletedFalse())
                : candidateAreas;
    }

    private AreaMatch findMatchingArea(List<RuleArea> candidateAreas, String normalizedPincode, Double lat, Double lng) {
        String rejectReason = "OUTSIDE_GEOFENCE";
        boolean pincodeMatchedAreaExists = false;

        for (RuleArea area : candidateAreas) {
            pincodeMatchedAreaExists = pincodeMatchedAreaExists
                    || pincodeMatches(normalizedPincode, area);
            AreaFence fence = areaFence(area);
            if (fence.empty()) {
                return AreaMatch.matched(area, fence.zoneId(), fence.cityId(), pincodeMatchedAreaExists);
            }
            if (lat == null || lng == null) {
                rejectReason = "COORDINATES_REQUIRED_FOR_GEOFENCE";
                continue;
            }
            if (fence.contains(lat, lng)) {
                return AreaMatch.matched(area, fence.zoneId(), fence.cityId(), pincodeMatchedAreaExists);
            }
        }

        return AreaMatch.rejected(rejectReason, pincodeMatchedAreaExists);
    }

    private boolean pincodeMatches(String normalizedPincode, RuleArea area) {
        return normalizedPincode != null && normalizedPincode.equals(clean(area.pincode()));
    }

    private AreaFence areaFence(RuleArea area) {
        Long areaZoneId = area.zoneId();
        Long areaCityId = area.cityId();
        List<List<double[]>> polygons = new ArrayList<>(extractPolygons(area.polygonJson()));
        List<CircleFence> circles = new ArrayList<>(extractCircles(area.polygonJson()));

        if (areaZoneId == null) {
            return new AreaFence(areaZoneId, areaCityId, polygons, circles);
        }

        ZoneEntity zone = zoneRepository.findById(areaZoneId).orElse(null);
        if (zone == null || zone.isDeleted() || !zone.isActive()) {
            return new AreaFence(areaZoneId, areaCityId, polygons, circles);
        }

        polygons.addAll(extractPolygons(zone.getPolygonJson()));
        circles.addAll(extractCircles(zone.getPolygonJson()));
        if (areaCityId == null && zone.getCity() != null) {
            areaCityId = zone.getCity().getId();
        }
        return new AreaFence(areaZoneId, areaCityId, polygons, circles);
    }

    public void validateBookingAddress(LexhoAddress address) {
        if (address == null) {
            throw new BookingValidationException("ADDRESS_NOT_FOUND", "Address not found for this customer");
        }

        String pincode = clean(address.getPincode());
        String city = clean(address.getCity());

        List<ServiceableAreaEntity> candidateAreas = new ArrayList<>();
        if (pincode != null) {
            candidateAreas.addAll(serviceableAreaRepository.findByPincodeAndActiveTrueAndDeletedFalse(pincode));
        }
        if (candidateAreas.isEmpty() && city != null) {
            candidateAreas.addAll(serviceableAreaRepository.findByCityIgnoreCaseAndActiveTrueAndDeletedFalse(city));
        }
        if (candidateAreas.isEmpty()) {
            throw new BookingValidationException("BOOKING_OUTSIDE_SERVICE_ZONE",
                    "Selected address is outside supported service area");
        }

        Double lat = toDouble(address.getLatitude());
        Double lng = toDouble(address.getLongitude());

        if (lat == null || lng == null || lat == 0.0d || lng == 0.0d) {
            throw new BookingValidationException("SAVED_ADDRESS_COORDINATES_MISSING",
                    "Saved address coordinates are missing. Send latitude/longitude in booking request or update the saved address.");
        }

        List<List<double[]>> polygons = bookingPolygons(candidateAreas);

        if (polygons.isEmpty()) {
            // No polygon configured yet for this area; keep serviceability by area match.
            return;
        }

        boolean inside = polygons.stream().anyMatch(poly -> isInsidePolygon(lat, lng, poly));
        if (!inside) {
            throw new BookingValidationException("BOOKING_OUTSIDE_GEOFENCE",
                    "Selected address is outside configured service zone");
        }
    }

    private List<List<double[]>> bookingPolygons(List<ServiceableAreaEntity> candidateAreas) {
        List<List<double[]>> polygons = new ArrayList<>();
        for (ServiceableAreaEntity area : candidateAreas) {
            polygons.addAll(extractPolygons(area.getPolygonJson()));
            polygons.addAll(activeZonePolygons(area.getZoneId()));
        }
        return polygons;
    }

    private List<List<double[]>> activeZonePolygons(Long zoneId) {
        if (zoneId == null) {
            return List.of();
        }
        ZoneEntity zone = zoneRepository.findById(zoneId).orElse(null);
        if (zone == null || zone.isDeleted() || !zone.isActive()) {
            return List.of();
        }
        return extractPolygons(zone.getPolygonJson());
    }

    private List<List<double[]>> extractPolygons(String polygonJson) {
        List<List<double[]>> polygons = new ArrayList<>();
        if (polygonJson == null || polygonJson.isBlank()) {
            return polygons;
        }
        try {
            JsonNode root = objectMapper.readTree(polygonJson);
            collectPolygons(root, polygons);
        } catch (JsonProcessingException ignored) {
            // Invalid polygon JSON should not crash booking flow.
        }
        return polygons;
    }

    private List<CircleFence> extractCircles(String polygonJson) {
        List<CircleFence> circles = new ArrayList<>();
        if (polygonJson == null || polygonJson.isBlank()) {
            return circles;
        }
        try {
            JsonNode root = objectMapper.readTree(polygonJson);
            collectCircles(root, circles);
        } catch (JsonProcessingException ignored) {
            // Keep tolerant parsing.
        }
        return circles;
    }

    private void collectCircles(JsonNode node, List<CircleFence> out) {
        if (node == null || node.isNull()) {
            return;
        }
        if (node.isObject()) {
            collectCirclesFromObject(node, out);
            return;
        }
        if (node.isArray()) {
            for (JsonNode child : node) {
                collectCircles(child, out);
            }
        }
    }

    private void collectCirclesFromObject(JsonNode node, List<CircleFence> out) {
        String type = geoJsonType(node);
        if ("Circle".equalsIgnoreCase(type)) {
            addCircleFence(node, out);
        } else if ("Feature".equalsIgnoreCase(type)) {
            collectCircles(node.get("geometry"), out);
        } else if ("FeatureCollection".equalsIgnoreCase(type)) {
            collectCircleFeatures(node.get("features"), out);
        }
    }

    private void addCircleFence(JsonNode node, List<CircleFence> out) {
        Double radius = parseDoubleNode(node.get("radiusMeters"));
        JsonNode center = node.get("center");
        if (center == null || !center.isArray() || center.size() < 2) {
            return;
        }
        Double centerLng = parseDoubleNode(center.get(0));
        Double centerLat = parseDoubleNode(center.get(1));
        if (centerLat != null && centerLng != null && radius != null && radius > 0) {
            out.add(new CircleFence(centerLat, centerLng, radius));
        }
    }

    private void collectCircleFeatures(JsonNode features, List<CircleFence> out) {
        if (features == null || !features.isArray()) {
            return;
        }
        for (JsonNode feature : features) {
            collectCircles(feature, out);
        }
    }

    private void collectPolygons(JsonNode node, List<List<double[]>> out) {
        if (node == null || node.isNull()) {
            return;
        }
        if (node.isObject()) {
            collectPolygonsFromObject(node, out);
            return;
        }
        if (node.isArray()) {
            collectPolygonArray(node, out);
        }
    }

    private void collectPolygonsFromObject(JsonNode node, List<List<double[]>> out) {
        String type = geoJsonType(node);
        if ("Polygon".equalsIgnoreCase(type)) {
            extractPolygonCoordinates(node.get(LexhoConstants.FIELD_COORDINATES), out);
        } else if ("Feature".equalsIgnoreCase(type)) {
            collectPolygons(node.get("geometry"), out);
        } else if ("FeatureCollection".equalsIgnoreCase(type)) {
            collectPolygonFeatures(node.get("features"), out);
        } else if (node.has(LexhoConstants.FIELD_COORDINATES)) {
            extractPolygonCoordinates(node.get(LexhoConstants.FIELD_COORDINATES), out);
        }
    }

    private void collectPolygonFeatures(JsonNode features, List<List<double[]>> out) {
        if (features == null || !features.isArray()) {
            return;
        }
        for (JsonNode feature : features) {
            collectPolygons(feature, out);
        }
    }

    private void collectPolygonArray(JsonNode node, List<List<double[]>> out) {
        if (isPolygonCoordinateArray(node)) {
            extractPolygonCoordinates(node, out);
            return;
        }
        List<double[]> ring = extractRing(node);
        if (ring.size() >= 3) {
            out.add(ring);
        }
    }

    private boolean isPolygonCoordinateArray(JsonNode node) {
        return node.size() > 0
                && node.get(0).isArray()
                && node.get(0).size() > 0
                && node.get(0).get(0).isArray();
    }

    private String geoJsonType(JsonNode node) {
        JsonNode type = node.get("type");
        return type == null ? null : type.asText();
    }

    private void extractPolygonCoordinates(JsonNode coordinates, List<List<double[]>> out) {
        if (coordinates == null || !coordinates.isArray() || coordinates.isEmpty()) {
            return;
        }
        JsonNode firstRingNode = coordinates.get(0);
        if (firstRingNode == null || !firstRingNode.isArray()) {
            return;
        }
        List<double[]> ring = extractRing(firstRingNode);
        if (ring.size() >= 3) {
            out.add(ring);
        }
    }

    private List<double[]> extractRing(JsonNode ringNode) {
        List<double[]> ring = new ArrayList<>();
        for (JsonNode point : ringNode) {
            if (point == null || !point.isArray() || point.size() < 2) {
                continue;
            }
            Double lng = parseDoubleNode(point.get(0));
            Double lat = parseDoubleNode(point.get(1));
            if (lat != null && lng != null) {
                ring.add(new double[]{lat, lng});
            }
        }
        return ring;
    }

    private static boolean isPointInsidePolygon(double lat, double lng, List<double[]> polygon) {
        boolean inside = false;
        int n = polygon.size();
        for (int i = 0, j = n - 1; i < n; j = i++) {
            double yi = polygon.get(i)[0];
            double xi = polygon.get(i)[1];
            double yj = polygon.get(j)[0];
            double xj = polygon.get(j)[1];

            boolean intersect = ((yi > lat) != (yj > lat))
                    && (lng < (xj - xi) * (lat - yi) / ((yj - yi) + 1e-12) + xi);
            if (intersect) {
                inside = !inside;
            }
        }
        return inside;
    }

    private boolean isInsidePolygon(double lat, double lng, List<double[]> polygon) {
        return isPointInsidePolygon(lat, lng, polygon);
    }

    private boolean isInsideCircle(double lat, double lng, CircleFence circle) {
        return GeoDistanceCalculator.haversineMeters(lat, lng, circle.centerLat, circle.centerLng) <= circle.radiusMeters;
    }

    private Double parseDoubleNode(JsonNode node) {
        if (node == null || node.isNull()) {
            return null;
        }
        if (node.isNumber()) {
            return node.asDouble();
        }
        if (node.isTextual()) {
            try {
                return Double.parseDouble(node.asText());
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        return null;
    }

    private Double toDouble(BigDecimal value) {
        return value == null ? null : value.doubleValue();
    }

    private String clean(String value) {
        return AdminGeoText.clean(value);
    }

    private List<RuleArea> fromRules(List<ServiceabilityRuleEntity> rules) {
        List<RuleArea> list = new ArrayList<>();
        for (ServiceabilityRuleEntity rule : rules) {
            list.add(new RuleArea(rule.getPincode(), rule.getCityId(), rule.getZoneId(), rule.getPolygonJson()));
        }
        return list;
    }

    private List<RuleArea> fromAreas(List<ServiceableAreaEntity> areas) {
        List<RuleArea> list = new ArrayList<>();
        for (ServiceableAreaEntity area : areas) {
            list.add(new RuleArea(area.getPincode(), area.getCityId(), area.getZoneId(), area.getPolygonJson()));
        }
        return list;
    }

    public static class GeoResolution {
        private final boolean serviceable;
        private final String reason;
        private final Long matchedCityId;
        private final Long matchedZoneId;
        private final String matchedPincode;

        private GeoResolution(boolean serviceable, String reason, Long matchedCityId, Long matchedZoneId, String matchedPincode) {
            this.serviceable = serviceable;
            this.reason = reason;
            this.matchedCityId = matchedCityId;
            this.matchedZoneId = matchedZoneId;
            this.matchedPincode = matchedPincode;
        }

        public static GeoResolution serviceable(Long matchedCityId, Long matchedZoneId, String matchedPincode) {
            return new GeoResolution(true, null, matchedCityId, matchedZoneId, matchedPincode);
        }

        public static GeoResolution notServiceable(String reason) {
            return new GeoResolution(false, reason, null, null, null);
        }

        public boolean isServiceable() {
            return serviceable;
        }

        public String getReason() {
            return reason;
        }

        public Long getMatchedCityId() {
            return matchedCityId;
        }

        public Long getMatchedZoneId() {
            return matchedZoneId;
        }

        public String getMatchedPincode() {
            return matchedPincode;
        }
    }

    private record CircleFence(double centerLat, double centerLng, double radiusMeters) {
    }

    private record AreaFence(Long zoneId, Long cityId, List<List<double[]>> polygons, List<CircleFence> circles) {
        private boolean empty() {
            return polygons.isEmpty() && circles.isEmpty();
        }

        private boolean contains(double lat, double lng) {
            return polygons.stream().anyMatch(poly -> GeoFenceValidationService.isPointInsidePolygon(lat, lng, poly))
                    || circles.stream().anyMatch(circle -> GeoDistanceCalculator.haversineMeters(
                    lat, lng, circle.centerLat, circle.centerLng) <= circle.radiusMeters);
        }
    }

    private record AreaMatch(RuleArea area, Long zoneId, Long cityId, String rejectReason,
                             boolean pincodeMatchedAreaExists) {
        private static AreaMatch matched(RuleArea area, Long zoneId, Long cityId, boolean pincodeMatchedAreaExists) {
            return new AreaMatch(area, zoneId, cityId, null, pincodeMatchedAreaExists);
        }

        private static AreaMatch rejected(String rejectReason, boolean pincodeMatchedAreaExists) {
            return new AreaMatch(null, null, null, rejectReason, pincodeMatchedAreaExists);
        }

        private boolean matched() {
            return area != null;
        }
    }

    private record RuleArea(String pincode, Long cityId, Long zoneId, String polygonJson) {
    }
}

package com.lexho.user_service.admin.geo.service;

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

        List<RuleArea> candidateAreas = new ArrayList<>();
        if (normalizedPincode != null) {
            candidateAreas.addAll(fromRules(serviceabilityRuleRepository.findByPincodeAndActiveTrueAndDeletedFalse(normalizedPincode)));
            if (candidateAreas.isEmpty()) {
                candidateAreas.addAll(fromAreas(serviceableAreaRepository.findByPincodeAndActiveTrueAndDeletedFalse(normalizedPincode)));
            }
        }
        if (candidateAreas.isEmpty() && normalizedCity != null) {
            candidateAreas.addAll(fromRules(serviceabilityRuleRepository.findByCityIgnoreCaseAndActiveTrueAndDeletedFalse(normalizedCity)));
            if (candidateAreas.isEmpty()) {
                candidateAreas.addAll(fromAreas(serviceableAreaRepository.findByCityIgnoreCaseAndActiveTrueAndDeletedFalse(normalizedCity)));
            }
        }
        if (candidateAreas.isEmpty() && lat != null && lng != null) {
            candidateAreas.addAll(fromRules(serviceabilityRuleRepository.findByActiveTrueAndDeletedFalse()));
            if (candidateAreas.isEmpty()) {
                candidateAreas.addAll(fromAreas(serviceableAreaRepository.findByActiveTrueAndDeletedFalse()));
            }
        }
        if (candidateAreas.isEmpty()) {
            return GeoResolution.notServiceable("NO_MATCHING_SERVICEABLE_AREA");
        }

        RuleArea matchedArea = null;
        Long matchedZoneId = null;
        Long matchedCityId = null;
        String rejectReason = "OUTSIDE_GEOFENCE";
        boolean pincodeMatchedAreaExists = false;

        for (RuleArea area : candidateAreas) {
            if (normalizedPincode != null && normalizedPincode.equals(clean(area.pincode()))) {
                pincodeMatchedAreaExists = true;
            }
            Long areaZoneId = area.zoneId();
            Long areaCityId = area.cityId();

            List<List<double[]>> polygons = new ArrayList<>();
            polygons.addAll(extractPolygons(area.polygonJson()));
            List<CircleFence> circles = new ArrayList<>();
            circles.addAll(extractCircles(area.polygonJson()));
            if (areaZoneId != null) {
                ZoneEntity zone = zoneRepository.findById(areaZoneId).orElse(null);
                if (zone != null && !zone.isDeleted() && zone.isActive()) {
                    polygons.addAll(extractPolygons(zone.getPolygonJson()));
                    circles.addAll(extractCircles(zone.getPolygonJson()));
                    if (areaCityId == null && zone.getCity() != null) {
                        areaCityId = zone.getCity().getId();
                    }
                }
            }

            if (polygons.isEmpty() && circles.isEmpty()) {
                matchedArea = area;
                matchedZoneId = areaZoneId;
                matchedCityId = areaCityId;
                rejectReason = null;
                break;
            }

            if (lat == null || lng == null) {
                rejectReason = "COORDINATES_REQUIRED_FOR_GEOFENCE";
                continue;
            }

            boolean inside = polygons.stream().anyMatch(poly -> isInsidePolygon(lat, lng, poly))
                    || circles.stream().anyMatch(circle -> isInsideCircle(lat, lng, circle));
            if (inside) {
                matchedArea = area;
                matchedZoneId = areaZoneId;
                matchedCityId = areaCityId;
                rejectReason = null;
                break;
            }
        }

        if (matchedArea == null) {
            if (pincodeMatchedAreaExists) {
                return GeoResolution.notServiceable("PARTIAL_AREA_UNSUPPORTED");
            }
            return GeoResolution.notServiceable(rejectReason == null ? "OUTSIDE_GEOFENCE" : rejectReason);
        }

        return GeoResolution.serviceable(
                matchedCityId,
                matchedZoneId,
                clean(matchedArea.pincode())
        );
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

        List<List<double[]>> polygons = new ArrayList<>();
        for (ServiceableAreaEntity area : candidateAreas) {
            polygons.addAll(extractPolygons(area.getPolygonJson()));
            if (area.getZoneId() != null) {
                ZoneEntity zone = zoneRepository.findById(area.getZoneId()).orElse(null);
                if (zone != null && !zone.isDeleted() && zone.isActive()) {
                    polygons.addAll(extractPolygons(zone.getPolygonJson()));
                }
            }
        }

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

    private List<List<double[]>> extractPolygons(String polygonJson) {
        List<List<double[]>> polygons = new ArrayList<>();
        if (polygonJson == null || polygonJson.isBlank()) {
            return polygons;
        }
        try {
            JsonNode root = objectMapper.readTree(polygonJson);
            collectPolygons(root, polygons);
        } catch (Exception ignored) {
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
        } catch (Exception ignored) {
            // Keep tolerant parsing.
        }
        return circles;
    }

    private void collectCircles(JsonNode node, List<CircleFence> out) {
        if (node == null || node.isNull()) {
            return;
        }
        if (node.isObject()) {
            JsonNode type = node.get("type");
            if (type != null && "Circle".equalsIgnoreCase(type.asText())) {
                Double radius = parseDoubleNode(node.get("radiusMeters"));
                JsonNode center = node.get("center");
                if (center != null && center.isArray() && center.size() >= 2) {
                    Double centerLng = parseDoubleNode(center.get(0));
                    Double centerLat = parseDoubleNode(center.get(1));
                    if (centerLat != null && centerLng != null && radius != null && radius > 0) {
                        out.add(new CircleFence(centerLat, centerLng, radius));
                    }
                }
            } else if (type != null && "Feature".equalsIgnoreCase(type.asText())) {
                collectCircles(node.get("geometry"), out);
            } else if (type != null && "FeatureCollection".equalsIgnoreCase(type.asText())) {
                JsonNode features = node.get("features");
                if (features != null && features.isArray()) {
                    for (JsonNode feature : features) {
                        collectCircles(feature, out);
                    }
                }
            }
            return;
        }
        if (node.isArray()) {
            for (JsonNode child : node) {
                collectCircles(child, out);
            }
        }
    }

    private void collectPolygons(JsonNode node, List<List<double[]>> out) {
        if (node == null || node.isNull()) {
            return;
        }
        if (node.isObject()) {
            JsonNode type = node.get("type");
            if (type != null && "Polygon".equalsIgnoreCase(type.asText())) {
                JsonNode coordinates = node.get("coordinates");
                extractPolygonCoordinates(coordinates, out);
            } else if (type != null && "Feature".equalsIgnoreCase(type.asText())) {
                collectPolygons(node.get("geometry"), out);
            } else if (type != null && "FeatureCollection".equalsIgnoreCase(type.asText())) {
                JsonNode features = node.get("features");
                if (features != null && features.isArray()) {
                    for (JsonNode feature : features) {
                        collectPolygons(feature, out);
                    }
                }
            } else if (node.has("coordinates")) {
                extractPolygonCoordinates(node.get("coordinates"), out);
            }
            return;
        }
        if (node.isArray()) {
            // Raw array support: either [[[lng,lat]...]] or [[lng,lat]...]
            if (node.size() > 0 && node.get(0).isArray() && node.get(0).size() > 0 && node.get(0).get(0).isArray()) {
                extractPolygonCoordinates(node, out);
            } else {
                List<double[]> ring = extractRing(node);
                if (ring.size() >= 3) {
                    out.add(ring);
                }
            }
        }
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

    private boolean isInsidePolygon(double lat, double lng, List<double[]> polygon) {
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

    private boolean isInsideCircle(double lat, double lng, CircleFence circle) {
        return haversineMeters(lat, lng, circle.centerLat, circle.centerLng) <= circle.radiusMeters;
    }

    private double haversineMeters(double lat1, double lon1, double lat2, double lon2) {
        final double earthRadius = 6371000.0d;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return earthRadius * c;
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
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
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

    private record RuleArea(String pincode, Long cityId, Long zoneId, String polygonJson) {
    }
}

package com.lexho.user_service.admin.geo.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lexho.user_service.admin.geo.entity.ZoneEntity;
import com.lexho.user_service.admin.geo.repository.ZoneRepository;
import com.lexho.user_service.admin.serviceablearea.entity.ServiceableAreaEntity;
import com.lexho.user_service.admin.serviceablearea.repository.ServiceableAreaRepository;
import com.lexho.user_service.booking.exception.BookingValidationException;
import com.lexho.user_service.lexho.entity.LexhoAddress;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Service
public class GeoFenceValidationService {

    private final ServiceableAreaRepository serviceableAreaRepository;
    private final ZoneRepository zoneRepository;
    private final ObjectMapper objectMapper;

    public GeoFenceValidationService(ServiceableAreaRepository serviceableAreaRepository,
                                     ZoneRepository zoneRepository,
                                     ObjectMapper objectMapper) {
        this.serviceableAreaRepository = serviceableAreaRepository;
        this.zoneRepository = zoneRepository;
        this.objectMapper = objectMapper;
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

        if (lat == null || lng == null) {
            throw new BookingValidationException("BOOKING_ADDRESS_COORDINATES_REQUIRED",
                    "Address coordinates are required for booking");
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
}

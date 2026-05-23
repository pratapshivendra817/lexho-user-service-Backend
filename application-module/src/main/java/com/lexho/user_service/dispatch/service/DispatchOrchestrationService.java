package com.lexho.user_service.dispatch.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lexho.booking.entity.Order;
import com.lexho.booking.repository.OrderRepository;
import com.lexho.common.enums.OrderStatus;
import com.lexho.user_service.admin.serviceablearea.repository.ServiceableAreaRepository;
import com.lexho.user_service.admin.entity.AdminServiceEntity;
import com.lexho.user_service.admin.repository.AdminServiceEntityRepository;
import com.lexho.user_service.dispatch.entity.BookingEventEntity;
import com.lexho.user_service.dispatch.entity.CurrentPartnerLocationEntity;
import com.lexho.user_service.dispatch.entity.PartnerShiftEntity;
import com.lexho.user_service.dispatch.repository.BookingEventRepository;
import com.lexho.user_service.dispatch.repository.CurrentPartnerLocationRepository;
import com.lexho.user_service.dispatch.repository.PartnerShiftRepository;
import com.lexho.user_service.dispatch.repository.PincodeAdjacencyRepository;
import com.lexho.user_service.dispatch.repository.PartnerServiceSkillRepository;
import com.lexho.user_service.exception.BadRequestException;
import com.lexho.user_service.exception.ResourceNotFoundException;
import com.lexho.user_service.lexho.entity.LexhoAddress;
import com.lexho.user_service.lexho.repository.LexhoAddressRepository;
import com.lexho.user_service.partnerrequest.entity.PartnerServiceRequestEntity;
import com.lexho.user_service.partnerrequest.model.PartnerServiceRequestStatus;
import com.lexho.user_service.partnerrequest.repository.PartnerServiceRequestRepository;
import com.lexho.partner.entity.Partner;
import com.lexho.partner.repository.PartnerRepository;
import com.lexho.user_service.booking.model.BookingType;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.*;

@Service
public class DispatchOrchestrationService {
    public static final int INSTANT_SLA_MINUTES = 30;
    public static final int PARTNER_RESPONSE_WINDOW_MINUTES = 2;
    private static final double ETA_SPEED_KMPH = 25.0;

    private final OrderRepository orderRepository;
    private final LexhoAddressRepository lexhoAddressRepository;
    private final ServiceableAreaRepository serviceableAreaRepository;
    private final PartnerShiftRepository partnerShiftRepository;
    private final CurrentPartnerLocationRepository currentPartnerLocationRepository;
    private final PincodeAdjacencyRepository pincodeAdjacencyRepository;
    private final PartnerRepository partnerRepository;
    private final PartnerServiceRequestRepository partnerServiceRequestRepository;
    private final PartnerServiceSkillRepository partnerServiceSkillRepository;
    private final AdminServiceEntityRepository adminServiceEntityRepository;
    private final BookingEventRepository bookingEventRepository;
    private final DispatchScoringPolicy dispatchScoringPolicy;
    private final ObjectMapper objectMapper;

    public DispatchOrchestrationService(OrderRepository orderRepository,
                                        LexhoAddressRepository lexhoAddressRepository,
                                        ServiceableAreaRepository serviceableAreaRepository,
                                        PartnerShiftRepository partnerShiftRepository,
                                        CurrentPartnerLocationRepository currentPartnerLocationRepository,
                                        PincodeAdjacencyRepository pincodeAdjacencyRepository,
                                        PartnerRepository partnerRepository,
                                        PartnerServiceRequestRepository partnerServiceRequestRepository,
                                        PartnerServiceSkillRepository partnerServiceSkillRepository,
                                        AdminServiceEntityRepository adminServiceEntityRepository,
                                        BookingEventRepository bookingEventRepository,
                                        DispatchScoringPolicy dispatchScoringPolicy,
                                        ObjectMapper objectMapper) {
        this.orderRepository = orderRepository;
        this.lexhoAddressRepository = lexhoAddressRepository;
        this.serviceableAreaRepository = serviceableAreaRepository;
        this.partnerShiftRepository = partnerShiftRepository;
        this.currentPartnerLocationRepository = currentPartnerLocationRepository;
        this.pincodeAdjacencyRepository = pincodeAdjacencyRepository;
        this.partnerRepository = partnerRepository;
        this.partnerServiceRequestRepository = partnerServiceRequestRepository;
        this.partnerServiceSkillRepository = partnerServiceSkillRepository;
        this.adminServiceEntityRepository = adminServiceEntityRepository;
        this.bookingEventRepository = bookingEventRepository;
        this.dispatchScoringPolicy = dispatchScoringPolicy;
        this.objectMapper = objectMapper;
    }

    @Async
    public void dispatchOrderAsync(Long orderId, Long customerId, Long addressId, Long serviceId, Integer quantity, LocalDateTime scheduledAt) {
        dispatchOrder(orderId, customerId, addressId, serviceId, quantity, scheduledAt);
    }

    @Transactional
    public int dispatchOrder(Long orderId, Long customerId, Long addressId, Long serviceId, Integer quantity, LocalDateTime scheduledAt) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));

        LexhoAddress address = lexhoAddressRepository.findByIdAndUserId(addressId, customerId)
                .orElseThrow(() -> new BadRequestException("Address not found"));

        String pincode = normalizePincode(address.getPincode());
        Long resolvedSubServiceId = serviceId;
        AdminServiceEntity requestedService = adminServiceEntityRepository.findByIdAndDeletedFalse(serviceId).orElse(null);
        if (requestedService == null) {
            // Backward-compatible fallback: legacy booking payloads may send partnerId.
            Partner partnerFromLegacyId = partnerRepository.findById(serviceId).orElse(null);
            if (partnerFromLegacyId != null && partnerFromLegacyId.getSubCategoryId() != null) {
                resolvedSubServiceId = partnerFromLegacyId.getSubCategoryId();
                requestedService = adminServiceEntityRepository.findByIdAndDeletedFalse(resolvedSubServiceId).orElse(null);
            }
        }
        if (requestedService == null) {
            throw new BadRequestException("Requested service not found");
        }
        Long requestedCategoryId = requestedService.getCategory() == null ? null : requestedService.getCategory().getId();
        if (!serviceableAreaRepository.existsByPincodeAndActiveTrueAndDeletedFalse(pincode)) {
            saveEvent(orderId, "PARTNER_SEARCH_FAILED", Map.of("reason", "PINCODE_NOT_SERVICEABLE", "pincode", pincode));
            throw new BadRequestException("Service is not available for this pincode");
        }

        saveEvent(orderId, "BOOKING_CREATED", Map.of("orderId", orderId, "customerId", customerId));
        boolean instant = BookingType.INSTANT.name().equalsIgnoreCase(order.getBookingType());
        saveEvent(orderId, instant ? "SEARCHING_PARTNER" : "PARTNER_SEARCH", Map.of("pincode", pincode));
        OrderStatus searchStatus = instant ? OrderStatus.SEARCHING_PARTNER : OrderStatus.PARTNER_SEARCH;
        if (order.getStatus() == OrderStatus.BOOKING_CREATED && order.getStatus().canTransitionTo(searchStatus)) {
            order.setStatus(searchStatus);
            orderRepository.save(order);
        }

        List<String> searchPincodes = new ArrayList<>();
        searchPincodes.add(pincode);
        pincodeAdjacencyRepository.findBySourcePincodeAndActiveTrueOrderByPriorityAsc(pincode)
                .forEach(a -> searchPincodes.add(a.getAdjacentPincode()));

        List<PartnerShiftEntity> onlineShifts = partnerShiftRepository.findByCurrentPincodeInAndOnlineTrue(searchPincodes);
        if (onlineShifts.isEmpty()) {
            saveEvent(orderId, "PARTNER_SEARCH_FAILED", Map.of("reason", "NO_ONLINE_PARTNER"));
            return 0;
        }

        Map<Long, Partner> partnersById = new HashMap<>();
        for (Partner partner : partnerRepository.findAllById(onlineShifts.stream().map(PartnerShiftEntity::getPartnerId).toList())) {
            partnersById.put(partner.getId(), partner);
        }

        List<Candidate> eligible = new ArrayList<>();
        for (PartnerShiftEntity shift : onlineShifts) {
            Partner partner = partnersById.get(shift.getPartnerId());
            if (partner == null) {
                continue;
            }
            if (!Boolean.TRUE.equals(partner.getAvailable())) {
                continue;
            }
            if (!hasRequiredSkill(partner, resolvedSubServiceId, requestedCategoryId)) {
                continue;
            }
            // Allow dispatch in local/dev environments where legacy partner profiles
            // may not carry synchronized verification status.
            if (shift.getActiveJobsCount() >= shift.getMaxActiveJobs()) {
                continue;
            }
            if (shift.getShiftStart() != null && shift.getShiftEnd() != null) {
                LocalDateTime now = LocalDateTime.now();
                if (now.isBefore(shift.getShiftStart()) || now.isAfter(shift.getShiftEnd())) {
                    continue;
                }
            }
            BigDecimal distance = calculateDistanceKm(address, shift.getPartnerId());
            if (!dispatchScoringPolicy.isWithinDispatchRadius(distance)) {
                continue;
            }
            int etaMinutes = estimateEtaMinutes(distance);
            if (instant && etaMinutes > INSTANT_SLA_MINUTES) {
                continue;
            }
            eligible.add(new Candidate(shift, partner, distance, etaMinutes));
        }

        if (eligible.isEmpty()) {
            saveEvent(orderId, "PARTNER_SEARCH_FAILED", Map.of("reason", "NO_ELIGIBLE_PARTNER"));
            return 0;
        }

        eligible.sort(dispatchScoringPolicy.comparator(pincode));

        int dispatchAttempt = (int) partnerServiceRequestRepository.findByCartId(orderId).stream().count() + 1;
        int created = 0;
        for (Candidate candidate : eligible.stream().limit(5).toList()) {
            if (partnerServiceRequestRepository.existsByCartIdAndPartnerIdAndStatus(orderId, candidate.partner().getId(), PartnerServiceRequestStatus.PENDING)) {
                continue;
            }
            PartnerServiceRequestEntity request = new PartnerServiceRequestEntity();
            request.setPartnerId(candidate.partner().getId());
            request.setCartId(orderId);
            request.setCustomerId(customerId);
            request.setServiceId(resolvedSubServiceId);
            request.setQuantity(quantity);
            request.setScheduledTime(scheduledAt);
            request.setFullAddress(address.getHouseNo() + ", " + address.getArea() + ", " + address.getCity());
            request.setCity(address.getCity());
            request.setLatitude(address.getLatitude());
            request.setLongitude(address.getLongitude());
            request.setTargetPincode(pincode);
            request.setDispatchAttempt(dispatchAttempt);
            request.setDistanceKm(candidate.distance());
            request.setEtaMinutes(candidate.etaMinutes());
            request.setExpiresAt(LocalDateTime.now().plusMinutes(PARTNER_RESPONSE_WINDOW_MINUTES));
            request.setStatus(PartnerServiceRequestStatus.PENDING);
            partnerServiceRequestRepository.save(request);
            created++;
        }

        if (created > 0) {
            saveEvent(orderId, instant ? "INSTANT_DISPATCHED" : "PARTNER_ASSIGNED", Map.of("candidateCount", created, "pincode", pincode));
            if (!instant && order.getStatus() == OrderStatus.PARTNER_SEARCH && order.getStatus().canTransitionTo(OrderStatus.PARTNER_ASSIGNED)) {
                order.setStatus(OrderStatus.PARTNER_ASSIGNED);
                orderRepository.save(order);
            }
        } else {
            saveEvent(orderId, "PARTNER_SEARCH_FAILED", Map.of("reason", "NO_NEW_REQUEST_CREATED"));
        }
        return created;
    }

    @Transactional
    public void markPartnerAccepted(Long orderId, Long partnerId, Integer etaMinutesAtAcceptance) {
        Order order = orderRepository.findByIdForUpdate(orderId).orElseThrow(() -> new ResourceNotFoundException("Order not found"));
        boolean instant = BookingType.INSTANT.name().equalsIgnoreCase(order.getBookingType());
        if (instant) {
            order.setStatus(OrderStatus.PARTNER_ASSIGNED);
            order.setSlaMinutes(INSTANT_SLA_MINUTES);
            int eta = etaMinutesAtAcceptance == null ? INSTANT_SLA_MINUTES : etaMinutesAtAcceptance;
            order.setPromisedStartTime(LocalDateTime.now().plusMinutes(eta));
            order.setUnavailableReason(null);
        } else if (order.getStatus() == OrderStatus.CREATED || order.getStatus() == OrderStatus.PARTNER_ASSIGNED
                || order.getStatus() == OrderStatus.PARTNER_SEARCH || order.getStatus() == OrderStatus.BOOKING_CREATED) {
            order.setStatus(OrderStatus.PARTNER_ACCEPTED);
        }
        orderRepository.save(order);
        PartnerShiftEntity shift = partnerShiftRepository.findByPartnerId(partnerId).orElse(null);
        if (shift != null) {
            shift.setActiveJobsCount(shift.getActiveJobsCount() + 1);
            partnerShiftRepository.save(shift);
        }
        saveEvent(orderId, "PARTNER_ACCEPTED", Map.of("partnerId", partnerId, "etaMinutes", etaMinutesAtAcceptance));
    }

    @Transactional
    public void markPartnerRejected(Long orderId, Long partnerId) {
        saveEvent(orderId, "PARTNER_REJECTED", Map.of("partnerId", partnerId));
    }

    @Transactional
    public void saveEvent(Long orderId, String eventType, Object payload) {
        BookingEventEntity event = new BookingEventEntity();
        event.setBookingId(orderId);
        event.setEventType(eventType);
        event.setEventPayload(toJson(payload));
        bookingEventRepository.save(event);
    }

    @Transactional
    public OrderStatus transitionOrderStatus(Long orderId, OrderStatus targetStatus) {
        Order order = orderRepository.findById(orderId).orElseThrow(() -> new ResourceNotFoundException("Order not found"));
        OrderStatus current = order.getStatus();
        if (current == null) {
            current = OrderStatus.BOOKING_CREATED;
        }
        if (!current.canTransitionTo(targetStatus)) {
            throw new BadRequestException("Invalid order status transition from " + current + " to " + targetStatus);
        }
        order.setStatus(targetStatus);
        orderRepository.save(order);
        saveEvent(orderId, targetStatus.name(), Map.of("previous", current.name(), "next", targetStatus.name()));
        return targetStatus;
    }

    private String toJson(Object payload) {
        if (payload == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException e) {
            return "{\"error\":\"SERIALIZATION_FAILED\"}";
        }
    }

    private String normalizePincode(String pincode) {
        if (pincode == null || pincode.isBlank()) {
            throw new BadRequestException("Address pincode is missing");
        }
        return pincode.trim();
    }

    private BigDecimal calculateDistanceKm(LexhoAddress address, Long partnerId) {
        if (address.getLatitude() == null || address.getLongitude() == null) {
            return null;
        }
        CurrentPartnerLocationEntity location = currentPartnerLocationRepository.findByPartnerId(partnerId).orElse(null);
        if (location == null) {
            return null;
        }
        double km = haversineKm(
                address.getLatitude().doubleValue(),
                address.getLongitude().doubleValue(),
                location.getLatitude().doubleValue(),
                location.getLongitude().doubleValue()
        );
        return BigDecimal.valueOf(km).setScale(3, RoundingMode.HALF_UP);
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

    public int estimateEtaMinutes(BigDecimal distanceKm) {
        if (distanceKm == null) {
            return 20;
        }
        double hours = distanceKm.doubleValue() / ETA_SPEED_KMPH;
        int minutes = (int) Math.ceil(hours * 60.0);
        return Math.max(minutes, 1);
    }

    public record Candidate(PartnerShiftEntity shift, Partner partner, BigDecimal distance, int etaMinutes) {}

    private boolean hasRequiredSkill(Partner partner, Long requestedSubServiceId, Long requestedCategoryId) {
        if (partner.getSubCategoryId() != null && partner.getSubCategoryId().equals(requestedSubServiceId)) {
            return true;
        }
        if (requestedCategoryId != null && partner.getCategoryId() != null && partner.getCategoryId().equals(requestedCategoryId)) {
            return true;
        }
        if (partnerServiceSkillRepository.existsByPartnerIdAndSubServiceId(partner.getId(), requestedSubServiceId)) {
            return true;
        }
        return requestedCategoryId != null
                && partnerServiceSkillRepository.existsByPartnerIdAndCategoryId(partner.getId(), requestedCategoryId);
    }
}

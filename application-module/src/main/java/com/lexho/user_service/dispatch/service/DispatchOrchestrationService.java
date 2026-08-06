package com.lexho.user_service.dispatch.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lexho.booking.entity.Order;
import com.lexho.booking.repository.OrderRepository;
import com.lexho.common.enums.OrderStatus;
import com.lexho.user_service.admin.serviceablearea.repository.ServiceableAreaRepository;
import com.lexho.user_service.admin.entity.AdminServiceEntity;
import com.lexho.user_service.admin.repository.AdminServiceEntityRepository;
import com.lexho.user_service.config.DispatchProperties;
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
import com.lexho.provider.entity.Partner;
import com.lexho.provider.repository.PartnerRepository;
import com.lexho.user_service.booking.model.BookingType;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class DispatchOrchestrationService {
    public static final int INSTANT_SLA_MINUTES = 30;
    public static final int PARTNER_RESPONSE_WINDOW_MINUTES = 2;
    private static final double ETA_SPEED_KMPH = 25.0;
    private static final ZoneId APPLICATION_ZONE = ZoneId.systemDefault();

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
    private final DispatchProperties dispatchProperties;
    private final TrackingLifecycleService trackingLifecycleService;

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
                                        ObjectMapper objectMapper,
                                        TrackingLifecycleService trackingLifecycleService,
                                        DispatchProperties dispatchProperties) {
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
        this.trackingLifecycleService = trackingLifecycleService;
        this.dispatchProperties = dispatchProperties;
    }

    @Async
    @Transactional
    public void dispatchOrderAsync(Long orderId, Long customerId, Long addressId, Long serviceId, Integer quantity, LocalDateTime scheduledAt) {
        dispatchOrderInternal(orderId, customerId, addressId, serviceId, quantity, scheduledAt);
    }

    @Transactional
    public int dispatchOrder(Long orderId, Long customerId, Long addressId, Long serviceId, Integer quantity, LocalDateTime scheduledAt) {
        return dispatchOrderInternal(orderId, customerId, addressId, serviceId, quantity, scheduledAt);
    }

    private int dispatchOrderInternal(Long orderId, Long customerId, Long addressId, Long serviceId,
                                      Integer quantity, LocalDateTime scheduledAt) {
        DispatchContext context = buildDispatchContext(orderId, customerId, addressId, serviceId, quantity, scheduledAt);
        ensureServiceable(context);
        saveEvent(orderId, "BOOKING_CREATED", Map.of("orderId", orderId, "customerId", customerId));

        if (shouldDeferDispatch(context)) {
            return 0;
        }

        startPartnerSearch(context);
        int created = dispatchAttemptsForOrder(context);
        finishDispatch(context, created);
        return created;
    }

    private DispatchContext buildDispatchContext(Long orderId, Long customerId, Long addressId, Long serviceId,
                                                 Integer quantity, LocalDateTime scheduledAt) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));
        LexhoAddress address = lexhoAddressRepository.findByIdAndUserId(addressId, customerId)
                .orElseThrow(() -> new BadRequestException("Address not found"));
        ResolvedService resolvedService = resolveRequestedService(serviceId);
        return new DispatchContext(
                orderId,
                customerId,
                quantity,
                scheduledAt,
                order,
                address,
                normalizePincode(address.getPincode()),
                resolvedService.subServiceId(),
                categoryId(resolvedService.service()),
                BookingType.INSTANT.name().equalsIgnoreCase(order.getBookingType()),
                BookingType.SCHEDULED.name().equalsIgnoreCase(order.getBookingType())
        );
    }

    private ResolvedService resolveRequestedService(Long serviceId) {
        AdminServiceEntity requestedService = adminServiceEntityRepository.findByIdAndDeletedFalse(serviceId).orElse(null);
        if (requestedService != null) {
            return new ResolvedService(serviceId, requestedService);
        }
        return resolveLegacyPartnerService(serviceId);
    }

    private ResolvedService resolveLegacyPartnerService(Long serviceId) {
        Partner partnerFromLegacyId = partnerRepository.findById(serviceId).orElse(null);
        if (partnerFromLegacyId == null || partnerFromLegacyId.getSubCategoryId() == null) {
            throw new BadRequestException("Requested service not found");
        }
        Long subServiceId = partnerFromLegacyId.getSubCategoryId();
        AdminServiceEntity requestedService = adminServiceEntityRepository.findByIdAndDeletedFalse(subServiceId)
                .orElseThrow(() -> new BadRequestException("Requested service not found"));
        return new ResolvedService(subServiceId, requestedService);
    }

    private Long categoryId(AdminServiceEntity requestedService) {
        return requestedService.getCategory() == null ? null : requestedService.getCategory().getId();
    }

    private void ensureServiceable(DispatchContext context) {
        if (serviceableAreaRepository.existsByPincodeAndActiveTrueAndDeletedFalse(context.pincode())) {
            return;
        }
        saveEvent(context.orderId(), "PARTNER_SEARCH_FAILED",
                Map.of("reason", "PINCODE_NOT_SERVICEABLE", "pincode", context.pincode()));
        throw new BadRequestException("Service is not available for this pincode");
    }

    private boolean shouldDeferDispatch(DispatchContext context) {
        if (!context.scheduled() || shouldActivateScheduledDispatch(context.scheduledAt())) {
            return false;
        }
        saveEvent(context.orderId(), "DISPATCH_DEFERRED", Map.of(
                "reason", "PRE_ARRIVAL_WINDOW_NOT_REACHED",
                "scheduledAt", context.scheduledAt(),
                "activationMinutesBefore", getScheduledPreDispatchMinutes()
        ));
        return true;
    }

    private void startPartnerSearch(DispatchContext context) {
        saveEvent(context.orderId(), context.instant() ? "SEARCHING_PARTNER" : "PARTNER_SEARCH",
                Map.of("pincode", context.pincode()));
        OrderStatus searchStatus = context.instant() ? OrderStatus.SEARCHING_PARTNER : OrderStatus.PARTNER_SEARCH;
        if (context.order().getStatus() == OrderStatus.BOOKING_CREATED
                && context.order().getStatus().canTransitionTo(searchStatus)) {
            context.order().setStatus(searchStatus);
            orderRepository.save(context.order());
        }
    }

    private int dispatchAttemptsForOrder(DispatchContext context) {
        List<PartnerServiceRequestEntity> existingRequests = partnerServiceRequestRepository.findByCartId(context.orderId());
        Set<Long> requestedPartnerIds = existingRequests.stream()
                .map(PartnerServiceRequestEntity::getPartnerId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        int currentAttempt = existingRequests.stream()
                .map(PartnerServiceRequestEntity::getDispatchAttempt)
                .filter(Objects::nonNull)
                .max(Integer::compareTo)
                .orElse(0);

        List<Candidate> initialEligible = findEligibleCandidates(context, getInitialSearchRadiusKm(), requestedPartnerIds);
        if (!initialEligible.isEmpty()) {
            initialEligible.sort(dispatchScoringPolicy.comparator(context.pincode()));
            return createPartnerRequests(context, initialEligible, currentAttempt + 1);
        }

        if (getMaxSearchRadiusKm() <= getInitialSearchRadiusKm()) {
            saveEvent(context.orderId(), "PARTNER_SEARCH_FAILED", Map.of("reason", "NO_ELIGIBLE_PARTNER"));
            return 0;
        }

        saveEvent(context.orderId(), "PARTNER_SEARCH_RADIUS_EXPANDED", Map.of(
                "fromRadiusKm", getInitialSearchRadiusKm(),
                "toRadiusKm", getMaxSearchRadiusKm()
        ));
        List<Candidate> expandedEligible = findEligibleCandidates(context, getMaxSearchRadiusKm(), requestedPartnerIds);
        if (expandedEligible.isEmpty()) {
            saveEvent(context.orderId(), "PARTNER_SEARCH_FAILED", Map.of("reason", "NO_ELIGIBLE_PARTNER"));
            return 0;
        }
        expandedEligible.sort(dispatchScoringPolicy.comparator(context.pincode()));
        return createPartnerRequests(context, expandedEligible, Math.max(2, currentAttempt + 1));
    }

    private List<Candidate> findEligibleCandidates(DispatchContext context,
                                                   double radiusKm,
                                                   Set<Long> excludedPartnerIds) {
        List<Long> nearbyPartnerIds = findNearbyPartnerIds(context, radiusKm);
        if (nearbyPartnerIds.isEmpty()) {
            return List.of();
        }
        List<PartnerShiftEntity> onlineShifts = partnerShiftRepository.findByPartnerIdInAndDeletedFalse(nearbyPartnerIds).stream()
                .filter(shift -> shift.isOnline() && !shift.isDeleted())
                .filter(shift -> !excludedPartnerIds.contains(shift.getPartnerId()))
                .toList();
        if (onlineShifts.isEmpty()) {
            return List.of();
        }

        Map<Long, Partner> partnersById = findPartnersById(onlineShifts);
        List<Candidate> eligible = new ArrayList<>();
        for (PartnerShiftEntity shift : onlineShifts) {
            candidateForShift(context, partnersById, shift, radiusKm).ifPresent(eligible::add);
        }
        return eligible;
    }

    private Map<Long, Partner> findPartnersById(List<PartnerShiftEntity> onlineShifts) {
        Map<Long, Partner> partnersById = new HashMap<>();
        List<Long> partnerIds = onlineShifts.stream().map(PartnerShiftEntity::getPartnerId).toList();
        for (Partner partner : partnerRepository.findAllById(partnerIds)) {
            partnersById.put(partner.getId(), partner);
        }
        return partnersById;
    }

    private Optional<Candidate> candidateForShift(DispatchContext context,
                                                  Map<Long, Partner> partnersById,
                                                  PartnerShiftEntity shift,
                                                  double radiusKm) {
        Partner partner = partnersById.get(shift.getPartnerId());
        if (!isEligiblePartner(context, partner, shift)) {
            return Optional.empty();
        }
        BigDecimal distance = calculateDistanceKm(context.address(), shift.getPartnerId());
        int etaMinutes = estimateEtaMinutes(distance);
        if (!isDispatchable(context, distance, etaMinutes, radiusKm)) {
            return Optional.empty();
        }
        return Optional.of(new Candidate(shift, partner, distance, etaMinutes));
    }

    private boolean isEligiblePartner(DispatchContext context, Partner partner, PartnerShiftEntity shift) {
        return partner != null
                && Boolean.TRUE.equals(partner.getAvailable())
                && isApprovedPartner(partner)
                && hasRequiredSkill(partner, context.resolvedSubServiceId(), context.requestedCategoryId())
                && shift.getActiveJobsCount() < shift.getMaxActiveJobs()
                && isShiftActive(shift);
    }

    private boolean isShiftActive(PartnerShiftEntity shift) {
        if (shift.getShiftStart() == null || shift.getShiftEnd() == null) {
            return true;
        }
        LocalDateTime currentTime = now();
        return !currentTime.isBefore(shift.getShiftStart()) && !currentTime.isAfter(shift.getShiftEnd());
    }

    private boolean isDispatchable(DispatchContext context, BigDecimal distance, int etaMinutes, double radiusKm) {
        return dispatchScoringPolicy.isWithinDispatchRadius(distance, radiusKm)
                && etaMinutes <= getMaxEtaMinutes()
                && (!context.instant() || dispatchScoringPolicy.isWithinDispatchRadius(distance, getMaxSearchRadiusKm()));
    }

    private int createPartnerRequests(DispatchContext context, List<Candidate> eligible, int dispatchAttempt) {
        int created = 0;
        for (Candidate candidate : eligible.stream().limit(5).toList()) {
            if (hasPendingRequest(context.orderId(), candidate.partner().getId())) {
                continue;
            }
            partnerServiceRequestRepository.save(toPartnerServiceRequest(context, candidate, dispatchAttempt));
            created++;
        }
        return created;
    }

    private boolean hasPendingRequest(Long orderId, Long partnerId) {
        return partnerServiceRequestRepository.existsByCartIdAndPartnerIdAndStatus(
                orderId,
                partnerId,
                PartnerServiceRequestStatus.PENDING
        );
    }

    private PartnerServiceRequestEntity toPartnerServiceRequest(DispatchContext context, Candidate candidate,
                                                                int dispatchAttempt) {
        PartnerServiceRequestEntity request = new PartnerServiceRequestEntity();
        request.setPartnerId(candidate.partner().getId());
        request.setCartId(context.orderId());
        request.setCustomerId(context.customerId());
        request.setServiceId(context.resolvedSubServiceId());
        request.setQuantity(context.quantity());
        request.setScheduledTime(context.scheduledAt());
        request.setFullAddress(fullAddress(context.address()));
        request.setCity(context.address().getCity());
        request.setLatitude(context.address().getLatitude());
        request.setLongitude(context.address().getLongitude());
        request.setTargetPincode(context.pincode());
        request.setDispatchAttempt(dispatchAttempt);
        request.setDistanceKm(candidate.distance());
        request.setEtaMinutes(candidate.etaMinutes());
        request.setExpiresAt(now().plusMinutes(PARTNER_RESPONSE_WINDOW_MINUTES));
        request.setStatus(PartnerServiceRequestStatus.PENDING);
        return request;
    }

    private String fullAddress(LexhoAddress address) {
        return address.getHouseNo() + ", " + address.getArea() + ", " + address.getCity();
    }

    private void finishDispatch(DispatchContext context, int created) {
        if (created <= 0) {
            saveEvent(context.orderId(), "PARTNER_SEARCH_FAILED", Map.of("reason", "NO_NEW_REQUEST_CREATED"));
            return;
        }
        saveEvent(context.orderId(), context.instant() ? "INSTANT_DISPATCHED" : "PARTNER_ASSIGNED",
                Map.of("candidateCount", created, "pincode", context.pincode()));
        assignScheduledPartnerIfAllowed(context);
    }

    private void assignScheduledPartnerIfAllowed(DispatchContext context) {
        if (context.instant() || context.order().getStatus() != OrderStatus.PARTNER_SEARCH
                || !context.order().getStatus().canTransitionTo(OrderStatus.PARTNER_ASSIGNED)) {
            return;
        }
        context.order().setStatus(OrderStatus.PARTNER_ASSIGNED);
        orderRepository.save(context.order());
    }

    @Transactional
    public void markPartnerAccepted(Long orderId, Long partnerId, Integer etaMinutesAtAcceptance) {
        Order order = orderRepository.findByIdForUpdate(orderId).orElseThrow(() -> new ResourceNotFoundException("Order not found"));
        if (order.getAssignedPartnerId() != null && !order.getAssignedPartnerId().equals(partnerId)) {
            throw new BadRequestException("Order is already assigned to another partner");
        }
        boolean instant = BookingType.INSTANT.name().equalsIgnoreCase(order.getBookingType());
        if (instant) {
            order.setStatus(OrderStatus.PARTNER_ASSIGNED);
            order.setSlaMinutes(INSTANT_SLA_MINUTES);
            int eta = etaMinutesAtAcceptance == null ? INSTANT_SLA_MINUTES : etaMinutesAtAcceptance;
            order.setPromisedStartTime(now().plusMinutes(eta));
            order.setUnavailableReason(null);
        } else if (order.getStatus() == OrderStatus.CREATED || order.getStatus() == OrderStatus.PARTNER_ASSIGNED
                || order.getStatus() == OrderStatus.PARTNER_SEARCH || order.getStatus() == OrderStatus.BOOKING_CREATED) {
            order.setStatus(OrderStatus.PARTNER_ACCEPTED);
        }
        order.setAssignedPartnerId(partnerId);
        orderRepository.save(order);
        syncTrackingStateFromOrder(order, order.getStatus());
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
    public int retryDispatch(Long orderId) {
        Order order = orderRepository.findById(orderId).orElse(null);
        if (order == null || order.getUserId() == null) {
            return 0;
        }
        PartnerServiceRequestEntity dispatchSnapshot = partnerServiceRequestRepository.findByCartId(orderId).stream()
                .max(Comparator.comparing(PartnerServiceRequestEntity::getCreatedAt))
                .orElse(null);
        if (dispatchSnapshot == null) {
            return 0;
        }
        LexhoAddress retryAddress = snapshotAddress(dispatchSnapshot);
        ResolvedService resolvedService = resolveRequestedService(dispatchSnapshot.getServiceId());
        DispatchContext context = buildDispatchContext(
                order,
                retryAddress,
                dispatchSnapshot.getQuantity(),
                dispatchSnapshot.getScheduledTime(),
                resolvedService
        );
        startPartnerSearch(context);
        Set<Long> requestedPartnerIds = partnerServiceRequestRepository.findByCartId(orderId).stream()
                .map(PartnerServiceRequestEntity::getPartnerId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        List<Candidate> expandedEligible = findEligibleCandidates(context, getMaxSearchRadiusKm(), requestedPartnerIds);
        if (expandedEligible.isEmpty()) {
            saveEvent(orderId, "PARTNER_SEARCH_FAILED", Map.of("reason", "NO_ELIGIBLE_PARTNER"));
            return 0;
        }
        expandedEligible.sort(dispatchScoringPolicy.comparator(context.pincode()));
        int nextAttempt = partnerServiceRequestRepository.findByCartId(orderId).stream()
                .map(PartnerServiceRequestEntity::getDispatchAttempt)
                .filter(Objects::nonNull)
                .max(Integer::compareTo)
                .orElse(0) + 1;
        int created = createPartnerRequests(context, expandedEligible, Math.max(2, nextAttempt));
        if (created > 0) {
            saveEvent(orderId, "PARTNER_SEARCH_RADIUS_EXPANDED", Map.of("toRadiusKm", getMaxSearchRadiusKm()));
        }
        return created;
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
        Order order = orderRepository.findByIdForUpdate(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));
        OrderStatus current = order.getStatus();
        if (current == null) {
            current = OrderStatus.BOOKING_CREATED;
        }
        if (!current.canTransitionTo(targetStatus)) {
            throw new BadRequestException("Invalid order status transition from " + current + " to " + targetStatus);
        }
        order.setStatus(targetStatus);
        applyLifecycleTimestamp(order, targetStatus);
        orderRepository.save(order);
        syncTrackingStateFromOrder(order, targetStatus);
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
            return getMaxEtaMinutes() + 1;
        }
        double hours = distanceKm.doubleValue() / ETA_SPEED_KMPH;
        int minutes = (int) Math.ceil(hours * 60.0);
        return Math.max(minutes, 1);
    }

    public int maxEtaMinutes() {
        return getMaxEtaMinutes();
    }

    public record Candidate(PartnerShiftEntity shift, Partner partner, BigDecimal distance, int etaMinutes) {}

    private record ResolvedService(Long subServiceId, AdminServiceEntity service) {}

    private record DispatchContext(Long orderId,
                                   Long customerId,
                                   Integer quantity,
                                   LocalDateTime scheduledAt,
                                   Order order,
                                   LexhoAddress address,
                                   String pincode,
                                   Long resolvedSubServiceId,
                                   Long requestedCategoryId,
                                   boolean instant,
                                   boolean scheduled) {}

    private boolean shouldActivateScheduledDispatch(LocalDateTime scheduledAt) {
        if (scheduledAt == null) {
            return true;
        }
        LocalDateTime activationAt = scheduledAt.minusMinutes(getScheduledPreDispatchMinutes());
        return !now().isBefore(activationAt);
    }

    private void syncTrackingStateFromOrder(Order order, OrderStatus status) {
        if (order == null) {
            return;
        }
        Long partnerId = extractAssignedPartnerId(order);
        if (partnerId == null) {
            return;
        }
        TrackingLifecycleState state = trackingLifecycleService.deriveFromOrder(
                order.getBookingType(),
                status,
                TrackingLifecycleState.ASSIGNED
        );
        trackingLifecycleService.persistState(partnerId, state, order.getId());
    }

    private void applyLifecycleTimestamp(Order order, OrderStatus status) {
        LocalDateTime now = now();
        switch (status) {
            case ARRIVED, PARTNER_REACHED -> {
                if (order.getArrivedAt() == null) {
                    order.setArrivedAt(now);
                }
            }
            case SERVICE_STARTED -> {
                if (order.getStartedAt() == null) {
                    order.setStartedAt(now);
                }
            }
            case SERVICE_IN_PROGRESS -> {
                if (order.getInProgressAt() == null) {
                    order.setInProgressAt(now);
                }
            }
            case SERVICE_COMPLETED, DELIVERED, PAYMENT_COMPLETED -> {
                if (order.getCompletedAt() == null) {
                    order.setCompletedAt(now);
                }
            }
            default -> {
            }
        }
    }

    private Long extractAssignedPartnerId(Order order) {
        return order.getAssignedPartnerId();
    }

    private LocalDateTime now() {
        return LocalDateTime.now(APPLICATION_ZONE);
    }

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

    private List<Long> findNearbyPartnerIds(DispatchContext context, double radiusKm) {
        if (context.address().getLatitude() == null || context.address().getLongitude() == null) {
            return List.of();
        }
        return currentPartnerLocationRepository.findNearbyPartnerIds(
                context.address().getLatitude().doubleValue(),
                context.address().getLongitude().doubleValue(),
                radiusKm,
                200
        );
    }

    private boolean isApprovedPartner(Partner partner) {
        String verificationStatus = partner.getVerificationStatus();
        if (verificationStatus == null || verificationStatus.isBlank()) {
            return false;
        }
        String normalized = verificationStatus.trim().toUpperCase(Locale.ROOT);
        return "APPROVED".equals(normalized) || "VERIFIED".equals(normalized);
    }

    private double getInitialSearchRadiusKm() {
        return Math.max(0.1d, Math.min(getMaxSearchRadiusKm(), dispatchProperties.getPartner().getInitialSearchRadiusKm()));
    }

    private double getMaxSearchRadiusKm() {
        return Math.max(0.1d, Math.min(25.0d, dispatchProperties.getPartner().getMaxSearchRadiusKm()));
    }

    private int getMaxEtaMinutes() {
        return Math.max(1, Math.min(120, dispatchProperties.getBooking().getMaxEtaMinutes()));
    }

    private int getScheduledPreDispatchMinutes() {
        return Math.max(5, Math.min(240, dispatchProperties.getBooking().getPreDispatchMinutes()));
    }

    private DispatchContext buildDispatchContext(Order order,
                                                 LexhoAddress address,
                                                 Integer quantity,
                                                 LocalDateTime scheduledAt,
                                                 ResolvedService resolvedService) {
        return new DispatchContext(
                order.getId(),
                order.getUserId(),
                quantity == null ? 1 : quantity,
                scheduledAt,
                order,
                address,
                normalizePincode(address.getPincode()),
                resolvedService.subServiceId(),
                categoryId(resolvedService.service()),
                BookingType.INSTANT.name().equalsIgnoreCase(order.getBookingType()),
                BookingType.SCHEDULED.name().equalsIgnoreCase(order.getBookingType())
        );
    }

    private LexhoAddress snapshotAddress(PartnerServiceRequestEntity snapshot) {
        if (snapshot.getLatitude() == null || snapshot.getLongitude() == null || snapshot.getTargetPincode() == null) {
            throw new BadRequestException("Dispatch snapshot is incomplete");
        }
        LexhoAddress address = new LexhoAddress();
        address.setHouseNo(snapshot.getFullAddress());
        address.setArea(snapshot.getFullAddress());
        address.setCity(snapshot.getCity());
        address.setPincode(snapshot.getTargetPincode());
        address.setLatitude(snapshot.getLatitude());
        address.setLongitude(snapshot.getLongitude());
        return address;
    }
}

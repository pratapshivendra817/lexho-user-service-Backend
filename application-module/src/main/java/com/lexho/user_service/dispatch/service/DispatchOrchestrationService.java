package com.lexho.user_service.dispatch.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lexho.booking.entity.Order;
import com.lexho.booking.repository.OrderRepository;
import com.lexho.common.enums.OrderStatus;
import com.lexho.common.enums.PaymentStatus;
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
import com.lexho.user_service.partnerrequest.service.PartnerDispatchRealtimeService;
import com.lexho.provider.entity.Partner;
import com.lexho.user_service.lexhopartner.service.PartnerMarketplaceService;
import com.lexho.user_service.booking.model.BookingType;
import com.lexho.user_service.booking.service.BookingLifecycleEventService;
import com.lexho.user_service.util.GeoDistanceCalculator;
import org.springframework.beans.factory.annotation.Autowired;
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
    public static final int MAX_SCHEDULED_DISPATCH_ATTEMPTS = 5;
    private static final double ETA_SPEED_KMPH = 25.0;
    private static final ZoneId APPLICATION_ZONE = ZoneId.systemDefault();
    private static final String EVENT_PARTNER_SEARCH_FAILED = "PARTNER_SEARCH_FAILED";
    private static final String EVENT_DISPATCH_REJECTED = "DISPATCH_REJECTED";
    private static final String REASON_KEY = "reason";
    private static final String PARTNER_ID_KEY = "partnerId";
    private static final String PINCODE_KEY = "pincode";
    private static final String NO_ELIGIBLE_PARTNER_REASON = "NO_ELIGIBLE_PARTNER";
    private static final String DISPATCH_METADATA_MISSING_REASON = "DISPATCH_METADATA_MISSING";
    private static final String ORDER_NOT_FOUND_MESSAGE = "Order not found";
    private static final String SCHEDULED_DISPATCH_ATTEMPT_EVENT = "SCHEDULED_DISPATCH_ATTEMPT";

    private final OrderRepository orderRepository;
    private final LexhoAddressRepository lexhoAddressRepository;
    private final ServiceableAreaRepository serviceableAreaRepository;
    private final PartnerShiftRepository partnerShiftRepository;
    private final CurrentPartnerLocationRepository currentPartnerLocationRepository;
    private final PincodeAdjacencyRepository pincodeAdjacencyRepository;
    private final PartnerMarketplaceService partnerMarketplaceService;
    private final PartnerServiceRequestRepository partnerServiceRequestRepository;
    private final PartnerServiceSkillRepository partnerServiceSkillRepository;
    private final AdminServiceEntityRepository adminServiceEntityRepository;
    private final BookingEventRepository bookingEventRepository;
    private final DispatchScoringPolicy dispatchScoringPolicy;
    private final ObjectMapper objectMapper;
    private final DispatchProperties dispatchProperties;
    private final TrackingLifecycleService trackingLifecycleService;
    private final PartnerDispatchEligibilityService partnerDispatchEligibilityService;
    private final BookingLifecycleEventService bookingLifecycleEventService;
    private final PartnerDispatchRealtimeService partnerDispatchRealtimeService;

    @Autowired
    public DispatchOrchestrationService(OrderRepository orderRepository,
                                        LexhoAddressRepository lexhoAddressRepository,
                                        ServiceableAreaRepository serviceableAreaRepository,
                                        PartnerShiftRepository partnerShiftRepository,
                                        CurrentPartnerLocationRepository currentPartnerLocationRepository,
                                        PincodeAdjacencyRepository pincodeAdjacencyRepository,
                                        com.lexho.provider.repository.PartnerRepository partnerRepository,
                                        PartnerServiceRequestRepository partnerServiceRequestRepository,
                                        PartnerServiceSkillRepository partnerServiceSkillRepository,
                                        AdminServiceEntityRepository adminServiceEntityRepository,
                                        BookingEventRepository bookingEventRepository,
                                        DispatchScoringPolicy dispatchScoringPolicy,
                                        ObjectMapper objectMapper,
                                        TrackingLifecycleService trackingLifecycleService,
                                        DispatchProperties dispatchProperties,
                                        PartnerDispatchEligibilityService partnerDispatchEligibilityService,
                                        BookingLifecycleEventService bookingLifecycleEventService,
                                        PartnerDispatchRealtimeService partnerDispatchRealtimeService) {
        this.orderRepository = orderRepository;
        this.lexhoAddressRepository = lexhoAddressRepository;
        this.serviceableAreaRepository = serviceableAreaRepository;
        this.partnerShiftRepository = partnerShiftRepository;
        this.currentPartnerLocationRepository = currentPartnerLocationRepository;
        this.pincodeAdjacencyRepository = pincodeAdjacencyRepository;
        this.partnerMarketplaceService = new PartnerMarketplaceService(partnerRepository);
        this.partnerServiceRequestRepository = partnerServiceRequestRepository;
        this.partnerServiceSkillRepository = partnerServiceSkillRepository;
        this.adminServiceEntityRepository = adminServiceEntityRepository;
        this.bookingEventRepository = bookingEventRepository;
        this.dispatchScoringPolicy = dispatchScoringPolicy;
        this.objectMapper = objectMapper;
        this.trackingLifecycleService = trackingLifecycleService;
        this.dispatchProperties = dispatchProperties;
        this.partnerDispatchEligibilityService = partnerDispatchEligibilityService;
        this.bookingLifecycleEventService = bookingLifecycleEventService;
        this.partnerDispatchRealtimeService = partnerDispatchRealtimeService;
    }

    DispatchOrchestrationService(OrderRepository orderRepository,
                                 LexhoAddressRepository lexhoAddressRepository,
                                 ServiceableAreaRepository serviceableAreaRepository,
                                 PartnerShiftRepository partnerShiftRepository,
                                 CurrentPartnerLocationRepository currentPartnerLocationRepository,
                                 PincodeAdjacencyRepository pincodeAdjacencyRepository,
                                 com.lexho.provider.repository.PartnerRepository partnerRepository,
                                 PartnerServiceRequestRepository partnerServiceRequestRepository,
                                 PartnerServiceSkillRepository partnerServiceSkillRepository,
                                 AdminServiceEntityRepository adminServiceEntityRepository,
                                 BookingEventRepository bookingEventRepository,
                                 DispatchScoringPolicy dispatchScoringPolicy,
                                 ObjectMapper objectMapper,
                                 TrackingLifecycleService trackingLifecycleService,
                                 DispatchProperties dispatchProperties,
                                 PartnerDispatchEligibilityService partnerDispatchEligibilityService) {
        this(orderRepository, lexhoAddressRepository, serviceableAreaRepository, partnerShiftRepository,
                currentPartnerLocationRepository, pincodeAdjacencyRepository, partnerRepository,
                partnerServiceRequestRepository, partnerServiceSkillRepository, adminServiceEntityRepository,
                bookingEventRepository, dispatchScoringPolicy, objectMapper, trackingLifecycleService,
                dispatchProperties, partnerDispatchEligibilityService, null, null);
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
        saveBookingEvent(orderId, "BOOKING_CREATED", Map.of("orderId", orderId, "customerId", customerId));

        if (shouldWaitForPayment(context)) {
            saveBookingEvent(orderId, "DISPATCH_PAYMENT_PENDING", Map.of(REASON_KEY, "PAYMENT_PENDING"));
            publishLifecycle(context.order(), "PAYMENT_PENDING", Map.of(REASON_KEY, "PAYMENT_PENDING"));
            return 0;
        }

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
        Order order = orderRepository.findByIdForUpdate(orderId)
                .orElseThrow(() -> new ResourceNotFoundException(ORDER_NOT_FOUND_MESSAGE));
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
        Partner partnerFromLegacyId = partnerMarketplaceService.find(serviceId).orElse(null);
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
        saveBookingEvent(context.orderId(), EVENT_PARTNER_SEARCH_FAILED,
                Map.of(REASON_KEY, "PINCODE_NOT_SERVICEABLE", PINCODE_KEY, context.pincode()));
        throw new BadRequestException("Service is not available for this pincode");
    }

    private boolean shouldWaitForPayment(DispatchContext context) {
        return (context.instant() || context.scheduled())
                && context.order().getPaymentStatus() != PaymentStatus.SUCCESS;
    }

    private boolean shouldDeferDispatch(DispatchContext context) {
        if (!context.scheduled() || shouldActivateScheduledDispatch(context.scheduledAt())) {
            return false;
        }
        saveBookingEvent(context.orderId(), "DISPATCH_DEFERRED", Map.of(
                REASON_KEY, "PRE_ARRIVAL_WINDOW_NOT_REACHED",
                "scheduledAt", context.scheduledAt(),
                "activationMinutesBefore", getScheduledPreDispatchMinutes()
        ));
        return true;
    }

    private void startPartnerSearch(DispatchContext context) {
        saveBookingEvent(context.orderId(), context.instant() ? "SEARCHING_PARTNER" : "PARTNER_SEARCH",
                Map.of(PINCODE_KEY, context.pincode()));
        OrderStatus searchStatus = context.instant() ? OrderStatus.SEARCHING_PARTNER : OrderStatus.PARTNER_SEARCH;
        if (canEnterPartnerSearch(context.order().getStatus(), searchStatus)
                && applyStatusTransition(context.order(), searchStatus)) {
            orderRepository.save(context.order());
            publishLifecycle(context.order(), searchStatus.name(), Map.of(PINCODE_KEY, context.pincode()));
        }
    }

    private boolean canEnterPartnerSearch(OrderStatus currentStatus, OrderStatus searchStatus) {
        if (currentStatus == null) {
            return true;
        }
        if (currentStatus == OrderStatus.BOOKING_CREATED && currentStatus.canTransitionTo(searchStatus)) {
            return true;
        }
        return currentStatus == OrderStatus.CREATED
                || currentStatus == OrderStatus.CONFIRMED
                || currentStatus == OrderStatus.SCHEDULED;
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
            saveBookingEvent(context.orderId(), EVENT_PARTNER_SEARCH_FAILED,
                    Map.of(REASON_KEY, NO_ELIGIBLE_PARTNER_REASON));
            return 0;
        }

        saveBookingEvent(context.orderId(), "PARTNER_SEARCH_RADIUS_EXPANDED", Map.of(
                "fromRadiusKm", getInitialSearchRadiusKm(),
                "toRadiusKm", getMaxSearchRadiusKm()
        ));
        List<Candidate> expandedEligible = findEligibleCandidates(context, getMaxSearchRadiusKm(), requestedPartnerIds);
        if (expandedEligible.isEmpty()) {
            saveBookingEvent(context.orderId(), EVENT_PARTNER_SEARCH_FAILED,
                    Map.of(REASON_KEY, NO_ELIGIBLE_PARTNER_REASON));
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
        List<PartnerShiftEntity> shifts = partnerShiftRepository.findByPartnerIdInAndDeletedFalse(nearbyPartnerIds).stream()
                .filter(shift -> !shift.isDeleted())
                .filter(shift -> !excludedPartnerIds.contains(shift.getPartnerId()))
                .toList();
        if (shifts.isEmpty()) {
            return List.of();
        }

        Map<Long, Partner> partnersById = findPartnersById(shifts);
        List<Candidate> eligible = new ArrayList<>();
        for (PartnerShiftEntity shift : shifts) {
            candidateForShift(context, partnersById, shift, radiusKm).ifPresent(eligible::add);
        }
        return eligible;
    }

    private Map<Long, Partner> findPartnersById(List<PartnerShiftEntity> onlineShifts) {
        Map<Long, Partner> partnersById = new HashMap<>();
        List<Long> partnerIds = onlineShifts.stream().map(PartnerShiftEntity::getPartnerId).toList();
        for (Partner partner : partnerMarketplaceService.findAllByIds(partnerIds)) {
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
        PartnerDispatchEligibilityResult eligibility =
                partnerDispatchEligibilityService.canReceiveNewBookings(partner, shift);
        if (!eligibility.eligible()) {
            saveBookingEvent(context.orderId(), EVENT_DISPATCH_REJECTED, Map.of(
                    PARTNER_ID_KEY, shift == null ? null : shift.getPartnerId(),
                    REASON_KEY, eligibility.reason().name()
            ));
            return false;
        }
        boolean skilled = hasRequiredSkill(partner, context.resolvedSubServiceId(), context.requestedCategoryId());
        if (!skilled) {
            saveBookingEvent(context.orderId(), EVENT_DISPATCH_REJECTED, Map.of(
                    PARTNER_ID_KEY, shift == null ? null : shift.getPartnerId(),
                    REASON_KEY, "SKILL_MISMATCH"
            ));
        }
        return skilled;
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
            if (!revalidateCandidateForNewDispatch(context, candidate)) {
                continue;
            }
            PartnerServiceRequestEntity saved = partnerServiceRequestRepository.save(
                    toPartnerServiceRequest(context, candidate, dispatchAttempt));
            publishPartnerDispatch(saved, "REQUEST_CREATED");
            saveBookingEvent(context.orderId(), "DISPATCH_ELIGIBLE",
                    Map.of(PARTNER_ID_KEY, candidate.partner().getId()));
            publishLifecycle(context.order(), "PARTNER_REQUESTED",
                    Map.of(PARTNER_ID_KEY, candidate.partner().getId()));
            created++;
        }
        return created;
    }

    private boolean revalidateCandidateForNewDispatch(DispatchContext context, Candidate candidate) {
        PartnerShiftEntity lockedShift = partnerShiftRepository.findByPartnerIdForUpdate(candidate.partner().getId()).orElse(null);
        Partner lockedPartner = partnerMarketplaceService.find(candidate.partner().getId()).orElse(null);
        PartnerDispatchEligibilityResult eligibility =
                partnerDispatchEligibilityService.canReceiveNewBookings(lockedPartner, lockedShift);
        if (eligibility.eligible()
                && hasRequiredSkill(lockedPartner, context.resolvedSubServiceId(), context.requestedCategoryId())) {
            return true;
        }
        saveBookingEvent(context.orderId(), EVENT_DISPATCH_REJECTED, Map.of(
                PARTNER_ID_KEY, candidate.partner().getId(),
                REASON_KEY, eligibility.eligible() ? "SKILL_MISMATCH" : eligibility.reason().name()
        ));
        return false;
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
            saveBookingEvent(context.orderId(), EVENT_PARTNER_SEARCH_FAILED,
                    Map.of(REASON_KEY, "NO_NEW_REQUEST_CREATED"));
            return;
        }
        saveBookingEvent(context.orderId(), context.instant() ? "INSTANT_DISPATCHED" : "PARTNER_ASSIGNED",
                Map.of("candidateCount", created, PINCODE_KEY, context.pincode()));
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
        Order order = orderRepository.findByIdForUpdate(orderId)
                .orElseThrow(() -> new ResourceNotFoundException(ORDER_NOT_FOUND_MESSAGE));
        if (order.getAssignedPartnerId() != null && order.getAssignedPartnerId().equals(partnerId)
                && isAcceptedOrAssigned(order.getStatus())) {
            return;
        }
        if (order.getAssignedPartnerId() != null && !order.getAssignedPartnerId().equals(partnerId)) {
            throw new BadRequestException("Order is already assigned to another partner");
        }
        PartnerShiftEntity lockedShift = partnerShiftRepository.findByPartnerIdForUpdate(partnerId).orElse(null);
        Partner lockedPartner = partnerMarketplaceService.find(partnerId).orElse(null);
        PartnerDispatchEligibilityResult eligibility =
                partnerDispatchEligibilityService.canReceiveNewBookings(lockedPartner, lockedShift);
        if (!eligibility.eligible()) {
            saveBookingEvent(orderId, EVENT_DISPATCH_REJECTED,
                    Map.of(PARTNER_ID_KEY, partnerId, REASON_KEY, eligibility.reason().name()));
            throw new BadRequestException("Partner is not eligible for new dispatch: " + eligibility.reason().name());
        }
        boolean instant = BookingType.INSTANT.name().equalsIgnoreCase(order.getBookingType());
        if (instant) {
            if (!isAcceptableInstantStatus(order.getStatus())) {
                throw new BadRequestException("Order is not accepting partner assignment");
            }
            applyStatusTransition(order, OrderStatus.PARTNER_ASSIGNED);
            order.setSlaMinutes(INSTANT_SLA_MINUTES);
            int eta = etaMinutesAtAcceptance == null ? INSTANT_SLA_MINUTES : etaMinutesAtAcceptance;
            order.setPromisedStartTime(now().plusMinutes(eta));
            order.setUnavailableReason(null);
        } else if (order.getStatus() == OrderStatus.CREATED || order.getStatus() == OrderStatus.PARTNER_ASSIGNED
                || order.getStatus() == OrderStatus.PARTNER_SEARCH || order.getStatus() == OrderStatus.BOOKING_CREATED) {
            applyStatusTransition(order, OrderStatus.PARTNER_ACCEPTED);
        }
        order.setAssignedPartnerId(partnerId);
        orderRepository.save(order);
        syncTrackingStateFromOrder(order, order.getStatus());
        PartnerShiftEntity shift = lockedShift;
        if (shift != null) {
            shift.setActiveJobsCount(shift.getActiveJobsCount() + 1);
            partnerShiftRepository.save(shift);
        }
        saveBookingEvent(orderId, "PARTNER_ACCEPTED",
                Map.of(PARTNER_ID_KEY, partnerId, "etaMinutes", etaMinutesAtAcceptance));
        publishLifecycle(order, "PARTNER_ACCEPTED",
                Map.of(PARTNER_ID_KEY, partnerId, "etaMinutes", etaMinutesAtAcceptance));
    }

    private boolean isAcceptedOrAssigned(OrderStatus status) {
        return status == OrderStatus.PARTNER_ASSIGNED || status == OrderStatus.PARTNER_ACCEPTED;
    }

    private boolean isAcceptableInstantStatus(OrderStatus status) {
        return status == OrderStatus.CREATED
                || status == OrderStatus.CONFIRMED
                || status == OrderStatus.BOOKING_CREATED
                || status == OrderStatus.SEARCHING_PARTNER;
    }

    @Transactional
    public void markPartnerRejected(Long orderId, Long partnerId) {
        saveBookingEvent(orderId, "PARTNER_REJECTED", Map.of(PARTNER_ID_KEY, partnerId));
    }

    @Transactional
    public int retryDispatch(Long orderId) {
        Order order = orderRepository.findByIdForUpdate(orderId).orElse(null);
        if (order == null || order.getUserId() == null) {
            return 0;
        }
        if (BookingType.INSTANT.name().equalsIgnoreCase(order.getBookingType())
                && order.getPaymentStatus() != PaymentStatus.SUCCESS) {
            saveBookingEvent(orderId, "DISPATCH_PAYMENT_PENDING", Map.of(REASON_KEY, "PAYMENT_PENDING"));
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
            saveBookingEvent(orderId, EVENT_PARTNER_SEARCH_FAILED, Map.of(REASON_KEY, NO_ELIGIBLE_PARTNER_REASON));
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
            saveBookingEvent(orderId, "PARTNER_SEARCH_RADIUS_EXPANDED", Map.of("toRadiusKm", getMaxSearchRadiusKm()));
        }
        return created;
    }

    @Transactional
    public int dispatchPaidInstantBooking(Long orderId) {
        Order order = orderRepository.findByIdForUpdate(orderId)
                .orElseThrow(() -> new ResourceNotFoundException(ORDER_NOT_FOUND_MESSAGE));
        if (!BookingType.INSTANT.name().equalsIgnoreCase(order.getBookingType())) {
            return 0;
        }
        if (order.getPaymentStatus() != PaymentStatus.SUCCESS) {
            saveBookingEvent(orderId, "DISPATCH_PAYMENT_PENDING", Map.of(REASON_KEY, "PAYMENT_PENDING"));
            return 0;
        }
        if (order.getDispatchStartedAt() != null
                || order.getAssignedPartnerId() != null
                || order.getStatus() == OrderStatus.PARTNER_ASSIGNED
                || order.getStatus() == OrderStatus.PARTNER_ACCEPTED) {
            saveBookingEvent(orderId, "DISPATCH_ALREADY_STARTED", Map.of("status", order.getStatus().name()));
            return 0;
        }
        if (!isEligibleForInitialDispatch(order.getStatus())
                && order.getStatus() != OrderStatus.SEARCHING_PARTNER) {
            saveBookingEvent(orderId, "DISPATCH_SKIPPED_INVALID_STATUS",
                    Map.of("status", order.getStatus() == null ? "UNKNOWN" : order.getStatus().name()));
            return 0;
        }
        if (order.getDispatchAddressId() == null || order.getDispatchServiceId() == null) {
            saveBookingEvent(orderId, EVENT_PARTNER_SEARCH_FAILED,
                    Map.of(REASON_KEY, DISPATCH_METADATA_MISSING_REASON));
            throw new BadRequestException("Dispatch metadata is missing for booking " + orderId);
        }
        order.setDispatchStartedAt(now());
        orderRepository.save(order);
        publishLifecycle(order, "PAYMENT_CONFIRMED", Map.of());
        return dispatchOrderInternal(
                order.getId(),
                order.getUserId(),
                order.getDispatchAddressId(),
                order.getDispatchServiceId(),
                order.getDispatchQuantity() == null ? 1 : order.getDispatchQuantity(),
                order.getScheduledAt()
        );
    }

    @Transactional
    public int dispatchPaidScheduledBooking(Long orderId) {
        Order order = orderRepository.findByIdForUpdate(orderId)
                .orElseThrow(() -> new ResourceNotFoundException(ORDER_NOT_FOUND_MESSAGE));
        if (!BookingType.SCHEDULED.name().equalsIgnoreCase(order.getBookingType())) {
            return 0;
        }
        if (order.getPaymentStatus() != PaymentStatus.SUCCESS) {
            saveBookingEvent(orderId, "DISPATCH_PAYMENT_PENDING", Map.of(REASON_KEY, "PAYMENT_PENDING"));
            return 0;
        }
        if (order.getDispatchStartedAt() != null
                || order.getAssignedPartnerId() != null
                || order.getScheduledAt() == null
                || !shouldActivateScheduledDispatch(order.getScheduledAt())) {
            if (order.getDispatchStartedAt() != null || order.getAssignedPartnerId() != null) {
                saveBookingEvent(orderId, "DISPATCH_ALREADY_STARTED",
                        Map.of("status", order.getStatus() == null ? "UNKNOWN" : order.getStatus().name()));
            }
            return 0;
        }
        if (!isEligibleForInitialDispatch(order.getStatus())
                && order.getStatus() != OrderStatus.SCHEDULED
                && order.getStatus() != OrderStatus.PARTNER_SEARCH) {
            saveBookingEvent(orderId, "DISPATCH_SKIPPED_INVALID_STATUS",
                    Map.of("status", order.getStatus() == null ? "UNKNOWN" : order.getStatus().name()));
            return 0;
        }
        long previousAttempts = bookingEventRepository.countByBookingIdAndEventType(
                orderId, SCHEDULED_DISPATCH_ATTEMPT_EVENT);
        if (previousAttempts >= MAX_SCHEDULED_DISPATCH_ATTEMPTS) {
            order.setUnavailableReason("NO_PARTNER_AVAILABLE_MAX_ATTEMPTS");
            orderRepository.save(order);
            saveBookingEvent(orderId, "SCHEDULED_DISPATCH_EXHAUSTED",
                    Map.of("attempts", previousAttempts, REASON_KEY, "NO_PARTNER_AVAILABLE"));
            publishLifecycle(order, "DISPATCH_FAILED",
                    Map.of("attempts", previousAttempts, REASON_KEY, "NO_PARTNER_AVAILABLE"));
            return 0;
        }
        if (order.getDispatchAddressId() == null || order.getDispatchServiceId() == null) {
            order.setUnavailableReason(DISPATCH_METADATA_MISSING_REASON);
            orderRepository.save(order);
            saveBookingEvent(orderId, EVENT_PARTNER_SEARCH_FAILED,
                    Map.of(REASON_KEY, DISPATCH_METADATA_MISSING_REASON));
            publishLifecycle(order, "DISPATCH_FAILED",
                    Map.of(REASON_KEY, DISPATCH_METADATA_MISSING_REASON));
            return 0;
        }
        saveBookingEvent(orderId, SCHEDULED_DISPATCH_ATTEMPT_EVENT,
                Map.of("attempt", previousAttempts + 1));
        order.setDispatchStartedAt(now());
        order.setUnavailableReason(null);
        orderRepository.save(order);
        publishLifecycle(order, "PAYMENT_CONFIRMED", Map.of());
        int created = dispatchOrderInternal(
                order.getId(),
                order.getUserId(),
                order.getDispatchAddressId(),
                order.getDispatchServiceId(),
                order.getDispatchQuantity() == null ? 1 : order.getDispatchQuantity(),
                order.getScheduledAt()
        );
        if (created == 0) {
            order.setDispatchStartedAt(null);
            orderRepository.save(order);
            saveBookingEvent(orderId, "SCHEDULED_DISPATCH_RETRY_SCHEDULED",
                    Map.of("attempt", previousAttempts + 1, "nextScanSeconds", 60));
        }
        return created;
    }

    private boolean isEligibleForInitialDispatch(OrderStatus status) {
        return status == null
                || status == OrderStatus.BOOKING_CREATED
                || status == OrderStatus.CREATED
                || status == OrderStatus.CONFIRMED;
    }

    @Transactional
    public void handlePaymentFailed(Long orderId, String reason) {
        Order order = orderRepository.findByIdForUpdate(orderId).orElse(null);
        if (order == null || !BookingType.INSTANT.name().equalsIgnoreCase(order.getBookingType())) {
            return;
        }
        expirePendingRequests(orderId, "PAYMENT_FAILED");
        if (order.getStatus() == OrderStatus.SERVICE_STARTED
                || order.getStatus() == OrderStatus.SERVICE_IN_PROGRESS
                || order.getStatus() == OrderStatus.SERVICE_COMPLETED
                || order.getStatus() == OrderStatus.PAYMENT_COMPLETED
                || order.getStatus() == OrderStatus.DELIVERED) {
            return;
        }
        decrementActiveJobs(order.getAssignedPartnerId());
        order.setAssignedPartnerId(null);
        applyStatusTransition(order, OrderStatus.CANCELLED);
        order.setUnavailableReason("PAYMENT_FAILED");
        orderRepository.save(order);
        saveBookingEvent(orderId, "PAYMENT_FAILED", Map.of(REASON_KEY, reason == null ? "PAYMENT_FAILED" : reason));
        publishLifecycle(order, "PAYMENT_FAILED",
                Map.of(REASON_KEY, reason == null ? "PAYMENT_FAILED" : reason));
    }

    private void decrementActiveJobs(Long partnerId) {
        if (partnerId == null) {
            return;
        }
        partnerShiftRepository.findByPartnerIdForUpdate(partnerId).ifPresent(shift -> {
            shift.setActiveJobsCount(Math.max(0, shift.getActiveJobsCount() - 1));
            partnerShiftRepository.save(shift);
        });
    }

    @Transactional
    public int expireStuckInstantSearches(LocalDateTime createdBefore) {
        int expired = 0;
        for (Order candidate : orderRepository.findStuckInstantSearchOrders(
                BookingType.INSTANT.name(), OrderStatus.SEARCHING_PARTNER, createdBefore)) {
            Order order = orderRepository.findByIdForUpdate(candidate.getId()).orElse(null);
            if (order == null
                    || order.getAssignedPartnerId() != null
                    || order.getStatus() != OrderStatus.SEARCHING_PARTNER
                    || order.getPaymentStatus() != PaymentStatus.SUCCESS) {
                continue;
            }
            expirePendingRequests(order.getId(), "SEARCH_TIMEOUT");
            applyStatusTransition(order, OrderStatus.INSTANT_FAILED);
            order.setUnavailableReason("SEARCH_TIMEOUT");
            orderRepository.save(order);
            saveBookingEvent(order.getId(), "SEARCH_TIMEOUT", Map.of(REASON_KEY, "SEARCH_TIMEOUT"));
            publishLifecycle(order, "SEARCH_TIMEOUT", Map.of(REASON_KEY, "SEARCH_TIMEOUT"));
            expired++;
        }
        return expired;
    }

    private void expirePendingRequests(Long orderId, String reason) {
        List<PartnerServiceRequestEntity> pending =
                partnerServiceRequestRepository.findByCartIdAndStatus(orderId, PartnerServiceRequestStatus.PENDING);
        for (PartnerServiceRequestEntity request : pending) {
            request.setStatus(PartnerServiceRequestStatus.EXPIRED);
            request.setRejectionReason(reason);
        }
        partnerServiceRequestRepository.saveAll(pending);
    }

    @Transactional
    public void saveEvent(Long orderId, String eventType, Object payload) {
        saveBookingEvent(orderId, eventType, payload);
    }

    private void saveBookingEvent(Long orderId, String eventType, Object payload) {
        BookingEventEntity event = new BookingEventEntity();
        event.setBookingId(orderId);
        event.setEventType(eventType);
        event.setEventPayload(toJson(payload));
        bookingEventRepository.save(event);
    }

    @Transactional
    public OrderStatus transitionOrderStatus(Long orderId, OrderStatus targetStatus) {
        Order order = orderRepository.findByIdForUpdate(orderId)
                .orElseThrow(() -> new ResourceNotFoundException(ORDER_NOT_FOUND_MESSAGE));
        OrderStatus current = order.getStatus();
        if (current == null) {
            current = OrderStatus.BOOKING_CREATED;
        }
        if (current == targetStatus) {
            return targetStatus;
        }
        if (!current.canTransitionTo(targetStatus)) {
            throw new BadRequestException("Invalid order status transition from " + current + " to " + targetStatus);
        }
        order.setStatus(targetStatus);
        applyLifecycleTimestamp(order, targetStatus);
        orderRepository.save(order);
        syncTrackingStateFromOrder(order, targetStatus);
        saveBookingEvent(orderId, targetStatus.name(), Map.of("previous", current.name(), "next", targetStatus.name()));
        publishLifecycle(order, targetStatus.name(), Map.of("previous", current.name(), "next", targetStatus.name()));
        return targetStatus;
    }

    private boolean applyStatusTransition(Order order, OrderStatus targetStatus) {
        OrderStatus current = order.getStatus();
        if (current == targetStatus) {
            return false;
        }
        if (current == null) {
            current = OrderStatus.BOOKING_CREATED;
        }
        if (!current.canTransitionTo(targetStatus)) {
            throw new BadRequestException("Invalid order status transition from " + current + " to " + targetStatus);
        }
        order.setStatus(targetStatus);
        applyLifecycleTimestamp(order, targetStatus);
        return true;
    }

    private void publishLifecycle(Order order, String eventName, Map<String, Object> details) {
        if (bookingLifecycleEventService != null) {
            bookingLifecycleEventService.publish(order, eventName, details);
        }
    }

    private void publishPartnerDispatch(PartnerServiceRequestEntity request, String eventName) {
        if (partnerDispatchRealtimeService != null) {
            partnerDispatchRealtimeService.publishRequestEvent(request, eventName);
        }
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
        double km = GeoDistanceCalculator.haversineKm(
                address.getLatitude().doubleValue(),
                address.getLongitude().doubleValue(),
                location.getLatitude().doubleValue(),
                location.getLongitude().doubleValue()
        );
        return BigDecimal.valueOf(km).setScale(3, RoundingMode.HALF_UP);
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
        if (partner == null || partner.getId() == null || requestedSubServiceId == null) {
            return false;
        }
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

package com.lexho.user_service.partnerrequest.service;

import com.lexho.provider.entity.Partner;
import com.lexho.booking.entity.Order;
import com.lexho.booking.repository.OrderRepository;
import com.lexho.common.enums.OrderStatus;
import com.lexho.common.enums.PaymentStatus;
import com.lexho.user_service.booking.exception.InstantBookingUnavailableException;
import com.lexho.user_service.booking.model.BookingType;
import com.lexho.user_service.booking.model.InstantUnavailableCode;
import com.lexho.user_service.dispatch.service.DispatchOrchestrationService;
import com.lexho.user_service.dispatch.service.PartnerDispatchEligibilityService;
import com.lexho.user_service.dispatch.repository.PartnerServiceSkillRepository;
import com.lexho.user_service.dispatch.repository.PartnerShiftRepository;
import com.lexho.user_service.admin.entity.AdminServiceEntity;
import com.lexho.user_service.admin.repository.AdminServiceEntityRepository;
import com.lexho.user_service.exception.BadRequestException;
import com.lexho.user_service.exception.ResourceNotFoundException;
import com.lexho.user_service.partnerrequest.dto.PagedResponse;
import com.lexho.user_service.partnerrequest.dto.PartnerIncomingRequestResponse;
import com.lexho.user_service.partnerrequest.dto.PartnerRejectRequest;
import com.lexho.user_service.partnerrequest.dto.PartnerRequestActionResponse;
import com.lexho.user_service.partnerrequest.dto.PartnerRequestAddressResponse;
import com.lexho.user_service.partnerrequest.dto.PartnerStatusResponse;
import com.lexho.user_service.partnerrequest.dto.PartnerStatusUpdateRequest;
import com.lexho.user_service.partnerrequest.entity.PartnerServiceRequestEntity;
import com.lexho.user_service.partnerrequest.model.PartnerServiceRequestStatus;
import com.lexho.user_service.partnerrequest.repository.PartnerServiceRequestRepository;
import com.lexho.user_service.lexhopartner.repository.PartnerLeaveRepository;
import com.lexho.user_service.lexhopartner.repository.PartnerOperationalProfileRepository;
import com.lexho.user_service.lexhopartner.service.PartnerMarketplaceService;
import com.lexho.user_service.localization.dynamic.DynamicContentTranslationService;
import com.lexho.user_service.subscription.service.SubscriptionAutoDispatchService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class PartnerRequestService {
    private com.lexho.user_service.subscription.service.SubscriptionVisitOrderService subscriptionVisitOrders;

    @org.springframework.beans.factory.annotation.Autowired
    public void setSubscriptionVisitOrders(com.lexho.user_service.subscription.service.SubscriptionVisitOrderService service) {
        this.subscriptionVisitOrders = service;
    }

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final String PARTNER_NOT_FOUND = "partner not found";
    private static final String REQUEST_NOT_FOUND = "request not found";
    private static final String REQUEST_EXPIRED_REASON = "REQUEST_EXPIRED";
    private static final String ACCEPTED_BY_ANOTHER_PARTNER = "Accepted by another partner";

    private final PartnerMarketplaceService partnerMarketplaceService;
    private final PartnerServiceRequestRepository partnerServiceRequestRepository;
    private final DispatchOrchestrationService dispatchOrchestrationService;
    private final OrderRepository orderRepository;
    private final PartnerLeaveRepository partnerLeaveRepository;
    private final SubscriptionAutoDispatchService subscriptionAutoDispatchService;
    private final DynamicContentTranslationService dynamicTranslationService;
    private final PartnerShiftRepository partnerShiftRepository;
    private final PartnerDispatchEligibilityService partnerDispatchEligibilityService;
    private final PartnerOperationalProfileRepository partnerProfileRepository;
    private final PartnerDispatchRealtimeService partnerDispatchRealtimeService;

    @org.springframework.beans.factory.annotation.Autowired(required = false)
    private PartnerServiceSkillRepository partnerServiceSkillRepository;

    @org.springframework.beans.factory.annotation.Autowired(required = false)
    private AdminServiceEntityRepository adminServiceEntityRepository;

    public PartnerRequestService(com.lexho.provider.repository.PartnerRepository partnerRepository,
                                 PartnerServiceRequestRepository partnerServiceRequestRepository,
                                 DispatchOrchestrationService dispatchOrchestrationService,
                                 OrderRepository orderRepository,
                                 PartnerLeaveRepository partnerLeaveRepository,
                                 SubscriptionAutoDispatchService subscriptionAutoDispatchService,
                                 DynamicContentTranslationService dynamicTranslationService,
                                 PartnerShiftRepository partnerShiftRepository,
                                 PartnerDispatchEligibilityService partnerDispatchEligibilityService,
                                 PartnerOperationalProfileRepository partnerProfileRepository,
                                 PartnerDispatchRealtimeService partnerDispatchRealtimeService) {
        this.partnerMarketplaceService = new PartnerMarketplaceService(partnerRepository);
        this.partnerServiceRequestRepository = partnerServiceRequestRepository;
        this.dispatchOrchestrationService = dispatchOrchestrationService;
        this.orderRepository = orderRepository;
        this.partnerLeaveRepository = partnerLeaveRepository;
        this.subscriptionAutoDispatchService = subscriptionAutoDispatchService;
        this.dynamicTranslationService = dynamicTranslationService;
        this.partnerShiftRepository = partnerShiftRepository;
        this.partnerDispatchEligibilityService = partnerDispatchEligibilityService;
        this.partnerProfileRepository = partnerProfileRepository;
        this.partnerDispatchRealtimeService = partnerDispatchRealtimeService;
    }

    @org.springframework.beans.factory.annotation.Autowired
    public void setPartnerMarketplaceService(PartnerMarketplaceService service) {
        // Spring-provided facade supersedes the compatibility constructor dependency.
    }

    @Transactional
    public PartnerStatusResponse updateOnlineStatus(Long partnerId, PartnerStatusUpdateRequest request) {
        Partner partner = partnerMarketplaceService.require(partnerId);
        var profile = partnerProfileRepository.findById(partnerId).orElse(null);
        if (profile != null
                && (profile.getPartnerOperationalStatus() == com.lexho.user_service.lexhopartner.model.PartnerOperationalStatus.BLOCKED
                || profile.getPartnerOperationalStatus() == com.lexho.user_service.lexhopartner.model.PartnerOperationalStatus.REJECTED)) {
            throw new BadRequestException("Blocked or rejected partners cannot be activated");
        }
        if (profile != null) {
            profile.setOnline(Boolean.TRUE.equals(request.getIsOnline()));
            partnerProfileRepository.save(profile);
        }
        partner.setAvailable(profile == null
                ? Boolean.TRUE.equals(request.getIsOnline())
                : profile.isOnline());
        Partner saved = partnerMarketplaceService.save(partner);
        PartnerStatusResponse response = new PartnerStatusResponse();
        response.setPartnerId(saved.getId());
        response.setOnline(Boolean.TRUE.equals(saved.getAvailable()));
        return response;
    }

    @Transactional(readOnly = true)
    public PagedResponse<PartnerIncomingRequestResponse> getIncomingRequests(Long partnerId,
                                                                              PartnerServiceRequestStatus status,
                                                                              int page,
                                                                              int size) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), 100);
        if (partnerLeaveRepository.existsByPartnerIdAndFromDateLessThanEqualAndToDateGreaterThanEqual(
                partnerId, LocalDateTime.now().toLocalDate(), LocalDateTime.now().toLocalDate())) {
            return new PagedResponse<>(List.of(), 0, 0, safePage, safeSize);
        }
        PageRequest pageable = PageRequest.of(safePage, safeSize);
        Page<PartnerServiceRequestEntity> rows = status == null
                ? partnerServiceRequestRepository.findByPartnerIdOrderByCreatedAtDesc(partnerId, pageable)
                : partnerServiceRequestRepository.findByPartnerIdAndStatusOrderByCreatedAtDesc(partnerId, status, pageable);
        List<PartnerIncomingRequestResponse> content = rows.getContent().stream().map(this::expireIfNeeded).map(this::mapIncoming).toList();
        return new PagedResponse<>(content, rows.getTotalElements(), rows.getTotalPages(), safePage, safeSize);
    }

    @Transactional(readOnly = true)
    public PartnerIncomingRequestResponse getRequestDetail(Long partnerId, Long requestId) {
        PartnerServiceRequestEntity request = partnerServiceRequestRepository.findByIdAndPartnerId(requestId, partnerId)
                .orElseThrow(() -> new ResourceNotFoundException(REQUEST_NOT_FOUND));
        return mapIncoming(expireIfNeeded(request));
    }

    @Transactional(noRollbackFor = {BadRequestException.class, InstantBookingUnavailableException.class})
    public PartnerRequestActionResponse accept(Long partnerId, Long requestId) {
        PartnerServiceRequestEntity request = partnerServiceRequestRepository.findByIdAndPartnerIdForUpdate(requestId, partnerId)
                .orElseThrow(() -> new ResourceNotFoundException(REQUEST_NOT_FOUND));
        if (request.getStatus() == PartnerServiceRequestStatus.ACCEPTED && isAlreadyAssignedToPartner(request, partnerId)) {
            return actionResponse(request);
        }
        if (request.getStatus() != PartnerServiceRequestStatus.PENDING) {
            throw new BadRequestException("request is not pending");
        }
        if (request.getExpiresAt() != null && LocalDateTime.now().isAfter(request.getExpiresAt())) {
            request.setStatus(PartnerServiceRequestStatus.EXPIRED);
            request.setRejectionReason(REQUEST_EXPIRED_REASON);
            partnerServiceRequestRepository.save(request);
            publishDispatchEvent(request, "REQUEST_EXPIRED");
            maybeContinueRequestDispatch(request);
            throw new InstantBookingUnavailableException(
                    InstantUnavailableCode.SLOT_EXPIRED.name(),
                    "Request expired. Please wait for the next dispatch."
            );
        }

        if (isSubscriptionRequest(request)) {
            var profile = partnerProfileRepository.findByIdForUpdate(partnerId)
                    .orElseThrow(() -> new ResourceNotFoundException(PARTNER_NOT_FOUND));
            partnerDispatchEligibilityService.requireEligibleForNewDispatch(
                    profile,
                    partnerShiftRepository.findByPartnerIdForUpdate(partnerId).orElse(null)
            );
            requireCurrentSkill(partnerId, request, null);
            Long visitBookingId = subscriptionVisitOrders == null
                    ? request.getCartId()
                    : subscriptionVisitOrders.accept(request, partnerId);
            request.setCartId(visitBookingId);
            request.setStatus(PartnerServiceRequestStatus.ACCEPTED);
            partnerServiceRequestRepository.save(request);
            rejectPeerSubscriptionRequests(request);
            publishDispatchEvent(request, "REQUEST_ACCEPTED");
            subscriptionAutoDispatchService.markPartnerAccepted(request.getSubscriptionId(), request.getSubscriptionVisitId(), partnerId);
            return actionResponse(request);
        }

        Order order = orderRepository.findByIdForUpdate(request.getCartId())
                .orElseThrow(() -> new ResourceNotFoundException("order not found"));
        boolean instant = BookingType.INSTANT.name().equalsIgnoreCase(order.getBookingType());
        if (isAlreadyAssignedToPartner(order, partnerId)) {
            request.setStatus(PartnerServiceRequestStatus.ACCEPTED);
            partnerServiceRequestRepository.save(request);
            publishDispatchEvent(request, "REQUEST_ACCEPTED");
            return actionResponse(request);
        }
        if (instant && order.getStatus() == OrderStatus.PARTNER_ASSIGNED) {
            request.setStatus(PartnerServiceRequestStatus.REJECTED);
            request.setRejectionReason(ACCEPTED_BY_ANOTHER_PARTNER);
            partnerServiceRequestRepository.save(request);
            publishDispatchEvent(request, "REQUEST_REJECTED");
            throw new BadRequestException("request is not pending");
        }
        if (!canAcceptOrder(order)) {
            request.setStatus(PartnerServiceRequestStatus.REJECTED);
            request.setRejectionReason("ORDER_NOT_ACCEPTING_ASSIGNMENT");
            partnerServiceRequestRepository.save(request);
            publishDispatchEvent(request, "REQUEST_REJECTED");
            throw new BadRequestException("Order is not accepting partner assignment");
        }
        boolean scheduled = BookingType.SCHEDULED.name().equalsIgnoreCase(order.getBookingType());
        if ((instant || scheduled) && order.getPaymentStatus() != PaymentStatus.SUCCESS) {
            request.setStatus(PartnerServiceRequestStatus.REJECTED);
            request.setRejectionReason(order.getPaymentStatus() == PaymentStatus.FAILED ? "PAYMENT_FAILED" : "PAYMENT_PENDING");
            partnerServiceRequestRepository.save(request);
            publishDispatchEvent(request, "REQUEST_REJECTED");
            throw new BadRequestException("Order payment is not confirmed");
        }
        int etaAtAccept = request.getEtaMinutes() == null
                ? dispatchOrchestrationService.estimateEtaMinutes(request.getDistanceKm())
                : request.getEtaMinutes();
        if (instant && etaAtAccept > dispatchOrchestrationService.maxEtaMinutes()) {
            request.setStatus(PartnerServiceRequestStatus.REJECTED);
            request.setRejectionReason(InstantUnavailableCode.NO_PARTNER_IN_30_MIN.name());
            partnerServiceRequestRepository.save(request);
            publishDispatchEvent(request, "REQUEST_REJECTED");
            maybeContinueDispatch(order.getId());
            throw new InstantBookingUnavailableException(
                    InstantUnavailableCode.NO_PARTNER_IN_30_MIN.name(),
                    "No partner available right now. Please book a scheduled slot."
            );
        }

        Partner partner = partnerMarketplaceService.require(partnerId);
        partnerDispatchEligibilityService.requireEligibleForNewDispatch(
                partner,
                partnerShiftRepository.findByPartnerIdForUpdate(partnerId).orElse(null)
        );
        requireCurrentSkill(partnerId, request, partner);

        request.setStatus(PartnerServiceRequestStatus.ACCEPTED);
        partnerServiceRequestRepository.save(request);
        publishDispatchEvent(request, "REQUEST_ACCEPTED");

        List<PartnerServiceRequestEntity> peers = partnerServiceRequestRepository
                .findByCartIdAndStatus(request.getCartId(), PartnerServiceRequestStatus.PENDING);
        for (PartnerServiceRequestEntity peer : peers) {
            if (!peer.getId().equals(request.getId())) {
                peer.setStatus(PartnerServiceRequestStatus.REJECTED);
                peer.setRejectionReason(ACCEPTED_BY_ANOTHER_PARTNER);
                publishDispatchEvent(peer, "REQUEST_REJECTED");
            }
        }
        partnerServiceRequestRepository.saveAll(peers);
        dispatchOrchestrationService.markPartnerAccepted(request.getCartId(), partnerId, etaAtAccept);
        return actionResponse(request);
    }

    @Transactional
    public PartnerRequestActionResponse reject(Long partnerId, Long requestId, PartnerRejectRequest rejectRequest) {
        PartnerServiceRequestEntity request = partnerServiceRequestRepository.findByIdAndPartnerId(requestId, partnerId)
                .orElseThrow(() -> new ResourceNotFoundException(REQUEST_NOT_FOUND));
        if (request.getStatus() != PartnerServiceRequestStatus.PENDING) {
            throw new BadRequestException("request is not pending");
        }
        request.setStatus(PartnerServiceRequestStatus.REJECTED);
        request.setRejectionReason(rejectRequest == null ? null : rejectRequest.getReason());
        partnerServiceRequestRepository.save(request);
        publishDispatchEvent(request, "REQUEST_REJECTED");
        if (isSubscriptionRequest(request)) {
            maybeContinueRequestDispatch(request);
            return actionResponse(request);
        }
        dispatchOrchestrationService.markPartnerRejected(request.getCartId(), partnerId);
        maybeContinueDispatch(request.getCartId());
        return actionResponse(request);
    }

    @Transactional
    public PartnerIncomingRequestResponse createDevPendingRequest(Long partnerId) {
        Long seedOrderId = orderRepository.findAll(PageRequest.of(0, 1, Sort.by(Sort.Direction.DESC, "id")))
                .stream()
                .findFirst()
                .map(Order::getId)
                .orElse(System.currentTimeMillis());

        PartnerServiceRequestEntity request = new PartnerServiceRequestEntity();
        request.setPartnerId(partnerId);
        request.setCartId(seedOrderId);
        request.setCustomerId(99999L);
        request.setServiceId(1L);
        request.setServiceType("Deep Cleaning");
        request.setQuantity(1);
        request.setScheduledTime(java.time.LocalDateTime.now().plusHours(2));
        request.setFullAddress("Dev Seed Address");
        request.setCity("Dev City");
        request.setTargetPincode("000000");
        request.setDispatchAttempt(1);
        request.setDistanceKm(java.math.BigDecimal.valueOf(devSeedDistanceKm()));
        request.setEtaMinutes(10);
        request.setExpiresAt(LocalDateTime.now().plusMinutes(2));
        request.setStatus(PartnerServiceRequestStatus.PENDING);

        PartnerServiceRequestEntity saved = partnerServiceRequestRepository.save(request);
        return mapIncoming(saved);
    }

    private PartnerRequestActionResponse actionResponse(PartnerServiceRequestEntity request) {
        PartnerRequestActionResponse response = new PartnerRequestActionResponse();
        response.setRequestId(request.getId());
        response.setStatus(request.getStatus());
        response.setOrderId(request.getCartId());
        return response;
    }

    @Transactional
    public int expirePendingRequests() {
        List<PartnerServiceRequestEntity> expired = partnerServiceRequestRepository
                .findByStatusAndExpiresAtBefore(PartnerServiceRequestStatus.PENDING, LocalDateTime.now());
        for (PartnerServiceRequestEntity request : expired) {
            request.setStatus(PartnerServiceRequestStatus.EXPIRED);
            request.setRejectionReason(REQUEST_EXPIRED_REASON);
            publishDispatchEvent(request, "REQUEST_EXPIRED");
        }
        partnerServiceRequestRepository.saveAll(expired);
        expired.forEach(this::maybeContinueRequestDispatch);
        return expired.size();
    }

    private PartnerServiceRequestEntity expireIfNeeded(PartnerServiceRequestEntity request) {
        if (request.getStatus() == PartnerServiceRequestStatus.PENDING
                && request.getExpiresAt() != null
                && LocalDateTime.now().isAfter(request.getExpiresAt())) {
            request.setStatus(PartnerServiceRequestStatus.EXPIRED);
            request.setRejectionReason(REQUEST_EXPIRED_REASON);
            PartnerServiceRequestEntity saved = partnerServiceRequestRepository.save(request);
            publishDispatchEvent(saved, "REQUEST_EXPIRED");
            maybeContinueRequestDispatch(saved);
            return saved;
        }
        return request;
    }

    private PartnerIncomingRequestResponse mapIncoming(PartnerServiceRequestEntity request) {
        PartnerIncomingRequestResponse response = new PartnerIncomingRequestResponse();
        response.setRequestId(request.getId());
        response.setCartId(request.getCartId());
        response.setSubscriptionId(request.getSubscriptionId());
        response.setSubscriptionVisitId(request.getSubscriptionVisitId());
        response.setCustomerId(request.getCustomerId());
        response.setServiceId(request.getServiceId());
        response.setServiceType(translatedServiceType(request));
        response.setQuantity(request.getQuantity());
        response.setScheduledTime(request.getScheduledTime());
        response.setStatus(request.getStatus());
        response.setCreatedAt(request.getCreatedAt());
        response.setEtaMinutes(request.getEtaMinutes());
        response.setExpiresAt(request.getExpiresAt());

        PartnerRequestAddressResponse addressResponse = new PartnerRequestAddressResponse();
        addressResponse.setFullAddress(request.getFullAddress());
        addressResponse.setCity(request.getCity());
        addressResponse.setLatitude(request.getLatitude() == null ? null : request.getLatitude().doubleValue());
        addressResponse.setLongitude(request.getLongitude() == null ? null : request.getLongitude().doubleValue());
        response.setAddress(addressResponse);
        return response;
    }

    private String translatedServiceType(PartnerServiceRequestEntity request) {
        if (dynamicTranslationService == null || request.getServiceType() == null || request.getServiceType().isBlank()) {
            return request.getServiceType();
        }
        return dynamicTranslationService.translateForPartner(
                request.getPartnerId(),
                null,
                "partner_service_request",
                request.getId(),
                "service_type",
                request.getServiceType());
    }

    private void maybeMarkInstantFailed(Long orderId) {
        Order order = orderRepository.findByIdForUpdate(orderId).orElse(null);
        if (order == null || !BookingType.INSTANT.name().equalsIgnoreCase(order.getBookingType())) {
            return;
        }
        long pendingCount = partnerServiceRequestRepository.countByCartIdAndStatus(orderId, PartnerServiceRequestStatus.PENDING);
        long acceptedCount = partnerServiceRequestRepository.countByCartIdAndStatus(orderId, PartnerServiceRequestStatus.ACCEPTED);
        if (pendingCount == 0 && acceptedCount == 0) {
            applyOrderStatusTransition(order, OrderStatus.INSTANT_FAILED);
            order.setUnavailableReason(InstantUnavailableCode.INSTANT_UNAVAILABLE.name());
            orderRepository.save(order);
            dispatchOrchestrationService.saveEvent(orderId, "INSTANT_FAILED",
                    java.util.Map.of("reason", InstantUnavailableCode.INSTANT_UNAVAILABLE.name()));
        }
    }

    private void applyOrderStatusTransition(Order order, OrderStatus targetStatus) {
        OrderStatus current = order.getStatus();
        if (current == targetStatus) {
            return;
        }
        if (current == null || !current.canTransitionTo(targetStatus)) {
            throw new BadRequestException("Invalid order status transition from " + current + " to " + targetStatus);
        }
        order.setStatus(targetStatus);
    }

    private boolean isAlreadyAssignedToPartner(PartnerServiceRequestEntity request, Long partnerId) {
        Order order = orderRepository.findByIdForUpdate(request.getCartId()).orElse(null);
        return isAlreadyAssignedToPartner(order, partnerId);
    }

    private boolean isAlreadyAssignedToPartner(Order order, Long partnerId) {
        return order != null
                && order.getAssignedPartnerId() != null
                && order.getAssignedPartnerId().equals(partnerId)
                && (order.getStatus() == OrderStatus.PARTNER_ASSIGNED || order.getStatus() == OrderStatus.PARTNER_ACCEPTED);
    }

    private boolean canAcceptOrder(Order order) {
        if (order == null || order.getStatus() == null) {
            return false;
        }
        if (BookingType.INSTANT.name().equalsIgnoreCase(order.getBookingType())) {
            return order.getStatus() == OrderStatus.CREATED
                    || order.getStatus() == OrderStatus.CONFIRMED
                    || order.getStatus() == OrderStatus.BOOKING_CREATED
                    || order.getStatus() == OrderStatus.SEARCHING_PARTNER;
        }
        return order.getStatus() == OrderStatus.CREATED
                || order.getStatus() == OrderStatus.CONFIRMED
                || order.getStatus() == OrderStatus.BOOKING_CREATED
                || order.getStatus() == OrderStatus.SCHEDULED
                || order.getStatus() == OrderStatus.PARTNER_SEARCH
                || order.getStatus() == OrderStatus.PARTNER_ASSIGNED;
    }

    private void maybeContinueDispatch(Long orderId) {
        Order order = orderRepository.findByIdForUpdate(orderId).orElse(null);
        if (order == null) {
            return;
        }
        long pendingCount = partnerServiceRequestRepository.countByCartIdAndStatus(orderId, PartnerServiceRequestStatus.PENDING);
        long acceptedCount = partnerServiceRequestRepository.countByCartIdAndStatus(orderId, PartnerServiceRequestStatus.ACCEPTED);
        if (pendingCount > 0 || acceptedCount > 0) {
            return;
        }
        int created = dispatchOrchestrationService.retryDispatch(orderId);
        if (created <= 0) {
            maybeMarkInstantFailed(orderId);
        }
    }

    private boolean isSubscriptionRequest(PartnerServiceRequestEntity request) {
        return request.getSubscriptionId() != null && request.getSubscriptionVisitId() != null;
    }

    private void requireCurrentSkill(Long partnerId, PartnerServiceRequestEntity request, Partner partner) {
        if (partnerServiceSkillRepository == null || request.getServiceId() == null) {
            return;
        }
        Long requestedSubServiceId = request.getServiceId();
        Long requestedCategoryId = requestedCategoryId(requestedSubServiceId);
        if (hasCurrentSkill(partnerId, requestedSubServiceId, requestedCategoryId, partner)) {
            return;
        }
        request.setStatus(PartnerServiceRequestStatus.REJECTED);
        request.setRejectionReason("SKILL_MISMATCH");
        partnerServiceRequestRepository.save(request);
        publishDispatchEvent(request, "REQUEST_REJECTED");
        throw new BadRequestException("Partner skill no longer matches this request");
    }

    private boolean hasCurrentSkill(Long partnerId, Long requestedSubServiceId, Long requestedCategoryId, Partner partner) {
        if (partner != null) {
            if (partner.getSubCategoryId() != null && partner.getSubCategoryId().equals(requestedSubServiceId)) {
                return true;
            }
            if (requestedCategoryId != null
                    && partner.getCategoryId() != null
                    && partner.getCategoryId().equals(requestedCategoryId)) {
                return true;
            }
        }
        if (partnerServiceSkillRepository.existsByPartnerIdAndSubServiceId(partnerId, requestedSubServiceId)) {
            return true;
        }
        return requestedCategoryId != null
                && partnerServiceSkillRepository.existsByPartnerIdAndCategoryId(partnerId, requestedCategoryId);
    }

    private Long requestedCategoryId(Long serviceId) {
        if (adminServiceEntityRepository == null || serviceId == null) {
            return null;
        }
        return adminServiceEntityRepository.findByIdAndDeletedFalse(serviceId)
                .map(AdminServiceEntity::getCategory)
                .map(category -> category == null ? null : category.getId())
                .orElse(null);
    }

    private double devSeedDistanceKm() {
        return 0.2 + SECURE_RANDOM.nextDouble() * 2.8;
    }

    private void rejectPeerSubscriptionRequests(PartnerServiceRequestEntity request) {
        List<PartnerServiceRequestEntity> peers = partnerServiceRequestRepository
                .findBySubscriptionVisitIdAndStatus(request.getSubscriptionVisitId(), PartnerServiceRequestStatus.PENDING);
        for (PartnerServiceRequestEntity peer : peers) {
            if (!peer.getId().equals(request.getId())) {
                peer.setStatus(PartnerServiceRequestStatus.REJECTED);
                peer.setRejectionReason(ACCEPTED_BY_ANOTHER_PARTNER);
                publishDispatchEvent(peer, "REQUEST_REJECTED");
            }
        }
        partnerServiceRequestRepository.saveAll(peers);
    }

    private void publishDispatchEvent(PartnerServiceRequestEntity request, String eventName) {
        if (partnerDispatchRealtimeService != null) {
            partnerDispatchRealtimeService.publishRequestEvent(request, eventName);
        }
    }

    private void maybeContinueRequestDispatch(PartnerServiceRequestEntity request) {
        if (isSubscriptionRequest(request)) {
            maybeContinueSubscriptionDispatch(request.getSubscriptionId(), request.getSubscriptionVisitId());
            return;
        }
        maybeContinueDispatch(request.getCartId());
    }

    private void maybeContinueSubscriptionDispatch(Long subscriptionId, Long visitId) {
        long pendingCount = partnerServiceRequestRepository.countBySubscriptionVisitIdAndStatus(visitId, PartnerServiceRequestStatus.PENDING);
        long acceptedCount = partnerServiceRequestRepository.countBySubscriptionVisitIdAndStatus(visitId, PartnerServiceRequestStatus.ACCEPTED);
        if (pendingCount > 0 || acceptedCount > 0) {
            return;
        }
        subscriptionAutoDispatchService.retryDispatch(subscriptionId, visitId);
    }

}

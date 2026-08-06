package com.lexho.user_service.partnerrequest.service;

import com.lexho.provider.entity.Partner;
import com.lexho.provider.repository.PartnerRepository;
import com.lexho.booking.entity.Order;
import com.lexho.booking.repository.OrderRepository;
import com.lexho.common.enums.OrderStatus;
import com.lexho.user_service.booking.exception.InstantBookingUnavailableException;
import com.lexho.user_service.booking.model.BookingType;
import com.lexho.user_service.booking.model.InstantUnavailableCode;
import com.lexho.user_service.dispatch.service.DispatchOrchestrationService;
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
import com.lexho.user_service.subscription.service.SubscriptionAutoDispatchService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

@Service
public class PartnerRequestService {

    private final PartnerRepository partnerRepository;
    private final PartnerServiceRequestRepository partnerServiceRequestRepository;
    private final DispatchOrchestrationService dispatchOrchestrationService;
    private final OrderRepository orderRepository;
    private final PartnerLeaveRepository partnerLeaveRepository;
    private final SubscriptionAutoDispatchService subscriptionAutoDispatchService;

    public PartnerRequestService(PartnerRepository partnerRepository,
                                 PartnerServiceRequestRepository partnerServiceRequestRepository,
                                 DispatchOrchestrationService dispatchOrchestrationService,
                                 OrderRepository orderRepository,
                                 PartnerLeaveRepository partnerLeaveRepository,
                                 SubscriptionAutoDispatchService subscriptionAutoDispatchService) {
        this.partnerRepository = partnerRepository;
        this.partnerServiceRequestRepository = partnerServiceRequestRepository;
        this.dispatchOrchestrationService = dispatchOrchestrationService;
        this.orderRepository = orderRepository;
        this.partnerLeaveRepository = partnerLeaveRepository;
        this.subscriptionAutoDispatchService = subscriptionAutoDispatchService;
    }

    @Transactional
    public PartnerStatusResponse updateOnlineStatus(Long partnerId, PartnerStatusUpdateRequest request) {
        Partner partner = partnerRepository.findById(partnerId)
                .orElseThrow(() -> new ResourceNotFoundException("partner not found"));
        partner.setAvailable(Boolean.TRUE.equals(request.getIsOnline()));
        Partner saved = partnerRepository.save(partner);
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
                .orElseThrow(() -> new ResourceNotFoundException("request not found"));
        return mapIncoming(expireIfNeeded(request));
    }

    @Transactional
    public PartnerRequestActionResponse accept(Long partnerId, Long requestId) {
        PartnerServiceRequestEntity request = partnerServiceRequestRepository.findByIdAndPartnerIdForUpdate(requestId, partnerId)
                .orElseThrow(() -> new ResourceNotFoundException("request not found"));
        if (request.getStatus() != PartnerServiceRequestStatus.PENDING) {
            throw new BadRequestException("request is not pending");
        }
        if (request.getExpiresAt() != null && LocalDateTime.now().isAfter(request.getExpiresAt())) {
            request.setStatus(PartnerServiceRequestStatus.EXPIRED);
            request.setRejectionReason("REQUEST_EXPIRED");
            partnerServiceRequestRepository.save(request);
            maybeContinueRequestDispatch(request);
            throw new InstantBookingUnavailableException(
                    InstantUnavailableCode.SLOT_EXPIRED.name(),
                    "Request expired. Please wait for the next dispatch."
            );
        }

        if (isSubscriptionRequest(request)) {
            request.setStatus(PartnerServiceRequestStatus.ACCEPTED);
            partnerServiceRequestRepository.save(request);
            rejectPeerSubscriptionRequests(request);
            subscriptionAutoDispatchService.markPartnerAccepted(request.getSubscriptionId(), request.getSubscriptionVisitId(), partnerId);
            return actionResponse(request);
        }

        Order order = orderRepository.findByIdForUpdate(request.getCartId())
                .orElseThrow(() -> new ResourceNotFoundException("order not found"));
        boolean instant = BookingType.INSTANT.name().equalsIgnoreCase(order.getBookingType());
        if (instant && order.getStatus() == OrderStatus.PARTNER_ASSIGNED) {
            request.setStatus(PartnerServiceRequestStatus.REJECTED);
            request.setRejectionReason("Accepted by another partner");
            partnerServiceRequestRepository.save(request);
            throw new BadRequestException("request is not pending");
        }
        int etaAtAccept = request.getEtaMinutes() == null
                ? dispatchOrchestrationService.estimateEtaMinutes(request.getDistanceKm())
                : request.getEtaMinutes();
        if (instant && etaAtAccept > dispatchOrchestrationService.maxEtaMinutes()) {
            request.setStatus(PartnerServiceRequestStatus.REJECTED);
            request.setRejectionReason(InstantUnavailableCode.NO_PARTNER_IN_30_MIN.name());
            partnerServiceRequestRepository.save(request);
            maybeContinueDispatch(order.getId());
            throw new InstantBookingUnavailableException(
                    InstantUnavailableCode.NO_PARTNER_IN_30_MIN.name(),
                    "No partner available right now. Please book a scheduled slot."
            );
        }

        request.setStatus(PartnerServiceRequestStatus.ACCEPTED);
        partnerServiceRequestRepository.save(request);

        List<PartnerServiceRequestEntity> peers = partnerServiceRequestRepository
                .findByCartIdAndStatus(request.getCartId(), PartnerServiceRequestStatus.PENDING);
        for (PartnerServiceRequestEntity peer : peers) {
            if (!peer.getId().equals(request.getId())) {
                peer.setStatus(PartnerServiceRequestStatus.REJECTED);
                peer.setRejectionReason("Accepted by another partner");
            }
        }
        partnerServiceRequestRepository.saveAll(peers);
        dispatchOrchestrationService.markPartnerAccepted(request.getCartId(), partnerId, etaAtAccept);
        return actionResponse(request);
    }

    @Transactional
    public PartnerRequestActionResponse reject(Long partnerId, Long requestId, PartnerRejectRequest rejectRequest) {
        PartnerServiceRequestEntity request = partnerServiceRequestRepository.findByIdAndPartnerId(requestId, partnerId)
                .orElseThrow(() -> new ResourceNotFoundException("request not found"));
        if (request.getStatus() != PartnerServiceRequestStatus.PENDING) {
            throw new BadRequestException("request is not pending");
        }
        request.setStatus(PartnerServiceRequestStatus.REJECTED);
        request.setRejectionReason(rejectRequest == null ? null : rejectRequest.getReason());
        partnerServiceRequestRepository.save(request);
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
        request.setDistanceKm(java.math.BigDecimal.valueOf(ThreadLocalRandom.current().nextDouble(0.2, 3.0)));
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
        return response;
    }

    @Transactional
    public int expirePendingRequests() {
        List<PartnerServiceRequestEntity> expired = partnerServiceRequestRepository
                .findByStatusAndExpiresAtBefore(PartnerServiceRequestStatus.PENDING, LocalDateTime.now());
        for (PartnerServiceRequestEntity request : expired) {
            request.setStatus(PartnerServiceRequestStatus.EXPIRED);
            request.setRejectionReason("REQUEST_EXPIRED");
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
            request.setRejectionReason("REQUEST_EXPIRED");
            PartnerServiceRequestEntity saved = partnerServiceRequestRepository.save(request);
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
        response.setServiceType(request.getServiceType());
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

    private void maybeMarkInstantFailed(Long orderId) {
        Order order = orderRepository.findByIdForUpdate(orderId).orElse(null);
        if (order == null || !BookingType.INSTANT.name().equalsIgnoreCase(order.getBookingType())) {
            return;
        }
        long pendingCount = partnerServiceRequestRepository.countByCartIdAndStatus(orderId, PartnerServiceRequestStatus.PENDING);
        long acceptedCount = partnerServiceRequestRepository.countByCartIdAndStatus(orderId, PartnerServiceRequestStatus.ACCEPTED);
        if (pendingCount == 0 && acceptedCount == 0) {
            order.setStatus(OrderStatus.INSTANT_FAILED);
            order.setUnavailableReason(InstantUnavailableCode.INSTANT_UNAVAILABLE.name());
            orderRepository.save(order);
            dispatchOrchestrationService.saveEvent(orderId, "INSTANT_FAILED",
                    java.util.Map.of("reason", InstantUnavailableCode.INSTANT_UNAVAILABLE.name()));
        }
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

    private void rejectPeerSubscriptionRequests(PartnerServiceRequestEntity request) {
        List<PartnerServiceRequestEntity> peers = partnerServiceRequestRepository
                .findBySubscriptionVisitIdAndStatus(request.getSubscriptionVisitId(), PartnerServiceRequestStatus.PENDING);
        for (PartnerServiceRequestEntity peer : peers) {
            if (!peer.getId().equals(request.getId())) {
                peer.setStatus(PartnerServiceRequestStatus.REJECTED);
                peer.setRejectionReason("Accepted by another partner");
            }
        }
        partnerServiceRequestRepository.saveAll(peers);
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

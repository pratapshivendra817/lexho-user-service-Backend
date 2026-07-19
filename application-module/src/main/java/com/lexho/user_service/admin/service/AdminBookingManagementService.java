package com.lexho.user_service.admin.service;

import com.lexho.booking.entity.Order;
import com.lexho.booking.repository.OrderRepository;
import com.lexho.common.enums.OrderStatus;
import com.lexho.common.enums.PaymentStatus;
import com.lexho.user_service.admin.entity.Payout;
import com.lexho.user_service.admin.repository.PayoutRepository;
import com.lexho.user_service.dispatch.entity.BookingEventEntity;
import com.lexho.user_service.dispatch.entity.CurrentPartnerLocationEntity;
import com.lexho.user_service.dispatch.repository.BookingEventRepository;
import com.lexho.user_service.dispatch.repository.CurrentPartnerLocationRepository;
import com.lexho.user_service.dto.OrderResponseDTO;
import com.lexho.user_service.exception.BadRequestException;
import com.lexho.user_service.exception.ResourceNotFoundException;
import com.lexho.user_service.lexhopartner.entity.PartnerOrder;
import com.lexho.user_service.lexhopartner.repository.PartnerOrderRepository;
import com.lexho.user_service.map.dto.NavigationRouteResponse;
import com.lexho.user_service.map.service.NavigationEtaPushService;
import com.lexho.user_service.map.service.PartnerNavigationService;
import com.lexho.user_service.service.OrderService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class AdminBookingManagementService {

    private final OrderRepository orderRepository;
    private final OrderService orderService;
    private final BookingEventRepository bookingEventRepository;
    private final CurrentPartnerLocationRepository currentPartnerLocationRepository;
    private final PartnerOrderRepository partnerOrderRepository;
    private final PayoutRepository payoutRepository;
    private final PartnerNavigationService partnerNavigationService;
    private final NavigationEtaPushService navigationEtaPushService;

    public AdminBookingManagementService(OrderRepository orderRepository,
                                         OrderService orderService,
                                         BookingEventRepository bookingEventRepository,
                                         CurrentPartnerLocationRepository currentPartnerLocationRepository,
                                         PartnerOrderRepository partnerOrderRepository,
                                         PayoutRepository payoutRepository,
                                         PartnerNavigationService partnerNavigationService,
                                         NavigationEtaPushService navigationEtaPushService) {
        this.orderRepository = orderRepository;
        this.orderService = orderService;
        this.bookingEventRepository = bookingEventRepository;
        this.currentPartnerLocationRepository = currentPartnerLocationRepository;
        this.partnerOrderRepository = partnerOrderRepository;
        this.payoutRepository = payoutRepository;
        this.partnerNavigationService = partnerNavigationService;
        this.navigationEtaPushService = navigationEtaPushService;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> list(String search, String status, int page, int size, String sortBy, String sortDir) {
        OrderStatus parsedStatus = parseStatus(status);
        Pageable pageable = PageRequest.of(
                Math.max(page, 0),
                Math.min(Math.max(size, 1), 100),
                "asc".equalsIgnoreCase(sortDir) ? Sort.by(sortByOrDefault(sortBy)).ascending() : Sort.by(sortByOrDefault(sortBy)).descending()
        );
        Page<Order> result = orderRepository.searchOrders(normalize(search), parsedStatus, pageable);
        return Map.of(
                "items", result.getContent().stream().map(this::toRow).toList(),
                "page", result.getNumber(),
                "size", result.getSize(),
                "totalElements", result.getTotalElements(),
                "totalPages", result.getTotalPages()
        );
    }

    @Transactional(readOnly = true)
    public Map<String, Object> byId(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));
        return toRow(order);
    }

    @Transactional
    public Map<String, Object> updateStatus(Long orderId, OrderStatus newStatus) {
        if (newStatus == null) {
            throw new BadRequestException("status is required");
        }
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));
        if (!order.getStatus().canTransitionTo(newStatus)) {
            throw new BadRequestException("Invalid status transition");
        }
        OrderResponseDTO updated = orderService.updateStatus(orderId, newStatus);
        return Map.of(
                "id", updated.getId(),
                "status", updated.getStatus(),
                "paymentStatus", updated.getPaymentStatus(),
                "totalAmount", updated.getTotalAmount()
        );
    }

    @Transactional
    public Map<String, Object> cancel(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));
        if (!order.getStatus().canCancel()) {
            throw new BadRequestException("Order cannot be cancelled at this stage");
        }
        order.setStatus(OrderStatus.CANCELLED);
        Order saved = orderRepository.save(order);
        return Map.of("id", saved.getId(), "status", saved.getStatus().name());
    }

    @Transactional(readOnly = true)
    public Map<String, Object> liveTracking(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));

        Optional<PartnerOrder> partnerOrder = partnerOrderRepository.findById(orderId);
        Long partnerId = partnerOrder.map(PartnerOrder::getAssignedPartnerId).orElse(null);
        CurrentPartnerLocationEntity location = partnerId == null
                ? null
                : currentPartnerLocationRepository.findByPartnerIdAndDeletedFalse(partnerId).orElse(null);

        List<Map<String, Object>> events = bookingEventRepository.findByBookingIdOrderByCreatedAtAsc(orderId).stream()
                .map(this::toEventRow)
                .toList();
        NavigationRouteResponse navigation = resolveNavigation(orderId, true);
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("bookingId", order.getId());
        response.put("status", order.getStatus() == null ? null : order.getStatus().name());
        response.put("paymentStatus", order.getPaymentStatus() == null ? null : order.getPaymentStatus().name());
        response.put("scheduledAt", order.getScheduledAt());
        response.put("partnerId", partnerId);
        response.put("partnerLocation", location == null ? null : toLocationRow(location));
        response.put("navigation", navigation);
        response.put("events", events);
        return response;
    }

    @Transactional(readOnly = true)
    public NavigationRouteResponse navigation(Long orderId) {
        return resolveNavigation(orderId, true);
    }

    @Transactional
    public Map<String, Object> overridePartnerLocation(Long orderId, Double latitude, Double longitude, String pincode) {
        if (latitude == null || longitude == null) {
            throw new BadRequestException("latitude and longitude are required");
        }
        PartnerOrder partnerOrder = partnerOrderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Partner order not found"));
        if (partnerOrder.getAssignedPartnerId() == null) {
            throw new BadRequestException("No partner assigned for this booking");
        }
        CurrentPartnerLocationEntity location = currentPartnerLocationRepository
                .findByPartnerIdAndDeletedFalse(partnerOrder.getAssignedPartnerId())
                .orElseGet(CurrentPartnerLocationEntity::new);
        location.setPartnerId(partnerOrder.getAssignedPartnerId());
        location.setLatitude(BigDecimal.valueOf(latitude));
        location.setLongitude(BigDecimal.valueOf(longitude));
        location.setPincode(pincode);
        location.setSource("ADMIN_OVERRIDE");
        location.setUpdatedBy("ADMIN");
        CurrentPartnerLocationEntity saved = currentPartnerLocationRepository.save(location);
        NavigationRouteResponse navigation = resolveNavigation(orderId, true);
        navigationEtaPushService.publish(orderId, navigation);
        return Map.of(
                "bookingId", orderId,
                "partnerId", saved.getPartnerId(),
                "location", toLocationRow(saved),
                "navigation", navigation
        );
    }

    @Transactional(readOnly = true)
    public Map<String, Object> paymentReconciliation(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));
        Payout payout = payoutRepository.findTopByReferenceBookingIdOrderByCreatedAtDesc(orderId).orElse(null);
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("bookingId", order.getId());
        response.put("amount", order.getTotalAmount());
        response.put("bookingStatus", order.getStatus() == null ? null : order.getStatus().name());
        response.put("paymentStatus", order.getPaymentStatus() == null ? null : order.getPaymentStatus().name());
        response.put("payout", payout == null ? null : toPayoutRow(payout));
        return response;
    }

    @Transactional
    public Map<String, Object> updatePaymentStatus(Long orderId,
                                                   PaymentStatus paymentStatus,
                                                   String reason,
                                                   Long adminId) {
        if (paymentStatus == null) {
            throw new BadRequestException("paymentStatus is required");
        }
        if (reason == null || reason.isBlank()) {
            throw new BadRequestException("reason is required");
        }
        if (paymentStatus == PaymentStatus.SUCCESS || paymentStatus == PaymentStatus.REFUNDED) {
            throw new BadRequestException("SUCCESS and REFUNDED must be updated through Razorpay reconciliation");
        }
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));
        order.setPaymentStatus(paymentStatus);
        Order saved = orderRepository.save(order);
        BookingEventEntity event = new BookingEventEntity();
        event.setBookingId(orderId);
        event.setEventType("ADMIN_PAYMENT_OVERRIDE");
        event.setEventPayload("{\"adminId\":" + adminId
                + ",\"paymentStatus\":\"" + paymentStatus.name()
                + "\",\"reason\":\"" + sanitizeJson(reason) + "\"}");
        bookingEventRepository.save(event);
        return Map.of(
                "bookingId", saved.getId(),
                "paymentStatus", saved.getPaymentStatus().name(),
                "status", saved.getStatus() == null ? null : saved.getStatus().name()
        );
    }

    @Transactional
    public Map<String, Object> createOrUpdatePayout(Long orderId, Long providerId, Double amount, String status) {
        if (providerId == null || amount == null || amount <= 0) {
            throw new BadRequestException("providerId and positive amount are required");
        }
        String normalizedStatus = (status == null || status.isBlank()) ? "PENDING" : status.trim().toUpperCase();
        Payout payout = payoutRepository.findTopByReferenceBookingIdOrderByCreatedAtDesc(orderId)
                .orElseGet(Payout::new);
        payout.setProviderId(providerId);
        payout.setReferenceBookingId(orderId);
        payout.setAmount(amount);
        payout.setStatus(normalizedStatus);
        Payout saved = payoutRepository.save(payout);
        return Map.of(
                "payoutId", saved.getId(),
                "providerId", saved.getProviderId(),
                "amount", saved.getAmount(),
                "status", saved.getStatus()
        );
    }

    private Map<String, Object> toRow(Order order) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("id", order.getId());
        row.put("userId", order.getUserId());
        row.put("status", order.getStatus() == null ? null : order.getStatus().name());
        row.put("paymentStatus", order.getPaymentStatus() == null ? null : order.getPaymentStatus().name());
        row.put("bookingType", order.getBookingType());
        row.put("address", order.getAddress());
        row.put("totalAmount", order.getTotalAmount());
        row.put("scheduledAt", order.getScheduledAt());
        row.put("createdAt", order.getCreatedAt());
        row.put("updatedAt", order.getUpdatedAt());
        return row;
    }

    private Map<String, Object> toEventRow(BookingEventEntity event) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("id", event.getId());
        row.put("bookingId", event.getBookingId());
        row.put("eventType", event.getEventType());
        row.put("eventPayload", event.getEventPayload());
        row.put("createdAt", event.getCreatedAt());
        return row;
    }

    private Map<String, Object> toLocationRow(CurrentPartnerLocationEntity location) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("partnerId", location.getPartnerId());
        row.put("latitude", location.getLatitude());
        row.put("longitude", location.getLongitude());
        row.put("pincode", location.getPincode());
        row.put("source", location.getSource());
        row.put("updatedAt", location.getUpdatedAt());
        return row;
    }

    private Map<String, Object> toPayoutRow(Payout payout) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("id", payout.getId());
        row.put("providerId", payout.getProviderId());
        row.put("referenceBookingId", payout.getReferenceBookingId());
        row.put("amount", payout.getAmount());
        row.put("status", payout.getStatus());
        row.put("createdAt", payout.getCreatedAt());
        return row;
    }

    private String normalize(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String sanitizeJson(String value) {
        return value.trim().replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private OrderStatus parseStatus(String status) {
        if (status == null || status.isBlank()) {
            return null;
        }
        try {
            return OrderStatus.valueOf(status.trim().toUpperCase());
        } catch (Exception ex) {
            throw new BadRequestException("Invalid status");
        }
    }

    private String sortByOrDefault(String sortBy) {
        if (sortBy == null || sortBy.isBlank()) {
            return "createdAt";
        }
        return switch (sortBy) {
            case "id", "createdAt", "updatedAt", "scheduledAt", "totalAmount", "status", "paymentStatus" -> sortBy;
            default -> "createdAt";
        };
    }

    private NavigationRouteResponse resolveNavigation(Long bookingId, boolean activateTracking) {
        try {
            return activateTracking
                    ? partnerNavigationService.etaForBooking(bookingId)
                    : partnerNavigationService.etaForBookingSnapshot(bookingId);
        } catch (ResourceNotFoundException | BadRequestException ex) {
            return null;
        }
    }
}

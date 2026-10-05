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
import com.lexho.user_service.util.StringSanitizers;
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

    private static final String ORDER_NOT_FOUND = "Order not found";
    private static final String ID_KEY = "id";
    private static final String STATUS_KEY = "status";
    private static final String PAYMENT_STATUS_KEY = "paymentStatus";
    private static final String BOOKING_ID_KEY = "bookingId";
    private static final String PARTNER_ID_KEY = "partnerId";
    private static final String AMOUNT_KEY = "amount";
    private static final String CREATED_AT_KEY = "createdAt";
    private static final String UPDATED_AT_KEY = "updatedAt";
    private static final String SCHEDULED_AT_KEY = "scheduledAt";
    private static final String TOTAL_AMOUNT_KEY = "totalAmount";
    private static final String DEFAULT_SORT_FIELD = CREATED_AT_KEY;
    private static final String EVENT_TYPE_ADMIN_PAYMENT_OVERRIDE = "ADMIN_PAYMENT_OVERRIDE";
    private static final String DEFAULT_PAYOUT_STATUS = "PENDING";

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
        Order order = orderRepository.findByIdForUpdate(orderId)
                .orElseThrow(() -> new ResourceNotFoundException(ORDER_NOT_FOUND));
        return toRow(order);
    }

    @Transactional
    public Map<String, Object> updateStatus(Long orderId, OrderStatus newStatus) {
        if (newStatus == null) {
            throw new BadRequestException("status is required");
        }
        Order order = orderRepository.findByIdForUpdate(orderId)
                .orElseThrow(() -> new ResourceNotFoundException(ORDER_NOT_FOUND));
        if (!order.getStatus().canTransitionTo(newStatus)) {
            throw new BadRequestException("Invalid status transition");
        }
        OrderResponseDTO updated = orderService.updateStatus(orderId, newStatus);
        return Map.of(
                ID_KEY, updated.getId(),
                STATUS_KEY, updated.getStatus(),
                PAYMENT_STATUS_KEY, updated.getPaymentStatus(),
                TOTAL_AMOUNT_KEY, updated.getTotalAmount()
        );
    }

    @Transactional
    public Map<String, Object> cancel(Long orderId) {
        try {
            OrderResponseDTO cancelled = orderService.cancelOrder(orderId);
            return Map.of(ID_KEY, cancelled.getId(), STATUS_KEY, cancelled.getStatus());
        } catch (IllegalStateException ex) {
            throw new BadRequestException("Order cannot be cancelled at this stage");
        }
    }

    @Transactional(readOnly = true)
    public Map<String, Object> liveTracking(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException(ORDER_NOT_FOUND));

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
        response.put(BOOKING_ID_KEY, order.getId());
        response.put(STATUS_KEY, order.getStatus() == null ? null : order.getStatus().name());
        response.put(PAYMENT_STATUS_KEY, order.getPaymentStatus() == null ? null : order.getPaymentStatus().name());
        response.put(SCHEDULED_AT_KEY, order.getScheduledAt());
        response.put(PARTNER_ID_KEY, partnerId);
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
                BOOKING_ID_KEY, orderId,
                PARTNER_ID_KEY, saved.getPartnerId(),
                "location", toLocationRow(saved),
                "navigation", navigation
        );
    }

    @Transactional(readOnly = true)
    public Map<String, Object> paymentReconciliation(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException(ORDER_NOT_FOUND));
        Payout payout = payoutRepository.findTopByReferenceBookingIdOrderByCreatedAtDesc(orderId).orElse(null);
        Map<String, Object> response = new LinkedHashMap<>();
        response.put(BOOKING_ID_KEY, order.getId());
        response.put(AMOUNT_KEY, order.getTotalAmount());
        response.put("bookingStatus", order.getStatus() == null ? null : order.getStatus().name());
        response.put(PAYMENT_STATUS_KEY, order.getPaymentStatus() == null ? null : order.getPaymentStatus().name());
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
                .orElseThrow(() -> new ResourceNotFoundException(ORDER_NOT_FOUND));
        order.setPaymentStatus(paymentStatus);
        Order saved = orderRepository.save(order);
        BookingEventEntity event = new BookingEventEntity();
        event.setBookingId(orderId);
        event.setEventType(EVENT_TYPE_ADMIN_PAYMENT_OVERRIDE);
        event.setEventPayload("{\"adminId\":" + adminId
                + ",\"paymentStatus\":\"" + paymentStatus.name()
                + "\",\"reason\":\"" + sanitizeJson(reason) + "\"}");
        bookingEventRepository.save(event);
        return Map.of(
                BOOKING_ID_KEY, saved.getId(),
                PAYMENT_STATUS_KEY, saved.getPaymentStatus().name(),
                STATUS_KEY, saved.getStatus() == null ? null : saved.getStatus().name()
        );
    }

    @Transactional
    public Map<String, Object> createOrUpdatePayout(Long orderId, Long providerId, Double amount, String status) {
        if (providerId == null || amount == null || amount <= 0) {
            throw new BadRequestException("providerId and positive amount are required");
        }
        String normalizedStatus = (status == null || status.isBlank()) ? DEFAULT_PAYOUT_STATUS : status.trim().toUpperCase();
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
                AMOUNT_KEY, saved.getAmount(),
                STATUS_KEY, saved.getStatus()
        );
    }

    private Map<String, Object> toRow(Order order) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put(ID_KEY, order.getId());
        row.put("userId", order.getUserId());
        row.put(STATUS_KEY, order.getStatus() == null ? null : order.getStatus().name());
        row.put(PAYMENT_STATUS_KEY, order.getPaymentStatus() == null ? null : order.getPaymentStatus().name());
        row.put("bookingType", order.getBookingType());
        row.put("address", order.getAddress());
        row.put(TOTAL_AMOUNT_KEY, order.getTotalAmount());
        row.put(SCHEDULED_AT_KEY, order.getScheduledAt());
        row.put(CREATED_AT_KEY, order.getCreatedAt());
        row.put(UPDATED_AT_KEY, order.getUpdatedAt());
        return row;
    }

    private Map<String, Object> toEventRow(BookingEventEntity event) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put(ID_KEY, event.getId());
        row.put(BOOKING_ID_KEY, event.getBookingId());
        row.put("eventType", event.getEventType());
        row.put("eventPayload", event.getEventPayload());
        row.put(CREATED_AT_KEY, event.getCreatedAt());
        return row;
    }

    private Map<String, Object> toLocationRow(CurrentPartnerLocationEntity location) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put(PARTNER_ID_KEY, location.getPartnerId());
        row.put("latitude", location.getLatitude());
        row.put("longitude", location.getLongitude());
        row.put("pincode", location.getPincode());
        row.put("source", location.getSource());
        row.put(UPDATED_AT_KEY, location.getUpdatedAt());
        return row;
    }

    private Map<String, Object> toPayoutRow(Payout payout) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put(ID_KEY, payout.getId());
        row.put("providerId", payout.getProviderId());
        row.put("referenceBookingId", payout.getReferenceBookingId());
        row.put(AMOUNT_KEY, payout.getAmount());
        row.put(STATUS_KEY, payout.getStatus());
        row.put(CREATED_AT_KEY, payout.getCreatedAt());
        return row;
    }

    private String normalize(String value) {
        return StringSanitizers.trimToNull(value);
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
            return DEFAULT_SORT_FIELD;
        }
        return switch (sortBy) {
            case ID_KEY, CREATED_AT_KEY, UPDATED_AT_KEY, SCHEDULED_AT_KEY, TOTAL_AMOUNT_KEY, STATUS_KEY,
                    PAYMENT_STATUS_KEY -> sortBy;
            default -> DEFAULT_SORT_FIELD;
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

package com.lexho.user_service.lexhopartner.service;

import com.lexho.user_service.exception.BadRequestException;
import com.lexho.user_service.exception.ResourceNotFoundException;
import com.lexho.user_service.lexhopartner.dto.DashboardStatsDto;
import com.lexho.user_service.lexhopartner.dto.PartnerEarningTransactionDto;
import com.lexho.user_service.lexhopartner.dto.PartnerEarningsSummaryDto;
import com.lexho.user_service.lexhopartner.dto.OrderResponseDto;
import com.lexho.user_service.lexhopartner.dto.OrderTimelineDto;
import com.lexho.user_service.lexhopartner.dto.PageResponseDto;
import com.lexho.user_service.lexhopartner.entity.OrderStatus;
import com.lexho.user_service.lexhopartner.entity.OrderStatusHistory;
import com.lexho.user_service.lexhopartner.entity.PartnerOrder;
import com.lexho.common.enums.PaymentStatus;
import com.lexho.user_service.lexhopartner.repository.OrderStatusHistoryRepository;
import com.lexho.user_service.lexhopartner.repository.PartnerOrderRepository;
import com.lexho.user_service.localization.dynamic.DynamicContentTranslationService;
import com.lexho.user_service.notification.service.NotificationTriggerService;
import com.lexho.user_service.observability.BusinessMetricsService;
import com.lexho.user_service.lexhopartner.util.AuditLogUtil;
import com.lexho.user_service.util.ServicePinHasher;
import com.lexho.user_service.util.LexhoConstants;
import com.lexho.user_service.dispatch.entity.PartnerShiftEntity;
import com.lexho.user_service.dispatch.repository.PartnerShiftRepository;
import com.lexho.user_service.dispatch.service.PartnerDispatchEligibilityService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.security.SecureRandom;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Service
public class PartnerOrderService {
    private com.lexho.user_service.subscription.service.SubscriptionVisitOrderService subscriptionVisitOrders;

    @org.springframework.beans.factory.annotation.Autowired
    public void setSubscriptionVisitOrders(com.lexho.user_service.subscription.service.SubscriptionVisitOrderService service) {
        this.subscriptionVisitOrders = service;
    }

    private static final int PIN_EXPIRY_MINUTES = 15;
    private static final int MAX_PIN_ATTEMPTS = 5;
    private static final String ORDER_NOT_FOUND = "Order not found";
    private static final String ORDER_ID_AUDIT_PREFIX = "orderId=";
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final String PIN_SECRET = System.getenv().getOrDefault(
            "SERVICE_PIN_SECRET",
            "lexho-local-service-pin-secret-change-in-production"
    );

    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of(
            "id", "customerName", "amount", "status", "paymentMode",
            "assignedPartnerId", "acceptedAt", "startedAt", "completedAt",
            "rejectedAt", LexhoConstants.FIELD_CREATED_AT
    );

    private final PartnerOrderRepository partnerOrderRepository;
    private final OrderStatusHistoryRepository orderStatusHistoryRepository;
    private final com.lexho.user_service.lexhopartner.util.FileStorageUtil fileStorageUtil;
    private final AuditLogUtil auditLogUtil;
    private final PartnerShiftRepository partnerShiftRepository;
    private final PartnerSettlementService partnerSettlementService;
    private final BusinessMetricsService businessMetricsService;
    private final DynamicContentTranslationService dynamicTranslationService;
    private final NotificationTriggerService notificationTriggerService;
    private final PartnerDispatchEligibilityService partnerDispatchEligibilityService;
    private final CompletionSettingsService completionSettingsService;

    public PartnerOrderService(PartnerOrderRepository partnerOrderRepository,
                               OrderStatusHistoryRepository orderStatusHistoryRepository,
                               com.lexho.user_service.lexhopartner.util.FileStorageUtil fileStorageUtil,
                               AuditLogUtil auditLogUtil,
                               PartnerShiftRepository partnerShiftRepository,
                               PartnerSettlementService partnerSettlementService,
                               BusinessMetricsService businessMetricsService) {
        this(partnerOrderRepository,
                orderStatusHistoryRepository,
                fileStorageUtil,
                auditLogUtil,
                partnerShiftRepository,
                partnerSettlementService,
                businessMetricsService,
                Optional.empty(),
                Optional.empty(),
                new PartnerDispatchEligibilityService(),
                Optional.empty());
    }

    @Autowired
    public PartnerOrderService(PartnerOrderRepository partnerOrderRepository,
                               OrderStatusHistoryRepository orderStatusHistoryRepository,
                               com.lexho.user_service.lexhopartner.util.FileStorageUtil fileStorageUtil,
                               AuditLogUtil auditLogUtil,
                               PartnerShiftRepository partnerShiftRepository,
                               PartnerSettlementService partnerSettlementService,
                               BusinessMetricsService businessMetricsService,
                               Optional<DynamicContentTranslationService> dynamicTranslationService,
                               Optional<NotificationTriggerService> notificationTriggerService,
                               PartnerDispatchEligibilityService partnerDispatchEligibilityService,
                               Optional<CompletionSettingsService> completionSettingsService) {
        this.partnerOrderRepository = partnerOrderRepository;
        this.orderStatusHistoryRepository = orderStatusHistoryRepository;
        this.fileStorageUtil = fileStorageUtil;
        this.auditLogUtil = auditLogUtil;
        this.partnerShiftRepository = partnerShiftRepository;
        this.partnerSettlementService = partnerSettlementService;
        this.businessMetricsService = businessMetricsService;
        this.dynamicTranslationService = optionalValue(dynamicTranslationService);
        this.notificationTriggerService = optionalValue(notificationTriggerService);
        this.partnerDispatchEligibilityService = partnerDispatchEligibilityService;
        this.completionSettingsService = optionalValue(completionSettingsService);
    }

    private static <T> T optionalValue(Optional<T> optional) {
        return optional == null ? null : optional.orElse(null);
    }

    @Transactional(readOnly = true)
    public PageResponseDto<OrderResponseDto> getNewOrders(Long partnerId, int page, int size, String sortBy, String direction) {
        Pageable pageable = pageable(page, size, sortBy, direction);
        Page<PartnerOrder> result = partnerOrderRepository.findPartnerVisibleOpenOrders(pageable);
        return mapPage(partnerId, result);
    }

    @Transactional(readOnly = true)
    public OrderResponseDto getOrder(Long partnerId, Long orderId) {
        PartnerOrder order = partnerOrderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException(ORDER_NOT_FOUND));
        if (order.getAssignedPartnerId() != null
                && !partnerDispatchEligibilityService.canManageActiveBooking(partnerId, order)) {
            throw new BadRequestException("Order is assigned to another partner");
        }
        return toDto(partnerId, order);
    }

    @Transactional
    public OrderResponseDto acceptOrder(Long partnerId, Long orderId) {
        return transition(partnerId, orderId, List.of(OrderStatus.CREATED, OrderStatus.ASSIGNED), OrderStatus.ACCEPTED, "accepted");
    }

    @Transactional
    public OrderResponseDto rejectOrder(Long partnerId, Long orderId) {
        return transition(partnerId, orderId, List.of(OrderStatus.CREATED, OrderStatus.ASSIGNED), OrderStatus.REJECTED, "rejected");
    }

    @Transactional
    public OrderResponseDto arriveOrder(Long partnerId, Long orderId) {
        PartnerOrder order = getOwnedOrderForUpdate(partnerId, orderId);
        if (order.getStatus() == OrderStatus.ARRIVED) {
            return toDto(partnerId, order);
        }
        assertAllowed(order.getStatus(), List.of(OrderStatus.ACCEPTED, OrderStatus.ASSIGNED));
        ensureStartPin(order, LocalDateTime.now());
        return saveTransition(order, OrderStatus.ARRIVED, "partner arrived");
    }

    @Transactional
    public OrderResponseDto startOrder(Long partnerId, Long orderId, MultipartFile beforeImage, String pin) {
        PartnerOrder order = getOwnedOrderForUpdate(partnerId, orderId);
        if (order.getStatus() == OrderStatus.IN_PROGRESS || order.getStatus() == OrderStatus.STARTED) {
            return toDto(partnerId, order);
        }
        assertAllowed(order.getStatus(), List.of(OrderStatus.ARRIVED));
        LocalDateTime now = LocalDateTime.now();
        verifyPin(order, PinPurpose.START, pin, now);
        if (hasFile(beforeImage)) {
            order.setBeforeImageUrls(fileStorageUtil.storeImage(beforeImage, "order-before"));
        }
        return saveTransition(order, OrderStatus.IN_PROGRESS, "service start PIN verified");
    }

    @Transactional
    public OrderResponseDto startOrder(Long partnerId, Long orderId, MultipartFile beforeImage) {
        throw new BadRequestException("Customer start PIN verification is required");
    }

    @Transactional
    public OrderResponseDto requestCompletePin(Long partnerId, Long orderId) {
        PartnerOrder order = getOwnedOrderForUpdate(partnerId, orderId);
        if (order.getStatus() == OrderStatus.COMPLETED) {
            return toDto(partnerId, order);
        }
        assertAllowed(order.getStatus(), List.of(OrderStatus.IN_PROGRESS));
        ensureCompletePin(order, LocalDateTime.now());
        partnerOrderRepository.save(order);
        auditLogUtil.audit(partnerId, "SERVICE_COMPLETE_PIN_REQUESTED", ORDER_ID_AUDIT_PREFIX + order.getId());
        return toDto(partnerId, order);
    }

    @Transactional
    public OrderResponseDto completeOrder(Long partnerId, Long orderId, MultipartFile afterImage, String pin) {
        PartnerOrder order = getOwnedOrderForUpdate(partnerId, orderId);
        assertAllowed(order.getStatus(), List.of(OrderStatus.IN_PROGRESS));
        if (order.getServiceStartPinConsumedAt() == null) {
            throw new BadRequestException("Service has not been started with customer PIN verification");
        }
        verifyPin(order, PinPurpose.COMPLETE, pin, LocalDateTime.now());
        if (hasFile(afterImage)) {
            order.setAfterImageUrls(fileStorageUtil.storeImage(afterImage, "order-after"));
        }
        return saveTransition(order, OrderStatus.COMPLETED, "service completed");
    }

    @Transactional
    public OrderResponseDto completeOrder(Long partnerId, Long orderId, MultipartFile afterImage) {
        throw new BadRequestException("Customer complete PIN verification is required");
    }

    @Transactional
    public OrderResponseDto adminCompleteOrder(Long adminId, Long orderId, String reason) {
        PartnerOrder order = partnerOrderRepository.findByIdForUpdate(orderId)
                .orElseThrow(() -> new ResourceNotFoundException(ORDER_NOT_FOUND));
        assertAllowed(order.getStatus(), List.of(OrderStatus.IN_PROGRESS));
        if (order.getServiceStartPinConsumedAt() == null) {
            throw new BadRequestException("Service has not been started with customer PIN verification");
        }
        String normalizedReason = reason == null || reason.isBlank()
                ? "admin completion override"
                : "admin completion override: " + reason.trim();
        return saveTransition(order, OrderStatus.COMPLETED, normalizedReason, "ADMIN", adminId);
    }

    @Transactional(readOnly = true)
    public PageResponseDto<OrderResponseDto> getHistory(Long partnerId, int page, int size, String sortBy, String direction) {
        Pageable pageable = pageable(page, size, sortBy, direction);
        return mapPage(partnerId, partnerOrderRepository.findByAssignedPartnerId(partnerId, pageable));
    }

    @Transactional(readOnly = true)
    public List<OrderTimelineDto> timeline(Long partnerId, Long orderId) {
        PartnerOrder order = partnerOrderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException(ORDER_NOT_FOUND));
        if (order.getAssignedPartnerId() != null
                && !partnerDispatchEligibilityService.canManageActiveBooking(partnerId, order)) {
            throw new BadRequestException("Order is assigned to another partner");
        }
        return orderStatusHistoryRepository.findByOrderIdOrderByCreatedAtAsc(orderId).stream()
                .map(history -> new OrderTimelineDto(
                        history.getId(),
                        orderId,
                        history.getFromStatus(),
                        history.getToStatus(),
                        history.getRemarks(),
                        history.getActorType(),
                        history.getActorId(),
                        history.getCreatedAt()
                ))
                .toList();
    }

    @Transactional(readOnly = true)
    public DashboardStatsDto dashboard(Long partnerId) {
        LocalDateTime now = LocalDateTime.now();
        LocalDate today = now.toLocalDate();
        LocalDateTime startOfToday = today.atStartOfDay();
        LocalDateTime startOfWeek = today.minusDays(today.getDayOfWeek().getValue() - 1L).atStartOfDay();

        PartnerEarningsSummaryDto settlementSummary = partnerSettlementService.earningsSummary(partnerId);
        Long completedOrders = partnerOrderRepository.countByAssignedPartnerIdAndStatus(partnerId, OrderStatus.COMPLETED);
        Long acceptedOrders = partnerOrderRepository.countByAssignedPartnerIdAndStatus(partnerId, OrderStatus.ACCEPTED);
        Long arrivedOrders = partnerOrderRepository.countByAssignedPartnerIdAndStatus(partnerId, OrderStatus.ARRIVED);
        Long inProgressOrders = partnerOrderRepository.countByAssignedPartnerIdAndStatus(partnerId, OrderStatus.IN_PROGRESS);
        Long legacyStartedOrders = partnerOrderRepository.countByAssignedPartnerIdAndStatus(partnerId, OrderStatus.STARTED);
        Long pendingOrders = acceptedOrders + arrivedOrders + inProgressOrders + legacyStartedOrders;
        Long activeDeliveries = inProgressOrders + legacyStartedOrders;
        Double totalOnlineHours = calculateTodayOnlineHours(partnerId, now);
        return new DashboardStatsDto(
                settlementSummary.todayEarnings(),
                completedOrders,
                pendingOrders,
                totalOnlineHours,
                activeDeliveries,
                settlementSummary.weeklyEarnings()
        );
    }

    @Transactional(readOnly = true)
    public PartnerEarningsSummaryDto earningsSummary(Long partnerId) {
        return partnerSettlementService.earningsSummary(partnerId);
    }

    @Transactional(readOnly = true)
    public PageResponseDto<PartnerEarningTransactionDto> earningTransactions(Long partnerId,
                                                                              LocalDate fromDate,
                                                                              LocalDate toDate,
                                                                              int page,
                                                                              int size,
                                                                              String sortBy,
                                                                              String direction) {
        return partnerSettlementService.earningTransactions(partnerId, fromDate, toDate, page, size);
    }

    private Double calculateTodayOnlineHours(Long partnerId, LocalDateTime now) {
        PartnerShiftEntity shift = partnerShiftRepository.findByPartnerId(partnerId).orElse(null);
        if (shift == null || shift.getShiftStart() == null) {
            return 0.0;
        }
        LocalDateTime start = shift.getShiftStart();
        LocalDateTime end = shift.getShiftEnd();
        if (shift.isOnline() || end == null || end.isAfter(now)) {
            end = now;
        }
        LocalDateTime dayStart = now.toLocalDate().atStartOfDay();
        if (end.isBefore(dayStart) || !end.isAfter(start)) {
            return 0.0;
        }
        LocalDateTime effectiveStart = start.isBefore(dayStart) ? dayStart : start;
        if (!end.isAfter(effectiveStart)) {
            return 0.0;
        }
        long minutes = ChronoUnit.MINUTES.between(effectiveStart, end);
        return Math.round((minutes / 60.0) * 100.0) / 100.0;
    }

    private OrderResponseDto transition(Long partnerId, Long orderId, List<OrderStatus> from, OrderStatus to, String remarks) {
        PartnerOrder order = partnerOrderRepository.findByIdForUpdate(orderId)
                .orElseThrow(() -> new ResourceNotFoundException(ORDER_NOT_FOUND));
        if (order.getAssignedPartnerId() != null
                && !partnerDispatchEligibilityService.canManageActiveBooking(partnerId, order)) {
            throw new BadRequestException("Order is assigned to another partner");
        }
        if (to == OrderStatus.ACCEPTED) {
            boolean instant = "INSTANT".equalsIgnoreCase(order.getBookingType());
            if (instant && order.getAssignedPartnerId() == null) {
                throw new BadRequestException("Instant orders must be accepted through partner requests");
            }
            if (instant && order.getPaymentStatus() != PaymentStatus.SUCCESS) {
                throw new BadRequestException("Order payment is not confirmed");
            }
            PartnerShiftEntity shift = partnerShiftRepository.findByPartnerIdForUpdate(partnerId).orElse(null);
            if (shift == null || !shift.isOnline() || shift.getActiveJobsCount() >= shift.getMaxActiveJobs()) {
                throw new BadRequestException("Partner is not eligible for new dispatch");
            }
        }
        assertAllowed(order.getStatus(), from);
        order.setAssignedPartnerId(partnerId);
        return saveTransition(order, to, remarks);
    }

    private OrderResponseDto saveTransition(PartnerOrder order, OrderStatus to, String remarks) {
        return saveTransition(order, to, remarks, "PARTNER", order.getAssignedPartnerId());
    }

    private OrderResponseDto saveTransition(PartnerOrder order,
                                            OrderStatus to,
                                            String remarks,
                                            String actorType,
                                            Long actorId) {
        OrderStatus from = order.getStatus();
        order.setStatus(to);
        LocalDateTime now = LocalDateTime.now();
        switch (to) {
            case ACCEPTED -> order.setAcceptedAt(now);
            case ARRIVED -> {
                if (order.getArrivedAt() == null) {
                    order.setArrivedAt(now);
                }
            }
            case STARTED -> order.setStartedAt(now);
            case IN_PROGRESS -> {
                if (order.getStartedAt() == null) {
                    order.setStartedAt(now);
                }
                if (order.getInProgressAt() == null) {
                    order.setInProgressAt(now);
                }
            }
            case COMPLETED -> order.setCompletedAt(now);
            case REJECTED -> order.setRejectedAt(now);
            default -> {
            }
        }
        PartnerOrder saved = partnerOrderRepository.save(order);
        if (subscriptionVisitOrders != null) {
            subscriptionVisitOrders.syncStatus(saved);
        }
        if (to == OrderStatus.ACCEPTED) {
            businessMetricsService.incrementPartnerAcceptance();
        }
        if (to == OrderStatus.COMPLETED) {
            businessMetricsService.incrementOrdersCompleted();
            partnerSettlementService.ensurePayout(saved.getId());
            decrementActiveJobs(saved.getAssignedPartnerId());
        }
        publishLifecycleNotification(saved, to);
        OrderStatusHistory history = new OrderStatusHistory();
        history.setOrder(saved);
        history.setFromStatus(from);
        history.setToStatus(to);
        history.setRemarks(remarks);
        history.setActorType(actorType);
        history.setActorId(actorId);
        orderStatusHistoryRepository.save(history);
        auditLogUtil.audit(actorId, "ORDER_" + to.name(), ORDER_ID_AUDIT_PREFIX + saved.getId());
        return toDto(saved.getAssignedPartnerId(), saved);
    }

    private void publishLifecycleNotification(PartnerOrder order, OrderStatus to) {
        if (notificationTriggerService == null) {
            return;
        }
        if (to == OrderStatus.IN_PROGRESS && order.getCustomerId() != null) {
            notificationTriggerService.serviceStarted(order.getCustomerId(), String.valueOf(order.getId()));
        }
        if (to == OrderStatus.COMPLETED) {
            if (order.getCustomerId() != null) {
                notificationTriggerService.serviceCompleted(order.getCustomerId(), String.valueOf(order.getId()));
            }
            if (order.getAssignedPartnerId() != null) {
                notificationTriggerService.partnerServiceCompleted(
                        order.getAssignedPartnerId(),
                        String.valueOf(order.getId()),
                        partnerSettlementService.netEarningForOrder(order.getId()).orElse(null)
                );
            }
        }
    }

    private void ensureStartPin(PartnerOrder order, LocalDateTime now) {
        if (activePin(order.getServiceStartPinNonce(), order.getServiceStartPinExpiresAt(),
                order.getServiceStartPinConsumedAt(), now)) {
            return;
        }
        order.setServiceStartPinNonce(newNonce());
        order.setServiceStartPinExpiresAt(now.plusMinutes(PIN_EXPIRY_MINUTES));
        order.setServiceStartPinAttempts(0);
        order.setServiceStartPinConsumedAt(null);
    }

    private void ensureCompletePin(PartnerOrder order, LocalDateTime now) {
        if (activePin(order.getServiceCompletePinNonce(), order.getServiceCompletePinExpiresAt(),
                order.getServiceCompletePinConsumedAt(), now)) {
            return;
        }
        order.setServiceCompletePinNonce(newNonce());
        order.setServiceCompletePinExpiresAt(now.plusMinutes(PIN_EXPIRY_MINUTES));
        order.setServiceCompletePinAttempts(0);
        order.setServiceCompletePinConsumedAt(null);
    }

    private void verifyPin(PartnerOrder order, PinPurpose purpose, String submittedPin, LocalDateTime now) {
        String normalizedPin = normalizePin(submittedPin);
        if (purpose == PinPurpose.START) {
            ensurePinVerifiable(order.getServiceStartPinNonce(), order.getServiceStartPinExpiresAt(),
                    order.getServiceStartPinConsumedAt(), order.getServiceStartPinAttempts(), now);
            if (!expectedPin(order, purpose).equals(normalizedPin)) {
                order.setServiceStartPinAttempts(order.getServiceStartPinAttempts() + 1);
                partnerOrderRepository.save(order);
            auditLogUtil.audit(order.getAssignedPartnerId(), "SERVICE_START_PIN_FAILED", ORDER_ID_AUDIT_PREFIX + order.getId());
                throw new BadRequestException("Invalid or expired start PIN");
            }
            order.setServiceStartPinConsumedAt(now);
        auditLogUtil.audit(order.getAssignedPartnerId(), "SERVICE_START_PIN_VERIFIED", ORDER_ID_AUDIT_PREFIX + order.getId());
            return;
        }
        ensurePinVerifiable(order.getServiceCompletePinNonce(), order.getServiceCompletePinExpiresAt(),
                order.getServiceCompletePinConsumedAt(), order.getServiceCompletePinAttempts(), now);
        if (!expectedPin(order, purpose).equals(normalizedPin)) {
            order.setServiceCompletePinAttempts(order.getServiceCompletePinAttempts() + 1);
            partnerOrderRepository.save(order);
            auditLogUtil.audit(order.getAssignedPartnerId(), "SERVICE_COMPLETE_PIN_FAILED", ORDER_ID_AUDIT_PREFIX + order.getId());
            throw new BadRequestException("Invalid or expired complete PIN");
        }
        order.setServiceCompletePinConsumedAt(now);
        auditLogUtil.audit(order.getAssignedPartnerId(), "SERVICE_COMPLETE_PIN_VERIFIED", ORDER_ID_AUDIT_PREFIX + order.getId());
    }

    private void ensurePinVerifiable(String nonce,
                                     LocalDateTime expiresAt,
                                     LocalDateTime consumedAt,
                                     int attempts,
                                     LocalDateTime now) {
        if (!activePin(nonce, expiresAt, consumedAt, now)) {
            throw new BadRequestException("PIN is invalid or expired");
        }
        if (attempts >= MAX_PIN_ATTEMPTS) {
            throw new BadRequestException("PIN attempt limit exceeded");
        }
    }

    private boolean activePin(String nonce, LocalDateTime expiresAt, LocalDateTime consumedAt, LocalDateTime now) {
        return nonce != null
                && expiresAt != null
                && consumedAt == null
                && expiresAt.isAfter(now);
    }

    private String normalizePin(String pin) {
        if (pin == null || !pin.matches("\\d{4}")) {
            throw new BadRequestException("PIN must be 4 digits");
        }
        return pin;
    }

    public String serviceStartPinForCustomer(PartnerOrder order) {
        return activePin(order.getServiceStartPinNonce(), order.getServiceStartPinExpiresAt(),
                order.getServiceStartPinConsumedAt(), LocalDateTime.now()) ? expectedPin(order, PinPurpose.START) : null;
    }

    public String serviceCompletePinForCustomer(PartnerOrder order) {
        return activePin(order.getServiceCompletePinNonce(), order.getServiceCompletePinExpiresAt(),
                order.getServiceCompletePinConsumedAt(), LocalDateTime.now()) ? expectedPin(order, PinPurpose.COMPLETE) : null;
    }

    private String expectedPin(PartnerOrder order, PinPurpose purpose) {
        String nonce = purpose == PinPurpose.START ? order.getServiceStartPinNonce() : order.getServiceCompletePinNonce();
        return ServicePinHasher.expectedPin(order.getId(), purpose.name(), nonce, PIN_SECRET);
    }

    private String newNonce() {
        byte[] bytes = new byte[16];
        SECURE_RANDOM.nextBytes(bytes);
        return HexFormat.of().formatHex(bytes);
    }

    private PartnerOrder getOwnedOrder(Long partnerId, Long orderId) {
        PartnerOrder order = partnerOrderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException(ORDER_NOT_FOUND));
        if (!partnerDispatchEligibilityService.canManageActiveBooking(partnerId, order)) {
            throw new BadRequestException("Order is not assigned to this partner");
        }
        return order;
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

    private PartnerOrder getOwnedOrderForUpdate(Long partnerId, Long orderId) {
        PartnerOrder order = partnerOrderRepository.findByIdForUpdate(orderId)
                .orElseThrow(() -> new ResourceNotFoundException(ORDER_NOT_FOUND));
        if (!partnerDispatchEligibilityService.canManageActiveBooking(partnerId, order)) {
            throw new BadRequestException("Order is not assigned to this partner");
        }
        return order;
    }

    private void assertAllowed(OrderStatus current, List<OrderStatus> allowed) {
        if (!allowed.contains(current)) {
            throw new BadRequestException("Invalid order status transition from " + current);
        }
    }

    private boolean hasFile(MultipartFile file) {
        return file != null && !file.isEmpty();
    }

    private PageResponseDto<OrderResponseDto> mapPage(Long partnerId, Page<PartnerOrder> page) {
        return new PageResponseDto<>(page.getContent().stream().map(order -> toDto(partnerId, order)).toList(), page.getNumber(), page.getSize(),
                page.getTotalElements(), page.getTotalPages());
    }

    private Pageable pageable(int page, int size, String sortBy, String direction) {
        String field = sanitizeSortBy(sortBy);
        Sort sort = "asc".equalsIgnoreCase(direction) ? Sort.by(field).ascending() : Sort.by(field).descending();
        return PageRequest.of(page, size, sort);
    }

    private String sanitizeSortBy(String sortBy) {
        if (sortBy == null || sortBy.isBlank()) {
            return LexhoConstants.FIELD_CREATED_AT;
        }
        String candidate = sortBy.trim();
        return ALLOWED_SORT_FIELDS.contains(candidate) ? candidate : LexhoConstants.FIELD_CREATED_AT;
    }

    private OrderResponseDto toDto(Long partnerId, PartnerOrder order) {
        return OrderResponseDto.builder()
                .id(order.getId())
                .customerName(order.getCustomerName())
                .customerPhone(order.getCustomerPhone())
                .address(order.getAddress())
                .latitude(order.getLatitude())
                .longitude(order.getLongitude())
                .serviceType(translatedServiceType(partnerId, order))
                .bookingType(order.getBookingType())
                .scheduledAt(order.getScheduledAt())
                .amount(partnerSettlementService.partnerVisibleAmountForOrder(order))
                .paymentMode(order.getPaymentMode())
                .status(order.getStatus())
                .jobStatus(jobStatus(order.getStatus()))
                .assignedPartnerId(order.getAssignedPartnerId())
                .acceptedAt(order.getAcceptedAt())
                .arrivedAt(order.getArrivedAt())
                .startedAt(order.getStartedAt())
                .inProgressAt(order.getInProgressAt())
                .completedAt(order.getCompletedAt())
                .rejectedAt(order.getRejectedAt())
                .serviceStartPinExpiresAt(order.getServiceStartPinExpiresAt())
                .serviceCompletePinExpiresAt(order.getServiceCompletePinExpiresAt())
                .startPinRequired(order.getStatus() == OrderStatus.ARRIVED)
                .completePinRequired(order.getStatus() == OrderStatus.IN_PROGRESS)
                .completionRequested(activePin(order.getServiceCompletePinNonce(), order.getServiceCompletePinExpiresAt(),
                        order.getServiceCompletePinConsumedAt(), LocalDateTime.now()))
                .reviewEligible(order.getStatus() == OrderStatus.COMPLETED
                        && (completionSettingsService == null || completionSettingsService.reviewEligibleFor(order)))
                .createdAt(order.getCreatedAt())
                .build();
    }

    private String jobStatus(OrderStatus status) {
        if (status == OrderStatus.STARTED || status == OrderStatus.IN_PROGRESS) {
            return "IN_PROGRESS";
        }
        return status == null ? null : status.name();
    }

    private String translatedServiceType(Long partnerId, PartnerOrder order) {
        String serviceType = order.getServiceType();
        if (dynamicTranslationService == null || serviceType == null || serviceType.isBlank()) {
            return serviceType;
        }
        return dynamicTranslationService.translateForPartner(
                partnerId, null, "partner_order", order.getId(), "service_type", serviceType);
    }

    private enum PinPurpose {
        START,
        COMPLETE
    }
}

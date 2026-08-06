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
import com.lexho.user_service.lexhopartner.repository.OrderStatusHistoryRepository;
import com.lexho.user_service.lexhopartner.repository.PartnerOrderRepository;
import com.lexho.user_service.observability.BusinessMetricsService;
import com.lexho.user_service.lexhopartner.util.AuditLogUtil;
import com.lexho.user_service.dispatch.entity.PartnerShiftEntity;
import com.lexho.user_service.dispatch.repository.PartnerShiftRepository;
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
import java.util.List;
import java.util.Set;

@Service
public class PartnerOrderService {
    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of(
            "id", "customerName", "amount", "status", "paymentMode",
            "assignedPartnerId", "acceptedAt", "startedAt", "completedAt",
            "rejectedAt", "createdAt"
    );

    private final PartnerOrderRepository partnerOrderRepository;
    private final OrderStatusHistoryRepository orderStatusHistoryRepository;
    private final com.lexho.user_service.lexhopartner.util.FileStorageUtil fileStorageUtil;
    private final AuditLogUtil auditLogUtil;
    private final PartnerShiftRepository partnerShiftRepository;
    private final PartnerSettlementService partnerSettlementService;
    private final BusinessMetricsService businessMetricsService;

    public PartnerOrderService(PartnerOrderRepository partnerOrderRepository,
                               OrderStatusHistoryRepository orderStatusHistoryRepository,
                               com.lexho.user_service.lexhopartner.util.FileStorageUtil fileStorageUtil,
                               AuditLogUtil auditLogUtil,
                               PartnerShiftRepository partnerShiftRepository,
                               PartnerSettlementService partnerSettlementService,
                               BusinessMetricsService businessMetricsService) {
        this.partnerOrderRepository = partnerOrderRepository;
        this.orderStatusHistoryRepository = orderStatusHistoryRepository;
        this.fileStorageUtil = fileStorageUtil;
        this.auditLogUtil = auditLogUtil;
        this.partnerShiftRepository = partnerShiftRepository;
        this.partnerSettlementService = partnerSettlementService;
        this.businessMetricsService = businessMetricsService;
    }

    @Transactional(readOnly = true)
    public PageResponseDto<OrderResponseDto> getNewOrders(int page, int size, String sortBy, String direction) {
        Pageable pageable = pageable(page, size, sortBy, direction);
        Page<PartnerOrder> result = partnerOrderRepository.findByStatusInAndAssignedPartnerIdIsNull(
                List.of(OrderStatus.CREATED, OrderStatus.ASSIGNED), pageable);
        return mapPage(result);
    }

    @Transactional(readOnly = true)
    public OrderResponseDto getOrder(Long partnerId, Long orderId) {
        PartnerOrder order = partnerOrderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));
        if (order.getAssignedPartnerId() != null && !order.getAssignedPartnerId().equals(partnerId)) {
            throw new BadRequestException("Order is assigned to another partner");
        }
        return toDto(order);
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
    public OrderResponseDto startOrder(Long partnerId, Long orderId, MultipartFile beforeImage) {
        PartnerOrder order = getOwnedOrder(partnerId, orderId);
        assertAllowed(order.getStatus(), List.of(OrderStatus.ACCEPTED));
        if (hasFile(beforeImage)) {
            order.setBeforeImageUrls(fileStorageUtil.storeImage(beforeImage, "order-before"));
        }
        return saveTransition(order, OrderStatus.STARTED, "service started");
    }

    @Transactional
    public OrderResponseDto completeOrder(Long partnerId, Long orderId, MultipartFile afterImage) {
        PartnerOrder order = getOwnedOrder(partnerId, orderId);
        assertAllowed(order.getStatus(), List.of(OrderStatus.STARTED));
        if (hasFile(afterImage)) {
            order.setAfterImageUrls(fileStorageUtil.storeImage(afterImage, "order-after"));
        }
        return saveTransition(order, OrderStatus.COMPLETED, "service completed");
    }

    @Transactional(readOnly = true)
    public PageResponseDto<OrderResponseDto> getHistory(Long partnerId, int page, int size, String sortBy, String direction) {
        Pageable pageable = pageable(page, size, sortBy, direction);
        return mapPage(partnerOrderRepository.findByAssignedPartnerId(partnerId, pageable));
    }

    @Transactional(readOnly = true)
    public List<OrderTimelineDto> timeline(Long partnerId, Long orderId) {
        PartnerOrder order = partnerOrderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));
        if (order.getAssignedPartnerId() != null && !order.getAssignedPartnerId().equals(partnerId)) {
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
        Long startedOrders = partnerOrderRepository.countByAssignedPartnerIdAndStatus(partnerId, OrderStatus.STARTED);
        Long pendingOrders = acceptedOrders + startedOrders;
        Long activeDeliveries = startedOrders;
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
        PartnerOrder order = partnerOrderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));
        if (order.getAssignedPartnerId() != null && !order.getAssignedPartnerId().equals(partnerId)) {
            throw new BadRequestException("Order is assigned to another partner");
        }
        assertAllowed(order.getStatus(), from);
        order.setAssignedPartnerId(partnerId);
        return saveTransition(order, to, remarks);
    }

    private OrderResponseDto saveTransition(PartnerOrder order, OrderStatus to, String remarks) {
        OrderStatus from = order.getStatus();
        order.setStatus(to);
        LocalDateTime now = LocalDateTime.now();
        switch (to) {
            case ACCEPTED -> order.setAcceptedAt(now);
            case STARTED -> order.setStartedAt(now);
            case COMPLETED -> order.setCompletedAt(now);
            case REJECTED -> order.setRejectedAt(now);
            default -> {
            }
        }
        PartnerOrder saved = partnerOrderRepository.save(order);
        if (to == OrderStatus.ACCEPTED) {
            businessMetricsService.incrementPartnerAcceptance();
        }
        if (to == OrderStatus.COMPLETED) {
            businessMetricsService.incrementOrdersCompleted();
            partnerSettlementService.ensurePayout(saved.getId());
        }
        OrderStatusHistory history = new OrderStatusHistory();
        history.setOrder(saved);
        history.setFromStatus(from);
        history.setToStatus(to);
        history.setRemarks(remarks);
        history.setActorType("PARTNER");
        history.setActorId(saved.getAssignedPartnerId());
        orderStatusHistoryRepository.save(history);
        auditLogUtil.audit(saved.getAssignedPartnerId(), "ORDER_" + to.name(), "orderId=" + saved.getId());
        return toDto(saved);
    }

    private PartnerOrder getOwnedOrder(Long partnerId, Long orderId) {
        PartnerOrder order = partnerOrderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));
        if (order.getAssignedPartnerId() == null || !order.getAssignedPartnerId().equals(partnerId)) {
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

    private PageResponseDto<OrderResponseDto> mapPage(Page<PartnerOrder> page) {
        return new PageResponseDto<>(page.getContent().stream().map(this::toDto).toList(), page.getNumber(), page.getSize(),
                page.getTotalElements(), page.getTotalPages());
    }

    private Pageable pageable(int page, int size, String sortBy, String direction) {
        String field = sanitizeSortBy(sortBy);
        Sort sort = "asc".equalsIgnoreCase(direction) ? Sort.by(field).ascending() : Sort.by(field).descending();
        return PageRequest.of(page, size, sort);
    }

    private String sanitizeSortBy(String sortBy) {
        if (sortBy == null || sortBy.isBlank()) {
            return "createdAt";
        }
        String candidate = sortBy.trim();
        return ALLOWED_SORT_FIELDS.contains(candidate) ? candidate : "createdAt";
    }

    private OrderResponseDto toDto(PartnerOrder order) {
        return OrderResponseDto.builder()
                .id(order.getId())
                .customerName(order.getCustomerName())
                .customerPhone(order.getCustomerPhone())
                .address(order.getAddress())
                .latitude(order.getLatitude())
                .longitude(order.getLongitude())
                .serviceType(order.getServiceType())
                .amount(order.getAmount())
                .paymentMode(order.getPaymentMode())
                .status(order.getStatus())
                .assignedPartnerId(order.getAssignedPartnerId())
                .acceptedAt(order.getAcceptedAt())
                .startedAt(order.getStartedAt())
                .completedAt(order.getCompletedAt())
                .rejectedAt(order.getRejectedAt())
                .createdAt(order.getCreatedAt())
                .build();
    }

}

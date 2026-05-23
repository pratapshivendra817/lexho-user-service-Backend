package com.lexho.user_service.lexhopartner.service;

import com.lexho.user_service.exception.BadRequestException;
import com.lexho.user_service.exception.ResourceNotFoundException;
import com.lexho.user_service.lexhopartner.dto.DashboardStatsDto;
import com.lexho.user_service.lexhopartner.dto.PartnerEarningTransactionDto;
import com.lexho.user_service.lexhopartner.dto.PartnerEarningsSummaryDto;
import com.lexho.user_service.lexhopartner.dto.OrderResponseDto;
import com.lexho.user_service.lexhopartner.dto.PageResponseDto;
import com.lexho.user_service.lexhopartner.entity.OrderStatus;
import com.lexho.user_service.lexhopartner.entity.OrderStatusHistory;
import com.lexho.user_service.lexhopartner.entity.PartnerOrder;
import com.lexho.user_service.lexhopartner.repository.OrderStatusHistoryRepository;
import com.lexho.user_service.lexhopartner.repository.PartnerOrderRepository;
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

@Service
public class PartnerOrderService {

    private final PartnerOrderRepository partnerOrderRepository;
    private final OrderStatusHistoryRepository orderStatusHistoryRepository;
    private final com.lexho.user_service.lexhopartner.util.FileStorageUtil fileStorageUtil;
    private final AuditLogUtil auditLogUtil;
    private final PartnerShiftRepository partnerShiftRepository;

    public PartnerOrderService(PartnerOrderRepository partnerOrderRepository,
                               OrderStatusHistoryRepository orderStatusHistoryRepository,
                               com.lexho.user_service.lexhopartner.util.FileStorageUtil fileStorageUtil,
                               AuditLogUtil auditLogUtil,
                               PartnerShiftRepository partnerShiftRepository) {
        this.partnerOrderRepository = partnerOrderRepository;
        this.orderStatusHistoryRepository = orderStatusHistoryRepository;
        this.fileStorageUtil = fileStorageUtil;
        this.auditLogUtil = auditLogUtil;
        this.partnerShiftRepository = partnerShiftRepository;
    }

    @Transactional(readOnly = true)
    public PageResponseDto<OrderResponseDto> getNewOrders(int page, int size, String sortBy, String direction) {
        Pageable pageable = pageable(page, size, sortBy, direction);
        Page<PartnerOrder> result = partnerOrderRepository.findByStatusInAndAssignedPartnerIdIsNull(
                List.of(OrderStatus.CREATED, OrderStatus.ASSIGNED), pageable);
        return mapPage(result);
    }

    @Transactional
    public OrderResponseDto acceptOrder(Long partnerId, Long orderId) {
        return transition(partnerId, orderId, List.of(OrderStatus.CREATED, OrderStatus.ASSIGNED), OrderStatus.ACCEPTED, "accepted");
    }

    @Transactional
    public OrderResponseDto rejectOrder(Long partnerId, Long orderId) {
        return transition(partnerId, orderId, List.of(OrderStatus.CREATED, OrderStatus.ASSIGNED), OrderStatus.CANCELLED, "rejected");
    }

    @Transactional
    public OrderResponseDto startOrder(Long partnerId, Long orderId, MultipartFile beforeImage) {
        PartnerOrder order = getOwnedOrder(partnerId, orderId);
        assertAllowed(order.getStatus(), List.of(OrderStatus.ACCEPTED));
        order.setBeforeImageUrls(fileStorageUtil.storeImage(beforeImage, "order-before"));
        return saveTransition(order, OrderStatus.STARTED, "service started");
    }

    @Transactional
    public OrderResponseDto completeOrder(Long partnerId, Long orderId, MultipartFile afterImage) {
        PartnerOrder order = getOwnedOrder(partnerId, orderId);
        assertAllowed(order.getStatus(), List.of(OrderStatus.STARTED));
        order.setAfterImageUrls(fileStorageUtil.storeImage(afterImage, "order-after"));
        return saveTransition(order, OrderStatus.COMPLETED, "service completed");
    }

    @Transactional(readOnly = true)
    public PageResponseDto<OrderResponseDto> getHistory(Long partnerId, int page, int size, String sortBy, String direction) {
        Pageable pageable = pageable(page, size, sortBy, direction);
        return mapPage(partnerOrderRepository.findByAssignedPartnerId(partnerId, pageable));
    }

    @Transactional(readOnly = true)
    public DashboardStatsDto dashboard(Long partnerId) {
        LocalDateTime now = LocalDateTime.now();
        LocalDate today = now.toLocalDate();
        LocalDateTime startOfToday = today.atStartOfDay();
        LocalDateTime startOfWeek = today.minusDays(today.getDayOfWeek().getValue() - 1L).atStartOfDay();

        Double earnings = partnerOrderRepository.sumAmountByPartnerAndStatusAndUpdatedAtBetween(
                partnerId, OrderStatus.COMPLETED, startOfToday, now);
        Double weeklyEarnings = partnerOrderRepository.sumAmountByPartnerAndStatusAndUpdatedAtBetween(
                partnerId, OrderStatus.COMPLETED, startOfWeek, now);
        Long completedOrders = partnerOrderRepository.countByAssignedPartnerIdAndStatus(partnerId, OrderStatus.COMPLETED);
        Long acceptedOrders = partnerOrderRepository.countByAssignedPartnerIdAndStatus(partnerId, OrderStatus.ACCEPTED);
        Long startedOrders = partnerOrderRepository.countByAssignedPartnerIdAndStatus(partnerId, OrderStatus.STARTED);
        Long pendingOrders = acceptedOrders + startedOrders;
        Long activeDeliveries = startedOrders;
        Double totalOnlineHours = calculateTodayOnlineHours(partnerId, now);
        return new DashboardStatsDto(
                earnings == null ? 0.0 : earnings,
                completedOrders,
                pendingOrders,
                totalOnlineHours,
                activeDeliveries,
                weeklyEarnings == null ? 0.0 : weeklyEarnings
        );
    }

    @Transactional(readOnly = true)
    public PartnerEarningsSummaryDto earningsSummary(Long partnerId) {
        LocalDateTime now = LocalDateTime.now();
        LocalDate today = now.toLocalDate();
        LocalDateTime startOfToday = today.atStartOfDay();
        LocalDateTime startOfWeek = today.minusDays(today.getDayOfWeek().getValue() - 1L).atStartOfDay();
        LocalDateTime startOfMonth = today.withDayOfMonth(1).atStartOfDay();

        Double todayEarnings = partnerOrderRepository.sumAmountByPartnerAndStatusAndUpdatedAtBetween(
                partnerId, OrderStatus.COMPLETED, startOfToday, now);
        Double weeklyEarnings = partnerOrderRepository.sumAmountByPartnerAndStatusAndUpdatedAtBetween(
                partnerId, OrderStatus.COMPLETED, startOfWeek, now);
        Double monthlyEarnings = partnerOrderRepository.sumAmountByPartnerAndStatusAndUpdatedAtBetween(
                partnerId, OrderStatus.COMPLETED, startOfMonth, now);
        Double lifetimeEarnings = partnerOrderRepository.sumAmountByPartnerAndStatus(partnerId, OrderStatus.COMPLETED);

        Long completedToday = partnerOrderRepository.countByPartnerAndStatusAndUpdatedAtBetween(
                partnerId, OrderStatus.COMPLETED, startOfToday, now);
        Long completedWeek = partnerOrderRepository.countByPartnerAndStatusAndUpdatedAtBetween(
                partnerId, OrderStatus.COMPLETED, startOfWeek, now);
        Long completedMonth = partnerOrderRepository.countByPartnerAndStatusAndUpdatedAtBetween(
                partnerId, OrderStatus.COMPLETED, startOfMonth, now);
        Long completedLifetime = partnerOrderRepository.countByPartnerAndStatus(partnerId, OrderStatus.COMPLETED);

        double safeLifetime = lifetimeEarnings == null ? 0d : lifetimeEarnings;
        long safeCompleted = completedLifetime == null ? 0L : completedLifetime;
        double aov = safeCompleted <= 0 ? 0d : round2(safeLifetime / safeCompleted);

        return new PartnerEarningsSummaryDto(
                todayEarnings == null ? 0d : todayEarnings,
                weeklyEarnings == null ? 0d : weeklyEarnings,
                monthlyEarnings == null ? 0d : monthlyEarnings,
                safeLifetime,
                completedToday == null ? 0L : completedToday,
                completedWeek == null ? 0L : completedWeek,
                completedMonth == null ? 0L : completedMonth,
                safeCompleted,
                aov
        );
    }

    @Transactional(readOnly = true)
    public PageResponseDto<PartnerEarningTransactionDto> earningTransactions(Long partnerId,
                                                                              LocalDate fromDate,
                                                                              LocalDate toDate,
                                                                              int page,
                                                                              int size,
                                                                              String sortBy,
                                                                              String direction) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime start = fromDate == null ? LocalDate.of(1970, 1, 1).atStartOfDay() : fromDate.atStartOfDay();
        LocalDateTime end = toDate == null ? now : toDate.plusDays(1).atStartOfDay().minusNanos(1);
        if (end.isBefore(start)) {
            throw new BadRequestException("toDate must be greater than or equal to fromDate");
        }

        Pageable pageable = pageable(page, size, sortBy == null || sortBy.isBlank() ? "updatedAt" : sortBy, direction);
        Page<PartnerOrder> result = partnerOrderRepository.findByAssignedPartnerIdAndStatusAndUpdatedAtBetween(
                partnerId, OrderStatus.COMPLETED, start, end, pageable);

        List<PartnerEarningTransactionDto> rows = result.getContent().stream()
                .map(this::toEarningTransaction)
                .toList();
        return new PageResponseDto<>(rows, result.getNumber(), result.getSize(), result.getTotalElements(), result.getTotalPages());
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
        PartnerOrder saved = partnerOrderRepository.save(order);
        OrderStatusHistory history = new OrderStatusHistory();
        history.setOrder(saved);
        history.setFromStatus(from);
        history.setToStatus(to);
        history.setRemarks(remarks);
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

    private PageResponseDto<OrderResponseDto> mapPage(Page<PartnerOrder> page) {
        return new PageResponseDto<>(page.getContent().stream().map(this::toDto).toList(), page.getNumber(), page.getSize(),
                page.getTotalElements(), page.getTotalPages());
    }

    private Pageable pageable(int page, int size, String sortBy, String direction) {
        String field = sortBy == null || sortBy.isBlank() ? "createdAt" : sortBy;
        Sort sort = "asc".equalsIgnoreCase(direction) ? Sort.by(field).ascending() : Sort.by(field).descending();
        return PageRequest.of(page, size, sort);
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
                .createdAt(order.getCreatedAt())
                .build();
    }

    private PartnerEarningTransactionDto toEarningTransaction(PartnerOrder order) {
        double gross = order.getAmount() == null ? 0d : order.getAmount();
        double settlementAmount = round2(gross * 0.85d);
        return new PartnerEarningTransactionDto(
                order.getId(),
                order.getCustomerName(),
                order.getServiceType(),
                round2(gross),
                settlementAmount,
                "SETTLED",
                order.getUpdatedAt()
        );
    }

    private double round2(double value) {
        return Math.round(value * 100d) / 100d;
    }
}

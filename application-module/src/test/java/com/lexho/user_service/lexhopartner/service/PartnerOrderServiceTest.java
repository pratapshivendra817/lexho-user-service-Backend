package com.lexho.user_service.lexhopartner.service;

import com.lexho.user_service.exception.BadRequestException;
import com.lexho.user_service.exception.ResourceNotFoundException;
import com.lexho.user_service.lexhopartner.dto.PartnerEarningsSummaryDto;
import com.lexho.user_service.lexhopartner.entity.OrderStatus;
import com.lexho.user_service.lexhopartner.entity.OrderStatusHistory;
import com.lexho.user_service.lexhopartner.entity.PartnerOrder;
import com.lexho.user_service.lexhopartner.repository.OrderStatusHistoryRepository;
import com.lexho.user_service.lexhopartner.repository.PartnerOrderRepository;
import com.lexho.user_service.lexhopartner.util.AuditLogUtil;
import com.lexho.user_service.lexhopartner.util.FileStorageUtil;
import com.lexho.user_service.dispatch.entity.PartnerShiftEntity;
import com.lexho.user_service.dispatch.repository.PartnerShiftRepository;
import com.lexho.user_service.observability.BusinessMetricsService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.mock.web.MockMultipartFile;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PartnerOrderServiceTest {

    @Mock
    private PartnerOrderRepository partnerOrderRepository;
    @Mock
    private OrderStatusHistoryRepository orderStatusHistoryRepository;
    @Mock
    private FileStorageUtil fileStorageUtil;
    @Mock
    private AuditLogUtil auditLogUtil;
    @Mock
    private PartnerShiftRepository partnerShiftRepository;
    @Mock
    private PartnerSettlementService partnerSettlementService;
    @Mock
    private BusinessMetricsService businessMetricsService;

    @InjectMocks
    private PartnerOrderService partnerOrderService;

    @Test
    void acceptOrderShouldUpdateStatus() {
        PartnerOrder order = order(1L, OrderStatus.CREATED, null);
        when(partnerOrderRepository.findById(1L)).thenReturn(Optional.of(order));
        when(partnerOrderRepository.save(any(PartnerOrder.class))).thenAnswer(inv -> inv.getArgument(0));

        var response = partnerOrderService.acceptOrder(11L, 1L);

        assertEquals(OrderStatus.ACCEPTED, response.status());
        assertEquals(11L, response.assignedPartnerId());
        org.junit.jupiter.api.Assertions.assertNotNull(response.acceptedAt());
    }

    @Test
    void rejectOrderShouldUseRejectedTerminalStatus() {
        PartnerOrder order = order(1L, OrderStatus.CREATED, null);
        when(partnerOrderRepository.findById(1L)).thenReturn(Optional.of(order));
        when(partnerOrderRepository.save(any(PartnerOrder.class))).thenAnswer(inv -> inv.getArgument(0));

        var response = partnerOrderService.rejectOrder(11L, 1L);

        assertEquals(OrderStatus.REJECTED, response.status());
        org.junit.jupiter.api.Assertions.assertNotNull(response.rejectedAt());
    }

    @Test
    void startOrderShouldFailForInvalidState() {
        PartnerOrder order = order(1L, OrderStatus.CREATED, 10L);
        when(partnerOrderRepository.findById(1L)).thenReturn(Optional.of(order));

        assertThrows(BadRequestException.class, () -> partnerOrderService.startOrder(10L, 1L, null));
    }

    @Test
    void completeOrderShouldFailBeforeStart() {
        PartnerOrder order = order(1L, OrderStatus.ACCEPTED, 10L);
        when(partnerOrderRepository.findById(1L)).thenReturn(Optional.of(order));

        assertThrows(BadRequestException.class, () -> partnerOrderService.completeOrder(10L, 1L, null));
    }

    @Test
    void newOrdersAndHistoryUsePagingAndMapResults() {
        PartnerOrder order = order(7L, OrderStatus.CREATED, null);
        when(partnerOrderRepository.findByStatusInAndAssignedPartnerIdIsNull(any(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(order)));
        when(partnerOrderRepository.findByAssignedPartnerId(eq(11L), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(order)));

        var newOrders = partnerOrderService.getNewOrders(1, 5, "createdAt", "asc");
        var history = partnerOrderService.getHistory(11L, 1, 5, null, null);

        assertThat(newOrders.content()).hasSize(1);
        assertThat(history.content()).hasSize(1);
        assertThat(history.content().get(0).id()).isEqualTo(7L);
    }

    @Test
    void newOrdersFallsBackToSafeSortFieldForUnknownInput() {
        PartnerOrder order = order(7L, OrderStatus.CREATED, null);
        when(partnerOrderRepository.findByStatusInAndAssignedPartnerIdIsNull(any(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(order)));

        partnerOrderService.getNewOrders(0, 20, "drop table orders", "asc");

        org.mockito.ArgumentCaptor<Pageable> pageableCaptor = org.mockito.ArgumentCaptor.forClass(Pageable.class);
        verify(partnerOrderRepository).findByStatusInAndAssignedPartnerIdIsNull(any(), pageableCaptor.capture());
        Sort.Order sortOrder = pageableCaptor.getValue().getSort().getOrderFor("createdAt");
        assertThat(sortOrder).isNotNull();
        assertThat(sortOrder.getDirection()).isEqualTo(Sort.Direction.ASC);
    }

    @Test
    void getOrderAndTimelineRejectForeignPartnerAndMissingOrder() {
        PartnerOrder order = order(3L, OrderStatus.ASSIGNED, 99L);
        when(partnerOrderRepository.findById(3L)).thenReturn(Optional.of(order));
        when(partnerOrderRepository.findById(4L)).thenReturn(Optional.empty());

        assertThrows(BadRequestException.class, () -> partnerOrderService.getOrder(11L, 3L));
        assertThrows(BadRequestException.class, () -> partnerOrderService.timeline(11L, 3L));
        assertThrows(ResourceNotFoundException.class, () -> partnerOrderService.getOrder(11L, 4L));
    }

    @Test
    void timelineMapsHistoryRows() {
        PartnerOrder order = order(5L, OrderStatus.ACCEPTED, 11L);
        OrderStatusHistory history = new OrderStatusHistory();
        history.setId(1L);
        history.setFromStatus(OrderStatus.CREATED);
        history.setToStatus(OrderStatus.ACCEPTED);
        history.setRemarks("accepted");
        history.setActorType("PARTNER");
        history.setActorId(11L);
        history.setCreatedAt(LocalDateTime.now());
        when(partnerOrderRepository.findById(5L)).thenReturn(Optional.of(order));
        when(orderStatusHistoryRepository.findByOrderIdOrderByCreatedAtAsc(5L)).thenReturn(List.of(history));

        var timeline = partnerOrderService.timeline(11L, 5L);

        assertThat(timeline).hasSize(1);
        assertThat(timeline.get(0).fromStatus()).isEqualTo(OrderStatus.CREATED);
        assertThat(timeline.get(0).toStatus()).isEqualTo(OrderStatus.ACCEPTED);
    }

    @Test
    void startAndCompleteOrderStoreImagesAndTriggerCompletionSideEffects() {
        PartnerOrder accepted = order(8L, OrderStatus.ACCEPTED, 10L);
        PartnerOrder started = order(9L, OrderStatus.STARTED, 10L);
        when(partnerOrderRepository.findById(8L)).thenReturn(Optional.of(accepted));
        when(partnerOrderRepository.findById(9L)).thenReturn(Optional.of(started));
        when(partnerOrderRepository.save(any(PartnerOrder.class))).thenAnswer(inv -> inv.getArgument(0));
        when(fileStorageUtil.storeImage(any(), eq("order-before"))).thenReturn("before-url");
        when(fileStorageUtil.storeImage(any(), eq("order-after"))).thenReturn("after-url");

        var startedDto = partnerOrderService.startOrder(10L, 8L, new MockMultipartFile("file", new byte[]{1}));
        var completedDto = partnerOrderService.completeOrder(10L, 9L, new MockMultipartFile("file", new byte[]{2}));

        assertThat(startedDto.status()).isEqualTo(OrderStatus.STARTED);
        assertThat(started.getAfterImageUrls()).isEqualTo("after-url");
        assertThat(completedDto.status()).isEqualTo(OrderStatus.COMPLETED);
        verify(partnerSettlementService).ensurePayout(9L);
        verify(businessMetricsService).incrementOrdersCompleted();
    }

    @Test
    void startAndCompleteOrderAllowMissingImages() {
        PartnerOrder accepted = order(18L, OrderStatus.ACCEPTED, 10L);
        PartnerOrder started = order(19L, OrderStatus.STARTED, 10L);
        when(partnerOrderRepository.findById(18L)).thenReturn(Optional.of(accepted));
        when(partnerOrderRepository.findById(19L)).thenReturn(Optional.of(started));
        when(partnerOrderRepository.save(any(PartnerOrder.class))).thenAnswer(inv -> inv.getArgument(0));

        var startedDto = partnerOrderService.startOrder(10L, 18L, null);
        var completedDto = partnerOrderService.completeOrder(10L, 19L, null);

        assertThat(startedDto.status()).isEqualTo(OrderStatus.STARTED);
        assertThat(completedDto.status()).isEqualTo(OrderStatus.COMPLETED);
        assertThat(accepted.getBeforeImageUrls()).isNull();
        assertThat(started.getAfterImageUrls()).isNull();
        verify(fileStorageUtil, never()).storeImage(any(), any());
        verify(partnerSettlementService).ensurePayout(19L);
        verify(businessMetricsService).incrementOrdersCompleted();
    }

    @Test
    void dashboardCalculatesPendingOrdersDeliveriesAndOnlineHours() {
        PartnerShiftEntity shift = new PartnerShiftEntity();
        shift.setPartnerId(10L);
        shift.setOnline(true);
        shift.setShiftStart(LocalDateTime.now().minusHours(2));
        shift.setShiftEnd(LocalDateTime.now().plusHours(1));
        when(partnerShiftRepository.findByPartnerId(10L)).thenReturn(Optional.of(shift));
        when(partnerSettlementService.earningsSummary(10L)).thenReturn(new PartnerEarningsSummaryDto(
                120.0, 600.0, 0.0, 0.0, 0.0, 0.0, 0L, 0L, 0L, 0L, 0.0));
        when(partnerOrderRepository.countByAssignedPartnerIdAndStatus(10L, OrderStatus.COMPLETED)).thenReturn(4L);
        when(partnerOrderRepository.countByAssignedPartnerIdAndStatus(10L, OrderStatus.ACCEPTED)).thenReturn(2L);
        when(partnerOrderRepository.countByAssignedPartnerIdAndStatus(10L, OrderStatus.STARTED)).thenReturn(1L);

        var dashboard = partnerOrderService.dashboard(10L);

        assertThat(dashboard.totalCompletedOrders()).isEqualTo(4L);
        assertThat(dashboard.pendingOrders()).isEqualTo(3L);
        assertThat(dashboard.activeDeliveries()).isEqualTo(1L);
        assertThat(dashboard.todayEarnings()).isEqualTo(120.0);
        assertThat(dashboard.totalOnlineHours()).isGreaterThan(0.0);
    }

    @Test
    void dashboardReturnsZeroHoursForMissingOrExpiredShift() {
        when(partnerSettlementService.earningsSummary(10L)).thenReturn(new PartnerEarningsSummaryDto(
                0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0L, 0L, 0L, 0L, 0.0));
        when(partnerOrderRepository.countByAssignedPartnerIdAndStatus(any(), any())).thenReturn(0L);
        when(partnerShiftRepository.findByPartnerId(10L)).thenReturn(Optional.empty());

        assertThat(partnerOrderService.dashboard(10L).totalOnlineHours()).isZero();

        PartnerShiftEntity stale = new PartnerShiftEntity();
        stale.setShiftStart(LocalDateTime.now().minusDays(1).minusHours(3));
        stale.setShiftEnd(LocalDateTime.now().minusDays(1).minusHours(1));
        when(partnerShiftRepository.findByPartnerId(10L)).thenReturn(Optional.of(stale));
        assertThat(partnerOrderService.dashboard(10L).totalOnlineHours()).isZero();
    }

    @Test
    void earningsSummaryAndTransactionsDelegateToSettlementService() {
        PartnerEarningsSummaryDto summary = new PartnerEarningsSummaryDto(1.0, 2.0, 0.0, 0.0, 0.0, 0.0, 0L, 0L, 0L, 0L, 0.0);
        when(partnerSettlementService.earningsSummary(10L)).thenReturn(summary);
        when(partnerSettlementService.earningTransactions(eq(10L), any(), any(), eq(1), eq(10)))
                .thenReturn(null);

        assertThat(partnerOrderService.earningsSummary(10L)).isSameAs(summary);
        assertThat(partnerOrderService.earningTransactions(10L, null, null, 1, 10, null, "desc")).isNull();
    }

    @Test
    void ownedOrderChecksAssignedPartner() {
        PartnerOrder unassigned = order(9L, OrderStatus.ACCEPTED, null);
        when(partnerOrderRepository.findById(9L)).thenReturn(Optional.of(unassigned));

        assertThrows(BadRequestException.class, () -> partnerOrderService.startOrder(10L, 9L, null));
        verify(fileStorageUtil, never()).storeImage(any(), any());
    }

    private PartnerOrder order(Long id, OrderStatus status, Long assignedPartnerId) {
        PartnerOrder order = new PartnerOrder();
        order.setId(id);
        order.setStatus(status);
        order.setAssignedPartnerId(assignedPartnerId);
        order.setCustomerName("Customer");
        order.setCustomerPhone("9999999999");
        order.setAddress("Address");
        order.setLatitude(12.3);
        order.setLongitude(45.6);
        order.setServiceType("Cleaning");
        order.setAmount(250.0);
        order.setCreatedAt(LocalDateTime.now());
        return order;
    }
}

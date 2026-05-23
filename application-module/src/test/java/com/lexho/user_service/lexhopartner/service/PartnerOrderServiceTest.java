package com.lexho.user_service.lexhopartner.service;

import com.lexho.user_service.exception.BadRequestException;
import com.lexho.user_service.lexhopartner.entity.OrderStatus;
import com.lexho.user_service.lexhopartner.entity.PartnerOrder;
import com.lexho.user_service.lexhopartner.repository.OrderStatusHistoryRepository;
import com.lexho.user_service.lexhopartner.repository.PartnerOrderRepository;
import com.lexho.user_service.lexhopartner.util.AuditLogUtil;
import com.lexho.user_service.lexhopartner.util.FileStorageUtil;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
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

    @InjectMocks
    private PartnerOrderService partnerOrderService;

    @Test
    void acceptOrderShouldUpdateStatus() {
        PartnerOrder order = new PartnerOrder();
        order.setId(1L);
        order.setStatus(OrderStatus.CREATED);
        when(partnerOrderRepository.findById(1L)).thenReturn(Optional.of(order));
        when(partnerOrderRepository.save(any(PartnerOrder.class))).thenAnswer(inv -> inv.getArgument(0));

        var response = partnerOrderService.acceptOrder(11L, 1L);

        assertEquals(OrderStatus.ACCEPTED, response.status());
        assertEquals(11L, response.assignedPartnerId());
    }

    @Test
    void startOrderShouldFailForInvalidState() {
        PartnerOrder order = new PartnerOrder();
        order.setId(1L);
        order.setAssignedPartnerId(10L);
        order.setStatus(OrderStatus.CREATED);
        when(partnerOrderRepository.findById(1L)).thenReturn(Optional.of(order));

        assertThrows(BadRequestException.class, () -> partnerOrderService.startOrder(10L, 1L, null));
    }
}

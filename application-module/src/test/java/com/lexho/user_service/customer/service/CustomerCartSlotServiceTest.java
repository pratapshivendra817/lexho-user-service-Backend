package com.lexho.user_service.customer.service;

import com.lexho.booking.repository.OrderRepository;
import com.lexho.user_service.dispatch.repository.PartnerShiftRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CustomerCartSlotServiceTest {

    @Mock
    private OrderRepository orderRepository;
    @Mock
    private PartnerShiftRepository partnerShiftRepository;
    @Mock
    private SlotDateTimePolicy slotDateTimePolicy;

    @InjectMocks
    private CustomerCartSlotService customerCartSlotService;

    @Test
    void shouldKeepDateFidelityForDifferentDays() {
        LocalDate d1 = LocalDate.of(2026, 5, 19);
        LocalDate d2 = LocalDate.of(2026, 5, 20);

        when(slotDateTimePolicy.today()).thenReturn(LocalDate.of(2026, 5, 18));
        when(slotDateTimePolicy.now()).thenReturn(LocalDateTime.of(2026, 5, 18, 8, 0));
        when(slotDateTimePolicy.zoneId()).thenReturn(java.time.ZoneId.of("Asia/Kolkata"));
        when(slotDateTimePolicy.isExpired(any(LocalDateTime.class))).thenReturn(false);
        when(orderRepository.countByScheduledAtGreaterThanEqualAndScheduledAtLessThanAndStatusIn(any(), any(), anySet())).thenReturn(0L);
        when(partnerShiftRepository.existsByOnlineTrue()).thenReturn(true);

        Map<String, Object> result1 = customerCartSlotService.slotsForDate(d1);
        Map<String, Object> result2 = customerCartSlotService.slotsForDate(d2);

        assertEquals("2026-05-19", result1.get("date"));
        assertEquals("2026-05-20", result2.get("date"));

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> slots1 = (List<Map<String, Object>>) result1.get("slots");
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> slots2 = (List<Map<String, Object>>) result2.get("slots");
        assertFalse(slots1.isEmpty());
        assertEquals(slots1.get(0).keySet(), slots2.get(0).keySet());

        ArgumentCaptor<LocalDateTime> fromCaptor = ArgumentCaptor.forClass(LocalDateTime.class);
        verify(orderRepository, atLeast(12)).countByScheduledAtGreaterThanEqualAndScheduledAtLessThanAndStatusIn(
                fromCaptor.capture(), any(LocalDateTime.class), anySet());
        assertTrue(fromCaptor.getAllValues().stream().anyMatch(v -> v.toLocalDate().equals(d1)));
        assertTrue(fromCaptor.getAllValues().stream().anyMatch(v -> v.toLocalDate().equals(d2)));
    }
}

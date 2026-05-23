package com.lexho.user_service.customer.service;

import com.lexho.booking.repository.OrderRepository;
import com.lexho.common.enums.OrderStatus;
import com.lexho.user_service.dispatch.repository.PartnerShiftRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class CustomerCartSlotService {

    private static final int SLOT_CAPACITY = 20;
    private static final Set<OrderStatus> ACTIVE_STATUSES = Set.of(
            OrderStatus.CREATED,
            OrderStatus.CONFIRMED,
            OrderStatus.PREPARING,
            OrderStatus.OUT_FOR_DELIVERY
    );

    private static final LocalTime SLOT_DAY_START = LocalTime.of(6, 0);
    private static final LocalTime SLOT_DAY_END = LocalTime.of(23, 0);
    private static final int SLOT_MINUTES = 15;

    private final OrderRepository orderRepository;
    private final PartnerShiftRepository partnerShiftRepository;
    private final SlotDateTimePolicy slotDateTimePolicy;

    public Map<String, Object> slotsForDate(LocalDate date) {
        LocalDate today = slotDateTimePolicy.today();
        if (date.isBefore(today)) {
            throw new IllegalArgumentException("Date cannot be in the past");
        }

        List<Map<String, Object>> slots = new ArrayList<>();
        LocalDateTime now = slotDateTimePolicy.now();

        for (SlotWindow window : slotWindows()) {
            LocalDateTime from = LocalDateTime.of(date, window.from());
            LocalDateTime to = LocalDateTime.of(date, window.to());

            long booked = orderRepository.countByScheduledAtGreaterThanEqualAndScheduledAtLessThanAndStatusIn(
                    from, to, ACTIVE_STATUSES
            );

            int remaining = Math.max(SLOT_CAPACITY - (int) booked, 0);
            boolean futureSlot = !slotDateTimePolicy.isExpired(from);
            boolean available = futureSlot && remaining > 0;

            slots.add(Map.of(
                    "slot", window.label(),
                    "label", window.label(),
                    "startTime", window.from().toString(),
                    "endTime", window.to().toString(),
                    "isInstantEligible", isInstantEligible(date, from),
                    "etaMinutes", estimateInstantEtaMinutes(),
                    "capacity", SLOT_CAPACITY,
                    "booked", booked,
                    "remaining", remaining,
                    "available", available
            ));
        }

        return Map.of(
                "date", date.toString(),
                "timezone", slotDateTimePolicy.zoneId().getId(),
                "isEstimated", true,
                "slots", slots
        );
    }

    public Map<String, Object> slotRowForDate(LocalDate date, String slotLabel) {
        if (slotLabel == null || slotLabel.isBlank()) {
            return null;
        }
        Object rows = slotsForDate(date).get("slots");
        if (!(rows instanceof List<?> list)) {
            return null;
        }
        for (Object row : list) {
            if (!(row instanceof Map<?, ?> map)) {
                continue;
            }
            Object slot = map.get("slot");
            if (slot != null && slotLabel.equals(String.valueOf(slot))) {
                @SuppressWarnings("unchecked")
                Map<String, Object> typed = (Map<String, Object>) map;
                return typed;
            }
        }
        return null;
    }

    private record SlotWindow(String label, LocalTime from, LocalTime to) {
    }

    private List<SlotWindow> slotWindows() {
        List<SlotWindow> windows = new ArrayList<>();
        LocalTime from = SLOT_DAY_START;
        while (from.isBefore(SLOT_DAY_END)) {
            LocalTime to = from.plusMinutes(SLOT_MINUTES);
            if (to.isAfter(SLOT_DAY_END)) {
                break;
            }
            windows.add(new SlotWindow(formatSlot(from, to), from, to));
            from = to;
        }
        return windows;
    }

    private String formatSlot(LocalTime from, LocalTime to) {
        return from + "-" + to;
    }

    private boolean isInstantEligible(LocalDate date, LocalDateTime slotStart) {
        if (!date.equals(slotDateTimePolicy.today())) {
            return false;
        }
        if (slotDateTimePolicy.isExpired(slotStart)) {
            return false;
        }
        return partnerShiftRepository.existsByOnlineTrue();
    }

    private int estimateInstantEtaMinutes() {
        return partnerShiftRepository.existsByOnlineTrue() ? 20 : 999;
    }
}

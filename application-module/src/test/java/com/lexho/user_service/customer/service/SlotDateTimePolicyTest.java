package com.lexho.user_service.customer.service;

import com.lexho.user_service.booking.exception.BookingValidationException;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;

import static org.junit.jupiter.api.Assertions.*;

class SlotDateTimePolicyTest {

    private final SlotDateTimePolicy policy = new SlotDateTimePolicy();

    @Test
    void shouldParseSlotStartForGivenDate() {
        LocalDateTime value = policy.parseSlotStart(LocalDate.of(2026, 5, 19), "09:00-09:15");
        assertEquals(LocalDateTime.of(2026, 5, 19, 9, 0), value);
    }

    @Test
    void shouldParseLastValidSlotOfDay() {
        LocalDateTime value = policy.parseSlotStart(LocalDate.of(2026, 5, 19), "22:45-23:00");
        assertEquals(LocalDateTime.of(2026, 5, 19, 22, 45), value);
    }

    @Test
    void shouldRejectInvalidSlotFormat() {
        assertThrows(BookingValidationException.class, () -> policy.parseSlotStart(LocalDate.of(2026, 5, 19), "11:00"));
    }

    @Test
    void shouldRejectNon15MinuteSlotRange() {
        assertThrows(BookingValidationException.class, () -> policy.parseSlotStart(LocalDate.of(2026, 5, 19), "09:00-10:00"));
    }

    @Test
    void shouldUseIstTimezone() {
        assertEquals(ZoneId.of("Asia/Kolkata"), policy.zoneId());
    }
}

package com.lexho.user_service.customer.service;

import com.lexho.user_service.booking.exception.BookingValidationException;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

@Component
public class SlotDateTimePolicy {

    private static final ZoneId IST_ZONE = ZoneId.of("Asia/Kolkata");
    private static final DateTimeFormatter SLOT_TIME_FORMATTER = DateTimeFormatter.ofPattern("H:mm");
    private static final LocalTime DAY_START = LocalTime.of(6, 0);
    private static final LocalTime DAY_END = LocalTime.of(23, 0);
    private static final int SLOT_MINUTES = 15;

    public ZoneId zoneId() {
        return IST_ZONE;
    }

    public LocalDateTime now() {
        return LocalDateTime.now(IST_ZONE);
    }

    public LocalDate today() {
        return LocalDate.now(IST_ZONE);
    }

    public boolean isExpired(LocalDateTime slotStart) {
        return !slotStart.isAfter(now());
    }

    public LocalDateTime parseSlotStart(LocalDate bookingDate, String slot) {
        String[] parts = slot.trim().split("-");
        if (parts.length != 2) {
            throw new BookingValidationException("INVALID_SLOT_FORMAT", "Invalid slot format. Use HH:mm-HH:mm");
        }

        LocalTime startTime;
        LocalTime endTime;
        try {
            startTime = LocalTime.parse(parts[0].trim(), SLOT_TIME_FORMATTER);
            endTime = LocalTime.parse(parts[1].trim(), SLOT_TIME_FORMATTER);
        } catch (Exception ex) {
            throw new BookingValidationException("INVALID_SLOT_FORMAT", "Invalid slot format. Use HH:mm-HH:mm");
        }

        if (!endTime.isAfter(startTime)) {
            throw new BookingValidationException("INVALID_SLOT_FORMAT", "Slot end time must be after start time");
        }
        if (!isAllowedSlotRange(startTime, endTime)) {
            throw new BookingValidationException("INVALID_SLOT_RANGE", "Slot must be a 15-minute window between 06:00 and 23:00");
        }
        return LocalDateTime.of(bookingDate, startTime);
    }

    private boolean isAllowedSlotRange(LocalTime startTime, LocalTime endTime) {
        if (startTime.isBefore(DAY_START) || endTime.isAfter(DAY_END)) {
            return false;
        }
        if (startTime.getMinute() % SLOT_MINUTES != 0 || endTime.getMinute() % SLOT_MINUTES != 0) {
            return false;
        }
        return java.time.Duration.between(startTime, endTime).toMinutes() == SLOT_MINUTES;
    }
}

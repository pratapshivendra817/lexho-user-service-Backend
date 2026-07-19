package com.lexho.user_service.subscription.dto;

import java.time.LocalDate;

public record SubscriptionCalendarItemResponse(
        Long visitId,
        LocalDate visitDate,
        String timeSlot,
        String status
) {
}

package com.lexho.user_service.lexhopartner.dto;

import com.lexho.user_service.lexhopartner.entity.OrderStatus;
import com.lexho.user_service.lexhopartner.entity.PaymentMode;
import lombok.Builder;

import java.time.LocalDateTime;

@Builder
public record OrderResponseDto(
        Long id,
        String customerName,
        String customerPhone,
        String address,
        Double latitude,
        Double longitude,
        String serviceType,
        String bookingType,
        LocalDateTime scheduledAt,
        Double amount,
        PaymentMode paymentMode,
        OrderStatus status,
        String jobStatus,
        Long assignedPartnerId,
        LocalDateTime acceptedAt,
        LocalDateTime arrivedAt,
        LocalDateTime startedAt,
        LocalDateTime inProgressAt,
        LocalDateTime completedAt,
        LocalDateTime rejectedAt,
        LocalDateTime serviceStartPinExpiresAt,
        LocalDateTime serviceCompletePinExpiresAt,
        boolean startPinRequired,
        boolean completePinRequired,
        boolean completionRequested,
        boolean reviewEligible,
        LocalDateTime createdAt
) {
}

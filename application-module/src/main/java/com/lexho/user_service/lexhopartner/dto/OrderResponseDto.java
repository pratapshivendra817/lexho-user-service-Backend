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
        Double amount,
        PaymentMode paymentMode,
        OrderStatus status,
        Long assignedPartnerId,
        LocalDateTime createdAt
) {
}

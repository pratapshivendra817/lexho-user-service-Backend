package com.lexho.user_service.lexhopartner.dto;

import com.lexho.user_service.lexhopartner.entity.OrderStatus;

import java.time.LocalDateTime;

public record OrderTimelineDto(
        Long id,
        Long orderId,
        OrderStatus fromStatus,
        OrderStatus toStatus,
        String remarks,
        String actorType,
        Long actorId,
        LocalDateTime timestamp
) {
}

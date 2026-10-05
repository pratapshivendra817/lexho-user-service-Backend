package com.lexho.user_service.accountdeletion.dto;

import java.time.LocalDateTime;

public record AccountDeletionResponse(
        Long id,
        Long userId,
        String userType,
        String status,
        String requestReason,
        String rejectionReason,
        long activeBookings,
        long pendingPayments,
        long pendingRefunds,
        long pendingPartnerPayouts,
        LocalDateTime requestedAt,
        LocalDateTime reviewedAt,
        LocalDateTime processedAt) {
}

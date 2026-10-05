package com.lexho.user_service.referral.dto;

import com.lexho.user_service.referral.enums.ReferralStatus;

import java.time.LocalDateTime;

public record ReferralResponse(
        Long id,
        Long referrerUserId,
        Long referredUserId,
        String referralCode,
        ReferralStatus status,
        LocalDateTime createdAt,
        LocalDateTime qualifiedAt,
        LocalDateTime rewardedAt,
        Long qualifyingBookingId
) {
}

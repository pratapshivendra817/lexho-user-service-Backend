package com.lexho.user_service.lexhopartner.dto;

public record PartnerEarningsSummaryDto(
        Double todayEarnings,
        Double weeklyEarnings,
        Double monthlyEarnings,
        Double lifetimeEarnings,
        Long completedOrdersToday,
        Long completedOrdersWeek,
        Long completedOrdersMonth,
        Long completedOrdersLifetime,
        Double averageOrderValue
) {
}


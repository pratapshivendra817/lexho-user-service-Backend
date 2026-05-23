package com.lexho.user_service.lexhopartner.dto;

public record DashboardStatsDto(
        Double todayEarnings,
        Long totalCompletedOrders,
        Long pendingOrders,
        Double totalOnlineHours,
        Long activeDeliveries,
        Double weeklyEarnings
) {
}

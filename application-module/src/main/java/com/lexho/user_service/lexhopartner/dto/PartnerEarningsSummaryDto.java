package com.lexho.user_service.lexhopartner.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record PartnerEarningsSummaryDto(
        Double todayEarnings,
        Double weeklyEarnings,
        Double monthlyEarnings,
        Double availableBalance,
        Double pendingSettlement,
        Double lifetimeEarnings,
        Long completedOrdersToday,
        Long completedOrdersWeek,
        Long completedOrdersMonth,
        Long completedOrdersLifetime,
        Double averageOrderValue
) {
    @JsonProperty("todaySettlement")
    public Double todaySettlement() {
        return todayEarnings;
    }

    @JsonProperty("today")
    public Double today() {
        return todayEarnings;
    }

    @JsonProperty("weekSettlement")
    public Double weekSettlement() {
        return weeklyEarnings;
    }

    @JsonProperty("weeklySettlement")
    public Double weeklySettlement() {
        return weeklyEarnings;
    }

    @JsonProperty("weekEarnings")
    public Double weekEarnings() {
        return weeklyEarnings;
    }

    @JsonProperty("week")
    public Double week() {
        return weeklyEarnings;
    }

    @JsonProperty("monthSettlement")
    public Double monthSettlement() {
        return monthlyEarnings;
    }

    @JsonProperty("monthlySettlement")
    public Double monthlySettlement() {
        return monthlyEarnings;
    }

    @JsonProperty("monthEarnings")
    public Double monthEarnings() {
        return monthlyEarnings;
    }

    @JsonProperty("month")
    public Double month() {
        return monthlyEarnings;
    }

    @JsonProperty("lifetimeSettlement")
    public Double lifetimeSettlement() {
        return lifetimeEarnings;
    }

    @JsonProperty("totalSettlement")
    public Double totalSettlement() {
        return lifetimeEarnings;
    }

    @JsonProperty("lifetime")
    public Double lifetime() {
        return lifetimeEarnings;
    }

    @JsonProperty("withdrawableBalance")
    public Double withdrawableBalance() {
        return availableBalance;
    }

    @JsonProperty("pendingBalance")
    public Double pendingBalance() {
        return pendingSettlement;
    }

    @JsonProperty("reservedBalance")
    public Double reservedBalance() {
        return pendingSettlement;
    }
}

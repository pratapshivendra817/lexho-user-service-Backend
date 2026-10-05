package com.lexho.user_service.lexhopartner.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDateTime;

public record PartnerEarningTransactionDto(
        Long id,
        Long bookingId,
        String description,
        Double amount,
        Double netAmount,
        Double settlementAmount,
        String type,
        String status,
        LocalDateTime createdAt,
        String customerName,
        String serviceType
) {
    @JsonProperty("transactionId")
    public Long transactionId() {
        return id;
    }

    @JsonProperty("ledgerId")
    public Long ledgerId() {
        return id;
    }

    @JsonProperty("narration")
    public String narration() {
        return description;
    }

    @JsonProperty("transactionDate")
    public LocalDateTime transactionDate() {
        return createdAt;
    }

    @JsonProperty("settlementStatus")
    public String settlementStatus() {
        return status;
    }

    @JsonProperty("completedAt")
    public LocalDateTime completedAt() {
        return createdAt;
    }
}

package com.lexho.user_service.lexhopartner.entity;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.util.Locale;

@Converter(autoApply = false)
public class PartnerOrderStatusConverter implements AttributeConverter<OrderStatus, String> {

    @Override
    public String convertToDatabaseColumn(OrderStatus attribute) {
        if (attribute == null) {
            return null;
        }
        return switch (attribute) {
            case ASSIGNED -> "PARTNER_ASSIGNED";
            case ACCEPTED -> "PARTNER_ACCEPTED";
            case ARRIVED -> "ARRIVED";
            case STARTED -> "SERVICE_STARTED";
            case IN_PROGRESS -> "SERVICE_IN_PROGRESS";
            case COMPLETED -> "SERVICE_COMPLETED";
            case CANCELLED -> "CANCELLED";
            case CREATED -> "CREATED";
            case REJECTED -> "CANCELLED";
        };
    }

    @Override
    public OrderStatus convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.isBlank()) {
            return null;
        }
        return switch (dbData.trim().toUpperCase(Locale.ROOT)) {
            case "PARTNER_ASSIGNED" -> OrderStatus.ASSIGNED;
            case "PARTNER_ACCEPTED" -> OrderStatus.ACCEPTED;
            case "ARRIVED", "PARTNER_REACHED" -> OrderStatus.ARRIVED;
            case "SERVICE_STARTED", "STARTED" -> OrderStatus.STARTED;
            case "SERVICE_IN_PROGRESS", "IN_PROGRESS" -> OrderStatus.IN_PROGRESS;
            case "SERVICE_COMPLETED", "PAYMENT_COMPLETED", "DELIVERED", "COMPLETED" -> OrderStatus.COMPLETED;
            case "CANCELLED", "INSTANT_FAILED" -> OrderStatus.CANCELLED;
            case "REJECTED" -> OrderStatus.REJECTED;
            case "CREATED", "BOOKING_CREATED", "CONFIRMED", "SCHEDULED", "SEARCHING_PARTNER", "PARTNER_SEARCH" ->
                    OrderStatus.CREATED;
            default -> OrderStatus.valueOf(dbData.trim().toUpperCase(Locale.ROOT));
        };
    }
}

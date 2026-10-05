package com.lexho.booking.entity;

import com.lexho.common.enums.OrderStatus;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = false)
public class OrderStatusConverter implements AttributeConverter<OrderStatus, String> {

    private static final String LEGACY_ACCEPTED = "ACCEPTED";
    private static final String LEGACY_STARTED = "STARTED";
    private static final String LEGACY_IN_PROGRESS = "IN_PROGRESS";
    private static final String LEGACY_COMPLETED = "COMPLETED";

    @Override
    public String convertToDatabaseColumn(OrderStatus attribute) {
        return attribute == null ? null : attribute.name();
    }

    @Override
    public OrderStatus convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.isBlank()) {
            return null;
        }
        String normalized = dbData.trim();
        if (LEGACY_ACCEPTED.equalsIgnoreCase(dbData)) {
            return OrderStatus.PARTNER_ACCEPTED;
        }
        if (LEGACY_STARTED.equalsIgnoreCase(dbData)) {
            return OrderStatus.SERVICE_STARTED;
        }
        if (LEGACY_IN_PROGRESS.equalsIgnoreCase(dbData)) {
            return OrderStatus.SERVICE_IN_PROGRESS;
        }
        if (LEGACY_COMPLETED.equalsIgnoreCase(dbData)) {
            return OrderStatus.SERVICE_COMPLETED;
        }
        try {
            return OrderStatus.valueOf(normalized.toUpperCase());
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }
}

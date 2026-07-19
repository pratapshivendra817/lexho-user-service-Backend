package com.lexho.booking.entity;

import com.lexho.common.enums.OrderStatus;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = false)
public class OrderStatusConverter implements AttributeConverter<OrderStatus, String> {

    private static final String LEGACY_ACCEPTED = "ACCEPTED";

    @Override
    public String convertToDatabaseColumn(OrderStatus attribute) {
        return attribute == null ? null : attribute.name();
    }

    @Override
    public OrderStatus convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.isBlank()) {
            return null;
        }
        if (LEGACY_ACCEPTED.equalsIgnoreCase(dbData)) {
            return OrderStatus.PARTNER_ACCEPTED;
        }
        return OrderStatus.valueOf(dbData);
    }
}

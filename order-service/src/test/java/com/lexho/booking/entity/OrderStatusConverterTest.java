package com.lexho.booking.entity;

import com.lexho.common.enums.OrderStatus;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class OrderStatusConverterTest {

    private final OrderStatusConverter converter = new OrderStatusConverter();

    @Test
    void shouldMapLegacyAcceptedToPartnerAccepted() {
        assertEquals(OrderStatus.PARTNER_ACCEPTED, converter.convertToEntityAttribute("ACCEPTED"));
        assertEquals(OrderStatus.SERVICE_STARTED, converter.convertToEntityAttribute("STARTED"));
        assertEquals(OrderStatus.SERVICE_IN_PROGRESS, converter.convertToEntityAttribute("IN_PROGRESS"));
        assertEquals(OrderStatus.SERVICE_COMPLETED, converter.convertToEntityAttribute("COMPLETED"));
    }

    @Test
    void shouldKeepCurrentOrderStatusValuesUntouched() {
        assertEquals(OrderStatus.SERVICE_COMPLETED, converter.convertToEntityAttribute("SERVICE_COMPLETED"));
        assertEquals("PARTNER_ACCEPTED", converter.convertToDatabaseColumn(OrderStatus.PARTNER_ACCEPTED));
    }

    @Test
    void shouldReturnNullForBlankOrNullDatabaseValue() {
        assertNull(converter.convertToEntityAttribute(null));
        assertNull(converter.convertToEntityAttribute(" "));
        assertNull(converter.convertToDatabaseColumn(null));
    }
}

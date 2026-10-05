package com.lexho.user_service.lexhopartner.entity;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class PartnerOrderStatusConverterTest {

    private final PartnerOrderStatusConverter converter = new PartnerOrderStatusConverter();

    @Test
    void shouldMapCanonicalOrderStatusesToPartnerStatuses() {
        assertEquals(OrderStatus.ASSIGNED, converter.convertToEntityAttribute("PARTNER_ASSIGNED"));
        assertEquals(OrderStatus.ACCEPTED, converter.convertToEntityAttribute("PARTNER_ACCEPTED"));
        assertEquals(OrderStatus.IN_PROGRESS, converter.convertToEntityAttribute("SERVICE_IN_PROGRESS"));
        assertEquals(OrderStatus.COMPLETED, converter.convertToEntityAttribute("SERVICE_COMPLETED"));
        assertEquals(OrderStatus.COMPLETED, converter.convertToEntityAttribute("PAYMENT_COMPLETED"));
    }

    @Test
    void shouldKeepPartnerStatusValuesUntouched() {
        assertEquals(OrderStatus.ACCEPTED, converter.convertToEntityAttribute("ACCEPTED"));
        assertEquals(OrderStatus.IN_PROGRESS, converter.convertToEntityAttribute("IN_PROGRESS"));
        assertEquals("SERVICE_COMPLETED", converter.convertToDatabaseColumn(OrderStatus.COMPLETED));
    }

    @Test
    void shouldReturnNullForBlankOrNullDatabaseValue() {
        assertNull(converter.convertToEntityAttribute(null));
        assertNull(converter.convertToEntityAttribute(" "));
        assertNull(converter.convertToDatabaseColumn(null));
    }
}

package com.lexho.user_service.customer.service;

import com.lexho.user_service.admin.entity.AdminServiceEntity;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class CustomerServiceBookingModeMapperTest {
    private final CustomerServiceBookingModeMapper mapper = new CustomerServiceBookingModeMapper();

    @Test
    void mapsBlankFixedHourlyAndUnknownTypes() {
        assertEquals(List.of("FIXED"), mapper.bookingModes(null));
        assertEquals(List.of("FIXED"), mapper.bookingModes(service("fixed")));
        assertEquals(List.of("FIXED", "HOURLY"), mapper.bookingModes(service("fixed_hourly")));
        assertEquals(List.of("FIXED"), mapper.bookingModes(service("subscription")));
    }

    @Test
    void exposesHourlyValuesOnlyForHourlyMode() {
        AdminServiceEntity service = service("hourly");
        service.setBasePrice(BigDecimal.valueOf(500));
        service.setDiscountedPrice(BigDecimal.valueOf(450));
        List<String> hourly = List.of("HOURLY");
        assertEquals(BigDecimal.valueOf(450), mapper.hourlyRate(service, hourly));
        assertEquals(1, mapper.minHours(hourly));
        assertEquals(8, mapper.maxHours(hourly));
        assertEquals(1, mapper.hourStep(hourly));
        assertNull(mapper.hourlyRate(service, List.of("FIXED")));
        assertNull(mapper.minHours(List.of("FIXED")));
    }

    private AdminServiceEntity service(String type) {
        AdminServiceEntity service = new AdminServiceEntity();
        service.setServiceType(type);
        return service;
    }
}

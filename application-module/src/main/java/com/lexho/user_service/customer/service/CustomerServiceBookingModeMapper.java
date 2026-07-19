package com.lexho.user_service.customer.service;

import com.lexho.user_service.admin.entity.AdminServiceEntity;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Component
public class CustomerServiceBookingModeMapper {

    private static final String FIXED = "FIXED";
    private static final String HOURLY = "HOURLY";

    public List<String> bookingModes(AdminServiceEntity service) {
        List<String> modes = new ArrayList<>();
        String serviceType = service == null ? null : service.getServiceType();
        String normalized = serviceType == null ? "" : serviceType.toUpperCase(Locale.ROOT);

        if (normalized.contains(FIXED) || normalized.isBlank()) {
            modes.add(FIXED);
        }
        if (normalized.contains(HOURLY)) {
            modes.add(HOURLY);
        }
        if (modes.isEmpty()) {
            modes.add(FIXED);
        }
        return modes;
    }

    public BigDecimal hourlyRate(AdminServiceEntity service, List<String> bookingModes) {
        if (!bookingModes.contains(HOURLY) || service == null) {
            return null;
        }
        return service.getDiscountedPrice() != null ? service.getDiscountedPrice() : service.getBasePrice();
    }

    public Integer minHours(List<String> bookingModes) {
        return bookingModes.contains(HOURLY) ? 1 : null;
    }

    public Integer maxHours(List<String> bookingModes) {
        return bookingModes.contains(HOURLY) ? 8 : null;
    }

    public Integer hourStep(List<String> bookingModes) {
        return bookingModes.contains(HOURLY) ? 1 : null;
    }

}

package com.lexho.user_service.subscription.service;

import com.lexho.user_service.dispatch.entity.CurrentPartnerLocationEntity;
import com.lexho.user_service.lexho.entity.LexhoAddress;
import com.lexho.user_service.util.GeoDistanceCalculator;

import java.math.BigDecimal;
import java.math.RoundingMode;

final class SubscriptionDistanceSupport {

    private static final double ETA_SPEED_KMPH = 25.0d;

    private SubscriptionDistanceSupport() {
    }

    static BigDecimal calculateDistanceKm(LexhoAddress address, CurrentPartnerLocationEntity location) {
        double km = GeoDistanceCalculator.haversineKm(
                address.getLatitude().doubleValue(),
                address.getLongitude().doubleValue(),
                location.getLatitude().doubleValue(),
                location.getLongitude().doubleValue()
        );
        return BigDecimal.valueOf(km).setScale(3, RoundingMode.HALF_UP);
    }

    static int estimateEtaMinutes(BigDecimal distanceKm) {
        double hours = distanceKm.doubleValue() / ETA_SPEED_KMPH;
        int minutes = (int) Math.ceil(hours * 60.0d);
        return Math.max(minutes, 1);
    }

}

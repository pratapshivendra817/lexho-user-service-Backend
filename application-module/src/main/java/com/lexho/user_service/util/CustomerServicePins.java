package com.lexho.user_service.util;

import com.lexho.booking.entity.Order;
import com.lexho.common.enums.OrderStatus;
import java.time.LocalDateTime;
import java.util.Map;

/** PINs exposed only through the authenticated customer's booking response. */
public final class CustomerServicePins {
    private CustomerServicePins() {}

    public static void attach(Map<String, Object> payload, Order order) {
        LocalDateTime now = LocalDateTime.now();
        String secret = System.getenv().getOrDefault("SERVICE_PIN_SECRET",
                "lexho-local-service-pin-secret-change-in-production");
        boolean arrived = order.getStatus() == OrderStatus.ARRIVED
                || order.getStatus() == OrderStatus.PARTNER_REACHED;
        boolean started = order.getStatus() == OrderStatus.SERVICE_STARTED
                || order.getStatus() == OrderStatus.SERVICE_IN_PROGRESS;
        boolean startAvailable = arrived && active(order.getServiceStartPinNonce(),
                order.getServiceStartPinExpiresAt(), order.getServiceStartPinConsumedAt(), now);
        boolean endAvailable = started && active(order.getServiceCompletePinNonce(),
                order.getServiceCompletePinExpiresAt(), order.getServiceCompletePinConsumedAt(), now);
        payload.put("serviceStartPin", startAvailable
                ? ServicePinHasher.expectedPin(order.getId(), "START", order.getServiceStartPinNonce(), secret) : null);
        payload.put("serviceStartPinExpiresAt", startAvailable ? order.getServiceStartPinExpiresAt().toString() : null);
        payload.put("serviceCompletePin", endAvailable
                ? ServicePinHasher.expectedPin(order.getId(), "COMPLETE", order.getServiceCompletePinNonce(), secret) : null);
        payload.put("serviceCompletePinExpiresAt", endAvailable ? order.getServiceCompletePinExpiresAt().toString() : null);
    }

    private static boolean active(String nonce, LocalDateTime expiresAt, LocalDateTime consumedAt, LocalDateTime now) {
        return nonce != null && expiresAt != null && consumedAt == null && expiresAt.isAfter(now);
    }
}

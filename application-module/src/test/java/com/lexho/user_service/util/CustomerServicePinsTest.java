package com.lexho.user_service.util;

import com.lexho.booking.entity.Order;
import com.lexho.common.enums.OrderStatus;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class CustomerServicePinsTest {
    @Test
    void exposesOnlyActivePinsForTheCorrectStage() {
        Order order = new Order();
        order.setId(125L);
        order.setServiceStartPinNonce("start-nonce");
        order.setServiceStartPinExpiresAt(LocalDateTime.now().plusMinutes(10));
        order.setServiceCompletePinNonce("end-nonce");
        order.setServiceCompletePinExpiresAt(LocalDateTime.now().plusMinutes(10));
        Map<String, Object> payload = new HashMap<>();
        order.setStatus(OrderStatus.ARRIVED);
        CustomerServicePins.attach(payload, order);
        String secret = System.getenv().getOrDefault("SERVICE_PIN_SECRET", "lexho-local-service-pin-secret-change-in-production");
        assertEquals(ServicePinHasher.expectedPin(125L, "START", "start-nonce", secret), payload.get("serviceStartPin"));
        assertNotNull(payload.get("serviceStartPinExpiresAt"));
        assertNull(payload.get("serviceCompletePin"));
        order.setStatus(OrderStatus.SERVICE_IN_PROGRESS);
        CustomerServicePins.attach(payload, order);
        assertNull(payload.get("serviceStartPin"));
        assertEquals(ServicePinHasher.expectedPin(125L, "COMPLETE", "end-nonce", secret), payload.get("serviceCompletePin"));
        order.setServiceCompletePinConsumedAt(LocalDateTime.now());
        CustomerServicePins.attach(payload, order);
        assertNull(payload.get("serviceCompletePin"));
        order.setStatus(OrderStatus.ARRIVED);
        order.setServiceStartPinExpiresAt(LocalDateTime.now().minusSeconds(1));
        CustomerServicePins.attach(payload, order);
        assertNull(payload.get("serviceStartPin"));
        assertNull(payload.get("serviceStartPinExpiresAt"));
        order.setStatus(OrderStatus.CANCELLED);
        CustomerServicePins.attach(payload, order);
        assertNull(payload.get("serviceStartPin"));
        assertNull(payload.get("serviceCompletePin"));
    }
}

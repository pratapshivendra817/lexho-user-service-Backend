package com.lexho.common.enums;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OrderStatusTest {

    @Test
    void shouldAllowValidTransitions() {
        assertTrue(OrderStatus.CREATED.canTransitionTo(OrderStatus.CONFIRMED));
        assertTrue(OrderStatus.CONFIRMED.canTransitionTo(OrderStatus.PREPARING));
        assertTrue(OrderStatus.OUT_FOR_DELIVERY.canTransitionTo(OrderStatus.DELIVERED));
    }

    @Test
    void shouldRejectInvalidTransitions() {
        assertFalse(OrderStatus.CREATED.canTransitionTo(OrderStatus.DELIVERED));
        assertFalse(OrderStatus.CANCELLED.canTransitionTo(OrderStatus.CONFIRMED));
    }

    @Test
    void shouldReturnCancellationRule() {
        assertTrue(OrderStatus.CREATED.canCancel());
        assertTrue(OrderStatus.CONFIRMED.canCancel());
        assertFalse(OrderStatus.PREPARING.canCancel());
    }
}

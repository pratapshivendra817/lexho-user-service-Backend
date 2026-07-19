package com.lexho.common.enums;

import org.junit.jupiter.api.Test;

import java.util.EnumSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertIterableEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OrderStatusTest {

    @Test
    void valuesShouldExposeAllConstantsInDeclaredOrder() {
        OrderStatus[] values = OrderStatus.values();

        assertEquals(19, values.length);
        assertIterableEquals(
                java.util.List.of(
                        OrderStatus.SEARCHING_PARTNER,
                        OrderStatus.INSTANT_FAILED,
                        OrderStatus.SCHEDULED,
                        OrderStatus.BOOKING_CREATED,
                        OrderStatus.PARTNER_SEARCH,
                        OrderStatus.PARTNER_ASSIGNED,
                        OrderStatus.PARTNER_ACCEPTED,
                        OrderStatus.ARRIVED,
                        OrderStatus.PARTNER_REACHED,
                        OrderStatus.SERVICE_STARTED,
                        OrderStatus.SERVICE_IN_PROGRESS,
                        OrderStatus.SERVICE_COMPLETED,
                        OrderStatus.PAYMENT_COMPLETED,
                        OrderStatus.CREATED,
                        OrderStatus.CONFIRMED,
                        OrderStatus.PREPARING,
                        OrderStatus.OUT_FOR_DELIVERY,
                        OrderStatus.DELIVERED,
                        OrderStatus.CANCELLED
                ),
                java.util.List.of(values)
        );
    }

    @Test
    void valueOfShouldResolveEveryConstantByName() {
        for (OrderStatus status : OrderStatus.values()) {
            assertEquals(status, OrderStatus.valueOf(status.name()));
        }
    }

    @Test
    void valueOfShouldThrowForInvalidName() {
        assertThrows(IllegalArgumentException.class, () -> OrderStatus.valueOf("UNKNOWN"));
        assertThrows(NullPointerException.class, () -> OrderStatus.valueOf(null));
    }

    @Test
    void nameAndOrdinalShouldMatchDeclaredEnumLayout() {
        assertEquals("SEARCHING_PARTNER", OrderStatus.SEARCHING_PARTNER.name());
        assertEquals(0, OrderStatus.SEARCHING_PARTNER.ordinal());
        assertEquals(12, OrderStatus.PAYMENT_COMPLETED.ordinal());
        assertEquals(18, OrderStatus.CANCELLED.ordinal());
    }

    @Test
    void allowedTransitionsShouldMatchBusinessRulesForEveryStatus() {
        assertEquals(EnumSet.of(OrderStatus.PARTNER_ASSIGNED, OrderStatus.INSTANT_FAILED, OrderStatus.CANCELLED),
                EnumSet.copyOf(OrderStatus.SEARCHING_PARTNER.getAllowedTransitions()));
        assertTrue(OrderStatus.INSTANT_FAILED.getAllowedTransitions().isEmpty());
        assertEquals(EnumSet.of(OrderStatus.PARTNER_SEARCH, OrderStatus.PARTNER_ASSIGNED,
                        OrderStatus.PARTNER_ACCEPTED, OrderStatus.CANCELLED),
                EnumSet.copyOf(OrderStatus.SCHEDULED.getAllowedTransitions()));
        assertEquals(EnumSet.of(OrderStatus.PARTNER_SEARCH, OrderStatus.CANCELLED),
                EnumSet.copyOf(OrderStatus.BOOKING_CREATED.getAllowedTransitions()));
        assertEquals(EnumSet.of(OrderStatus.PARTNER_ASSIGNED, OrderStatus.CANCELLED),
                EnumSet.copyOf(OrderStatus.PARTNER_SEARCH.getAllowedTransitions()));
        assertEquals(EnumSet.of(OrderStatus.PARTNER_ACCEPTED, OrderStatus.CANCELLED),
                EnumSet.copyOf(OrderStatus.PARTNER_ASSIGNED.getAllowedTransitions()));
        assertEquals(EnumSet.of(OrderStatus.ARRIVED, OrderStatus.PARTNER_REACHED, OrderStatus.CANCELLED),
                EnumSet.copyOf(OrderStatus.PARTNER_ACCEPTED.getAllowedTransitions()));
        assertEquals(EnumSet.of(OrderStatus.SERVICE_STARTED, OrderStatus.CANCELLED),
                EnumSet.copyOf(OrderStatus.ARRIVED.getAllowedTransitions()));
        assertEquals(EnumSet.of(OrderStatus.SERVICE_STARTED, OrderStatus.CANCELLED),
                EnumSet.copyOf(OrderStatus.PARTNER_REACHED.getAllowedTransitions()));
        assertEquals(EnumSet.of(OrderStatus.SERVICE_IN_PROGRESS, OrderStatus.SERVICE_COMPLETED, OrderStatus.CANCELLED),
                EnumSet.copyOf(OrderStatus.SERVICE_STARTED.getAllowedTransitions()));
        assertEquals(EnumSet.of(OrderStatus.SERVICE_COMPLETED, OrderStatus.CANCELLED),
                EnumSet.copyOf(OrderStatus.SERVICE_IN_PROGRESS.getAllowedTransitions()));
        assertEquals(EnumSet.of(OrderStatus.PAYMENT_COMPLETED),
                EnumSet.copyOf(OrderStatus.SERVICE_COMPLETED.getAllowedTransitions()));
        assertTrue(OrderStatus.PAYMENT_COMPLETED.getAllowedTransitions().isEmpty());
        assertEquals(EnumSet.of(OrderStatus.CONFIRMED, OrderStatus.CANCELLED),
                EnumSet.copyOf(OrderStatus.CREATED.getAllowedTransitions()));
        assertEquals(EnumSet.of(OrderStatus.PREPARING, OrderStatus.CANCELLED),
                EnumSet.copyOf(OrderStatus.CONFIRMED.getAllowedTransitions()));
        assertEquals(EnumSet.of(OrderStatus.OUT_FOR_DELIVERY),
                EnumSet.copyOf(OrderStatus.PREPARING.getAllowedTransitions()));
        assertEquals(EnumSet.of(OrderStatus.DELIVERED),
                EnumSet.copyOf(OrderStatus.OUT_FOR_DELIVERY.getAllowedTransitions()));
        assertTrue(OrderStatus.DELIVERED.getAllowedTransitions().isEmpty());
        assertTrue(OrderStatus.CANCELLED.getAllowedTransitions().isEmpty());
    }

    @Test
    void canTransitionToShouldReturnTrueOnlyForAllowedStatuses() {
        for (OrderStatus status : OrderStatus.values()) {
            Set<OrderStatus> allowedTransitions = status.getAllowedTransitions();
            for (OrderStatus candidate : OrderStatus.values()) {
                if (allowedTransitions.contains(candidate)) {
                    assertTrue(status.canTransitionTo(candidate),
                            () -> status + " should allow transition to " + candidate);
                } else {
                    assertFalse(status.canTransitionTo(candidate),
                            () -> status + " should reject transition to " + candidate);
                }
            }
        }
    }

    @Test
    void canTransitionToShouldThrowForNullNextStatus() {
        assertThrows(NullPointerException.class, () -> OrderStatus.CREATED.canTransitionTo(null));
        assertThrows(NullPointerException.class, () -> OrderStatus.CANCELLED.canTransitionTo(null));
    }

    @Test
    void canCancelShouldBeTrueOnlyForPreServiceAndSearchStates() {
        assertTrue(OrderStatus.CREATED.canCancel());
        assertTrue(OrderStatus.CONFIRMED.canCancel());
        assertTrue(OrderStatus.BOOKING_CREATED.canCancel());
        assertTrue(OrderStatus.SEARCHING_PARTNER.canCancel());
        assertTrue(OrderStatus.SCHEDULED.canCancel());
        assertTrue(OrderStatus.PARTNER_SEARCH.canCancel());
        assertTrue(OrderStatus.PARTNER_ASSIGNED.canCancel());
        assertTrue(OrderStatus.PARTNER_ACCEPTED.canCancel());
        assertTrue(OrderStatus.ARRIVED.canCancel());
        assertTrue(OrderStatus.PARTNER_REACHED.canCancel());

        assertFalse(OrderStatus.INSTANT_FAILED.canCancel());
        assertFalse(OrderStatus.SERVICE_STARTED.canCancel());
        assertFalse(OrderStatus.SERVICE_IN_PROGRESS.canCancel());
        assertFalse(OrderStatus.SERVICE_COMPLETED.canCancel());
        assertFalse(OrderStatus.PAYMENT_COMPLETED.canCancel());
        assertFalse(OrderStatus.PREPARING.canCancel());
        assertFalse(OrderStatus.OUT_FOR_DELIVERY.canCancel());
        assertFalse(OrderStatus.DELIVERED.canCancel());
        assertFalse(OrderStatus.CANCELLED.canCancel());
    }
}

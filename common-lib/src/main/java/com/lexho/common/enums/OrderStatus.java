package com.lexho.common.enums;

import java.util.Set;

public enum OrderStatus {

    SEARCHING_PARTNER,
    INSTANT_FAILED,
    SCHEDULED,
    BOOKING_CREATED,
    PARTNER_SEARCH,
    PARTNER_ASSIGNED,
    PARTNER_ACCEPTED,
    ARRIVED,
    PARTNER_REACHED,
    SERVICE_STARTED,
    SERVICE_IN_PROGRESS,
    SERVICE_COMPLETED,
    PAYMENT_COMPLETED,
    CREATED,
    CONFIRMED,
    PREPARING,
    OUT_FOR_DELIVERY,
    DELIVERED,
    CANCELLED;

    public Set<OrderStatus> getAllowedTransitions() {
        return switch (this) {
            case SEARCHING_PARTNER -> Set.of(PARTNER_ASSIGNED, INSTANT_FAILED, CANCELLED);
            case INSTANT_FAILED -> Set.of();
            case SCHEDULED -> Set.of(PARTNER_SEARCH, PARTNER_ASSIGNED, PARTNER_ACCEPTED, CANCELLED);
            case BOOKING_CREATED -> Set.of(SEARCHING_PARTNER, PARTNER_SEARCH, PARTNER_ACCEPTED, CANCELLED);
            case PARTNER_SEARCH -> Set.of(PARTNER_ASSIGNED, PARTNER_ACCEPTED, CANCELLED);
            case PARTNER_ASSIGNED -> Set.of(PARTNER_ACCEPTED, CANCELLED);
            case PARTNER_ACCEPTED -> Set.of(ARRIVED, PARTNER_REACHED, CANCELLED);
            case ARRIVED -> Set.of(SERVICE_STARTED, CANCELLED);
            case PARTNER_REACHED -> Set.of(SERVICE_STARTED, CANCELLED);
            case SERVICE_STARTED -> Set.of(SERVICE_IN_PROGRESS, SERVICE_COMPLETED, CANCELLED);
            case SERVICE_IN_PROGRESS -> Set.of(SERVICE_COMPLETED, CANCELLED);
            case SERVICE_COMPLETED -> Set.of(PAYMENT_COMPLETED);
            case PAYMENT_COMPLETED -> Set.of();
            case CREATED -> Set.of(CONFIRMED, SCHEDULED, SEARCHING_PARTNER, PARTNER_SEARCH,
                    PARTNER_ACCEPTED, INSTANT_FAILED, CANCELLED);
            case CONFIRMED -> Set.of(SCHEDULED, SEARCHING_PARTNER, PARTNER_SEARCH,
                    PARTNER_ACCEPTED, INSTANT_FAILED, PREPARING, CANCELLED);
            case PREPARING -> Set.of(OUT_FOR_DELIVERY);
            case OUT_FOR_DELIVERY -> Set.of(DELIVERED);
            case DELIVERED, CANCELLED -> Set.of();
        };
    }

    public boolean canTransitionTo(OrderStatus next) {
        return getAllowedTransitions().contains(next);
    }
    public boolean canCancel() {
        return this == CREATED
                || this == CONFIRMED
                || this == BOOKING_CREATED
                || this == SEARCHING_PARTNER
                || this == SCHEDULED
                || this == PARTNER_SEARCH
                || this == PARTNER_ASSIGNED
                || this == PARTNER_ACCEPTED
                || this == ARRIVED
                || this == PARTNER_REACHED;
    }
}

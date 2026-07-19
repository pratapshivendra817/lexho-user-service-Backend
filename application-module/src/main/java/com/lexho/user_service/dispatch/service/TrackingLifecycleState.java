package com.lexho.user_service.dispatch.service;

public enum TrackingLifecycleState {
    ASSIGNED,
    EN_ROUTE,
    ARRIVED,
    JOB_STARTED,
    JOB_IN_PROGRESS,
    JOB_COMPLETED,
    CANCELLED,
    OFFLINE;

    public static TrackingLifecycleState from(String raw, TrackingLifecycleState fallback) {
        if (raw == null || raw.isBlank()) {
            return fallback;
        }
        try {
            return TrackingLifecycleState.valueOf(raw.trim().toUpperCase());
        } catch (IllegalArgumentException ignored) {
            return fallback;
        }
    }
}


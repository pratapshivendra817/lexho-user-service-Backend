package com.lexho.user_service.util;

public final class LexhoConstants {

    public static final String ACTOR_ADMIN = "ADMIN";
    public static final String ACTOR_ADMIN_PREFIX = "ADMIN:";
    public static final String BOOKING_NOT_FOUND = "Booking not found";
    public static final String CUSTOMER_NOT_FOUND = "Customer not found";

    public static final String FIELD_AMOUNT = "amount";
    public static final String FIELD_AVAILABLE = "available";
    public static final String FIELD_CAN_CALL_PARTNER = "canCallPartner";
    public static final String FIELD_CAN_CANCEL = "canCancel";
    public static final String FIELD_CAN_RAISE_ISSUE = "canRaiseIssue";
    public static final String FIELD_CAN_RESCHEDULE = "canReschedule";
    public static final String FIELD_CAN_TRACK_PARTNER = "canTrackPartner";
    public static final String FIELD_COORDINATES = "coordinates";
    public static final String FIELD_CREATED_AT = "createdAt";
    public static final String FIELD_ETA_MINUTES = "etaMinutes";
    public static final String FIELD_INSTANT_ELIGIBLE = "instantEligible";
    public static final String FIELD_RATING = "rating";
    public static final String FIELD_SCHEDULED_AT = "scheduledAt";
    public static final String FIELD_STATUS_HISTORY = "statusHistory";
    public static final String FIELD_STORAGE = "storage";
    public static final String FIELD_UPDATED_AT = "updatedAt";

    public static final String STATUS_ACTIVE = "ACTIVE";
    public static final String STATUS_CANCELLED = "CANCELLED";
    public static final String STATUS_COMPLETED = "COMPLETED";
    public static final String STATUS_INACTIVE = "INACTIVE";
    public static final String STATUS_INSTANT_FAILED = "INSTANT_FAILED";
    public static final String STATUS_PARTNER_ASSIGNED = "PARTNER_ASSIGNED";
    public static final String STATUS_SCHEDULED = "SCHEDULED";
    public static final String STATUS_SEARCHING_PARTNER = "SEARCHING_PARTNER";
    public static final String TYPE_SUBSCRIPTION = "SUBSCRIPTION";

    public static final String TABLE_POPULAR_SERVICES = "popular_services";

    private LexhoConstants() {
    }
}

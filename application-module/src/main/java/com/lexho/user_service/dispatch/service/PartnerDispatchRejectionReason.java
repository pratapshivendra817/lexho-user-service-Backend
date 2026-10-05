package com.lexho.user_service.dispatch.service;

public enum PartnerDispatchRejectionReason {
    DISPATCH_ELIGIBLE,
    PARTNER_NOT_FOUND,
    PARTNER_NOT_APPROVED,
    SHIFT_NOT_STARTED,
    PARTNER_OFFLINE,
    SERVICE_UNAVAILABLE,
    ACTIVE_JOB_LIMIT_REACHED
}

package com.lexho.user_service.dispatch.service;

public record PartnerDispatchEligibilityResult(boolean eligible, PartnerDispatchRejectionReason reason) {
    public static PartnerDispatchEligibilityResult allowed() {
        return new PartnerDispatchEligibilityResult(true, PartnerDispatchRejectionReason.DISPATCH_ELIGIBLE);
    }

    public static PartnerDispatchEligibilityResult rejected(PartnerDispatchRejectionReason reason) {
        return new PartnerDispatchEligibilityResult(false, reason);
    }
}

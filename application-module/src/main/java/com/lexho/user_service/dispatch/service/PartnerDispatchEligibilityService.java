package com.lexho.user_service.dispatch.service;

import com.lexho.provider.entity.Partner;
import com.lexho.user_service.dispatch.entity.PartnerShiftEntity;
import com.lexho.user_service.exception.BadRequestException;
import com.lexho.user_service.lexhopartner.entity.PartnerOrder;
import com.lexho.user_service.lexhopartner.entity.PartnerOperationalProfile;
import com.lexho.user_service.lexhopartner.repository.PartnerOperationalProfileRepository;
import com.lexho.user_service.lexhopartner.model.PartnerOperationalStatus;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Locale;
import java.util.Objects;

@Service
public class PartnerDispatchEligibilityService {

    private final Clock clock;
    private final PartnerOperationalProfileRepository partnerProfileRepository;
    private final boolean allowLegacyProfileFixtures;

    public PartnerDispatchEligibilityService() {
        this(Clock.systemDefaultZone(), null, true);
    }

    PartnerDispatchEligibilityService(Clock clock) {
        this(clock, null, true);
    }

    @org.springframework.beans.factory.annotation.Autowired
    public PartnerDispatchEligibilityService(PartnerOperationalProfileRepository partnerProfileRepository) {
        this(Clock.systemDefaultZone(), partnerProfileRepository, false);
    }

    private PartnerDispatchEligibilityService(Clock clock,
                                              PartnerOperationalProfileRepository partnerProfileRepository,
                                              boolean allowLegacyProfileFixtures) {
        this.clock = clock;
        this.partnerProfileRepository = partnerProfileRepository;
        this.allowLegacyProfileFixtures = allowLegacyProfileFixtures;
    }

    public PartnerDispatchEligibilityResult canReceiveNewBookings(Partner partner, PartnerShiftEntity shift) {
        if (partnerProfileRepository != null && partner != null) {
            PartnerOperationalProfile profile = partnerProfileRepository.findById(partner.getId()).orElse(null);
            return canReceiveNewBookings(profile, shift);
        }
        if (partner == null || shift == null || shift.isDeleted()) {
            return PartnerDispatchEligibilityResult.rejected(PartnerDispatchRejectionReason.PARTNER_NOT_FOUND);
        }
        if (!isApprovedPartner(partner.getVerificationStatus())) {
            return PartnerDispatchEligibilityResult.rejected(PartnerDispatchRejectionReason.PARTNER_NOT_APPROVED);
        }
        return evaluateShiftAndAvailability(shift, Boolean.TRUE.equals(partner.getAvailable()));
    }

    public PartnerDispatchEligibilityResult canReceiveNewBookings(PartnerOperationalProfile profile, PartnerShiftEntity shift) {
        if (profile == null || shift == null || profile.isDeleted() || shift.isDeleted()) {
            return PartnerDispatchEligibilityResult.rejected(PartnerDispatchRejectionReason.PARTNER_NOT_FOUND);
        }
        if (!profile.isVerified()
                || (!allowLegacyProfileFixtures && profile.getPartnerOperationalStatus() != PartnerOperationalStatus.APPROVED)
                || profile.getPartnerOperationalStatus() == PartnerOperationalStatus.BLOCKED
                || profile.getPartnerOperationalStatus() == PartnerOperationalStatus.REJECTED) {
            return PartnerDispatchEligibilityResult.rejected(PartnerDispatchRejectionReason.PARTNER_NOT_APPROVED);
        }
        return evaluateShiftAndAvailability(shift, profile.isOnline());
    }

    public void requireEligibleForNewDispatch(Partner partner, PartnerShiftEntity shift) {
        PartnerDispatchEligibilityResult result = canReceiveNewBookings(partner, shift);
        if (!result.eligible()) {
            throw new BadRequestException("Partner is not eligible for new dispatch: " + result.reason().name());
        }
    }

    public void requireEligibleForNewDispatch(PartnerOperationalProfile profile, PartnerShiftEntity shift) {
        PartnerDispatchEligibilityResult result = canReceiveNewBookings(profile, shift);
        if (!result.eligible()) {
            throw new BadRequestException("Partner is not eligible for new dispatch: " + result.reason().name());
        }
    }

    public boolean canManageActiveBooking(Long partnerId, PartnerOrder order) {
        return partnerId != null
                && order != null
                && order.getAssignedPartnerId() != null
                && Objects.equals(order.getAssignedPartnerId(), partnerId);
    }

    private PartnerDispatchEligibilityResult evaluateShiftAndAvailability(PartnerShiftEntity shift,
                                                                         boolean serviceAvailable) {
        if (!isShiftStarted(shift)) {
            return PartnerDispatchEligibilityResult.rejected(PartnerDispatchRejectionReason.SHIFT_NOT_STARTED);
        }
        if (!shift.isOnline()) {
            return PartnerDispatchEligibilityResult.rejected(PartnerDispatchRejectionReason.PARTNER_OFFLINE);
        }
        if (!serviceAvailable) {
            return PartnerDispatchEligibilityResult.rejected(PartnerDispatchRejectionReason.SERVICE_UNAVAILABLE);
        }
        if (shift.getActiveJobsCount() >= shift.getMaxActiveJobs()) {
            return PartnerDispatchEligibilityResult.rejected(PartnerDispatchRejectionReason.ACTIVE_JOB_LIMIT_REACHED);
        }
        return PartnerDispatchEligibilityResult.allowed();
    }

    private boolean isShiftStarted(PartnerShiftEntity shift) {
        if (shift.getShiftStart() == null) {
            return false;
        }
        LocalDateTime now = LocalDateTime.now(clock);
        return !now.isBefore(shift.getShiftStart())
                && (shift.getShiftEnd() == null || !now.isAfter(shift.getShiftEnd()));
    }

    private boolean isApprovedPartner(String verificationStatus) {
        if (verificationStatus == null || verificationStatus.isBlank()) {
            return false;
        }
        String normalized = verificationStatus.trim().toUpperCase(Locale.ROOT);
        return "APPROVED".equals(normalized) || "VERIFIED".equals(normalized);
    }
}

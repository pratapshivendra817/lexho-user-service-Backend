package com.lexho.user_service.dispatch.service;

import com.lexho.provider.entity.Partner;
import com.lexho.user_service.dispatch.entity.PartnerShiftEntity;
import com.lexho.user_service.lexhopartner.entity.OrderStatus;
import com.lexho.user_service.lexhopartner.entity.PartnerOrder;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

import static org.assertj.core.api.Assertions.assertThat;

class PartnerDispatchEligibilityServiceTest {

    private final Clock clock = Clock.fixed(Instant.parse("2026-08-21T10:00:00Z"), ZoneId.of("UTC"));
    private final PartnerDispatchEligibilityService service = new PartnerDispatchEligibilityService(clock);

    @Test
    void approvedShiftStartedOnlineServiceAvailableCanReceiveNewBookings() {
        assertThat(service.canReceiveNewBookings(partner("APPROVED", true), shift(true, -1, 1)).eligible()).isTrue();
    }

    @Test
    void pendingPartnerCannotReceiveNewBookings() {
        assertRejected(partner("PENDING", true), shift(true, -1, 1), PartnerDispatchRejectionReason.PARTNER_NOT_APPROVED);
    }

    @Test
    void rejectedPartnerCannotReceiveNewBookings() {
        assertRejected(partner("REJECTED", true), shift(true, -1, 1), PartnerDispatchRejectionReason.PARTNER_NOT_APPROVED);
    }

    @Test
    void approvedPartnerWithShiftNotStartedCannotReceiveNewBookings() {
        assertRejected(partner("APPROVED", true), shift(true, 1, 2), PartnerDispatchRejectionReason.SHIFT_NOT_STARTED);
    }

    @Test
    void approvedPartnerOfflineCannotReceiveNewBookings() {
        assertRejected(partner("APPROVED", true), shift(false, -1, 1), PartnerDispatchRejectionReason.PARTNER_OFFLINE);
    }

    @Test
    void approvedOnlinePartnerWithServiceUnavailableCannotReceiveNewBookings() {
        assertRejected(partner("APPROVED", false), shift(true, -1, 1), PartnerDispatchRejectionReason.SERVICE_UNAVAILABLE);
    }

    @Test
    void shiftEndedCannotReceiveNewBookings() {
        assertRejected(partner("APPROVED", true), shift(true, -3, -1), PartnerDispatchRejectionReason.SHIFT_NOT_STARTED);
    }

    @Test
    void activeBookingAccessIgnoresOfflineAndShiftEndedState() {
        PartnerOrder order = new PartnerOrder();
        order.setId(100L);
        order.setAssignedPartnerId(7L);
        order.setStatus(OrderStatus.IN_PROGRESS);

        assertThat(service.canManageActiveBooking(7L, order)).isTrue();
        assertThat(service.canManageActiveBooking(8L, order)).isFalse();
    }

    private void assertRejected(Partner partner,
                                PartnerShiftEntity shift,
                                PartnerDispatchRejectionReason reason) {
        PartnerDispatchEligibilityResult result = service.canReceiveNewBookings(partner, shift);
        assertThat(result.eligible()).isFalse();
        assertThat(result.reason()).isEqualTo(reason);
    }

    private Partner partner(String status, boolean available) {
        Partner partner = new Partner();
        partner.setId(7L);
        partner.setVerificationStatus(status);
        partner.setAvailable(available);
        return partner;
    }

    private PartnerShiftEntity shift(boolean online, int startHoursFromNow, int endHoursFromNow) {
        LocalDateTime now = LocalDateTime.ofInstant(clock.instant(), clock.getZone());
        PartnerShiftEntity shift = new PartnerShiftEntity();
        shift.setPartnerId(7L);
        shift.setOnline(online);
        shift.setActiveJobsCount(0);
        shift.setMaxActiveJobs(1);
        shift.setShiftStart(now.plusHours(startHoursFromNow));
        shift.setShiftEnd(now.plusHours(endHoursFromNow));
        return shift;
    }
}

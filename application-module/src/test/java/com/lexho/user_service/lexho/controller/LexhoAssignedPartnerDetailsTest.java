package com.lexho.user_service.lexho.controller;

import com.lexho.booking.entity.Order;
import com.lexho.common.enums.PaymentStatus;
import com.lexho.user_service.dispatch.entity.CurrentPartnerLocationEntity;
import com.lexho.user_service.dispatch.repository.CurrentPartnerLocationRepository;
import com.lexho.user_service.lexhopartner.entity.PartnerOperationalProfile;
import com.lexho.user_service.lexhopartner.repository.PartnerOperationalProfileRepository;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import java.math.BigDecimal;
import java.util.Map;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class LexhoAssignedPartnerDetailsTest {
    @Test
    void usesAssignedProfileAndNeverServiceName() {
        LexhoExperienceController controller = mock(LexhoExperienceController.class, CALLS_REAL_METHODS);
        PartnerOperationalProfileRepository profiles = mock(PartnerOperationalProfileRepository.class);
        ReflectionTestUtils.setField(controller, "partnerProfileRepository", profiles);
        Order order = new Order();
        order.setAssignedPartnerId(77L);
        PartnerOperationalProfile profile = new PartnerOperationalProfile();
        profile.setName("Assigned Cook");
        profile.setMobileNumber("9876543210");
        profile.setRating(4.8);
        when(profiles.findById(77L)).thenReturn(Optional.of(profile));
        Map<String, Object> result = ReflectionTestUtils.invokeMethod(controller, "partnerSummary", order, "PARTNER_ASSIGNED");
        assertEquals("77", result.get("id"));
        assertEquals("Assigned Cook", result.get("name"));
        assertEquals("9876543210", result.get("phone"));
        assertEquals(4.8, result.get("rating"));
        assertEquals("", result.get("profileImage"));
        when(profiles.findById(77L)).thenReturn(Optional.empty());
        result = ReflectionTestUtils.invokeMethod(controller, "partnerSummary", order, "PARTNER_ASSIGNED");
        assertEquals("Partner", result.get("name"));
        order.setAssignedPartnerId(null);
        assertNull(ReflectionTestUtils.invokeMethod(controller, "partnerSummary", order, "PARTNER_ASSIGNED"));
    }

    @Test
    void enablesAssignedMapOnlyWithPaidBookingAndPartnerLocation() {
        LexhoExperienceController controller = mock(LexhoExperienceController.class, CALLS_REAL_METHODS);
        CurrentPartnerLocationRepository locations = mock(CurrentPartnerLocationRepository.class);
        ReflectionTestUtils.setField(controller, "currentPartnerLocationRepository", locations);
        Order order = new Order();
        order.setAssignedPartnerId(77L);
        order.setPaymentStatus(PaymentStatus.SUCCESS);
        CurrentPartnerLocationEntity location = new CurrentPartnerLocationEntity();
        location.setLatitude(BigDecimal.valueOf(19.14));
        location.setLongitude(BigDecimal.valueOf(72.80));
        when(locations.findByPartnerIdAndDeletedFalse(77L)).thenReturn(Optional.of(location));
        Map<String, Object> result = ReflectionTestUtils.invokeMethod(controller, "mapTrackingPayload", order, "PARTNER_ASSIGNED");
        assertEquals(true, result.get("showMap"));
        order.setPaymentStatus(PaymentStatus.PENDING);
        result = ReflectionTestUtils.invokeMethod(controller, "mapTrackingPayload", order, "PARTNER_ASSIGNED");
        assertEquals(false, result.get("showMap"));
        order.setPaymentStatus(PaymentStatus.SUCCESS);
        when(locations.findByPartnerIdAndDeletedFalse(77L)).thenReturn(Optional.empty());
        result = ReflectionTestUtils.invokeMethod(controller, "mapTrackingPayload", order, "PARTNER_ASSIGNED");
        assertEquals(false, result.get("showMap"));
    }
}

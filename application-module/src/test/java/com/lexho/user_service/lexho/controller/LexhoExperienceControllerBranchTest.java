package com.lexho.user_service.lexho.controller;

import com.lexho.booking.entity.Order;
import com.lexho.common.enums.OrderStatus;
import com.lexho.common.enums.PaymentStatus;
import com.lexho.provider.entity.Partner;
import com.lexho.user_service.booking.exception.BookingValidationException;
import com.lexho.user_service.cartconfig.entity.CartConfigEntity;
import com.lexho.user_service.dispatch.entity.BookingEventEntity;
import com.lexho.user_service.dispatch.entity.CurrentPartnerLocationEntity;
import com.lexho.user_service.dispatch.repository.BookingEventRepository;
import com.lexho.user_service.dispatch.repository.CurrentPartnerLocationRepository;
import com.lexho.user_service.exception.BadRequestException;
import com.lexho.user_service.lexhopartner.entity.PartnerOperationalProfile;
import com.lexho.user_service.lexhopartner.repository.PartnerOperationalProfileRepository;
import com.lexho.user_service.review.repository.ReviewRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.lang.reflect.Constructor;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.LinkedHashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class LexhoExperienceControllerBranchTest {

    private LexhoExperienceController controller;

    @BeforeEach
    void setUp() throws ReflectiveOperationException {
        Constructor<?> constructor = LexhoExperienceController.class.getConstructors()[0];
        Class<?>[] parameterTypes = constructor.getParameterTypes();
        Object[] dependencies = new Object[parameterTypes.length];
        for (int index = 0; index < parameterTypes.length; index++) {
            dependencies[index] = mock(parameterTypes[index]);
        }
        controller = (LexhoExperienceController) constructor.newInstance(dependencies);
    }

    @Test
    void parsesAndNormalizesPaymentInputsAcrossAllBranches() {
        assertThat((LocalDateTime) invoke("parseDateTime", (String) null)).isNull();
        assertThat((LocalDateTime) invoke("parseDateTime", " ")).isNull();
        assertThat((LocalDateTime) invoke("parseDateTime", "2026-06-12T10:15:30"))
                .isEqualTo(LocalDateTime.of(2026, 6, 12, 10, 15, 30));
        assertThatThrownBy(() -> invoke("parseDateTime", "12/06/2026"))
                .isInstanceOf(BadRequestException.class);

        assertThat((String) invoke("normalizeMode", (String) null)).isEqualTo("UPI");
        assertThat((String) invoke("normalizeMode", " ")).isEqualTo("UPI");
        assertThat((String) invoke("normalizeMode", " card ")).isEqualTo("CARD");
        assertThatThrownBy(() -> invoke("normalizeMode", "cod"))
                .isInstanceOf(BadRequestException.class);
        assertThatThrownBy(() -> invoke("normalizeMode", "cash"))
                .isInstanceOf(BadRequestException.class);

        assertThat((Long) invoke("resolveBookingId", (String) null)).isNull();
        assertThat((Long) invoke("resolveBookingId", " ")).isNull();
        assertThat((Long) invoke("resolveBookingId", " 123 ")).isEqualTo(123L);
        assertThat((Long) invoke("resolveBookingId", "LEXHO-456-X")).isEqualTo(456L);
        assertThat((Long) invoke("resolveBookingId", "LEXHO")).isNull();

        assertThat((Integer) invoke("amountInPaise", new BigDecimal("10.125"), null)).isEqualTo(1013);
        assertThat((Integer) invoke("amountInPaise", null, 12.34d)).isEqualTo(1234);
        assertThat((Integer) invoke("amountInPaise", null, null)).isZero();

        Map<String, Object> minimal = invoke("paymentOptionsPayload", null, null, null);
        Map<String, Object> complete = invoke("paymentOptionsPayload", "B-1", 9L, new BigDecimal("20"));
        assertThat(minimal).doesNotContainKeys("bookingId", "cartId", "amount");
        assertThat(complete).containsEntry("bookingId", "B-1")
                .containsEntry("cartId", 9L)
                .containsEntry("amount", new BigDecimal("20"));
    }

    @Test
    void derivesEveryAssignmentAndResponseStatus() {
        assertThat((String) invoke("deriveAssignmentStatus", null, false)).isEqualTo("SEARCHING_PARTNER");

        assertAssignment(OrderStatus.SERVICE_IN_PROGRESS, null, false, "IN_PROGRESS");
        assertAssignment(OrderStatus.SERVICE_STARTED, null, false, "SERVICE_STARTED");
        assertAssignment(OrderStatus.SERVICE_COMPLETED, null, false, "COMPLETED");
        assertAssignment(OrderStatus.DELIVERED, null, false, "COMPLETED");
        assertAssignment(OrderStatus.PAYMENT_COMPLETED, null, false, "COMPLETED");
        assertAssignment(OrderStatus.ARRIVED, null, false, "ARRIVED");
        assertAssignment(OrderStatus.PARTNER_ACCEPTED, null, false, "PARTNER_ON_THE_WAY");
        assertAssignment(OrderStatus.PREPARING, null, false, "PARTNER_ON_THE_WAY");
        assertAssignment(OrderStatus.CANCELLED, null, false, "CANCELLED");
        assertAssignment(OrderStatus.INSTANT_FAILED, null, false, "NO_PARTNER_AVAILABLE");
        assertAssignment(OrderStatus.CREATED, null, true, "PARTNER_ASSIGNED");
        assertAssignment(OrderStatus.CREATED, "SCHEDULED", false, "SCHEDULED");
        assertAssignment(OrderStatus.CREATED, "INSTANT", false, "SEARCHING_PARTNER");

        assertResponse(OrderStatus.CREATED, null, "PENDING_ASSIGNMENT");
        assertResponse(OrderStatus.CONFIRMED, "SCHEDULED", "SCHEDULED");
        assertResponse(OrderStatus.PARTNER_ASSIGNED, null, "PARTNER_ASSIGNED");
        assertResponse(OrderStatus.OUT_FOR_DELIVERY, null, "ON_THE_WAY");
        assertResponse(OrderStatus.PARTNER_REACHED, null, "ARRIVED");
        assertResponse(OrderStatus.SERVICE_STARTED, null, "SERVICE_STARTED");
        assertResponse(OrderStatus.SERVICE_IN_PROGRESS, null, "SERVICE_IN_PROGRESS");
        assertResponse(OrderStatus.PAYMENT_COMPLETED, null, "COMPLETED");
        assertResponse(OrderStatus.INSTANT_FAILED, null, "EXPIRED");
        assertResponse(OrderStatus.CANCELLED, null, "CANCELLED");
        assertResponse(OrderStatus.SCHEDULED, null, "SCHEDULED");
        assertThat((String) invoke("responseStatus", null, null)).isEqualTo("PENDING_ASSIGNMENT");
    }

    @Test
    void mapsTrackingScreensMessagesEtaAndTimelineCases() {
        assertThat((String) invoke("bookingMessage", "PENDING", "PARTNER_ASSIGNED"))
                .isEqualTo("Partner assigned");
        assertThat((String) invoke("bookingMessage", "PENDING", "NO_PARTNER_AVAILABLE"))
                .isEqualTo("No partner available right now");
        assertThat((String) invoke("bookingMessage", "PENDING", "SCHEDULED"))
                .isEqualTo("Booking scheduled");
        assertThat((String) invoke("bookingMessage", "COMPLETED", "OTHER"))
                .isEqualTo("Service completed");
        assertThat((String) invoke("bookingMessage", "PENDING", "OTHER"))
                .isEqualTo("Finding partner");

        assertThat((String) invoke("trackingScreen", null, null)).isEqualTo("INSTANT_STATUS");
        assertTracking("SUBSCRIPTION", OrderStatus.CREATED, null, "SUBSCRIPTION_DETAILS");
        assertTracking("SCHEDULED", OrderStatus.CREATED, "SEARCHING_PARTNER", "SCHEDULED_STATUS");
        assertTracking("SCHEDULED", OrderStatus.PARTNER_ASSIGNED, "PARTNER_ASSIGNED", "INSTANT_STATUS");
        assertTracking("INSTANT", OrderStatus.PARTNER_ACCEPTED, null, "LIVE_TRACKING");
        assertTracking("INSTANT", OrderStatus.ARRIVED, null, "PARTNER_ARRIVED");
        assertTracking("INSTANT", OrderStatus.SERVICE_STARTED, null, "SERVICE_STARTED");
        assertTracking("INSTANT", OrderStatus.SERVICE_IN_PROGRESS, null, "SERVICE_IN_PROGRESS");
        assertTracking("INSTANT", OrderStatus.DELIVERED, null, "SERVICE_COMPLETED");
        assertTracking("INSTANT", OrderStatus.SCHEDULED, null, "SCHEDULED_STATUS");

        assertThat((Integer) invoke("deriveEtaMinutes", "COMPLETED", "OTHER")).isZero();
        assertThat((Integer) invoke("deriveEtaMinutes", "PENDING", "COMPLETED")).isZero();
        assertThat((Integer) invoke("deriveEtaMinutes", "PENDING", "PARTNER_ASSIGNED")).isEqualTo(12);
        assertThat((Integer) invoke("deriveEtaMinutes", "PENDING", "PARTNER_ON_THE_WAY")).isEqualTo(12);
        assertThat((Integer) invoke("deriveEtaMinutes", "PENDING", "SEARCHING_PARTNER")).isZero();

        assertThat((String) invoke("timelineStatus", "BOOKING_CREATED")).isEqualTo("BOOKING_CREATED");
        assertThat((String) invoke("timelineStatus", "partner_accepted")).isEqualTo("ON_THE_WAY");
        assertThat((String) invoke("timelineStatus", "partner_reached")).isEqualTo("ARRIVED");
        assertThat((String) invoke("timelineStatus", "payment_completed")).isEqualTo("COMPLETED");
        assertThat((String) invoke("timelineStatus", "unknown")).isNull();
        assertThat((String) invoke("timelineStatus", (String) null)).isNull();
    }

    @Test
    void generatesOpaqueReferralCodes() {
        Set<String> generated = new HashSet<>();

        for (int i = 0; i < 64; i++) {
            String code = LexhoExperienceController.nextReferralCode();
            assertThat(code).startsWith("LEX");
            assertThat(code).hasSize(11);
            assertThat(code.substring(3)).matches("[A-HJ-NP-Z2-9]{8}");
            generated.add(code);
        }

        assertThat(generated).hasSizeGreaterThan(1);
    }

    @Test
    void coversPartnerVisibilityPhoneTicketAndMapReasonBoundaries() {
        assertThat((Boolean) invoke("shouldExposePartner", "PARTNER_ASSIGNED")).isTrue();
        assertThat((Boolean) invoke("shouldExposePartner", "completed")).isTrue();
        assertThat((Boolean) invoke("shouldExposePartner", (String) null)).isFalse();
        assertThat((Boolean) invoke("shouldExposePartner", "SEARCHING_PARTNER")).isFalse();

        assertThat((String) invoke("sanitizePhone", (String) null)).isEmpty();
        assertThat((String) invoke("sanitizePhone", " ")).isEmpty();
        assertThat((String) invoke("sanitizePhone", "98765-43210")).isEqualTo("9876543210");
        assertThat((String) invoke("sanitizePhone", "+91 98765 43210")).isEqualTo("9876543210");
        assertThat((String) invoke("sanitizePhone", "12345")).isEmpty();

        assertThat((String) invoke("formatTicketId", (Long) null)).isEmpty();
        assertThat((String) invoke("formatTicketId", 42L)).isEqualTo("TCK-10042");
        assertThat((Long) invoke("parseTicketId", "TCK-10042")).isEqualTo(42L);
        assertThat((Long) invoke("parseTicketId", "42")).isEqualTo(42L);
        assertThatThrownBy(() -> invoke("parseTicketId", " "))
                .isInstanceOf(BadRequestException.class);
        assertThatThrownBy(() -> invoke("parseTicketId", "TCK"))
                .isInstanceOf(BadRequestException.class);

        assertThat((String) invoke("mapDisabledReason", false, true, 1L, true))
                .isEqualTo("PAYMENT_NOT_PAID");
        assertThat((String) invoke("mapDisabledReason", true, false, 1L, true))
                .isEqualTo("TRACKING_NOT_ACTIVE");
        assertThat((String) invoke("mapDisabledReason", true, true, null, true))
                .isEqualTo("PARTNER_NOT_ASSIGNED");
        assertThat((String) invoke("mapDisabledReason", true, true, 1L, false))
                .isEqualTo("PARTNER_LOCATION_UNAVAILABLE");
        assertThat((String) invoke("mapDisabledReason", true, true, 1L, true))
                .isEqualTo("UNAVAILABLE");
    }

    @Test
    void coversTrackingRefreshPermissionsAndPaymentRetryBranches() {
        assertThat((Integer) invoke("trackingRefreshSeconds", "ANY", "PARTNER_ASSIGNED")).isEqualTo(15);
        assertThat((Integer) invoke("trackingRefreshSeconds", "ANY", "PARTNER_ON_THE_WAY")).isEqualTo(5);
        assertThat((Integer) invoke("trackingRefreshSeconds", "ANY", "ARRIVED")).isEqualTo(5);
        assertThat((Integer) invoke("trackingRefreshSeconds", "ANY", "SERVICE_STARTED")).isEqualTo(15);
        assertThat((Integer) invoke("trackingRefreshSeconds", "SERVICE_IN_PROGRESS", "OTHER")).isEqualTo(30);
        assertThat((Integer) invoke("trackingRefreshSeconds", "COMPLETED", "OTHER")).isZero();

        Order scheduled = order(OrderStatus.SCHEDULED, "SCHEDULED");
        scheduled.setAssignedPartnerId(5L);
        assertThat((Boolean) invoke("canReschedule", scheduled)).isTrue();
        assertThat((Boolean) invoke("canCallPartner", scheduled, "PARTNER_ON_THE_WAY")).isTrue();
        assertThat((Boolean) invoke("canRaiseIssue", order(OrderStatus.SERVICE_COMPLETED, "INSTANT"))).isTrue();
        assertThat((Boolean) invoke("retryPaymentAllowed", paymentOrder(PaymentStatus.FAILED))).isTrue();
        assertThat((Boolean) invoke("retryPaymentAllowed", paymentOrder(PaymentStatus.EXPIRED))).isTrue();
        assertThat((Boolean) invoke("retryPaymentAllowed", paymentOrder(PaymentStatus.SUCCESS))).isFalse();

        Map<String, Object> permissions = invoke("actionPermissions", scheduled, "PARTNER_ON_THE_WAY", true);
        assertThat(permissions).containsEntry("canCancel", true)
                .containsEntry("canReschedule", true)
                .containsEntry("canCallPartner", true)
                .containsEntry("canTrackPartner", true)
                .containsEntry("canRaiseIssue", false);
    }

    @Test
    void coversTrackingPayloadStatusHistoryPartnerSummaryAndReviewMeta() {
        CurrentPartnerLocationRepository locationRepository =
                (CurrentPartnerLocationRepository) ReflectionTestUtils.getField(controller, "currentPartnerLocationRepository");
        BookingEventRepository bookingEventRepository =
                (BookingEventRepository) ReflectionTestUtils.getField(controller, "bookingEventRepository");
        ReviewRepository reviewRepository =
                (ReviewRepository) ReflectionTestUtils.getField(controller, "reviewRepository");
        com.lexho.provider.repository.PartnerRepository partnerRepository =
                (com.lexho.provider.repository.PartnerRepository) ReflectionTestUtils.getField(controller, "partnerRepository");
        PartnerOperationalProfileRepository partnerProfileRepository =
                (PartnerOperationalProfileRepository) ReflectionTestUtils.getField(controller, "partnerProfileRepository");

        CurrentPartnerLocationEntity location = new CurrentPartnerLocationEntity();
        location.setPartnerId(44L);
        location.setLatitude(new BigDecimal("28.6139"));
        location.setLongitude(new BigDecimal("77.2090"));
        location.setLastHeartbeatAt(LocalDateTime.of(2026, 6, 20, 10, 1));
        setField(location, "updatedAt", LocalDateTime.of(2026, 6, 20, 10, 0));
        when(locationRepository.findByPartnerIdAndDeletedFalse(44L)).thenReturn(Optional.of(location));

        Order trackingOrder = order(OrderStatus.PARTNER_ACCEPTED, "INSTANT");
        trackingOrder.setAssignedPartnerId(44L);
        trackingOrder.setPaymentStatus(PaymentStatus.SUCCESS);
        Map<String, Object> trackingPayload = invoke("mapTrackingPayload", trackingOrder, "PARTNER_ON_THE_WAY");
        assertThat(trackingPayload).containsEntry("showMap", true)
                .containsEntry("partnerAvailable", true);
        assertThat((Map<String, Object>) trackingPayload.get("map")).containsEntry("provider", "MAPBOX");

        BookingEventEntity event = new BookingEventEntity();
        event.setEventType("partner_accepted");
        setField(event, "createdAt", LocalDateTime.of(2026, 6, 20, 9, 0));
        when(bookingEventRepository.findByBookingIdOrderByCreatedAtAsc(90L)).thenReturn(java.util.List.of(event));
        Order eventOrder = order(OrderStatus.PARTNER_ACCEPTED, "INSTANT");
        eventOrder.setId(90L);
        @SuppressWarnings("unchecked")
        java.util.List<Map<String, Object>> eventHistory = invoke("statusHistory", eventOrder);
        assertThat(eventHistory).hasSize(1);
        assertThat(eventHistory.get(0)).containsEntry("status", "ON_THE_WAY");

        when(bookingEventRepository.findByBookingIdOrderByCreatedAtAsc(91L)).thenReturn(java.util.List.of());
        Order fallbackOrder = order(OrderStatus.PAYMENT_COMPLETED, "INSTANT");
        fallbackOrder.setId(91L);
        fallbackOrder.setAssignedPartnerId(44L);
        fallbackOrder.setCreatedAt(LocalDateTime.of(2026, 6, 20, 8, 0));
        fallbackOrder.setUpdatedAt(LocalDateTime.of(2026, 6, 20, 8, 5));
        fallbackOrder.setArrivedAt(LocalDateTime.of(2026, 6, 20, 8, 10));
        fallbackOrder.setStartedAt(LocalDateTime.of(2026, 6, 20, 8, 15));
        fallbackOrder.setInProgressAt(LocalDateTime.of(2026, 6, 20, 8, 20));
        assertThat((java.util.List<Map<String, Object>>) invoke("statusHistory", fallbackOrder)).hasSize(6);

        Partner partner = new Partner();
        partner.setId(44L);
        partner.setName("Legacy Partner");
        partner.setRating(4.1);
        partner.setImage("/legacy.png");
        when(partnerRepository.findById(44L)).thenReturn(Optional.of(partner));

        PartnerOperationalProfile partnerProfile = new PartnerOperationalProfile();
        partnerProfile.setId(44L);
        partnerProfile.setName("Assigned Partner");
        partnerProfile.setMobileNumber("+91 98765 43210");
        partnerProfile.setRating(4.8);
        when(partnerProfileRepository.findById(44L)).thenReturn(Optional.of(partnerProfile));

        com.lexho.user_service.dto.OrderResponseDTO orderResponse = new com.lexho.user_service.dto.OrderResponseDTO();
        com.lexho.user_service.dto.OrderItemDTO item = new com.lexho.user_service.dto.OrderItemDTO();
        item.setPartnerId(44L);
        item.setPartnerName("Fallback");
        orderResponse.setItems(java.util.List.of(item));
        @SuppressWarnings("unchecked")
        Map<String, Object> partnerSummary = (Map<String, Object>) invoke("partnerSummary", 44L, orderResponse, "PARTNER_ASSIGNED");
        assertThat(partnerSummary).containsEntry("id", "44")
                .containsEntry("name", "Assigned Partner")
                .containsEntry("phone", "9876543210")
                .containsEntry("rating", 4.8d);

        when(reviewRepository.existsByBookingIdAndDeletedFalse(91L)).thenReturn(false);
        Map<String, Object> meta = new LinkedHashMap<>();
        invoke("addReviewMeta", meta, fallbackOrder);
        assertThat(meta).containsEntry("canReview", true)
                .containsEntry("reviewSubmitted", false);
    }

    @Test
    void coversSubscriptionPayloadsMetaPagingAndNotifications() {
        com.lexho.user_service.subscription.repository.SubscriptionMasterRepository masterRepository =
                (com.lexho.user_service.subscription.repository.SubscriptionMasterRepository) ReflectionTestUtils.getField(controller, "subscriptionMasterRepository");
        com.lexho.user_service.subscription.repository.SubscriptionFrequencyRepository frequencyRepository =
                (com.lexho.user_service.subscription.repository.SubscriptionFrequencyRepository) ReflectionTestUtils.getField(controller, "subscriptionFrequencyRepository");
        com.lexho.user_service.subscription.repository.SubscriptionVisitRepository visitRepository =
                (com.lexho.user_service.subscription.repository.SubscriptionVisitRepository) ReflectionTestUtils.getField(controller, "subscriptionVisitRepository");

        Order order = order(OrderStatus.PAYMENT_COMPLETED, "SUBSCRIPTION");
        order.setId(51L);
        order.setAddress("Tower 21, Sector 44");
        order.setCreatedAt(LocalDateTime.of(2026, 6, 20, 9, 0));
        order.setPaymentStatus(PaymentStatus.FAILED);

        com.lexho.user_service.subscription.entity.SubscriptionMaster subscription =
                new com.lexho.user_service.subscription.entity.SubscriptionMaster();
        ReflectionTestUtils.setField(subscription, "id", 9L);
        subscription.setBookingId(51L);
        subscription.setCustomerId(7L);
        subscription.setServiceType("CLEANING");
        subscription.setStartDate(LocalDate.now().minusDays(1));
        subscription.setEndDate(LocalDate.now().plusDays(10));
        subscription.setVisits(6);
        subscription.setTimeSlot("10:00-11:00");
        subscription.setPrice(new BigDecimal("999"));
        subscription.setStatus(com.lexho.user_service.subscription.enums.SubscriptionStatus.ACTIVE);
        when(masterRepository.findByBookingIdAndCustomerId(51L, 7L)).thenReturn(Optional.of(subscription));
        when(masterRepository.findByBookingId(51L)).thenReturn(Optional.of(subscription));

        com.lexho.user_service.subscription.entity.SubscriptionFrequency monday =
                new com.lexho.user_service.subscription.entity.SubscriptionFrequency();
        monday.setDayName("Monday");
        com.lexho.user_service.subscription.entity.SubscriptionFrequency friday =
                new com.lexho.user_service.subscription.entity.SubscriptionFrequency();
        friday.setDayName("Friday");
        when(frequencyRepository.findBySubscriptionIdOrderByIdAsc(9L)).thenReturn(List.of(monday, friday));

        com.lexho.user_service.subscription.entity.SubscriptionVisit visit =
                new com.lexho.user_service.subscription.entity.SubscriptionVisit();
        visit.setVisitDate(LocalDate.now().plusDays(1));
        when(visitRepository.findBySubscriptionIdOrderByVisitDateAscIdAsc(9L)).thenReturn(List.of(visit));

        @SuppressWarnings("unchecked")
        Map<String, Object> bookingPayload = invoke("subscriptionBookingPayload", 7L, order);
        @SuppressWarnings("unchecked")
        Map<String, Object> summaryPayload = invoke("subscriptionSummaryPayload", order);
        Order fallbackSubscriptionOrder = order(OrderStatus.CREATED, "SUBSCRIPTION");
        fallbackSubscriptionOrder.setId(52L);
        fallbackSubscriptionOrder.setCreatedAt(LocalDateTime.of(2026, 6, 20, 8, 0));
        when(masterRepository.findByBookingId(52L)).thenReturn(Optional.empty());
        @SuppressWarnings("unchecked")
        Map<String, Object> summaryFallback = invoke("subscriptionSummaryPayload", fallbackSubscriptionOrder);
        @SuppressWarnings("unchecked")
        Map<String, Object> meta = invoke("subscriptionBookingMeta");
        @SuppressWarnings("unchecked")
        Map<String, Object> paged = invoke("pagedGroup",
                List.of(Map.of("id", 1), Map.of("id", 2), Map.of("id", 3)), 2, 2);

        assertThat(bookingPayload).containsEntry("frequencyLabel", "Monday, Friday")
                .containsEntry("retryPaymentAllowed", true)
                .containsEntry("partnerAvailable", false);
        assertThat(summaryPayload).containsEntry("subscriptionId", 9L);
        assertThat(summaryFallback).containsEntry("bookingType", "SUBSCRIPTION");
        assertThat(meta).containsEntry("trackingScreen", "SUBSCRIPTION_DETAILS")
                .containsEntry("canTrackPartner", false);
        assertThat(paged).containsEntry("page", 2)
                .containsEntry("hasMore", false);

        com.lexho.user_service.notification.dto.NotificationResponse notification =
                new com.lexho.user_service.notification.dto.NotificationResponse();
        notification.setId(8L);
        notification.setTitle("Booking");
        notification.setMessage("Confirmed");
        notification.setRedirectType("BOOKING");
        notification.setBookingId("51");
        notification.setStatus(com.lexho.notification.enums.NotificationStatus.READ);
        notification.setCreatedAt(LocalDateTime.of(2026, 6, 20, 10, 0));
        @SuppressWarnings("unchecked")
        Map<String, Object> notificationRow = invoke("toNotificationMap", notification);
        assertThat(notificationRow).containsEntry("read", true)
                .containsEntry("bookingId", "51");
    }

    @Test
    void coversScheduleParsingEstimatesAndAuditGuards() {
        com.lexho.user_service.customer.service.SlotDateTimePolicy slotPolicy =
                (com.lexho.user_service.customer.service.SlotDateTimePolicy) ReflectionTestUtils.getField(controller, "slotDateTimePolicy");
        com.lexho.user_service.customer.service.CustomerCartSlotService slotService =
                (com.lexho.user_service.customer.service.CustomerCartSlotService) ReflectionTestUtils.getField(controller, "customerCartSlotService");
        com.lexho.user_service.cartconfig.service.CartConfigService cartConfigService =
                (com.lexho.user_service.cartconfig.service.CartConfigService) ReflectionTestUtils.getField(controller, "cartConfigService");

        when(slotPolicy.zoneId()).thenReturn(ZoneId.systemDefault());
        when(slotPolicy.now()).thenReturn(LocalDateTime.of(2026, 6, 20, 10, 0));
        when(slotPolicy.today()).thenReturn(LocalDate.of(2026, 6, 20));

        CartConfigEntity config = new CartConfigEntity();
        config.setScheduledBookingWindowDays(5);
        when(cartConfigService.currentEntity()).thenReturn(config);

        assertThatThrownBy(() -> invoke("parseScheduledAt", null, null))
                .isInstanceOf(BookingValidationException.class);
        assertThatThrownBy(() -> invoke("parseScheduledAt", "20/06/2026", "10:00-11:00"))
                .isInstanceOf(BadRequestException.class);

        LocalDate validDate = LocalDate.of(2026, 6, 21);
        LocalDateTime validStart = LocalDateTime.of(2026, 6, 21, 10, 0);
        when(slotPolicy.parseSlotStart(validDate, "10:00-11:00")).thenReturn(validStart);
        when(slotService.slotRowForDate(validDate, "10:00-11:00")).thenReturn(Map.of("available", true));
        when(slotPolicy.isExpired(validStart)).thenReturn(false);

        assertThat((LocalDateTime) invoke("parseScheduledAt", "2026-06-21", "10:00-11:00")).isEqualTo(validStart);

        when(slotService.slotRowForDate(validDate, "11:00-12:00")).thenReturn(null);
        when(slotPolicy.parseSlotStart(validDate, "11:00-12:00")).thenReturn(validStart.plusHours(1));
        assertThatThrownBy(() -> invoke("parseScheduledAt", "2026-06-21", "11:00-12:00"))
                .isInstanceOf(BookingValidationException.class);

        when(slotService.slotRowForDate(validDate, "12:00-13:00")).thenReturn(Map.of("available", false));
        when(slotPolicy.parseSlotStart(validDate, "12:00-13:00")).thenReturn(validStart.plusHours(2));
        assertThatThrownBy(() -> invoke("parseScheduledAt", "2026-06-21", "12:00-13:00"))
                .isInstanceOf(BookingValidationException.class);

        when(slotService.slotRowForDate(validDate, "13:00-14:00")).thenReturn(Map.of("available", true));
        when(slotPolicy.parseSlotStart(validDate, "13:00-14:00")).thenReturn(validStart.plusHours(3));
        when(slotPolicy.isExpired(validStart.plusHours(3))).thenReturn(true);
        assertThatThrownBy(() -> invoke("parseScheduledAt", "2026-06-21", "13:00-14:00"))
                .isInstanceOf(BookingValidationException.class);

        assertThat((String) invoke("estimatedCompletionTime", order(OrderStatus.CREATED, "INSTANT"))).isEmpty();
        Order etaOrder = order(OrderStatus.SERVICE_STARTED, "INSTANT");
        etaOrder.setStartedAt(LocalDateTime.of(2026, 6, 20, 10, 0));
        assertThat((String) invoke("estimatedCompletionTime", etaOrder)).isEqualTo("11:00 AM");
        assertThat((Double) invoke("round2", 12.345d)).isEqualTo(12.35d);
        assertThat((String) invoke("utc", LocalDateTime.of(2026, 6, 20, 10, 0))).contains("T");
        assertThat((String) invoke("frequencyLabel", List.of())).isEqualTo("Custom");
        assertThat((String) invoke("frequencyLabel", List.of("Monday"))).isEqualTo("Monday");
        assertThat((String) invoke("frequencyLabel", List.of("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")))
                .isEqualTo("Everyday");

        when(((com.lexho.user_service.lexho.repository.LexhoReferralCodeRepository) ReflectionTestUtils.getField(controller, "referralCodeRepository"))
                .existsByCodeIgnoreCase(any())).thenReturn(false);
        assertThat((String) invoke("generateUniqueReferralCode", 7L)).startsWith("LEX");

        assertThatCode(() -> invoke("audit", 7L, 8L, 9L, "BOOKING", "old", "new")).doesNotThrowAnyException();
    }

    private void assertAssignment(OrderStatus status, String bookingType, boolean assigned, String expected) {
        Order order = order(status, bookingType);
        assertThat((String) invoke("deriveAssignmentStatus", order, assigned)).isEqualTo(expected);
    }

    private void assertResponse(OrderStatus status, String bookingType, String expected) {
        Order order = order(status, bookingType);
        assertThat((String) invoke("responseStatus", order, null)).isEqualTo(expected);
    }

    private void assertTracking(String bookingType, OrderStatus status, String assignmentStatus, String expected) {
        Order order = order(status, bookingType);
        assertThat((String) invoke("trackingScreen", order, assignmentStatus)).isEqualTo(expected);
    }

    private Order order(OrderStatus status, String bookingType) {
        Order order = new Order();
        order.setStatus(status);
        order.setBookingType(bookingType);
        return order;
    }

    private Order paymentOrder(PaymentStatus paymentStatus) {
        Order order = new Order();
        order.setPaymentStatus(paymentStatus);
        return order;
    }

    private void setField(Object target, String fieldName, Object value) {
        ReflectionTestUtils.setField(target, fieldName, value);
    }

    @SuppressWarnings("unchecked")
    private <T> T invoke(String method, Object... arguments) {
        return (T) ReflectionTestUtils.invokeMethod(controller, method, arguments);
    }
}

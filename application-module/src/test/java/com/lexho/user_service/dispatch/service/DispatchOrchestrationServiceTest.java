package com.lexho.user_service.dispatch.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lexho.booking.entity.Order;
import com.lexho.booking.repository.OrderRepository;
import com.lexho.common.enums.OrderStatus;
import com.lexho.common.enums.PaymentStatus;
import com.lexho.user_service.admin.entity.AdminCategoryEntity;
import com.lexho.user_service.admin.entity.AdminServiceEntity;
import com.lexho.user_service.dispatch.entity.PartnerShiftEntity;
import com.lexho.user_service.dispatch.repository.PincodeAdjacencyRepository;
import com.lexho.user_service.dispatch.repository.BookingEventRepository;
import com.lexho.user_service.dispatch.repository.CurrentPartnerLocationRepository;
import com.lexho.user_service.dispatch.repository.PartnerServiceSkillRepository;
import com.lexho.user_service.dispatch.repository.PartnerShiftRepository;
import com.lexho.user_service.admin.repository.AdminServiceEntityRepository;
import com.lexho.user_service.admin.serviceablearea.repository.ServiceableAreaRepository;
import com.lexho.user_service.config.DispatchProperties;
import com.lexho.user_service.exception.BadRequestException;
import com.lexho.user_service.lexho.entity.LexhoAddress;
import com.lexho.user_service.lexho.repository.LexhoAddressRepository;
import com.lexho.user_service.partnerrequest.entity.PartnerServiceRequestEntity;
import com.lexho.user_service.partnerrequest.model.PartnerServiceRequestStatus;
import com.lexho.user_service.partnerrequest.repository.PartnerServiceRequestRepository;
import com.lexho.provider.entity.Partner;
import com.lexho.provider.repository.PartnerRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DispatchOrchestrationServiceTest {

    @Mock
    private OrderRepository orderRepository;
    @Mock
    private LexhoAddressRepository lexhoAddressRepository;
    @Mock
    private ServiceableAreaRepository serviceableAreaRepository;
    @Mock
    private PartnerShiftRepository partnerShiftRepository;
    @Mock
    private CurrentPartnerLocationRepository currentPartnerLocationRepository;
    @Mock
    private PincodeAdjacencyRepository pincodeAdjacencyRepository;
    @Mock
    private PartnerRepository partnerRepository;
    @Mock
    private PartnerServiceRequestRepository partnerServiceRequestRepository;
    @Mock
    private PartnerServiceSkillRepository partnerServiceSkillRepository;
    @Mock
    private AdminServiceEntityRepository adminServiceEntityRepository;
    @Mock
    private BookingEventRepository bookingEventRepository;
    @Mock
    private DispatchScoringPolicy dispatchScoringPolicy;
    @Mock
    private ObjectMapper objectMapper;
    @Mock
    private TrackingLifecycleService trackingLifecycleService;

    private DispatchOrchestrationService dispatchOrchestrationService;

    @BeforeEach
    void setUp() {
        dispatchOrchestrationService = new DispatchOrchestrationService(
                orderRepository,
                lexhoAddressRepository,
                serviceableAreaRepository,
                partnerShiftRepository,
                currentPartnerLocationRepository,
                pincodeAdjacencyRepository,
                partnerRepository,
                partnerServiceRequestRepository,
                partnerServiceSkillRepository,
                adminServiceEntityRepository,
                bookingEventRepository,
                dispatchScoringPolicy,
                objectMapper,
                trackingLifecycleService,
                dispatchProperties(),
                new PartnerDispatchEligibilityService()
        );
        try {
            lenient().when(objectMapper.writeValueAsString(any())).thenReturn("{\"ok\":true}");
        } catch (Exception ignored) {
        }
    }

    @Test
    void markPartnerAcceptedInstantShouldSetPromisedStartTimeAndAssignedState() {
        Order order = new Order();
        order.setId(1L);
        order.setBookingType("INSTANT");
        order.setStatus(OrderStatus.SEARCHING_PARTNER);

        PartnerShiftEntity shift = new PartnerShiftEntity();
        shift.setPartnerId(99L);
        shift.setActiveJobsCount(1);

        when(orderRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(order));
        Partner partner = new Partner();
        partner.setId(99L);
        partner.setAvailable(true);
        partner.setVerificationStatus("APPROVED");
        shift.setOnline(true);
        shift.setShiftStart(LocalDateTime.now().minusHours(1));
        shift.setShiftEnd(LocalDateTime.now().plusHours(1));
        when(partnerShiftRepository.findByPartnerIdForUpdate(99L)).thenReturn(Optional.of(shift));
        when(partnerRepository.findById(99L)).thenReturn(Optional.of(partner));
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));
        when(partnerShiftRepository.save(any(PartnerShiftEntity.class))).thenAnswer(inv -> inv.getArgument(0));

        LocalDateTime before = LocalDateTime.now();
        dispatchOrchestrationService.markPartnerAccepted(1L, 99L, 12);
        LocalDateTime after = LocalDateTime.now();

        ArgumentCaptor<Order> orderCaptor = ArgumentCaptor.forClass(Order.class);
        verify(orderRepository).save(orderCaptor.capture());
        Order saved = orderCaptor.getValue();
        assertEquals(OrderStatus.PARTNER_ASSIGNED, saved.getStatus());
        assertEquals(DispatchOrchestrationService.INSTANT_SLA_MINUTES, saved.getSlaMinutes());
        assertNotNull(saved.getPromisedStartTime());
        assertEquals("INSTANT", saved.getBookingType());
        LocalDateTime minExpected = before.plusMinutes(12).minusSeconds(2);
        LocalDateTime maxExpected = after.plusMinutes(12).plusSeconds(2);
        boolean inRange = !saved.getPromisedStartTime().isBefore(minExpected)
                && !saved.getPromisedStartTime().isAfter(maxExpected);
        assertTrue(inRange);
    }

    @Test
    void markPartnerAcceptedShouldRejectConflictingPartnerAfterAssignment() {
        Order order = new Order();
        order.setId(2L);
        order.setBookingType("INSTANT");
        order.setStatus(OrderStatus.PARTNER_ASSIGNED);
        order.setAssignedPartnerId(99L);

        when(orderRepository.findByIdForUpdate(2L)).thenReturn(Optional.of(order));

        BadRequestException error = assertThrows(BadRequestException.class,
                () -> dispatchOrchestrationService.markPartnerAccepted(2L, 100L, 10));

        assertEquals("Order is already assigned to another partner", error.getMessage());
        verify(orderRepository, never()).save(any(Order.class));
        verify(partnerShiftRepository, never()).save(any(PartnerShiftEntity.class));
    }

    @Test
    void markPartnerAcceptedShouldRejectCancelledInstantOrder() {
        Order order = new Order();
        order.setId(3L);
        order.setBookingType("INSTANT");
        order.setStatus(OrderStatus.CANCELLED);

        Partner partner = new Partner();
        partner.setId(99L);
        partner.setAvailable(true);
        partner.setVerificationStatus("APPROVED");
        PartnerShiftEntity shift = new PartnerShiftEntity();
        shift.setPartnerId(99L);
        shift.setOnline(true);
        shift.setActiveJobsCount(0);
        shift.setMaxActiveJobs(3);
        shift.setShiftStart(LocalDateTime.now().minusHours(1));
        shift.setShiftEnd(LocalDateTime.now().plusHours(1));

        when(orderRepository.findByIdForUpdate(3L)).thenReturn(Optional.of(order));
        when(partnerShiftRepository.findByPartnerIdForUpdate(99L)).thenReturn(Optional.of(shift));
        when(partnerRepository.findById(99L)).thenReturn(Optional.of(partner));

        BadRequestException error = assertThrows(BadRequestException.class,
                () -> dispatchOrchestrationService.markPartnerAccepted(3L, 99L, 10));

        assertEquals("Order is not accepting partner assignment", error.getMessage());
        assertEquals(OrderStatus.CANCELLED, order.getStatus());
        assertEquals(0, shift.getActiveJobsCount());
        verify(orderRepository, never()).save(any(Order.class));
        verify(partnerShiftRepository, never()).save(any(PartnerShiftEntity.class));
    }

    @Test
    void dispatchOrderShouldRejectNonServiceablePincodeAndRecordFailureEvent() {
        Order order = baseOrder(1L, "INSTANT", OrderStatus.BOOKING_CREATED);
        LexhoAddress address = address("110001");
        AdminServiceEntity service = serviceEntity(5L, 2L);
        when(orderRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(order));
        when(lexhoAddressRepository.findByIdAndUserId(2L, 3L)).thenReturn(Optional.of(address));
        when(adminServiceEntityRepository.findByIdAndDeletedFalse(5L)).thenReturn(Optional.of(service));
        when(serviceableAreaRepository.existsByPincodeAndActiveTrueAndDeletedFalse("110001")).thenReturn(false);

        BadRequestException error = assertThrows(BadRequestException.class,
                () -> dispatchOrchestrationService.dispatchOrder(1L, 3L, 2L, 5L, 1, null));

        assertEquals("Service is not available for this pincode", error.getMessage());
        verify(bookingEventRepository, atLeastOnce()).save(argThat(event ->
                "PARTNER_SEARCH_FAILED".equals(event.getEventType())));
    }

    @Test
    void dispatchOrderShouldDeferScheduledBookingBeforeActivationWindow() {
        Order order = baseOrder(10L, "SCHEDULED", OrderStatus.BOOKING_CREATED);
        LexhoAddress address = address("110001");
        AdminServiceEntity service = serviceEntity(5L, 2L);
        LocalDateTime scheduledAt = LocalDateTime.now().plusHours(5);
        when(orderRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(order));
        when(lexhoAddressRepository.findByIdAndUserId(20L, 30L)).thenReturn(Optional.of(address));
        when(adminServiceEntityRepository.findByIdAndDeletedFalse(5L)).thenReturn(Optional.of(service));
        when(serviceableAreaRepository.existsByPincodeAndActiveTrueAndDeletedFalse("110001")).thenReturn(true);

        int created = dispatchOrchestrationService.dispatchOrder(10L, 30L, 20L, 5L, 2, scheduledAt);

        assertEquals(0, created);
        verify(bookingEventRepository).save(argThat(event -> "DISPATCH_DEFERRED".equals(event.getEventType())));
    }

    @Test
    void dispatchOrderShouldCreateRequestsAndAssignScheduledPartner() {
        Order order = baseOrder(11L, "SCHEDULED", OrderStatus.BOOKING_CREATED);
        LexhoAddress address = address("110001");
        AdminServiceEntity service = serviceEntity(5L, 2L);
        PartnerShiftEntity shift = new PartnerShiftEntity();
        shift.setPartnerId(99L);
        shift.setCurrentPincode("110001");
        shift.setOnline(true);
        shift.setActiveJobsCount(0);
        shift.setMaxActiveJobs(2);
        shift.setShiftStart(LocalDateTime.now().minusHours(1));
        shift.setShiftEnd(LocalDateTime.now().plusHours(1));
        Partner partner = new Partner();
        partner.setId(99L);
        partner.setAvailable(true);
        partner.setSubCategoryId(5L);
        partner.setVerificationStatus("APPROVED");
        com.lexho.user_service.dispatch.entity.CurrentPartnerLocationEntity location =
                new com.lexho.user_service.dispatch.entity.CurrentPartnerLocationEntity();
        location.setPartnerId(99L);
        location.setLatitude(java.math.BigDecimal.valueOf(28.4596));
        location.setLongitude(java.math.BigDecimal.valueOf(77.0267));

        when(orderRepository.findByIdForUpdate(11L)).thenReturn(Optional.of(order));
        when(lexhoAddressRepository.findByIdAndUserId(21L, 31L)).thenReturn(Optional.of(address));
        when(adminServiceEntityRepository.findByIdAndDeletedFalse(5L)).thenReturn(Optional.of(service));
        when(serviceableAreaRepository.existsByPincodeAndActiveTrueAndDeletedFalse("110001")).thenReturn(true);
        when(currentPartnerLocationRepository.findNearbyPartnerIds(anyDouble(), anyDouble(), eq(1.0d), eq(200))).thenReturn(List.of(99L));
        when(partnerShiftRepository.findByPartnerIdInAndDeletedFalse(List.of(99L))).thenReturn(List.of(shift));
        when(partnerRepository.findAllById(List.of(99L))).thenReturn(List.of(partner));
        when(currentPartnerLocationRepository.findByPartnerId(99L)).thenReturn(Optional.of(location));
        when(dispatchScoringPolicy.isWithinDispatchRadius(any(), anyDouble())).thenReturn(true);
        when(dispatchScoringPolicy.comparator("110001")).thenReturn(Comparator.comparing(candidate -> 0));
        when(partnerServiceRequestRepository.findByCartId(11L)).thenReturn(List.of());
        when(partnerServiceRequestRepository.existsByCartIdAndPartnerIdAndStatus(11L, 99L, PartnerServiceRequestStatus.PENDING))
                .thenReturn(false);
        when(partnerShiftRepository.findByPartnerIdForUpdate(99L)).thenReturn(Optional.of(shift));
        when(partnerRepository.findById(99L)).thenReturn(Optional.of(partner));
        when(partnerServiceRequestRepository.save(any(PartnerServiceRequestEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

        int created = dispatchOrchestrationService.dispatchOrder(11L, 31L, 21L, 5L, 2, LocalDateTime.now().plusMinutes(5));

        assertEquals(1, created);
        assertEquals(OrderStatus.PARTNER_ASSIGNED, order.getStatus());
        verify(partnerServiceRequestRepository).save(argThat(request ->
                request.getPartnerId().equals(99L)
                        && request.getServiceId().equals(5L)
                        && request.getTargetPincode().equals("110001")));
        verify(bookingEventRepository).save(argThat(event -> "PARTNER_ASSIGNED".equals(event.getEventType())));
    }

    @Test
    void dispatchOrderShouldMoveCreatedInstantBookingIntoPartnerSearch() {
        Order order = baseOrder(12L, "INSTANT", OrderStatus.CREATED);
        LexhoAddress address = address("110001");
        AdminServiceEntity service = serviceEntity(5L, 2L);
        PartnerShiftEntity shift = new PartnerShiftEntity();
        shift.setPartnerId(99L);
        shift.setCurrentPincode("110001");
        shift.setOnline(true);
        shift.setActiveJobsCount(0);
        shift.setMaxActiveJobs(2);
        shift.setShiftStart(LocalDateTime.now().minusHours(1));
        shift.setShiftEnd(LocalDateTime.now().plusHours(1));
        Partner partner = new Partner();
        partner.setId(99L);
        partner.setAvailable(true);
        partner.setSubCategoryId(5L);
        partner.setVerificationStatus("APPROVED");
        com.lexho.user_service.dispatch.entity.CurrentPartnerLocationEntity location =
                new com.lexho.user_service.dispatch.entity.CurrentPartnerLocationEntity();
        location.setPartnerId(99L);
        location.setLatitude(java.math.BigDecimal.valueOf(28.4596));
        location.setLongitude(java.math.BigDecimal.valueOf(77.0267));

        when(orderRepository.findByIdForUpdate(12L)).thenReturn(Optional.of(order));
        when(lexhoAddressRepository.findByIdAndUserId(22L, 32L)).thenReturn(Optional.of(address));
        when(adminServiceEntityRepository.findByIdAndDeletedFalse(5L)).thenReturn(Optional.of(service));
        when(serviceableAreaRepository.existsByPincodeAndActiveTrueAndDeletedFalse("110001")).thenReturn(true);
        when(currentPartnerLocationRepository.findNearbyPartnerIds(anyDouble(), anyDouble(), eq(1.0d), eq(200))).thenReturn(List.of(99L));
        when(partnerShiftRepository.findByPartnerIdInAndDeletedFalse(List.of(99L))).thenReturn(List.of(shift));
        when(partnerRepository.findAllById(List.of(99L))).thenReturn(List.of(partner));
        when(currentPartnerLocationRepository.findByPartnerId(99L)).thenReturn(Optional.of(location));
        when(dispatchScoringPolicy.isWithinDispatchRadius(any(), anyDouble())).thenReturn(true);
        when(dispatchScoringPolicy.comparator("110001")).thenReturn(Comparator.comparing(candidate -> 0));
        when(partnerServiceRequestRepository.findByCartId(12L)).thenReturn(List.of());
        when(partnerServiceRequestRepository.existsByCartIdAndPartnerIdAndStatus(12L, 99L, PartnerServiceRequestStatus.PENDING))
                .thenReturn(false);
        when(partnerShiftRepository.findByPartnerIdForUpdate(99L)).thenReturn(Optional.of(shift));
        when(partnerRepository.findById(99L)).thenReturn(Optional.of(partner));
        when(partnerServiceRequestRepository.save(any(PartnerServiceRequestEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

        int created = dispatchOrchestrationService.dispatchOrder(12L, 32L, 22L, 5L, 1, LocalDateTime.now());

        assertEquals(1, created);
        assertEquals(OrderStatus.SEARCHING_PARTNER, order.getStatus());
        verify(partnerServiceRequestRepository).save(argThat(request ->
                request.getCartId().equals(12L)
                        && request.getPartnerId().equals(99L)
                        && request.getStatus() == PartnerServiceRequestStatus.PENDING));
        verify(bookingEventRepository).save(argThat(event -> "SEARCHING_PARTNER".equals(event.getEventType())));
        verify(bookingEventRepository).save(argThat(event -> "INSTANT_DISPATCHED".equals(event.getEventType())));
    }

    @Test
    void dispatchOrderShouldUseLegacyPartnerServiceAndStopWhenNoOnlinePartners() {
        Order order = baseOrder(15L, "INSTANT", OrderStatus.BOOKING_CREATED);
        LexhoAddress address = address(" 110001 ");
        Partner legacyPartner = new Partner();
        legacyPartner.setId(700L);
        legacyPartner.setSubCategoryId(5L);
        AdminServiceEntity service = serviceEntity(5L, 2L);

        when(orderRepository.findByIdForUpdate(15L)).thenReturn(Optional.of(order));
        when(lexhoAddressRepository.findByIdAndUserId(22L, 32L)).thenReturn(Optional.of(address));
        when(adminServiceEntityRepository.findByIdAndDeletedFalse(700L)).thenReturn(Optional.empty());
        when(partnerRepository.findById(700L)).thenReturn(Optional.of(legacyPartner));
        when(adminServiceEntityRepository.findByIdAndDeletedFalse(5L)).thenReturn(Optional.of(service));
        when(serviceableAreaRepository.existsByPincodeAndActiveTrueAndDeletedFalse("110001")).thenReturn(true);
        when(currentPartnerLocationRepository.findNearbyPartnerIds(anyDouble(), anyDouble(), eq(1.0d), eq(200))).thenReturn(List.of());
        when(currentPartnerLocationRepository.findNearbyPartnerIds(anyDouble(), anyDouble(), eq(2.0d), eq(200))).thenReturn(List.of());

        int created = dispatchOrchestrationService.dispatchOrder(15L, 32L, 22L, 700L, 1, null);

        assertEquals(0, created);
        verify(partnerRepository).findById(700L);
        verify(bookingEventRepository, atLeastOnce()).save(argThat(event -> "PARTNER_SEARCH_FAILED".equals(event.getEventType())));
    }

    @Test
    void dispatchOrderShouldRecordFailureWhenAllCandidatesAlreadyHavePendingRequests() {
        Order order = baseOrder(16L, "INSTANT", OrderStatus.BOOKING_CREATED);
        LexhoAddress address = address("110001");
        AdminServiceEntity service = serviceEntity(5L, 2L);
        PartnerShiftEntity shift = new PartnerShiftEntity();
        shift.setPartnerId(99L);
        shift.setCurrentPincode("110001");
        shift.setOnline(true);
        shift.setActiveJobsCount(0);
        shift.setMaxActiveJobs(2);
        shift.setShiftStart(LocalDateTime.now().minusHours(1));
        shift.setShiftEnd(LocalDateTime.now().plusHours(1));
        Partner partner = new Partner();
        partner.setId(99L);
        partner.setAvailable(true);
        partner.setSubCategoryId(5L);
        partner.setVerificationStatus("APPROVED");
        com.lexho.user_service.dispatch.entity.CurrentPartnerLocationEntity location =
                new com.lexho.user_service.dispatch.entity.CurrentPartnerLocationEntity();
        location.setPartnerId(99L);
        location.setLatitude(java.math.BigDecimal.valueOf(28.4596));
        location.setLongitude(java.math.BigDecimal.valueOf(77.0267));

        when(orderRepository.findByIdForUpdate(16L)).thenReturn(Optional.of(order));
        when(lexhoAddressRepository.findByIdAndUserId(23L, 33L)).thenReturn(Optional.of(address));
        when(adminServiceEntityRepository.findByIdAndDeletedFalse(5L)).thenReturn(Optional.of(service));
        when(serviceableAreaRepository.existsByPincodeAndActiveTrueAndDeletedFalse("110001")).thenReturn(true);
        when(currentPartnerLocationRepository.findNearbyPartnerIds(anyDouble(), anyDouble(), eq(1.0d), eq(200))).thenReturn(List.of(99L));
        when(partnerShiftRepository.findByPartnerIdInAndDeletedFalse(List.of(99L))).thenReturn(List.of(shift));
        when(partnerRepository.findAllById(List.of(99L))).thenReturn(List.of(partner));
        when(currentPartnerLocationRepository.findByPartnerId(99L)).thenReturn(Optional.of(location));
        when(dispatchScoringPolicy.isWithinDispatchRadius(any(), anyDouble())).thenReturn(true);
        when(dispatchScoringPolicy.comparator("110001")).thenReturn(Comparator.comparing(candidate -> 0));
        when(partnerServiceRequestRepository.findByCartId(16L)).thenReturn(List.of(new PartnerServiceRequestEntity()));
        when(partnerServiceRequestRepository.existsByCartIdAndPartnerIdAndStatus(16L, 99L, PartnerServiceRequestStatus.PENDING))
                .thenReturn(true);

        int created = dispatchOrchestrationService.dispatchOrder(16L, 33L, 23L, 5L, 1, null);

        assertEquals(0, created);
        verify(partnerServiceRequestRepository, never()).save(any(PartnerServiceRequestEntity.class));
        verify(bookingEventRepository, atLeastOnce()).save(argThat(event -> "PARTNER_SEARCH_FAILED".equals(event.getEventType())));
    }

    @Test
    void transitionOrderStatusShouldApplyLifecycleAndRejectInvalidTransition() {
        Order order = baseOrder(12L, "INSTANT", OrderStatus.PARTNER_ACCEPTED);
        order.setAssignedPartnerId(77L);
        when(orderRepository.findByIdForUpdate(12L)).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(trackingLifecycleService.deriveFromOrder(eq("INSTANT"), eq(OrderStatus.ARRIVED), any()))
                .thenReturn(com.lexho.user_service.dispatch.service.TrackingLifecycleState.ARRIVED);

        OrderStatus result = dispatchOrchestrationService.transitionOrderStatus(12L, OrderStatus.ARRIVED);

        assertEquals(OrderStatus.ARRIVED, result);
        assertNotNull(order.getArrivedAt());
        verify(trackingLifecycleService).persistState(77L,
                com.lexho.user_service.dispatch.service.TrackingLifecycleState.ARRIVED, 12L);

        when(orderRepository.findByIdForUpdate(13L)).thenReturn(Optional.of(baseOrder(13L, "INSTANT", OrderStatus.CANCELLED)));
        assertThrows(BadRequestException.class,
                () -> dispatchOrchestrationService.transitionOrderStatus(13L, OrderStatus.SERVICE_STARTED));
    }

    @Test
    void transitionOrderStatusShouldDefaultMissingCurrentStatusAndSetProgressTimestamp() {
        Order createdOrder = baseOrder(17L, "INSTANT", null);
        createdOrder.setAssignedPartnerId(88L);
        Order startedOrder = baseOrder(18L, "INSTANT", OrderStatus.SERVICE_STARTED);
        startedOrder.setAssignedPartnerId(89L);

        when(orderRepository.findByIdForUpdate(17L)).thenReturn(Optional.of(createdOrder));
        when(orderRepository.findByIdForUpdate(18L)).thenReturn(Optional.of(startedOrder));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(trackingLifecycleService.deriveFromOrder(eq("INSTANT"), eq(OrderStatus.PARTNER_SEARCH), any()))
                .thenReturn(com.lexho.user_service.dispatch.service.TrackingLifecycleState.EN_ROUTE);
        when(trackingLifecycleService.deriveFromOrder(eq("INSTANT"), eq(OrderStatus.SERVICE_IN_PROGRESS), any()))
                .thenReturn(com.lexho.user_service.dispatch.service.TrackingLifecycleState.JOB_IN_PROGRESS);

        OrderStatus searchResult = dispatchOrchestrationService.transitionOrderStatus(17L, OrderStatus.PARTNER_SEARCH);
        OrderStatus progressResult = dispatchOrchestrationService.transitionOrderStatus(18L, OrderStatus.SERVICE_IN_PROGRESS);

        assertEquals(OrderStatus.PARTNER_SEARCH, searchResult);
        assertEquals(OrderStatus.SERVICE_IN_PROGRESS, progressResult);
        assertNotNull(startedOrder.getInProgressAt());
        verify(trackingLifecycleService).persistState(88L,
                com.lexho.user_service.dispatch.service.TrackingLifecycleState.EN_ROUTE, 17L);
        verify(trackingLifecycleService).persistState(89L,
                com.lexho.user_service.dispatch.service.TrackingLifecycleState.JOB_IN_PROGRESS, 18L);
    }

    @Test
    void dispatchPaidInstantBookingShouldWaitForSuccessfulPrepayment() {
        Order order = baseOrder(20L, "INSTANT", OrderStatus.CREATED);
        order.setPaymentStatus(PaymentStatus.PENDING);
        when(orderRepository.findByIdForUpdate(20L)).thenReturn(Optional.of(order));

        int created = dispatchOrchestrationService.dispatchPaidInstantBooking(20L);

        assertEquals(0, created);
        assertEquals(OrderStatus.CREATED, order.getStatus());
        verify(bookingEventRepository).save(argThat(event -> "DISPATCH_PAYMENT_PENDING".equals(event.getEventType())));
        verify(orderRepository, never()).save(any(Order.class));
    }

    @Test
    void dispatchPaidInstantBookingShouldBeIdempotentAfterDispatchStarted() {
        Order order = baseOrder(21L, "INSTANT", OrderStatus.SEARCHING_PARTNER);
        order.setPaymentStatus(PaymentStatus.SUCCESS);
        order.setDispatchStartedAt(LocalDateTime.now().minusMinutes(1));
        when(orderRepository.findByIdForUpdate(21L)).thenReturn(Optional.of(order));

        int created = dispatchOrchestrationService.dispatchPaidInstantBooking(21L);

        assertEquals(0, created);
        assertEquals(OrderStatus.SEARCHING_PARTNER, order.getStatus());
        verify(bookingEventRepository).save(argThat(event -> "DISPATCH_ALREADY_STARTED".equals(event.getEventType())));
        verify(orderRepository, never()).save(any(Order.class));
    }

    @Test
    void dispatchPaidScheduledBookingShouldRequirePaymentBeforeSearch() {
        Order order = baseOrder(23L, "SCHEDULED", OrderStatus.SCHEDULED);
        order.setPaymentStatus(PaymentStatus.PENDING);
        order.setScheduledAt(LocalDateTime.now().plusMinutes(5));
        when(orderRepository.findByIdForUpdate(23L)).thenReturn(Optional.of(order));

        int created = dispatchOrchestrationService.dispatchPaidScheduledBooking(23L);

        assertEquals(0, created);
        assertEquals(OrderStatus.SCHEDULED, order.getStatus());
        verify(bookingEventRepository).save(argThat(event ->
                "DISPATCH_PAYMENT_PENDING".equals(event.getEventType())));
        verify(orderRepository, never()).save(any(Order.class));
    }

    @Test
    void dispatchPaidScheduledBookingShouldStartInsidePreDispatchWindow() {
        Order order = baseOrder(24L, "SCHEDULED", OrderStatus.SCHEDULED);
        order.setUserId(32L);
        order.setDispatchAddressId(22L);
        order.setDispatchServiceId(5L);
        order.setScheduledAt(LocalDateTime.now().plusMinutes(5));
        LexhoAddress address = address("110001");
        AdminServiceEntity service = serviceEntity(5L, 2L);
        when(orderRepository.findByIdForUpdate(24L)).thenReturn(Optional.of(order));
        when(lexhoAddressRepository.findByIdAndUserId(22L, 32L)).thenReturn(Optional.of(address));
        when(adminServiceEntityRepository.findByIdAndDeletedFalse(5L)).thenReturn(Optional.of(service));
        when(serviceableAreaRepository.existsByPincodeAndActiveTrueAndDeletedFalse("110001")).thenReturn(true);
        when(currentPartnerLocationRepository.findNearbyPartnerIds(anyDouble(), anyDouble(), anyDouble(), anyInt()))
                .thenReturn(List.of());
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

        int created = dispatchOrchestrationService.dispatchPaidScheduledBooking(24L);

        assertEquals(0, created);
        assertNull(order.getDispatchStartedAt());
        assertEquals(OrderStatus.PARTNER_SEARCH, order.getStatus());
        verify(bookingEventRepository, atLeastOnce()).save(argThat(event ->
                "PARTNER_SEARCH_FAILED".equals(event.getEventType())));
        verify(bookingEventRepository).save(argThat(event ->
                "SCHEDULED_DISPATCH_RETRY_SCHEDULED".equals(event.getEventType())));
    }

    @Test
    void dispatchPaidScheduledBookingShouldStopAfterMaximumAttempts() {
        Order order = baseOrder(25L, "SCHEDULED", OrderStatus.PARTNER_SEARCH);
        order.setScheduledAt(LocalDateTime.now().plusMinutes(5));
        when(orderRepository.findByIdForUpdate(25L)).thenReturn(Optional.of(order));
        when(bookingEventRepository.countByBookingIdAndEventType(25L, "SCHEDULED_DISPATCH_ATTEMPT"))
                .thenReturn((long) DispatchOrchestrationService.MAX_SCHEDULED_DISPATCH_ATTEMPTS);
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

        int created = dispatchOrchestrationService.dispatchPaidScheduledBooking(25L);

        assertEquals(0, created);
        assertEquals("NO_PARTNER_AVAILABLE_MAX_ATTEMPTS", order.getUnavailableReason());
        verify(bookingEventRepository).save(argThat(event ->
                "SCHEDULED_DISPATCH_EXHAUSTED".equals(event.getEventType())));
    }

    @Test
    void dispatchPaidScheduledBookingShouldMarkMissingMetadataUnavailable() {
        Order order = baseOrder(26L, "SCHEDULED", OrderStatus.SCHEDULED);
        order.setScheduledAt(LocalDateTime.now().plusMinutes(5));
        when(orderRepository.findByIdForUpdate(26L)).thenReturn(Optional.of(order));
        when(bookingEventRepository.countByBookingIdAndEventType(26L, "SCHEDULED_DISPATCH_ATTEMPT"))
                .thenReturn(0L);
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

        int created = dispatchOrchestrationService.dispatchPaidScheduledBooking(26L);

        assertEquals(0, created);
        assertEquals("DISPATCH_METADATA_MISSING", order.getUnavailableReason());
        verify(orderRepository).save(order);
        verify(bookingEventRepository).save(argThat(event ->
                "PARTNER_SEARCH_FAILED".equals(event.getEventType())));
        verify(lexhoAddressRepository, never()).findByIdAndUserId(anyLong(), anyLong());
    }

    @Test
    void handlePaymentFailedShouldNotRewindTerminalLifecycle() {
        Order order = baseOrder(22L, "INSTANT", OrderStatus.SERVICE_STARTED);
        order.setAssignedPartnerId(99L);
        when(orderRepository.findByIdForUpdate(22L)).thenReturn(Optional.of(order));
        when(partnerServiceRequestRepository.findByCartIdAndStatus(22L, PartnerServiceRequestStatus.PENDING))
                .thenReturn(List.of());

        dispatchOrchestrationService.handlePaymentFailed(22L, "FAILED");

        assertEquals(OrderStatus.SERVICE_STARTED, order.getStatus());
        assertEquals(99L, order.getAssignedPartnerId());
        verify(orderRepository, never()).save(any(Order.class));
    }

    @Test
    void helperMethodsShouldCoverEtaSerializationAndScheduledAcceptance() throws Exception {
        assertEquals(16, dispatchOrchestrationService.estimateEtaMinutes(null));
        assertEquals(1, dispatchOrchestrationService.estimateEtaMinutes(java.math.BigDecimal.ZERO));

        Order scheduled = baseOrder(14L, "SCHEDULED", OrderStatus.CREATED);
        Partner partner = new Partner();
        partner.setId(55L);
        partner.setAvailable(true);
        partner.setVerificationStatus("APPROVED");
        PartnerShiftEntity shift = new PartnerShiftEntity();
        shift.setPartnerId(55L);
        shift.setOnline(true);
        shift.setActiveJobsCount(0);
        shift.setMaxActiveJobs(3);
        shift.setShiftStart(LocalDateTime.now().minusHours(1));
        shift.setShiftEnd(LocalDateTime.now().plusHours(1));
        when(orderRepository.findByIdForUpdate(14L)).thenReturn(Optional.of(scheduled));
        when(partnerRepository.findById(55L)).thenReturn(Optional.of(partner));
        when(partnerShiftRepository.findByPartnerIdForUpdate(55L)).thenReturn(Optional.of(shift));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

        dispatchOrchestrationService.markPartnerAccepted(14L, 55L, 20);

        assertEquals(OrderStatus.PARTNER_ACCEPTED, scheduled.getStatus());
        assertEquals(55L, scheduled.getAssignedPartnerId());

        when(objectMapper.writeValueAsString(any(Map.class)))
                .thenThrow(new com.fasterxml.jackson.core.JsonProcessingException("boom") {});
        dispatchOrchestrationService.saveEvent(99L, "TEST", Map.of("a", 1));
        verify(bookingEventRepository).save(argThat(event ->
                "TEST".equals(event.getEventType())
                        && "{\"error\":\"SERIALIZATION_FAILED\"}".equals(event.getEventPayload())));
    }

    @Test
    void markPartnerRejectedShouldPersistEventOnly() {
        dispatchOrchestrationService.markPartnerRejected(19L, 109L);

        verify(bookingEventRepository).save(argThat(event ->
                event.getBookingId().equals(19L)
                        && "PARTNER_REJECTED".equals(event.getEventType())));
    }

    private static Order baseOrder(Long id, String bookingType, OrderStatus status) {
        Order order = new Order();
        order.setId(id);
        order.setBookingType(bookingType);
        order.setStatus(status);
        order.setPaymentStatus(PaymentStatus.SUCCESS);
        order.setAssignedPartnerId(null);
        return order;
    }

    private static LexhoAddress address(String pincode) {
        LexhoAddress address = new LexhoAddress();
        address.setHouseNo("21");
        address.setArea("Sector 44");
        address.setCity("Gurugram");
        address.setPincode(pincode);
        address.setLatitude(java.math.BigDecimal.valueOf(28.4595));
        address.setLongitude(java.math.BigDecimal.valueOf(77.0266));
        return address;
    }

    private static AdminServiceEntity serviceEntity(Long serviceId, Long categoryId) {
        AdminCategoryEntity category = new AdminCategoryEntity();
        org.springframework.test.util.ReflectionTestUtils.setField(category, "id", categoryId);
        AdminServiceEntity service = new AdminServiceEntity();
        org.springframework.test.util.ReflectionTestUtils.setField(service, "id", serviceId);
        service.setCategory(category);
        return service;
    }

    private static DispatchProperties dispatchProperties() {
        DispatchProperties properties = new DispatchProperties();
        properties.getPartner().setInitialSearchRadiusKm(1.0d);
        properties.getPartner().setMaxSearchRadiusKm(2.0d);
        properties.getBooking().setMaxEtaMinutes(15);
        properties.getBooking().setPreDispatchMinutes(30);
        return properties;
    }
}

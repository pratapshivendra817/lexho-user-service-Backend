package com.lexho.user_service.subscription.service;

import com.lexho.booking.repository.OrderRepository;
import com.lexho.user_service.admin.geo.service.GeoFenceValidationService;
import com.lexho.booking.entity.Order;
import com.lexho.user_service.admin.entity.AdminServiceEntity;
import com.lexho.user_service.admin.repository.AdminServiceEntityRepository;
import com.lexho.user_service.audit.service.AuditLogService;
import com.lexho.user_service.exception.ResourceNotFoundException;
import com.lexho.user_service.lexho.entity.LexhoAddress;
import com.lexho.user_service.lexho.repository.LexhoAddressRepository;
import com.lexho.user_service.subscription.dto.CreateSubscriptionRequest;
import com.lexho.user_service.subscription.dto.RescheduleVisitRequest;
import com.lexho.user_service.subscription.entity.SubscriptionFrequency;
import com.lexho.user_service.subscription.entity.SubscriptionMaster;
import com.lexho.user_service.subscription.entity.SubscriptionVisit;
import com.lexho.user_service.subscription.enums.SubscriptionStatus;
import com.lexho.user_service.subscription.enums.VisitStatus;
import com.lexho.user_service.subscription.exception.SubscriptionValidationException;
import com.lexho.user_service.subscription.repository.SubscriptionFrequencyRepository;
import com.lexho.user_service.subscription.repository.SubscriptionMasterRepository;
import com.lexho.user_service.subscription.repository.SubscriptionVisitRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
class SubscriptionBookingServiceTest {

    @Mock
    private SubscriptionMasterRepository masterRepository;
    @Mock
    private SubscriptionFrequencyRepository frequencyRepository;
    @Mock
    private SubscriptionVisitRepository visitRepository;
    @Mock
    private AdminServiceEntityRepository serviceRepository;
    @Mock
    private LexhoAddressRepository addressRepository;
    @Mock
    private SubscriptionPricingService pricingService;
    @Mock
    private OrderRepository orderRepository;
    @Mock
    private AuditLogService auditLogService;
    @Mock
    private GeoFenceValidationService geoFenceValidationService;

    private SubscriptionBookingService service;

    @BeforeEach
    void setUp() {
        service = new SubscriptionBookingService(masterRepository, frequencyRepository, visitRepository,
                serviceRepository, addressRepository, pricingService, orderRepository, geoFenceValidationService);
        ReflectionTestUtils.setField(service, "auditLogService", auditLogService);
        lenient().when(pricingService.familySizeLabel(null)).thenReturn(null);
    }

    @Test
    void createRejectsInvalidDatesBlankSlotAndEmptyDaysTogether() {
        CreateSubscriptionRequest request = request(List.of(), LocalDate.now().plusDays(2),
                LocalDate.now(), " ", BigDecimal.ONE, null, null);

        assertThatThrownBy(() -> service.create(7L, request))
                .isInstanceOf(SubscriptionValidationException.class)
                .satisfies(ex -> assertThat(((SubscriptionValidationException) ex).getErrors())
                        .containsExactlyInAnyOrder(
                                "startDate must not be after endDate",
                                "timeSlot cannot be blank",
                                "frequencyDays cannot be empty"));
        verify(serviceRepository, never()).findByIdAndDeletedFalse(any());
    }

    @Test
    void createRejectsNullAndInvalidFrequencyDays() {
        CreateSubscriptionRequest nullDays = request(null, LocalDate.now(), LocalDate.now(), "10:00",
                BigDecimal.ONE, null, null);
        assertThatThrownBy(() -> service.create(7L, nullDays))
                .isInstanceOf(SubscriptionValidationException.class)
                .hasMessage("Validation failed");

        CreateSubscriptionRequest invalidDay = request(List.of("Funday"), LocalDate.now(),
                LocalDate.now().plusDays(2), "10:00", BigDecimal.ONE, null, null);
        assertThatThrownBy(() -> service.create(7L, invalidDay))
                .isInstanceOf(SubscriptionValidationException.class)
                .hasMessageContaining("Invalid frequency day");
    }

    @Test
    void createRejectsMissingInactiveAndNonSubscriptionServices() {
        CreateSubscriptionRequest request = request(List.of("MONDAY"), LocalDate.now(),
                LocalDate.now().plusDays(7), "10:00", BigDecimal.ONE, null, null);
        when(serviceRepository.findByIdAndDeletedFalse(2L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.create(7L, request))
                .isInstanceOf(SubscriptionValidationException.class)
                .hasMessage("Service not found");

        AdminServiceEntity inactive = adminService(false, "SUBSCRIPTION", "Cleaning");
        when(serviceRepository.findByIdAndDeletedFalse(2L)).thenReturn(Optional.of(inactive));
        assertThatThrownBy(() -> service.create(7L, request))
                .isInstanceOf(SubscriptionValidationException.class)
                .hasMessageContaining("does not support");

        AdminServiceEntity wrongMode = adminService(true, "ON_DEMAND", "Cleaning");
        when(serviceRepository.findByIdAndDeletedFalse(2L)).thenReturn(Optional.of(wrongMode));
        assertThatThrownBy(() -> service.create(7L, request))
                .isInstanceOf(SubscriptionValidationException.class)
                .hasMessageContaining("does not support");
    }

    @Test
    void createRejectsAddressNotOwnedByCustomer() {
        CreateSubscriptionRequest request = request(List.of("MONDAY"), LocalDate.now(),
                LocalDate.now().plusDays(7), "10:00", BigDecimal.ONE, null, null);
        when(serviceRepository.findByIdAndDeletedFalse(2L))
                .thenReturn(Optional.of(adminService(true, "SUBSCRIPTION", "Cleaning")));
        when(addressRepository.findByIdAndUserIdAndIsDeletedFalse(3L, 7L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.create(7L, request))
                .isInstanceOf(SubscriptionValidationException.class)
                .hasMessageContaining("does not belong");
    }

    @Test
    void createRejectsAddressOutsideServiceableArea() {
        CreateSubscriptionRequest request = request(List.of("MONDAY"), LocalDate.now().plusDays(1),
                LocalDate.now().plusDays(7), "10:00", BigDecimal.ONE, null, null);
        LexhoAddress address = new LexhoAddress();
        address.setLatitude(BigDecimal.valueOf(28.4595));
        address.setLongitude(BigDecimal.valueOf(77.0266));
        when(serviceRepository.findByIdAndDeletedFalse(2L))
                .thenReturn(Optional.of(adminService(true, "SUBSCRIPTION", "Cleaning")));
        when(addressRepository.findByIdAndUserIdAndIsDeletedFalse(3L, 7L)).thenReturn(Optional.of(address));
        doThrow(new RuntimeException("Selected address is outside supported service area"))
                .when(geoFenceValidationService).validateBookingAddress(address);

        assertThatThrownBy(() -> service.create(7L, request))
                .isInstanceOf(SubscriptionValidationException.class)
                .hasMessage("Selected address is outside supported service area");
    }

    @Test
    void activationOnlyPersistsPendingSubscription() {
        SubscriptionMaster pending = master(1L, SubscriptionStatus.PENDING);
        when(masterRepository.findByBookingId(90L)).thenReturn(Optional.of(pending));

        service.activateByBookingId(90L);

        assertThat(pending.getStatus()).isEqualTo(SubscriptionStatus.ACTIVE);
        verify(masterRepository).save(pending);

        SubscriptionMaster active = master(2L, SubscriptionStatus.ACTIVE);
        when(masterRepository.findByBookingId(91L)).thenReturn(Optional.of(active));
        service.activateByBookingId(91L);
        service.activateByBookingId(92L);
        verify(masterRepository, never()).save(active);
    }

    @Test
    void listReturnsEmptyAndAggregatesCompletedVisits() {
        when(masterRepository.findByCustomerIdOrderByIdDesc(7L))
                .thenReturn(List.of(), List.of(master(1L, SubscriptionStatus.ACTIVE)));
        assertThat(service.list(7L)).isEmpty();

        SubscriptionVisit completed = visit(11L, VisitStatus.COMPLETED, LocalDate.now().minusDays(1));
        SubscriptionVisit future = visit(12L, VisitStatus.SCHEDULED, LocalDate.now().plusDays(1));
        when(visitRepository.findBySubscriptionIdInOrderBySubscriptionIdAscVisitDateAscIdAsc(List.of(1L)))
                .thenReturn(List.of(completed, future));
        when(serviceRepository.findAllById(any())).thenReturn(List.of(adminService(true, "SUBSCRIPTION", "Home Care")));

        var result = service.list(7L);

        assertThat(result).singleElement().satisfies(item -> {
            assertThat(item.serviceName()).isEqualTo("Home Care");
            assertThat(item.completedVisits()).isEqualTo(1);
            assertThat(item.remainingVisits()).isEqualTo(2);
        });
    }

    @Test
    void detailCalendarCardsAndVisitsMapAllStatusGroups() {
        SubscriptionMaster master = master(1L, SubscriptionStatus.ACTIVE);
        when(masterRepository.findByIdAndCustomerId(1L, 7L)).thenReturn(Optional.of(master));
        SubscriptionFrequency frequency = new SubscriptionFrequency();
        frequency.setDayName("Monday");
        when(frequencyRepository.findBySubscriptionIdOrderByIdAsc(1L)).thenReturn(List.of(frequency));
        List<SubscriptionVisit> visits = List.of(
                visit(11L, VisitStatus.SCHEDULED, LocalDate.now().plusDays(1)),
                visit(12L, VisitStatus.RESCHEDULED, LocalDate.now().plusDays(2)),
                visit(13L, VisitStatus.COMPLETED, LocalDate.now().minusDays(1)),
                visit(14L, VisitStatus.CANCELLED, LocalDate.now().plusDays(3)),
                visit(15L, VisitStatus.SKIPPED, LocalDate.now().plusDays(4)));
        when(visitRepository.findBySubscriptionIdOrderByVisitDateAscIdAsc(1L)).thenReturn(visits);
        when(serviceRepository.findById(5L)).thenReturn(Optional.empty());
        when(pricingService.familySizeLabel(null)).thenReturn(null);

        assertThat(service.detail(7L, 1L).serviceName()).isEmpty();
        assertThat(service.calendar(7L, 1L)).hasSize(5);
        Map<String, Object> grouped = service.visits(7L, 1L);
        assertThat((List<?>) grouped.get("upcomingVisits")).hasSize(2);
        assertThat((List<?>) grouped.get("completedVisits")).hasSize(1);
        assertThat((List<?>) grouped.get("cancelledVisits")).hasSize(2);
        assertThat(service.subscriptionCard(7L, 1L))
                .containsEntry("planName", "Daily Cleaning")
                .containsEntry("canPause", true)
                .containsEntry("canResume", false);
    }

    @Test
    void cancelRejectsTerminalStatesAndCancelsActiveSubscriptionEvenWhenAuditFails() {
        SubscriptionMaster cancelled = master(1L, SubscriptionStatus.CANCELLED);
        when(masterRepository.findByIdAndCustomerId(1L, 7L)).thenReturn(Optional.of(cancelled));
        assertThatThrownBy(() -> service.cancel(7L, 1L))
                .isInstanceOf(SubscriptionValidationException.class);

        SubscriptionMaster active = master(2L, SubscriptionStatus.ACTIVE);
        when(masterRepository.findByIdAndCustomerId(2L, 7L)).thenReturn(Optional.of(active));
        doThrow(new IllegalStateException("audit unavailable")).when(auditLogService)
                .log(7L, null, 102L, "SUBSCRIPTION_CANCELLED", "ACTIVE", "CANCELLED");

        service.cancel(7L, 2L);

        assertThat(active.getStatus()).isEqualTo(SubscriptionStatus.CANCELLED);
        verify(visitRepository).cancelFutureScheduled(2L, VisitStatus.SCHEDULED,
                VisitStatus.CANCELLED, LocalDate.now());
    }

    @Test
    void pauseAndResumeEnforceStateTransitions() {
        SubscriptionMaster pending = master(1L, SubscriptionStatus.PENDING);
        when(masterRepository.findByIdAndCustomerId(1L, 7L)).thenReturn(Optional.of(pending));
        assertThatThrownBy(() -> service.pause(7L, 1L))
                .isInstanceOf(SubscriptionValidationException.class)
                .hasMessageContaining("ACTIVE");

        SubscriptionMaster active = master(2L, SubscriptionStatus.ACTIVE);
        when(masterRepository.findByIdAndCustomerId(2L, 7L)).thenReturn(Optional.of(active));
        when(masterRepository.save(active)).thenReturn(active);
        when(serviceRepository.findById(5L)).thenReturn(Optional.empty());
        when(frequencyRepository.findBySubscriptionIdOrderByIdAsc(2L)).thenReturn(List.of());
        when(visitRepository.findBySubscriptionIdOrderByVisitDateAscIdAsc(2L)).thenReturn(List.of());
        assertThat(service.pause(7L, 2L)).containsEntry("status", "PAUSED");

        when(masterRepository.findByIdAndCustomerId(2L, 7L)).thenReturn(Optional.of(active));
        when(masterRepository.save(active)).thenReturn(active);
        assertThat(service.resume(7L, 2L)).containsEntry("status", "ACTIVE");
        assertThatThrownBy(() -> service.resume(7L, 2L))
                .isInstanceOf(SubscriptionValidationException.class)
                .hasMessageContaining("PAUSED");
    }

    @Test
    void skipVisitCoversMissingPastWrongStateAndSuccess() {
        when(masterRepository.findByIdAndCustomerId(1L, 7L))
                .thenReturn(Optional.of(master(1L, SubscriptionStatus.ACTIVE)));
        when(visitRepository.findByIdAndSubscriptionId(99L, 1L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.skipVisit(7L, 1L, 99L))
                .isInstanceOf(ResourceNotFoundException.class);

        SubscriptionVisit past = visit(11L, VisitStatus.SCHEDULED, LocalDate.now());
        when(visitRepository.findByIdAndSubscriptionId(11L, 1L)).thenReturn(Optional.of(past));
        assertThatThrownBy(() -> service.skipVisit(7L, 1L, 11L))
                .isInstanceOf(SubscriptionValidationException.class)
                .hasMessageContaining("future");

        SubscriptionVisit completed = visit(12L, VisitStatus.COMPLETED, LocalDate.now().plusDays(1));
        when(visitRepository.findByIdAndSubscriptionId(12L, 1L)).thenReturn(Optional.of(completed));
        assertThatThrownBy(() -> service.skipVisit(7L, 1L, 12L))
                .isInstanceOf(SubscriptionValidationException.class)
                .hasMessageContaining("SCHEDULED");

        SubscriptionVisit scheduled = visit(13L, VisitStatus.SCHEDULED, LocalDate.now().plusDays(1));
        when(visitRepository.findByIdAndSubscriptionId(13L, 1L)).thenReturn(Optional.of(scheduled));
        service.skipVisit(7L, 1L, 13L);
        assertThat(scheduled.getStatus()).isEqualTo(VisitStatus.SKIPPED);
        verify(visitRepository).save(scheduled);
    }

    @Test
    void rescheduleVisitCoversDateStateDuplicateAndSuccess() {
        when(masterRepository.findByIdAndCustomerId(1L, 7L))
                .thenReturn(Optional.of(master(1L, SubscriptionStatus.ACTIVE)));
        SubscriptionVisit visit = visit(11L, VisitStatus.SCHEDULED, LocalDate.now().plusDays(1));
        when(visitRepository.findByIdAndSubscriptionId(11L, 1L)).thenReturn(Optional.of(visit));

        assertThatThrownBy(() -> service.rescheduleVisit(7L, 1L, 11L,
                new RescheduleVisitRequest(LocalDate.now())))
                .isInstanceOf(SubscriptionValidationException.class)
                .hasMessageContaining("future");

        visit.setStatus(VisitStatus.COMPLETED);
        assertThatThrownBy(() -> service.rescheduleVisit(7L, 1L, 11L,
                new RescheduleVisitRequest(LocalDate.now().plusDays(3))))
                .isInstanceOf(SubscriptionValidationException.class)
                .hasMessageContaining("SCHEDULED");

        visit.setStatus(VisitStatus.SCHEDULED);
        LocalDate newDate = LocalDate.now().plusDays(3);
        when(visitRepository.existsBySubscriptionIdAndVisitDateAndIdNot(1L, newDate, 11L))
                .thenReturn(true, false);
        assertThatThrownBy(() -> service.rescheduleVisit(7L, 1L, 11L,
                new RescheduleVisitRequest(newDate)))
                .isInstanceOf(SubscriptionValidationException.class)
                .hasMessageContaining("already exists");

        service.rescheduleVisit(7L, 1L, 11L, new RescheduleVisitRequest(newDate));
        assertThat(visit.getVisitDate()).isEqualTo(newDate);
        assertThat(visit.getRescheduledDate()).isEqualTo(newDate);
        assertThat(visit.getStatus()).isEqualTo(VisitStatus.RESCHEDULED);
    }

    @Test
    void createCleaningSubscriptionShouldPersistMasterVisitsAndPaymentBooking() {
        CreateSubscriptionRequest request = request(List.of("monday", "wednesday"),
                LocalDate.of(2026, 6, 22), LocalDate.of(2026, 6, 29), "10:00", BigDecimal.valueOf(2), null, "CLEANING");

        AdminServiceEntity adminService = adminService(true, "SUBSCRIPTION", "Cleaning");
        ReflectionTestUtils.setField(adminService, "id", 2L);
        LexhoAddress address = new LexhoAddress();
        address.setHouseNo("21");
        address.setArea("Sector 44");
        address.setCity("Gurugram");
        address.setState("Haryana");
        address.setPincode("122003");
        when(serviceRepository.findByIdAndDeletedFalse(2L)).thenReturn(Optional.of(adminService));
        when(addressRepository.findByIdAndUserIdAndIsDeletedFalse(3L, 7L)).thenReturn(Optional.of(address));
        when(pricingService.resolvePerVisitPrice(2L, BigDecimal.valueOf(2))).thenReturn(BigDecimal.valueOf(150));
        when(pricingService.familySizeLabel(null)).thenReturn(null);
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> {
            Order saved = invocation.getArgument(0);
            saved.setId(700L);
            return saved;
        });
        when(masterRepository.save(any())).thenAnswer(invocation -> {
            SubscriptionMaster master = invocation.getArgument(0);
            ReflectionTestUtils.setField(master, "id", 900L);
            return master;
        });
        when(frequencyRepository.saveAll(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(visitRepository.saveAll(any())).thenAnswer(invocation -> invocation.getArgument(0));

        var response = service.create(7L, request);

        assertThat(response.id()).isEqualTo(900L);
        assertThat(response.bookingId()).isEqualTo(700L);
        assertThat(response.serviceType()).isEqualTo("CLEANING");
        assertThat(response.visits()).isEqualTo(3);
        assertThat(response.price()).isEqualByComparingTo("450");
        assertThat(response.calendar()).hasSize(3);
        verify(orderRepository).save(any(Order.class));
        verify(frequencyRepository).saveAll(any());
        verify(visitRepository).saveAll(any());
    }

    @Test
    void createCookSubscriptionAndCardsShouldUseFamilySizeAndPlanFallbacks() {
        CreateSubscriptionRequest request = request(List.of("FRIDAY"),
                LocalDate.of(2026, 6, 26), LocalDate.of(2026, 6, 26), "18:00", null,
                com.lexho.user_service.subscription.enums.FamilySize.LARGE, "COOK");

        AdminServiceEntity cookService = adminService(true, "SUBSCRIPTION", "Cook Service");
        ReflectionTestUtils.setField(cookService, "id", 2L);
        LexhoAddress address = new LexhoAddress();
        address.setHouseNo("11");
        address.setArea("DLF");
        address.setCity("Gurugram");
        address.setState("Haryana");
        address.setPincode("122002");
        when(serviceRepository.findByIdAndDeletedFalse(2L)).thenReturn(Optional.of(cookService));
        when(addressRepository.findByIdAndUserIdAndIsDeletedFalse(3L, 7L)).thenReturn(Optional.of(address));
        when(pricingService.resolveCookPricePerVisit(2L, com.lexho.user_service.subscription.enums.FamilySize.LARGE))
                .thenReturn(BigDecimal.valueOf(300));
        when(pricingService.familySizeLabel(com.lexho.user_service.subscription.enums.FamilySize.LARGE))
                .thenReturn("Large family");
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> {
            Order saved = invocation.getArgument(0);
            saved.setId(701L);
            return saved;
        });
        when(masterRepository.save(any())).thenAnswer(invocation -> {
            SubscriptionMaster master = invocation.getArgument(0);
            ReflectionTestUtils.setField(master, "id", 901L);
            return master;
        });
        when(frequencyRepository.saveAll(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(visitRepository.saveAll(any())).thenAnswer(invocation -> invocation.getArgument(0));

        var response = service.create(7L, request);
        assertThat(response.familySize()).isEqualTo("LARGE");
        assertThat(response.familySizeLabel()).isEqualTo("Large family");

        SubscriptionMaster active = master(3L, SubscriptionStatus.ACTIVE);
        active.setServiceType("COOK");
        when(masterRepository.findByCustomerIdOrderByIdDesc(7L)).thenReturn(List.of(active));
        when(serviceRepository.findAllById(any())).thenReturn(List.of());
        when(frequencyRepository.findBySubscriptionIdOrderByIdAsc(3L)).thenReturn(List.of());
        when(visitRepository.findBySubscriptionIdOrderByVisitDateAscIdAsc(3L)).thenReturn(List.of());

        assertThat(service.subscriptionCards(7L)).singleElement()
                .extracting(item -> item.get("planName"))
                .isEqualTo("Daily Cook");
    }

    private CreateSubscriptionRequest request(List<String> days, LocalDate start, LocalDate end,
                                              String slot, BigDecimal duration,
                                              com.lexho.user_service.subscription.enums.FamilySize familySize,
                                              String serviceType) {
        return new CreateSubscriptionRequest(2L, 3L, null, serviceType, days, start, end,
                null, duration, familySize, slot, null);
    }

    private SubscriptionMaster master(Long id, SubscriptionStatus status) {
        SubscriptionMaster master = new SubscriptionMaster();
        ReflectionTestUtils.setField(master, "id", id);
        master.setCustomerId(7L);
        master.setServiceId(5L);
        master.setAddressId(6L);
        master.setBookingId(100L + id);
        master.setBookingMode("SUBSCRIPTION");
        master.setServiceType("CLEANING");
        master.setStartDate(LocalDate.now().minusDays(2));
        master.setEndDate(LocalDate.now().plusDays(20));
        master.setVisits(3);
        master.setDurationHours(BigDecimal.ONE);
        master.setPerVisitPrice(BigDecimal.TEN);
        master.setTimeSlot("10:00");
        master.setPrice(BigDecimal.valueOf(30));
        master.setStatus(status);
        return master;
    }

    private SubscriptionVisit visit(Long id, VisitStatus status, LocalDate date) {
        SubscriptionVisit visit = new SubscriptionVisit();
        ReflectionTestUtils.setField(visit, "id", id);
        visit.setSubscriptionId(1L);
        visit.setVisitDate(date);
        visit.setTimeSlot("10:00");
        visit.setStatus(status);
        return visit;
    }

    private AdminServiceEntity adminService(boolean active, String type, String name) {
        AdminServiceEntity service = new AdminServiceEntity();
        ReflectionTestUtils.setField(service, "id", 5L);
        service.setActive(active);
        service.setServiceType(type);
        service.setName(name);
        return service;
    }
}

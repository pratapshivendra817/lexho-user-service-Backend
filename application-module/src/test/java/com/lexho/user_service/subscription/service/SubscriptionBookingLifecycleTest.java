package com.lexho.user_service.subscription.service;

import com.lexho.booking.repository.OrderRepository;
import com.lexho.user_service.admin.geo.service.GeoFenceValidationService;
import com.lexho.user_service.admin.repository.AdminServiceEntityRepository;
import com.lexho.user_service.exception.ResourceNotFoundException;
import com.lexho.user_service.lexho.repository.LexhoAddressRepository;
import com.lexho.user_service.subscription.dto.RescheduleVisitRequest;
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
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SubscriptionBookingLifecycleTest {
    @Mock SubscriptionMasterRepository masterRepository;
    @Mock SubscriptionFrequencyRepository frequencyRepository;
    @Mock SubscriptionVisitRepository visitRepository;
    @Mock AdminServiceEntityRepository serviceRepository;
    @Mock LexhoAddressRepository addressRepository;
    @Mock SubscriptionPricingService pricingService;
    @Mock OrderRepository orderRepository;
    @Mock GeoFenceValidationService geoFenceValidationService;
    private SubscriptionBookingService service;

    @BeforeEach
    void setUp() {
        service = new SubscriptionBookingService(masterRepository, frequencyRepository, visitRepository,
                serviceRepository, addressRepository, pricingService, orderRepository, geoFenceValidationService);
    }

    @Test
    void listAndCardsReturnEmptyWithoutSecondaryQueries() {
        when(masterRepository.findByCustomerIdOrderByIdDesc(7L)).thenReturn(List.of());
        assertTrue(service.list(7L).isEmpty());
        assertTrue(service.subscriptionCards(7L).isEmpty());
        verifyNoInteractions(visitRepository, serviceRepository);
    }

    @Test
    void activateChangesOnlyPendingSubscription() {
        SubscriptionMaster pending = master(1L, 7L, SubscriptionStatus.PENDING);
        when(masterRepository.findByBookingId(99L)).thenReturn(Optional.of(pending));
        service.activateByBookingId(99L);
        assertEquals(SubscriptionStatus.ACTIVE, pending.getStatus());
        verify(masterRepository).save(pending);

        SubscriptionMaster active = master(2L, 7L, SubscriptionStatus.ACTIVE);
        when(masterRepository.findByBookingId(100L)).thenReturn(Optional.of(active));
        service.activateByBookingId(100L);
        verify(masterRepository, never()).save(active);
    }

    @Test
    void ownershipFailureIsReported() {
        when(masterRepository.findByIdAndCustomerId(1L, 7L)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> service.calendar(7L, 1L));
    }

    @Test
    void visitsGroupsUpcomingCompletedAndCancelledStates() {
        own(master(1L, 7L, SubscriptionStatus.ACTIVE));
        when(visitRepository.findBySubscriptionIdOrderByVisitDateAscIdAsc(1L)).thenReturn(List.of(
                visit(1L, VisitStatus.SCHEDULED, 2), visit(2L, VisitStatus.COMPLETED, -2),
                visit(3L, VisitStatus.SKIPPED, 3)));
        var result = service.visits(7L, 1L);
        assertEquals(1, ((List<?>) result.get("upcomingVisits")).size());
        assertEquals(1, ((List<?>) result.get("completedVisits")).size());
        assertEquals(1, ((List<?>) result.get("cancelledVisits")).size());
    }

    @Test
    void cancelPauseAndResumeEnforceLifecycle() {
        SubscriptionMaster active = master(1L, 7L, SubscriptionStatus.ACTIVE);
        own(active); when(masterRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        service.cancel(7L, 1L);
        assertEquals(SubscriptionStatus.CANCELLED, active.getStatus());
        verify(visitRepository).cancelFutureScheduled(eq(1L), eq(VisitStatus.SCHEDULED),
                eq(VisitStatus.CANCELLED), any(LocalDate.class));

        active.setStatus(SubscriptionStatus.ACTIVE);
        assertEquals("PAUSED", service.pause(7L, 1L).get("status"));
        assertEquals("ACTIVE", service.resume(7L, 1L).get("status"));

        active.setStatus(SubscriptionStatus.COMPLETED);
        assertThrows(SubscriptionValidationException.class, () -> service.cancel(7L, 1L));
        assertThrows(SubscriptionValidationException.class, () -> service.pause(7L, 1L));
    }

    @Test
    void skipAndRescheduleValidateDateStatusAndCollision() {
        own(master(1L, 7L, SubscriptionStatus.ACTIVE));
        SubscriptionVisit visit = visit(5L, VisitStatus.SCHEDULED, 2);
        when(visitRepository.findByIdAndSubscriptionId(5L, 1L)).thenReturn(Optional.of(visit));
        service.skipVisit(7L, 1L, 5L);
        assertEquals(VisitStatus.SKIPPED, visit.getStatus());

        visit.setStatus(VisitStatus.SCHEDULED);
        LocalDate newDate = LocalDate.now().plusDays(4);
        when(visitRepository.existsBySubscriptionIdAndVisitDateAndIdNot(1L, newDate, 5L)).thenReturn(true);
        assertThrows(SubscriptionValidationException.class,
                () -> service.rescheduleVisit(7L, 1L, 5L, new RescheduleVisitRequest(newDate)));

        when(visitRepository.existsBySubscriptionIdAndVisitDateAndIdNot(1L, newDate, 5L)).thenReturn(false);
        service.rescheduleVisit(7L, 1L, 5L, new RescheduleVisitRequest(newDate));
        assertEquals(VisitStatus.RESCHEDULED, visit.getStatus());
        assertEquals(newDate, visit.getVisitDate());
    }

    private void own(SubscriptionMaster master) {
        when(masterRepository.findByIdAndCustomerId(master.getId(), master.getCustomerId()))
                .thenReturn(Optional.of(master));
    }

    private SubscriptionMaster master(Long id, Long customerId, SubscriptionStatus status) {
        SubscriptionMaster master = new SubscriptionMaster();
        ReflectionTestUtils.setField(master, "id", id);
        master.setCustomerId(customerId); master.setServiceId(10L); master.setAddressId(20L);
        master.setBookingId(30L); master.setBookingMode("SUBSCRIPTION"); master.setServiceType("COOK");
        master.setStartDate(LocalDate.now()); master.setEndDate(LocalDate.now().plusMonths(1));
        master.setVisits(8); master.setTimeSlot("10:00-11:00"); master.setPrice(BigDecimal.TEN);
        master.setStatus(status);
        return master;
    }

    private SubscriptionVisit visit(Long id, VisitStatus status, int days) {
        SubscriptionVisit visit = new SubscriptionVisit();
        ReflectionTestUtils.setField(visit, "id", id);
        visit.setSubscriptionId(1L); visit.setVisitDate(LocalDate.now().plusDays(days));
        visit.setTimeSlot("10:00-11:00"); visit.setStatus(status);
        return visit;
    }
}

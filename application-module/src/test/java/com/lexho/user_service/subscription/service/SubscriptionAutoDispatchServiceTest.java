package com.lexho.user_service.subscription.service;

import com.lexho.booking.entity.Order;
import com.lexho.booking.repository.OrderRepository;
import com.lexho.common.enums.PaymentStatus;
import com.lexho.user_service.config.DispatchProperties;
import com.lexho.user_service.admin.repository.AdminServiceEntityRepository;
import com.lexho.user_service.admin.serviceablearea.repository.ServiceableAreaRepository;
import com.lexho.user_service.dispatch.entity.CurrentPartnerLocationEntity;
import com.lexho.user_service.dispatch.entity.PartnerShiftEntity;
import com.lexho.user_service.dispatch.repository.CurrentPartnerLocationRepository;
import com.lexho.user_service.dispatch.repository.PartnerServiceSkillRepository;
import com.lexho.user_service.dispatch.repository.PartnerShiftRepository;
import com.lexho.user_service.lexho.entity.LexhoAddress;
import com.lexho.user_service.lexho.repository.LexhoAddressRepository;
import com.lexho.user_service.lexhopartner.entity.PartnerOperationalProfile;
import com.lexho.user_service.lexhopartner.repository.PartnerOperationalProfileRepository;
import com.lexho.user_service.partnerrequest.entity.PartnerServiceRequestEntity;
import com.lexho.user_service.partnerrequest.repository.PartnerServiceRequestRepository;
import com.lexho.user_service.subscription.entity.SubscriptionMaster;
import com.lexho.user_service.subscription.entity.SubscriptionPartnerAssignment;
import com.lexho.user_service.subscription.entity.SubscriptionVisit;
import com.lexho.user_service.subscription.entity.SubscriptionVisitPartnerAssignment;
import com.lexho.user_service.subscription.enums.SubscriptionStatus;
import com.lexho.user_service.subscription.enums.VisitStatus;
import com.lexho.user_service.subscription.repository.SubscriptionMasterRepository;
import com.lexho.user_service.subscription.repository.SubscriptionPartnerAssignmentRepository;
import com.lexho.user_service.subscription.repository.SubscriptionVisitPartnerAssignmentRepository;
import com.lexho.user_service.subscription.repository.SubscriptionVisitRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SubscriptionAutoDispatchServiceTest {

    @Mock private SubscriptionMasterRepository subscriptionMasterRepository;
    @Mock private OrderRepository orderRepository;
    @Mock private SubscriptionVisitRepository subscriptionVisitRepository;
    @Mock private SubscriptionPartnerAssignmentRepository subscriptionPartnerAssignmentRepository;
    @Mock private SubscriptionVisitPartnerAssignmentRepository visitPartnerAssignmentRepository;
    @Mock private LexhoAddressRepository lexhoAddressRepository;
    @Mock private PartnerOperationalProfileRepository partnerProfileRepository;
    @Mock private PartnerShiftRepository partnerShiftRepository;
    @Mock private CurrentPartnerLocationRepository currentPartnerLocationRepository;
    @Mock private PartnerServiceSkillRepository partnerServiceSkillRepository;
    @Mock private PartnerServiceRequestRepository partnerServiceRequestRepository;
    @Mock private ServiceableAreaRepository serviceableAreaRepository;
    @Mock private AdminServiceEntityRepository adminServiceEntityRepository;

    private SubscriptionAutoDispatchService service;

    @BeforeEach
    void setUp() {
        service = new SubscriptionAutoDispatchService(
                subscriptionMasterRepository,
                orderRepository,
                subscriptionVisitRepository,
                subscriptionPartnerAssignmentRepository,
                visitPartnerAssignmentRepository,
                lexhoAddressRepository,
                partnerProfileRepository,
                partnerShiftRepository,
                currentPartnerLocationRepository,
                partnerServiceSkillRepository,
                partnerServiceRequestRepository,
                serviceableAreaRepository,
                adminServiceEntityRepository,
                dispatchProperties(),
                new com.lexho.user_service.dispatch.service.PartnerDispatchEligibilityService()
        );
        lenient().when(serviceableAreaRepository.existsByPincodeAndActiveTrueAndDeletedFalse("122001"))
                .thenReturn(true);
    }

    @Test
    void autoAssignDueVisitsShouldUsePrimaryAssignedPartnerFirst() {
        SubscriptionMaster subscription = subscription(10L);
        SubscriptionVisit visit = dueVisit(100L, 10L);
        SubscriptionPartnerAssignment assignment = new SubscriptionPartnerAssignment();
        assignment.setSubscriptionId(10L);
        assignment.setPartnerId(20L);

        when(subscriptionMasterRepository.findByStatusOrderByIdDesc(SubscriptionStatus.ACTIVE)).thenReturn(List.of(subscription));
        when(subscriptionMasterRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(subscription));
        when(subscriptionVisitRepository.findByIdForUpdate(100L)).thenReturn(Optional.of(visit));
        when(orderRepository.findByIdForUpdate(500L)).thenReturn(Optional.of(paidOrder()));
        when(subscriptionVisitRepository.findBySubscriptionIdInAndStatusInOrderByVisitDateAscIdAsc(
                argThat(ids -> ids != null && ids.contains(10L) && ids.size() == 1),
                argThat(statuses -> statuses != null && statuses.containsAll(List.of(VisitStatus.SCHEDULED, VisitStatus.RESCHEDULED)) && statuses.size() == 2)))
                .thenReturn(List.of(visit));
        when(visitPartnerAssignmentRepository.findByVisitId(100L)).thenReturn(Optional.empty());
        when(partnerServiceRequestRepository.findBySubscriptionVisitId(100L)).thenReturn(List.of());
        when(subscriptionPartnerAssignmentRepository.findBySubscriptionId(10L)).thenReturn(Optional.of(assignment));
        when(lexhoAddressRepository.findById(8L)).thenReturn(Optional.of(address()));
        when(partnerProfileRepository.findById(20L)).thenReturn(Optional.of(partner(20L, 4.9d)));
        when(partnerShiftRepository.findByPartnerId(20L)).thenReturn(Optional.of(activeShift(20L)));
        when(partnerServiceSkillRepository.existsByPartnerIdAndSubServiceId(20L, 5L)).thenReturn(true);
        when(currentPartnerLocationRepository.findByPartnerId(20L)).thenReturn(Optional.of(location(20L, 28.4599, 77.0270)));
        when(partnerServiceRequestRepository.save(any(PartnerServiceRequestEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        int assigned = service.autoAssignDueVisits();

        assertEquals(1, assigned);
        ArgumentCaptor<PartnerServiceRequestEntity> captor = ArgumentCaptor.forClass(PartnerServiceRequestEntity.class);
        verify(partnerServiceRequestRepository).save(captor.capture());
        assertEquals(20L, captor.getValue().getPartnerId());
        assertEquals("AUTO_PRIMARY_PARTNER", captor.getValue().getRejectionReason());
        assertEquals(10L, captor.getValue().getSubscriptionId());
        assertEquals(100L, captor.getValue().getSubscriptionVisitId());
        verify(visitPartnerAssignmentRepository, never()).save(any());
    }

    @Test
    void autoAssignDueVisitsShouldFallbackToNearestEligiblePartner() {
        SubscriptionMaster subscription = subscription(10L);
        SubscriptionVisit visit = dueVisit(100L, 10L);
        SubscriptionPartnerAssignment assignment = new SubscriptionPartnerAssignment();
        assignment.setSubscriptionId(10L);
        assignment.setPartnerId(20L);

        when(subscriptionMasterRepository.findByStatusOrderByIdDesc(SubscriptionStatus.ACTIVE)).thenReturn(List.of(subscription));
        when(subscriptionMasterRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(subscription));
        when(subscriptionVisitRepository.findByIdForUpdate(100L)).thenReturn(Optional.of(visit));
        when(orderRepository.findByIdForUpdate(500L)).thenReturn(Optional.of(paidOrder()));
        when(subscriptionVisitRepository.findBySubscriptionIdInAndStatusInOrderByVisitDateAscIdAsc(
                argThat(ids -> ids != null && ids.contains(10L) && ids.size() == 1),
                argThat(statuses -> statuses != null && statuses.containsAll(List.of(VisitStatus.SCHEDULED, VisitStatus.RESCHEDULED)) && statuses.size() == 2)))
                .thenReturn(List.of(visit));
        when(visitPartnerAssignmentRepository.findByVisitId(100L)).thenReturn(Optional.empty());
        when(partnerServiceRequestRepository.findBySubscriptionVisitId(100L)).thenReturn(List.of());
        when(subscriptionPartnerAssignmentRepository.findBySubscriptionId(10L)).thenReturn(Optional.of(assignment));
        when(lexhoAddressRepository.findById(8L)).thenReturn(Optional.of(address()));
        when(partnerProfileRepository.findById(20L)).thenReturn(Optional.of(partner(20L, 4.9d)));
        when(partnerShiftRepository.findByPartnerId(20L)).thenReturn(Optional.of(activeShift(20L)));
        when(partnerServiceSkillRepository.existsByPartnerIdAndSubServiceId(20L, 5L)).thenReturn(true);
        when(currentPartnerLocationRepository.findByPartnerId(20L)).thenReturn(Optional.of(location(20L, 28.5000, 77.1000)));
        when(currentPartnerLocationRepository.findNearbyPartnerIds(anyDouble(), anyDouble(), anyDouble(), eq(200)))
                .thenReturn(List.of(31L, 32L));
        when(partnerShiftRepository.findByPartnerIdInAndDeletedFalse(argThat(ids -> ids != null && ids.containsAll(List.of(31L, 32L)))))
                .thenReturn(List.of(activeShift(31L), activeShift(32L)));
        when(partnerProfileRepository.findAllById(argThat(ids -> {
            if (ids == null) {
                return false;
            }
            java.util.List<Long> values = new java.util.ArrayList<>();
            ids.forEach(values::add);
            return values.containsAll(List.of(31L, 32L));
        })))
                .thenReturn(List.of(partner(31L, 4.6d), partner(32L, 4.8d)));
        when(partnerProfileRepository.findById(31L)).thenReturn(Optional.of(partner(31L, 4.6d)));
        when(partnerShiftRepository.findByPartnerId(31L)).thenReturn(Optional.of(activeShift(31L)));
        when(partnerServiceSkillRepository.existsByPartnerIdAndSubServiceId(31L, 5L)).thenReturn(true);
        when(partnerServiceSkillRepository.existsByPartnerIdAndSubServiceId(32L, 5L)).thenReturn(true);
        when(currentPartnerLocationRepository.findByPartnerId(31L)).thenReturn(Optional.of(location(31L, 28.4597, 77.0268)));
        when(currentPartnerLocationRepository.findByPartnerId(32L)).thenReturn(Optional.of(location(32L, 28.4600, 77.0272)));
        when(partnerServiceRequestRepository.save(any(PartnerServiceRequestEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        int assigned = service.autoAssignDueVisits();

        assertEquals(1, assigned);
        ArgumentCaptor<PartnerServiceRequestEntity> captor = ArgumentCaptor.forClass(PartnerServiceRequestEntity.class);
        verify(partnerServiceRequestRepository).save(captor.capture());
        assertEquals(31L, captor.getValue().getPartnerId());
        assertEquals("AUTO_FALLBACK_DISPATCH", captor.getValue().getRejectionReason());
        verify(visitPartnerAssignmentRepository, never()).save(any());
    }

    @Test
    void autoAssignDueVisitsShouldSkipFutureOrAlreadyAssignedVisits() {
        SubscriptionMaster subscription = subscription(10L);
        SubscriptionVisit futureVisit = futureVisit(100L, 10L);
        SubscriptionVisit assignedVisit = dueVisit(101L, 10L);

        when(subscriptionMasterRepository.findByStatusOrderByIdDesc(SubscriptionStatus.ACTIVE)).thenReturn(List.of(subscription));
        when(subscriptionVisitRepository.findBySubscriptionIdInAndStatusInOrderByVisitDateAscIdAsc(
                argThat(ids -> ids != null && ids.contains(10L) && ids.size() == 1),
                argThat(statuses -> statuses != null && statuses.containsAll(List.of(VisitStatus.SCHEDULED, VisitStatus.RESCHEDULED)) && statuses.size() == 2)))
                .thenReturn(List.of(futureVisit, assignedVisit));
        int assigned = service.autoAssignDueVisits();

        assertEquals(0, assigned);
        verify(partnerServiceRequestRepository, never()).save(any());
        assertTrue(!service.autoAssignVisit(10L, 999L));
    }

    private static SubscriptionMaster subscription(Long id) {
        SubscriptionMaster subscription = new SubscriptionMaster();
        subscription.setCustomerId(7L);
        subscription.setServiceId(5L);
        subscription.setAddressId(8L);
        subscription.setBookingId(500L);
        subscription.setStatus(SubscriptionStatus.ACTIVE);
        org.springframework.test.util.ReflectionTestUtils.setField(subscription, "id", id);
        return subscription;
    }

    private static SubscriptionVisit dueVisit(Long id, Long subscriptionId) {
        SubscriptionVisit visit = new SubscriptionVisit();
        org.springframework.test.util.ReflectionTestUtils.setField(visit, "id", id);
        visit.setSubscriptionId(subscriptionId);
        visit.setVisitDate(LocalDate.now());
        visit.setTimeSlot(LocalTime.now().minusMinutes(10).format(DateTimeFormatter.ofPattern("HH:mm")));
        visit.setStatus(VisitStatus.SCHEDULED);
        return visit;
    }

    private static SubscriptionVisit futureVisit(Long id, Long subscriptionId) {
        SubscriptionVisit visit = new SubscriptionVisit();
        org.springframework.test.util.ReflectionTestUtils.setField(visit, "id", id);
        visit.setSubscriptionId(subscriptionId);
        visit.setVisitDate(LocalDate.now().plusDays(1));
        visit.setTimeSlot(LocalTime.now().plusHours(2).format(DateTimeFormatter.ofPattern("HH:mm")));
        visit.setStatus(VisitStatus.SCHEDULED);
        return visit;
    }

    private static LexhoAddress address() {
        LexhoAddress address = new LexhoAddress();
        address.setLatitude(BigDecimal.valueOf(28.4595));
        address.setLongitude(BigDecimal.valueOf(77.0266));
        address.setPincode("122001");
        return address;
    }

    private static Order paidOrder() {
        Order order = new Order();
        order.setPaymentStatus(PaymentStatus.SUCCESS);
        return order;
    }

    private static PartnerOperationalProfile partner(Long id, double rating) {
        PartnerOperationalProfile partner = new PartnerOperationalProfile();
        partner.setId(id);
        partner.setName("P" + id);
        partner.setOnline(true);
        partner.setVerified(true);
        partner.setDeleted(false);
        partner.setRating(rating);
        return partner;
    }

    private static PartnerShiftEntity activeShift(Long partnerId) {
        PartnerShiftEntity shift = new PartnerShiftEntity();
        shift.setPartnerId(partnerId);
        shift.setOnline(true);
        shift.setActiveJobsCount(0);
        shift.setMaxActiveJobs(3);
        shift.setShiftStart(LocalDateTime.now().minusHours(1));
        shift.setShiftEnd(LocalDateTime.now().plusHours(2));
        return shift;
    }

    private static CurrentPartnerLocationEntity location(Long partnerId, double lat, double lng) {
        CurrentPartnerLocationEntity location = new CurrentPartnerLocationEntity();
        location.setPartnerId(partnerId);
        location.setLatitude(BigDecimal.valueOf(lat));
        location.setLongitude(BigDecimal.valueOf(lng));
        return location;
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

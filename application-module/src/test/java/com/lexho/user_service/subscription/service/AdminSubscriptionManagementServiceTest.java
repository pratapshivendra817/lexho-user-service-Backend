package com.lexho.user_service.subscription.service;

import com.lexho.user_service.admin.entity.AdminServiceEntity;
import com.lexho.user_service.admin.repository.AdminServiceEntityRepository;
import com.lexho.user_service.config.DispatchProperties;
import com.lexho.user_service.dispatch.entity.CurrentPartnerLocationEntity;
import com.lexho.user_service.dispatch.entity.PartnerShiftEntity;
import com.lexho.user_service.dispatch.repository.CurrentPartnerLocationRepository;
import com.lexho.user_service.dispatch.repository.PartnerServiceSkillRepository;
import com.lexho.user_service.dispatch.repository.PartnerShiftRepository;
import com.lexho.user_service.exception.BadRequestException;
import com.lexho.user_service.exception.ResourceNotFoundException;
import com.lexho.user_service.lexho.entity.LexhoAddress;
import com.lexho.user_service.lexho.repository.LexhoAddressRepository;
import com.lexho.user_service.lexhopartner.entity.PartnerProfile;
import com.lexho.user_service.lexhopartner.repository.PartnerProfileRepository;
import com.lexho.user_service.subscription.dto.AdminPartnerAssignRequest;
import com.lexho.user_service.subscription.dto.AdminSubscriptionStatusUpdateRequest;
import com.lexho.user_service.subscription.dto.AdminSubscriptionVisitStatusUpdateRequest;
import com.lexho.user_service.subscription.entity.SubscriptionFrequency;
import com.lexho.user_service.subscription.entity.SubscriptionMaster;
import com.lexho.user_service.subscription.entity.SubscriptionPartnerAssignment;
import com.lexho.user_service.subscription.entity.SubscriptionVisit;
import com.lexho.user_service.subscription.entity.SubscriptionVisitPartnerAssignment;
import com.lexho.user_service.subscription.enums.FamilySize;
import com.lexho.user_service.subscription.enums.PartnerAssignmentStatus;
import com.lexho.user_service.subscription.enums.SubscriptionStatus;
import com.lexho.user_service.subscription.enums.VisitStatus;
import com.lexho.user_service.subscription.repository.SubscriptionFrequencyRepository;
import com.lexho.user_service.subscription.repository.SubscriptionMasterRepository;
import com.lexho.user_service.subscription.repository.SubscriptionPartnerAssignmentRepository;
import com.lexho.user_service.subscription.repository.SubscriptionVisitPartnerAssignmentRepository;
import com.lexho.user_service.subscription.repository.SubscriptionVisitRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminSubscriptionManagementServiceTest {

    @Mock SubscriptionMasterRepository subscriptionMasterRepository;
    @Mock SubscriptionFrequencyRepository subscriptionFrequencyRepository;
    @Mock SubscriptionVisitRepository subscriptionVisitRepository;
    @Mock AdminServiceEntityRepository serviceRepository;
    @Mock SubscriptionPartnerAssignmentRepository subscriptionPartnerAssignmentRepository;
    @Mock SubscriptionVisitPartnerAssignmentRepository visitPartnerAssignmentRepository;
    @Mock PartnerProfileRepository partnerProfileRepository;
    @Mock PartnerServiceSkillRepository partnerServiceSkillRepository;
    @Mock LexhoAddressRepository lexhoAddressRepository;
    @Mock PartnerShiftRepository partnerShiftRepository;
    @Mock CurrentPartnerLocationRepository currentPartnerLocationRepository;

    private AdminSubscriptionManagementService service;

    @BeforeEach
    void setUp() {
        service = new AdminSubscriptionManagementService(
                subscriptionMasterRepository,
                subscriptionFrequencyRepository,
                subscriptionVisitRepository,
                serviceRepository,
                subscriptionPartnerAssignmentRepository,
                visitPartnerAssignmentRepository,
                partnerProfileRepository,
                partnerServiceSkillRepository,
                lexhoAddressRepository,
                partnerShiftRepository,
                currentPartnerLocationRepository,
                dispatchProperties()
        );
    }

    @Test
    void listShouldSelectAllRepositoryBranchesAndMapVisitCounts() {
        SubscriptionMaster subscription = subscription(10L);
        SubscriptionVisit completed = visit(100L, VisitStatus.COMPLETED);
        AdminServiceEntity catalogService = new AdminServiceEntity();
        ReflectionTestUtils.setField(catalogService, "id", 5L);
        catalogService.setName("Home Cooking");
        var page = new PageImpl<>(List.of(subscription));
        when(subscriptionMasterRepository.findByCustomerIdAndStatusOrderByIdDesc(eq(7L), eq(SubscriptionStatus.ACTIVE), any()))
                .thenReturn(page);
        when(subscriptionVisitRepository.findBySubscriptionIdInOrderBySubscriptionIdAscVisitDateAscIdAsc(List.of(10L)))
                .thenReturn(List.of(completed));
        when(serviceRepository.findAllById(List.of(5L))).thenReturn(List.of(catalogService));

        var result = service.list(7L, SubscriptionStatus.ACTIVE, -1, 500);

        assertEquals(1, result.getTotalElements());
        assertEquals("Home Cooking", result.getContent().get(0).serviceName());
        assertEquals(1L, result.getContent().get(0).completedVisits());
        assertEquals(3L, result.getContent().get(0).remainingVisits());
        ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);
        verify(subscriptionMasterRepository).findByCustomerIdAndStatusOrderByIdDesc(eq(7L), eq(SubscriptionStatus.ACTIVE), pageable.capture());
        assertEquals(0, pageable.getValue().getPageNumber());
        assertEquals(100, pageable.getValue().getPageSize());

        when(subscriptionMasterRepository.findByCustomerIdOrderByIdDesc(eq(7L), any()))
                .thenReturn(new PageImpl<>(List.of()));
        when(subscriptionMasterRepository.findByStatusOrderByIdDesc(eq(SubscriptionStatus.PAUSED), any()))
                .thenReturn(new PageImpl<>(List.of()));
        when(subscriptionMasterRepository.findByOrderByIdDesc(any())).thenReturn(new PageImpl<>(List.of()));
        assertTrue(service.list(7L, null, 0, 10).isEmpty());
        assertTrue(service.list(null, SubscriptionStatus.PAUSED, 0, 10).isEmpty());
        assertTrue(service.list(null, null, 0, 10).isEmpty());
    }

    @Test
    void detailAndCalendarShouldMapFrequencyFamilyAndVisits() {
        SubscriptionMaster subscription = subscription(10L);
        SubscriptionFrequency frequency = new SubscriptionFrequency();
        frequency.setDayName("MONDAY");
        SubscriptionVisit visit = visit(100L, VisitStatus.SCHEDULED);
        AdminServiceEntity catalogService = new AdminServiceEntity();
        catalogService.setName("Cook");
        when(subscriptionMasterRepository.findById(10L)).thenReturn(Optional.of(subscription));
        when(subscriptionVisitRepository.findBySubscriptionIdOrderByVisitDateAscIdAsc(10L)).thenReturn(List.of(visit));
        when(subscriptionFrequencyRepository.findBySubscriptionIdOrderByIdAsc(10L)).thenReturn(List.of(frequency));
        when(serviceRepository.findById(5L)).thenReturn(Optional.of(catalogService));

        var detail = service.detail(10L);
        var calendar = service.calendar(10L);

        assertEquals("Cook", detail.serviceName());
        assertEquals(List.of("MONDAY"), detail.frequencyDays());
        assertEquals("3-4 Members", detail.familySizeLabel());
        assertEquals("SCHEDULED", calendar.get(0).status());
    }

    @Test
    void updateStatusShouldCancelFutureVisitsAndReturnDetail() {
        SubscriptionMaster subscription = subscription(10L);
        when(subscriptionMasterRepository.findById(10L)).thenReturn(Optional.of(subscription));
        when(subscriptionVisitRepository.findBySubscriptionIdOrderByVisitDateAscIdAsc(10L)).thenReturn(List.of());
        when(subscriptionFrequencyRepository.findBySubscriptionIdOrderByIdAsc(10L)).thenReturn(List.of());
        when(serviceRepository.findById(5L)).thenReturn(Optional.empty());

        var response = service.updateStatus(10L,
                new AdminSubscriptionStatusUpdateRequest(SubscriptionStatus.CANCELLED));

        assertEquals("CANCELLED", response.status());
        verify(subscriptionVisitRepository).cancelFutureScheduled(eq(10L), eq(VisitStatus.SCHEDULED),
                eq(VisitStatus.CANCELLED), any(LocalDate.class));
        verify(subscriptionMasterRepository).save(subscription);
    }

    @Test
    void updateVisitStatusShouldPersistAllowedStatusAndRejectRescheduled() {
        SubscriptionMaster subscription = subscription(10L);
        SubscriptionVisit visit = visit(100L, VisitStatus.SCHEDULED);
        when(subscriptionMasterRepository.findById(10L)).thenReturn(Optional.of(subscription));
        when(subscriptionVisitRepository.findByIdAndSubscriptionId(100L, 10L)).thenReturn(Optional.of(visit));
        when(subscriptionVisitRepository.save(visit)).thenReturn(visit);

        assertEquals("COMPLETED", service.updateVisitStatus(10L, 100L,
                new AdminSubscriptionVisitStatusUpdateRequest(VisitStatus.COMPLETED)).status());
        assertThrows(BadRequestException.class, () -> service.updateVisitStatus(10L, 100L,
                new AdminSubscriptionVisitStatusUpdateRequest(VisitStatus.RESCHEDULED)));
        when(subscriptionVisitRepository.findByIdAndSubscriptionId(101L, 10L)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> service.updateVisitStatus(10L, 101L,
                new AdminSubscriptionVisitStatusUpdateRequest(VisitStatus.COMPLETED)));
    }

    @Test
    void assignPartnerShouldCreateAssignmentAndNormalizeReason() {
        SubscriptionMaster subscription = subscription(10L);
        PartnerProfile partner = partner(20L, "Partner");
        LexhoAddress address = address();
        when(subscriptionMasterRepository.findById(10L)).thenReturn(Optional.of(subscription));
        when(partnerProfileRepository.findById(20L)).thenReturn(Optional.of(partner));
        when(partnerServiceSkillRepository.existsByPartnerIdAndSubServiceId(20L, 5L)).thenReturn(true);
        when(lexhoAddressRepository.findById(8L)).thenReturn(Optional.of(address));
        when(partnerShiftRepository.findByPartnerId(20L)).thenReturn(Optional.of(activeShift(20L)));
        when(currentPartnerLocationRepository.findByPartnerId(20L)).thenReturn(Optional.of(location(20L, 28.4599, 77.0270)));
        when(subscriptionPartnerAssignmentRepository.findBySubscriptionId(10L))
                .thenReturn(Optional.empty(), Optional.of(assignment(10L, 20L)));
        when(visitPartnerAssignmentRepository.findBySubscriptionIdOrderByVisitIdAsc(10L)).thenReturn(List.of());
        when(partnerProfileRepository.findAllById(any())).thenReturn(List.of(partner));

        var response = service.assignPartner(10L, new AdminPartnerAssignRequest(20L, "  preferred  "));

        assertEquals(20L, response.partnerId());
        ArgumentCaptor<SubscriptionPartnerAssignment> captor = ArgumentCaptor.forClass(SubscriptionPartnerAssignment.class);
        verify(subscriptionPartnerAssignmentRepository).save(captor.capture());
        assertEquals("preferred", captor.getValue().getReplacementReason());
        assertEquals(PartnerAssignmentStatus.ASSIGNED, captor.getValue().getStatus());
    }

    @Test
    void replacePartnerShouldRequireExistingAssignment() {
        SubscriptionMaster subscription = subscription(10L);
        PartnerProfile partner = partner(21L, "Replacement");
        when(lexhoAddressRepository.findById(8L)).thenReturn(Optional.of(address()));
        when(partnerShiftRepository.findByPartnerId(21L)).thenReturn(Optional.of(activeShift(21L)));
        when(currentPartnerLocationRepository.findByPartnerId(21L)).thenReturn(Optional.of(location(21L, 28.4599, 77.0270)));
        when(subscriptionMasterRepository.findById(10L)).thenReturn(Optional.of(subscription));
        when(partnerProfileRepository.findById(21L)).thenReturn(Optional.of(partner));
        when(partnerServiceSkillRepository.existsByPartnerIdAndSubServiceId(21L, 5L)).thenReturn(true);
        when(subscriptionPartnerAssignmentRepository.findBySubscriptionId(10L)).thenReturn(Optional.empty());

        assertThrows(BadRequestException.class,
                () -> service.replacePartner(10L, new AdminPartnerAssignRequest(21L, "reason")));
    }

    @Test
    void assignVisitPartnerShouldPersistAndMapFallbackName() {
        SubscriptionMaster subscription = subscription(10L);
        SubscriptionVisit visit = visit(100L, VisitStatus.SCHEDULED);
        PartnerProfile partner = partner(20L, null);
        partner.setFullName("Full Name");
        when(lexhoAddressRepository.findById(8L)).thenReturn(Optional.of(address()));
        when(partnerShiftRepository.findByPartnerId(20L)).thenReturn(Optional.of(activeShift(20L)));
        when(currentPartnerLocationRepository.findByPartnerId(20L)).thenReturn(Optional.of(location(20L, 28.4599, 77.0270)));
        when(subscriptionMasterRepository.findById(10L)).thenReturn(Optional.of(subscription));
        when(subscriptionVisitRepository.findByIdAndSubscriptionId(100L, 10L)).thenReturn(Optional.of(visit));
        when(partnerProfileRepository.findById(20L)).thenReturn(Optional.of(partner));
        when(partnerServiceSkillRepository.existsByPartnerIdAndSubServiceId(20L, 5L)).thenReturn(true);
        when(visitPartnerAssignmentRepository.findByVisitId(100L)).thenReturn(Optional.empty());
        when(visitPartnerAssignmentRepository.save(any())).thenAnswer(invocation -> {
            SubscriptionVisitPartnerAssignment saved = invocation.getArgument(0);
            ReflectionTestUtils.setField(saved, "id", 30L);
            return saved;
        });

        var response = service.assignVisitPartner(10L, 100L, new AdminPartnerAssignRequest(20L, " "));

        assertEquals("Full Name", response.partnerName());
        assertNull(response.reason());
    }

    @Test
    void assignmentShouldReturnPendingWithVisitAssignments() {
        when(subscriptionMasterRepository.findById(10L)).thenReturn(Optional.of(subscription(10L)));
        SubscriptionVisitPartnerAssignment visitAssignment = new SubscriptionVisitPartnerAssignment();
        ReflectionTestUtils.setField(visitAssignment, "id", 30L);
        visitAssignment.setVisitId(100L);
        visitAssignment.setPartnerId(20L);
        visitAssignment.setStatus(PartnerAssignmentStatus.ASSIGNED);
        when(subscriptionPartnerAssignmentRepository.findBySubscriptionId(10L)).thenReturn(Optional.empty());
        when(visitPartnerAssignmentRepository.findBySubscriptionIdOrderByVisitIdAsc(10L))
                .thenReturn(List.of(visitAssignment));
        when(partnerProfileRepository.findAllById(List.of(20L))).thenReturn(List.of(partner(20L, "Partner")));

        var response = service.assignment(10L);

        assertEquals("PENDING", response.status());
        assertEquals(1, response.visitAssignments().size());
    }

    @Test
    void partnerValidationShouldRejectMissingDeletedAndUnskilledPartners() {
        when(subscriptionMasterRepository.findById(10L)).thenReturn(Optional.of(subscription(10L)));
        when(partnerProfileRepository.findById(90L)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class,
                () -> service.assignPartner(10L, new AdminPartnerAssignRequest(90L, null)));

        PartnerProfile deleted = partner(91L, "Deleted");
        deleted.setDeleted(true);
        when(partnerProfileRepository.findById(91L)).thenReturn(Optional.of(deleted));
        assertThrows(BadRequestException.class,
                () -> service.assignPartner(10L, new AdminPartnerAssignRequest(91L, null)));

        PartnerProfile unskilled = partner(92L, "Unskilled");
        when(partnerProfileRepository.findById(92L)).thenReturn(Optional.of(unskilled));
        when(partnerServiceSkillRepository.existsByPartnerIdAndSubServiceId(92L, 5L)).thenReturn(false);
        assertThrows(BadRequestException.class,
                () -> service.assignPartner(10L, new AdminPartnerAssignRequest(92L, null)));
    }

    @Test
    void assignPartnerShouldRejectOfflineFarAndSlowPartners() {
        SubscriptionMaster subscription = subscription(10L);
        when(subscriptionMasterRepository.findById(10L)).thenReturn(Optional.of(subscription));
        when(lexhoAddressRepository.findById(8L)).thenReturn(Optional.of(address()));

        PartnerProfile offline = partner(30L, "Offline");
        offline.setOnline(false);
        when(partnerProfileRepository.findById(30L)).thenReturn(Optional.of(offline));
        assertThrows(BadRequestException.class,
                () -> service.assignPartner(10L, new AdminPartnerAssignRequest(30L, null)));

        PartnerProfile far = partner(31L, "Far");
        when(partnerProfileRepository.findById(31L)).thenReturn(Optional.of(far));
        when(partnerServiceSkillRepository.existsByPartnerIdAndSubServiceId(31L, 5L)).thenReturn(true);
        when(partnerShiftRepository.findByPartnerId(31L)).thenReturn(Optional.of(activeShift(31L)));
        when(currentPartnerLocationRepository.findByPartnerId(31L)).thenReturn(Optional.of(location(31L, 28.5000, 77.1000)));
        assertThrows(BadRequestException.class,
                () -> service.assignPartner(10L, new AdminPartnerAssignRequest(31L, null)));

        PartnerProfile slow = partner(32L, "Slow");
        when(partnerProfileRepository.findById(32L)).thenReturn(Optional.of(slow));
        when(partnerServiceSkillRepository.existsByPartnerIdAndSubServiceId(32L, 5L)).thenReturn(true);
        when(partnerShiftRepository.findByPartnerId(32L)).thenReturn(Optional.of(activeShift(32L)));
        when(currentPartnerLocationRepository.findByPartnerId(32L)).thenReturn(Optional.of(location(32L, 28.4830, 77.0266)));
        assertThrows(BadRequestException.class,
                () -> service.assignPartner(10L, new AdminPartnerAssignRequest(32L, null)));
    }

    @Test
    void missingSubscriptionShouldFailAllLookupBasedOperations() {
        when(subscriptionMasterRepository.findById(404L)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> service.detail(404L));
        assertThrows(ResourceNotFoundException.class, () -> service.calendar(404L));
    }

    private static SubscriptionMaster subscription(Long id) {
        SubscriptionMaster subscription = new SubscriptionMaster();
        ReflectionTestUtils.setField(subscription, "id", id);
        subscription.setCustomerId(7L);
        subscription.setServiceId(5L);
        subscription.setAddressId(8L);
        subscription.setBookingMode("SCHEDULED");
        subscription.setServiceType("COOK");
        subscription.setStartDate(LocalDate.of(2026, 6, 1));
        subscription.setEndDate(LocalDate.of(2026, 6, 30));
        subscription.setVisits(4);
        subscription.setDurationHours(BigDecimal.valueOf(2));
        subscription.setFamilySize(FamilySize.MEDIUM);
        subscription.setPerVisitPrice(BigDecimal.valueOf(500));
        subscription.setTimeSlot("10:00-12:00");
        subscription.setPrice(BigDecimal.valueOf(2000));
        subscription.setStatus(SubscriptionStatus.ACTIVE);
        return subscription;
    }

    private static SubscriptionVisit visit(Long id, VisitStatus status) {
        SubscriptionVisit visit = new SubscriptionVisit();
        ReflectionTestUtils.setField(visit, "id", id);
        visit.setSubscriptionId(10L);
        visit.setVisitDate(LocalDate.of(2026, 6, 15));
        visit.setTimeSlot("10:00-12:00");
        visit.setStatus(status);
        return visit;
    }

    private static PartnerProfile partner(Long id, String name) {
        PartnerProfile partner = new PartnerProfile();
        partner.setId(id);
        partner.setName(name);
        partner.setOnline(true);
        partner.setVerified(true);
        return partner;
    }

    private static LexhoAddress address() {
        LexhoAddress address = new LexhoAddress();
        address.setLatitude(BigDecimal.valueOf(28.4595));
        address.setLongitude(BigDecimal.valueOf(77.0266));
        return address;
    }

    private static PartnerShiftEntity activeShift(Long partnerId) {
        PartnerShiftEntity shift = new PartnerShiftEntity();
        shift.setPartnerId(partnerId);
        shift.setOnline(true);
        shift.setActiveJobsCount(0);
        shift.setMaxActiveJobs(3);
        shift.setShiftStart(java.time.LocalDateTime.now().minusHours(1));
        shift.setShiftEnd(java.time.LocalDateTime.now().plusHours(1));
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

    private static SubscriptionPartnerAssignment assignment(Long subscriptionId, Long partnerId) {
        SubscriptionPartnerAssignment assignment = new SubscriptionPartnerAssignment();
        assignment.setSubscriptionId(subscriptionId);
        assignment.setPartnerId(partnerId);
        assignment.setStatus(PartnerAssignmentStatus.ASSIGNED);
        return assignment;
    }
}

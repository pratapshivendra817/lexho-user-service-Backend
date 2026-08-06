package com.lexho.user_service.lexhopartner.service;

import com.lexho.user_service.audit.service.AuditLogService;
import com.lexho.user_service.dispatch.entity.PartnerShiftEntity;
import com.lexho.user_service.dispatch.repository.PartnerShiftRepository;
import com.lexho.user_service.exception.BadRequestException;
import com.lexho.user_service.lexhopartner.dto.PartnerLeaveRequest;
import com.lexho.user_service.lexhopartner.dto.PartnerServiceAreaRequest;
import com.lexho.user_service.lexhopartner.entity.OrderStatus;
import com.lexho.user_service.lexhopartner.entity.PartnerLeave;
import com.lexho.user_service.lexhopartner.entity.PartnerPenalty;
import com.lexho.user_service.lexhopartner.entity.PartnerProfile;
import com.lexho.user_service.lexhopartner.entity.PartnerServiceArea;
import com.lexho.user_service.lexhopartner.repository.PartnerLeaveRepository;
import com.lexho.user_service.lexhopartner.repository.PartnerOrderRepository;
import com.lexho.user_service.lexhopartner.repository.PartnerPenaltyRepository;
import com.lexho.user_service.lexhopartner.repository.PartnerProfileRepository;
import com.lexho.user_service.lexhopartner.repository.PartnerServiceAreaRepository;
import com.lexho.user_service.lexhopartner.repository.PartnerWarningCommentRepository;
import com.lexho.user_service.notification.service.NotificationManagementService;
import com.lexho.user_service.review.repository.ReviewRepository;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PartnerOperationsServiceAdditionalTest {

    private final PartnerLeaveRepository leaveRepository = mock(PartnerLeaveRepository.class);
    private final PartnerServiceAreaRepository serviceAreaRepository = mock(PartnerServiceAreaRepository.class);
    private final PartnerPenaltyRepository penaltyRepository = mock(PartnerPenaltyRepository.class);
    private final PartnerWarningCommentRepository warningCommentRepository = mock(PartnerWarningCommentRepository.class);
    private final PartnerOrderRepository orderRepository = mock(PartnerOrderRepository.class);
    private final PartnerProfileRepository profileRepository = mock(PartnerProfileRepository.class);
    private final PartnerShiftRepository shiftRepository = mock(PartnerShiftRepository.class);
    private final ReviewRepository reviewRepository = mock(ReviewRepository.class);
    private final AuditLogService auditLogService = mock(AuditLogService.class);
    private final NotificationManagementService notificationManagementService = mock(NotificationManagementService.class);
    private final PartnerWarningRealtimeService partnerWarningRealtimeService = mock(PartnerWarningRealtimeService.class);
    private final PartnerOperationsService service = new PartnerOperationsService(
            leaveRepository, serviceAreaRepository, penaltyRepository, warningCommentRepository, orderRepository,
            profileRepository, shiftRepository, reviewRepository, auditLogService, notificationManagementService,
            partnerWarningRealtimeService);

    @Test
    void createLeaveShouldValidateDatesAndMarkPartnerOffline() {
        PartnerProfile profile = new PartnerProfile();
        profile.setOnline(true);
        PartnerShiftEntity shift = new PartnerShiftEntity();
        shift.setOnline(true);
        when(profileRepository.findById(5L)).thenReturn(Optional.of(profile));
        when(shiftRepository.findByPartnerId(5L)).thenReturn(Optional.of(shift));
        when(leaveRepository.save(any(PartnerLeave.class))).thenAnswer(invocation -> {
            PartnerLeave saved = invocation.getArgument(0);
            ReflectionTestUtils.setField(saved, "id", 101L);
            return saved;
        });

        Map<String, Object> response = service.createLeave(5L,
                new PartnerLeaveRequest(LocalDate.now(), LocalDate.now().plusDays(1), " personal "));

        assertEquals("personal", response.get("reason"));
        assertFalse(profile.isOnline());
        assertFalse(shift.isOnline());
        verify(profileRepository).save(profile);
        verify(shiftRepository).save(shift);

        assertThrows(BadRequestException.class, () -> service.createLeave(5L,
                new PartnerLeaveRequest(LocalDate.now().plusDays(2), LocalDate.now(), "bad")));
    }

    @Test
    void updateServiceAreasShouldReplaceAreasAndValidatePincode() {
        when(serviceAreaRepository.save(any(PartnerServiceArea.class))).thenAnswer(invocation -> {
            PartnerServiceArea saved = invocation.getArgument(0);
            ReflectionTestUtils.setField(saved, "id", 201L);
            return saved;
        });

        List<Map<String, Object>> response = service.updateServiceAreas(5L,
                new PartnerServiceAreaRequest(List.of(new PartnerServiceAreaRequest.ServiceAreaItem(" 110001 ", " Delhi "))));

        assertEquals("110001", response.get(0).get("pincode"));
        assertEquals("Delhi", response.get(0).get("city"));
        verify(serviceAreaRepository).deleteByPartnerId(5L);

        assertThrows(BadRequestException.class, () -> service.updateServiceAreas(5L,
                new PartnerServiceAreaRequest(List.of(new PartnerServiceAreaRequest.ServiceAreaItem(" ", "Delhi")))));
    }

    @Test
    void performanceShouldCalculateRatesAndUseProfileRating() {
        PartnerProfile profile = new PartnerProfile();
        profile.setRating(4.6);
        when(orderRepository.countByAssignedPartnerId(5L)).thenReturn(10L);
        when(orderRepository.countByAssignedPartnerIdAndStatus(5L, OrderStatus.ACCEPTED)).thenReturn(2L);
        when(orderRepository.countByAssignedPartnerIdAndStatus(5L, OrderStatus.STARTED)).thenReturn(1L);
        when(orderRepository.countByAssignedPartnerIdAndStatus(5L, OrderStatus.COMPLETED)).thenReturn(5L);
        when(orderRepository.countByAssignedPartnerIdAndStatus(5L, OrderStatus.REJECTED)).thenReturn(1L);
        when(profileRepository.findById(5L)).thenReturn(Optional.of(profile));

        Map<String, Object> response = service.performance(5L);

        assertEquals(80.0, response.get("acceptanceRate"));
        assertEquals(62.5, response.get("completionRate"));
        assertEquals(10.0, response.get("cancellationRate"));
        assertEquals(4.6, response.get("averageRating"));
        assertEquals(4.6, response.get("rating"));
        assertEquals(0.0, response.get("onTimeArrivalRate"));
        assertEquals(0.0, response.get("onTimeArrivalPercentage"));
    }

    @Test
    void addPenaltyShouldValidateStatusAndWriteAuditLog() {
        when(penaltyRepository.save(any(PartnerPenalty.class))).thenAnswer(invocation -> {
            PartnerPenalty saved = invocation.getArgument(0);
            ReflectionTestUtils.setField(saved, "id", 301L);
            ReflectionTestUtils.setField(saved, "createdAt", LocalDateTime.now());
            return saved;
        });

        Map<String, Object> response = service.addPenalty(5L, " warning ", " late ", 99L);

        assertEquals("WARNING", response.get("status"));
        assertEquals("late", response.get("reason"));
        verify(auditLogService).log(99L, "ADMIN", "PENALTY_UPDATED", "PARTNER", "5", null, "WARNING");
        verify(notificationManagementService).createPartnerWarningNotification(5L, 301L, "late");
        assertThrows(BadRequestException.class, () -> service.addPenalty(5L, "INVALID", "bad", 99L));
    }
}

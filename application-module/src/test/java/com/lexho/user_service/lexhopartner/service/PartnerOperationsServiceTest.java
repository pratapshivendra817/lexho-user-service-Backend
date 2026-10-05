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
import com.lexho.user_service.lexhopartner.entity.PartnerOperationalProfile;
import com.lexho.user_service.lexhopartner.entity.PartnerServiceArea;
import com.lexho.user_service.lexhopartner.repository.PartnerWarningCommentRepository;
import com.lexho.user_service.lexhopartner.repository.PartnerLeaveRepository;
import com.lexho.user_service.lexhopartner.repository.PartnerOrderRepository;
import com.lexho.user_service.lexhopartner.repository.PartnerPenaltyRepository;
import com.lexho.user_service.lexhopartner.repository.PartnerOperationalProfileRepository;
import com.lexho.user_service.lexhopartner.repository.PartnerServiceAreaRepository;
import com.lexho.user_service.localization.service.PartnerLocalizationService;
import com.lexho.user_service.notification.service.NotificationManagementService;
import com.lexho.user_service.review.entity.ReviewEntity;
import com.lexho.user_service.review.repository.ReviewRepository;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PartnerOperationsServiceTest {

    private final PartnerLeaveRepository leaveRepository = mock(PartnerLeaveRepository.class);
    private final PartnerServiceAreaRepository serviceAreaRepository = mock(PartnerServiceAreaRepository.class);
    private final PartnerPenaltyRepository penaltyRepository = mock(PartnerPenaltyRepository.class);
    private final PartnerWarningCommentRepository warningCommentRepository = mock(PartnerWarningCommentRepository.class);
    private final PartnerOrderRepository orderRepository = mock(PartnerOrderRepository.class);
    private final PartnerOperationalProfileRepository profileRepository = mock(PartnerOperationalProfileRepository.class);
    private final PartnerShiftRepository shiftRepository = mock(PartnerShiftRepository.class);
    private final ReviewRepository reviewRepository = mock(ReviewRepository.class);
    private final AuditLogService auditLogService = mock(AuditLogService.class);
    private final NotificationManagementService notificationManagementService = mock(NotificationManagementService.class);
    private final PartnerWarningRealtimeService partnerWarningRealtimeService = mock(PartnerWarningRealtimeService.class);
    private final PartnerLocalizationService localizationService = mock(PartnerLocalizationService.class);

    private final PartnerOperationsService service = new PartnerOperationsService(
            leaveRepository,
            serviceAreaRepository,
            penaltyRepository,
            warningCommentRepository,
            orderRepository,
            profileRepository,
            shiftRepository,
            reviewRepository,
            auditLogService,
            notificationManagementService,
            partnerWarningRealtimeService,
            localizationService
    );

    @Test
    void createLeaveShouldTrimReasonAndForcePartnerOffline() {
        PartnerOperationalProfile profile = new PartnerOperationalProfile();
        profile.setId(10L);
        profile.setOnline(true);
        PartnerShiftEntity shift = new PartnerShiftEntity();
        shift.setPartnerId(10L);
        shift.setOnline(true);
        when(leaveRepository.save(any(PartnerLeave.class))).thenAnswer(invocation -> {
            PartnerLeave leave = invocation.getArgument(0);
            ReflectionTestUtils.setField(leave, "id", 101L);
            return leave;
        });
        when(profileRepository.findById(10L)).thenReturn(Optional.of(profile));
        when(shiftRepository.findByPartnerId(10L)).thenReturn(Optional.of(shift));

        Map<String, Object> result = service.createLeave(10L,
                new PartnerLeaveRequest(LocalDate.of(2026, 6, 20), LocalDate.of(2026, 6, 22), "  urgent  "));

        assertEquals(101L, result.get("id"));
        assertEquals("urgent", result.get("reason"));
        assertThat(profile.isOnline()).isFalse();
        assertThat(shift.isOnline()).isFalse();
        verify(profileRepository).save(profile);
        verify(shiftRepository).save(shift);
    }

    @Test
    void createLeaveShouldRejectInvalidRange() {
        assertThrows(BadRequestException.class, () -> service.createLeave(10L,
                new PartnerLeaveRequest(LocalDate.of(2026, 6, 23), LocalDate.of(2026, 6, 22), "x")));
    }

    @Test
    void leavesAndServiceAreasShouldMapNullFieldsAsEmptyStrings() {
        PartnerLeave leave = new PartnerLeave();
        ReflectionTestUtils.setField(leave, "id", 1L);
        leave.setFromDate(LocalDate.of(2026, 6, 20));
        leave.setToDate(LocalDate.of(2026, 6, 21));
        PartnerServiceArea area = new PartnerServiceArea();
        ReflectionTestUtils.setField(area, "id", 2L);
        area.setPartnerId(10L);
        area.setPincode("110001");
        when(leaveRepository.findByPartnerIdOrderByFromDateDesc(10L)).thenReturn(List.of(leave));
        when(serviceAreaRepository.findByPartnerIdOrderByPincodeAsc(10L)).thenReturn(List.of(area));

        assertEquals("", service.leaves(10L).get(0).get("reason"));
        assertEquals("", service.serviceAreas(10L).get(0).get("city"));
    }

    @Test
    void updateServiceAreasShouldReplaceExistingRowsAndTrimValues() {
        when(serviceAreaRepository.save(any(PartnerServiceArea.class))).thenAnswer(invocation -> {
            PartnerServiceArea saved = invocation.getArgument(0);
            ReflectionTestUtils.setField(saved, "id", saved.getPincode().equals("110001") ? 11L : 12L);
            return saved;
        });
        PartnerServiceAreaRequest request = new PartnerServiceAreaRequest(List.of(
                new PartnerServiceAreaRequest.ServiceAreaItem(" 110001 ", " Delhi "),
                new PartnerServiceAreaRequest.ServiceAreaItem("220002", null)
        ));

        List<Map<String, Object>> result = service.updateServiceAreas(10L, request);

        verify(serviceAreaRepository).deleteByPartnerId(10L);
        assertEquals(2, result.size());
        assertEquals("110001", result.get(0).get("pincode"));
        assertEquals("Delhi", result.get(0).get("city"));
        assertEquals("", result.get(1).get("city"));
    }

    @Test
    void updateServiceAreasShouldRejectBlankPincode() {
        PartnerServiceAreaRequest request = new PartnerServiceAreaRequest(List.of(
                new PartnerServiceAreaRequest.ServiceAreaItem(" ", "Delhi")
        ));

        assertThrows(BadRequestException.class, () -> service.updateServiceAreas(10L, request));
    }

    @Test
    void performanceShouldCalculatePercentagesAndFallbackRating() {
        when(orderRepository.countByAssignedPartnerId(10L)).thenReturn(10L);
        when(orderRepository.countByAssignedPartnerIdAndStatus(10L, OrderStatus.ACCEPTED)).thenReturn(3L);
        when(orderRepository.countByAssignedPartnerIdAndStatus(10L, OrderStatus.STARTED)).thenReturn(2L);
        when(orderRepository.countByAssignedPartnerIdAndStatus(10L, OrderStatus.COMPLETED)).thenReturn(4L);
        when(orderRepository.countByAssignedPartnerIdAndStatus(10L, OrderStatus.REJECTED)).thenReturn(1L);
        PartnerOperationalProfile profile = new PartnerOperationalProfile();
        profile.setId(10L);
        profile.setRating(4.678);
        when(profileRepository.findById(10L)).thenReturn(Optional.of(profile));

        Map<String, Object> performance = service.performance(10L);

        assertEquals(90.0, performance.get("acceptanceRate"));
        assertEquals(44.44, performance.get("completionRate"));
        assertEquals(10.0, performance.get("cancellationRate"));
        assertEquals(4.678, performance.get("averageRating"));
        assertEquals(4.678, performance.get("rating"));
        assertEquals(0.0, performance.get("onTimeArrivalRate"));
        assertEquals(0.0, performance.get("onTimeArrivalPercentage"));
    }

    @Test
    void ratingsShouldReturnRoundedAverageAndPagingMetadata() {
        ReviewEntity first = review(1L, 1001L, 4, "good", LocalDateTime.of(2026, 6, 20, 10, 0));
        ReviewEntity second = review(2L, 1002L, null, "late", LocalDateTime.of(2026, 6, 21, 10, 0));
        when(reviewRepository.findByProviderIdAndDeletedFalse(eq(10L), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(first, second), Pageable.ofSize(2).withPage(0), 2));
        when(reviewRepository.findByProviderIdAndDeletedFalse(eq(10L), eq(Pageable.ofSize(1000).withPage(0))))
                .thenReturn(new PageImpl<>(List.of(first, second)));

        Map<String, Object> ratings = service.ratings(10L, -1, 1000);

        assertEquals(2.0, ratings.get("averageRating"));
        assertEquals(2L, ratings.get("totalReviews"));
        Map<?, ?> reviews = (Map<?, ?>) ratings.get("reviews");
        assertEquals(0, reviews.get("page"));
        assertEquals(2, reviews.get("size"));
        assertThat((List<?>) reviews.get("content")).hasSize(2);
    }

    @Test
    void ratingsShouldReturnZeroAverageWhenNoReviewsExist() {
        when(reviewRepository.findByProviderIdAndDeletedFalse(eq(10L), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(), Pageable.ofSize(1).withPage(0), 0));

        Map<String, Object> ratings = service.ratings(10L, 0, 1);

        assertEquals(0.0, ratings.get("averageRating"));
        verify(reviewRepository, never()).findByProviderIdAndDeletedFalse(eq(10L), eq(Pageable.ofSize(1000).withPage(0)));
    }

    @Test
    void penaltiesAndAddPenaltyShouldValidateNormalizeAndAudit() {
        PartnerPenalty existing = new PartnerPenalty();
        ReflectionTestUtils.setField(existing, "id", 91L);
        existing.setStatus("WARNING");
        existing.setCreatedAt(LocalDateTime.of(2026, 6, 20, 9, 0));
        when(penaltyRepository.findByPartnerIdOrderByCreatedAtDesc(10L)).thenReturn(List.of(existing));
        when(penaltyRepository.save(any(PartnerPenalty.class))).thenAnswer(invocation -> {
            PartnerPenalty saved = invocation.getArgument(0);
            ReflectionTestUtils.setField(saved, "id", 92L);
            saved.setCreatedAt(LocalDateTime.of(2026, 6, 20, 12, 0));
            return saved;
        });

        assertEquals(1, service.penalties(10L).size());
        Map<String, Object> result = service.addPenalty(10L, " suspension ", "  repeat no-show  ", 99L);

        assertEquals("SUSPENSION", result.get("status"));
        assertEquals("repeat no-show", result.get("reason"));
        verify(auditLogService).log(99L, "ADMIN", "PENALTY_UPDATED", "PARTNER", "10", null, "SUSPENSION");
        verify(notificationManagementService, never()).createPartnerWarningNotification(any(), any(), any());
    }

    @Test
    void warningsShouldReturnOnlyWarningRowsAndAddPenaltyShouldNotifyForWarnings() {
        PartnerPenalty warning = new PartnerPenalty();
        ReflectionTestUtils.setField(warning, "id", 91L);
        warning.setStatus("WARNING");
        warning.setReason("docs missing");
        warning.setCreatedAt(LocalDateTime.of(2026, 6, 20, 9, 0));
        PartnerPenalty suspension = new PartnerPenalty();
        ReflectionTestUtils.setField(suspension, "id", 90L);
        suspension.setStatus("SUSPENSION");
        suspension.setCreatedAt(LocalDateTime.of(2026, 6, 19, 9, 0));
        when(penaltyRepository.findByPartnerIdOrderByCreatedAtDesc(10L)).thenReturn(List.of(warning, suspension));
        when(warningCommentRepository.findByWarning_IdOrderByCreatedAtAsc(91L)).thenReturn(List.of());
        when(penaltyRepository.save(any(PartnerPenalty.class))).thenAnswer(invocation -> {
            PartnerPenalty saved = invocation.getArgument(0);
            ReflectionTestUtils.setField(saved, "id", 92L);
            saved.setCreatedAt(LocalDateTime.of(2026, 6, 20, 12, 0));
            return saved;
        });

        List<Map<String, Object>> warnings = service.warnings(10L);
        Map<String, Object> added = service.addPenalty(10L, " warning ", "  late arrival  ", 99L);

        assertThat(warnings).hasSize(1);
        assertEquals("WARNING", warnings.get(0).get("type"));
        assertEquals("WARNING", warnings.get(0).get("category"));
        assertEquals("WARNING", warnings.get(0).get("warningStatus"));
        assertEquals("Review the issue and avoid repeating it on future jobs.", warnings.get(0).get("resolutionGuidance"));
        assertEquals("WARNING", added.get("status"));
        verify(notificationManagementService).createPartnerWarningNotification(10L, 92L, "late arrival");
    }

    @Test
    void createWarningShouldPersistWarningAndOptionallyNotify() {
        when(penaltyRepository.save(any(PartnerPenalty.class))).thenAnswer(invocation -> {
            PartnerPenalty saved = invocation.getArgument(0);
            ReflectionTestUtils.setField(saved, "id", 93L);
            saved.setCreatedAt(LocalDateTime.of(2026, 6, 21, 12, 0));
            return saved;
        });

        Map<String, Object> warning = service.createWarning(10L, "  document mismatch  ", 99L, false);

        assertEquals(93L, warning.get("warningId"));
        assertEquals("WARNING", warning.get("type"));
        assertEquals("document mismatch", warning.get("reason"));
        assertEquals("WARNING", warning.get("redirectType"));
        verify(auditLogService).log(99L, "ADMIN", "WARNING_CREATED", "PARTNER", "10", null, "WARNING");
        verify(notificationManagementService, never()).createPartnerWarningNotification(any(), any(), any());
    }

    @Test
    void warningDetailAcknowledgeAndDisputeShouldPersistMetadata() {
        PartnerPenalty warning = new PartnerPenalty();
        ReflectionTestUtils.setField(warning, "id", 94L);
        warning.setPartnerId(10L);
        warning.setStatus("WARNING");
        warning.setReason("document mismatch");
        warning.setCreatedAt(LocalDateTime.of(2026, 6, 22, 9, 0));
        when(penaltyRepository.findByIdAndPartnerId(94L, 10L)).thenReturn(Optional.of(warning));
        when(penaltyRepository.save(any(PartnerPenalty.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(warningCommentRepository.findByWarning_IdOrderByCreatedAtAsc(94L)).thenReturn(List.of());

        Map<String, Object> detail = service.warningDetail(10L, 94L);
        Map<String, Object> acknowledged = service.acknowledgeWarning(10L, 94L);
        Map<String, Object> disputed = service.disputeWarning(10L, 94L,
                "I have already submitted corrected documents",
                List.of("https://cdn.lexho.test/evidence.jpg"));

        assertEquals(94L, detail.get("warningId"));
        assertEquals(Boolean.TRUE, acknowledged.get("acknowledged"));
        assertEquals("OPEN", disputed.get("disputeStatus"));
        assertEquals("I have already submitted corrected documents", disputed.get("disputeReason"));
        verify(warningCommentRepository).save(any());
        verify(auditLogService).log(10L, "PARTNER", "WARNING_DISPUTED", "PARTNER_WARNING", "94", null, "OPEN");
        verify(partnerWarningRealtimeService, org.mockito.Mockito.atLeastOnce()).publish(eq(10L), eq(94L), any(), any(Map.class));
    }

    @Test
    void disputeShouldRejectSecondActiveSubmission() {
        PartnerPenalty warning = new PartnerPenalty();
        ReflectionTestUtils.setField(warning, "id", 95L);
        warning.setPartnerId(10L);
        warning.setStatus("WARNING");
        warning.setDisputeStatus("OPEN");
        when(penaltyRepository.findByIdAndPartnerId(95L, 10L)).thenReturn(Optional.of(warning));

        assertThrows(BadRequestException.class, () ->
                service.disputeWarning(10L, 95L, "Please review again", List.of()));
    }

    @Test
    void addPenaltyShouldRejectMissingOrUnsupportedStatus() {
        assertThrows(BadRequestException.class, () -> service.addPenalty(10L, " ", "x", 99L));
        assertThrows(BadRequestException.class, () -> service.addPenalty(10L, "hold", "x", 99L));
    }

    private ReviewEntity review(Long id, Long bookingId, Integer rating, String comment, LocalDateTime createdAt) {
        ReviewEntity review = new ReviewEntity();
        review.setId(id);
        review.setBookingId(bookingId);
        review.setRating(rating);
        review.setReviewComment(comment);
        review.setCreatedAt(createdAt);
        return review;
    }
}

package com.lexho.user_service.review.service;

import com.lexho.booking.entity.Booking;
import com.lexho.booking.entity.Order;
import com.lexho.booking.entity.OrderItem;
import com.lexho.booking.enums.BookingStatus;
import com.lexho.booking.repository.BookingRepository;
import com.lexho.booking.repository.OrderRepository;
import com.lexho.catalog.entity.ServiceCategory;
import com.lexho.common.enums.OrderStatus;
import com.lexho.provider.entity.Partner;
import com.lexho.provider.repository.PartnerRepository;
import com.lexho.user.entity.User;
import com.lexho.user.repository.UserRepository;
import com.lexho.user_service.admin.entity.AdminCategoryEntity;
import com.lexho.user_service.admin.entity.AdminServiceEntity;
import com.lexho.user_service.admin.repository.AdminServiceEntityRepository;
import com.lexho.user_service.admin.settings.service.AdminSettingsService;
import com.lexho.user_service.audit.service.AuditLogService;
import com.lexho.user_service.lexhopartner.entity.PartnerOperationalProfile;
import com.lexho.user_service.lexhopartner.repository.PartnerOperationalProfileRepository;
import com.lexho.user_service.review.dto.ReviewResponse;
import com.lexho.user_service.review.dto.CustomerReviewSubmitResponse;
import com.lexho.user_service.review.dto.ReportReviewRequest;
import com.lexho.user_service.review.dto.ReviewReportResponse;
import com.lexho.user_service.review.dto.ReviewSettingsUpdateRequest;
import com.lexho.user_service.review.dto.SubmitReviewRequest;
import com.lexho.user_service.review.dto.UpdateReviewRequest;
import com.lexho.user_service.review.entity.ReviewEntity;
import com.lexho.user_service.review.entity.ReviewModerationHistory;
import com.lexho.user_service.review.entity.ReviewReport;
import com.lexho.user_service.review.enums.ReviewReportStatus;
import com.lexho.user_service.review.enums.ReviewStatus;
import com.lexho.user_service.review.exception.ReviewAccessDeniedException;
import com.lexho.user_service.review.exception.ReviewNotFoundException;
import com.lexho.user_service.review.exception.ReviewValidationException;
import com.lexho.user_service.review.mapper.ReviewMapper;
import com.lexho.user_service.review.repository.ReviewModerationHistoryRepository;
import com.lexho.user_service.review.repository.ReviewReportRepository;
import com.lexho.user_service.review.repository.ReviewRepository;
import com.lexho.user_service.review.repository.ServiceCategoryLookupRepository;
import com.lexho.user_service.security.SecurityUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReviewManagementServiceTest {

    @Mock
    private ReviewRepository reviewRepository;
    @Mock
    private ReviewReportRepository reviewReportRepository;
    @Mock
    private BookingRepository bookingRepository;
    @Mock
    private OrderRepository orderRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private PartnerRepository partnerRepository;
    @Mock
    private PartnerOperationalProfileRepository partnerProfileRepository;
    @Mock
    private AdminServiceEntityRepository adminServiceEntityRepository;
    @Mock
    private AdminSettingsService adminSettingsService;
    @Mock
    private ReviewModerationHistoryRepository reviewModerationHistoryRepository;
    @Mock
    private ServiceCategoryLookupRepository serviceCategoryLookupRepository;
    @Mock
    private ReviewMapper reviewMapper;
    @Mock
    private SecurityUtils securityUtils;
    @Mock
    private AuditLogService auditLogService;

    @InjectMocks
    private ReviewManagementService reviewManagementService;

    @Test
    void submitReviewFailsWhenBookingNotCompleted() {
        SubmitReviewRequest request = new SubmitReviewRequest();
        request.setBookingId(101L);
        request.setRating(5);

        Booking booking = mockBooking(101L, 11L, BookingStatus.PENDING);
        when(securityUtils.getCurrentUserId()).thenReturn(Optional.of(11L));
        when(orderRepository.findById(101L)).thenReturn(Optional.empty());
        when(bookingRepository.findByIdAndDeletedFalse(101L)).thenReturn(Optional.of(booking));

        assertThrows(ReviewValidationException.class, () -> reviewManagementService.submitReview(request));
    }

    @Test
    void submitReviewFailsWhenDuplicateReviewExists() {
        SubmitReviewRequest request = new SubmitReviewRequest();
        request.setBookingId(101L);
        request.setRating(4);
        request.setReviewImages(List.of("a.jpg"));

        Booking booking = mockBooking(101L, 11L, BookingStatus.COMPLETED);
        when(securityUtils.getCurrentUserId()).thenReturn(Optional.of(11L));
        when(orderRepository.findById(101L)).thenReturn(Optional.empty());
        when(bookingRepository.findByIdAndDeletedFalse(101L)).thenReturn(Optional.of(booking));
        when(reviewRepository.existsByBookingIdAndDeletedFalse(101L)).thenReturn(true);

        assertThrows(ReviewValidationException.class, () -> reviewManagementService.submitReview(request));
    }

    @Test
    void submitCustomerReviewCreatesOrderBackedPendingReview() {
        SubmitReviewRequest request = new SubmitReviewRequest();
        request.setBookingId(20481L);
        request.setRating(5);
        request.setReviewText("Excellent service");

        Order order = new Order();
        order.setId(20481L);
        order.setUserId(11L);
        order.setAssignedPartnerId(22L);
        order.setStatus(OrderStatus.SERVICE_COMPLETED);
        java.util.List<OrderItem> items = new java.util.ArrayList<>();
        items.add(null);
        OrderItem item = new OrderItem();
        item.setPartnerId(33L);
        items.add(item);
        order.setItems(items);

        ReviewEntity saved = new ReviewEntity();
        saved.setId(1001L);
        saved.setBookingId(20481L);
        saved.setCustomerId(11L);
        saved.setProviderId(22L);
        saved.setRating(5);

        when(securityUtils.getCurrentUserId()).thenReturn(Optional.of(11L));
        when(orderRepository.findById(20481L)).thenReturn(Optional.of(order));
        when(reviewRepository.existsByBookingIdAndDeletedFalse(20481L)).thenReturn(false);
        when(reviewRepository.save(org.mockito.ArgumentMatchers.any(ReviewEntity.class))).thenReturn(saved);

        CustomerReviewSubmitResponse response = reviewManagementService.submitCustomerReview(request);

        assertEquals(1001L, response.reviewId());
        assertEquals(20481L, response.bookingId());
        assertEquals(5, response.rating());
        assertEquals("Excellent service", response.reviewText());
        assertEquals(true, response.reviewSubmitted());
        verify(auditLogService).log(11L, "CUSTOMER", "REVIEW_CREATED", "REVIEW", "1001", null,
                "bookingId=20481,rating=5");
        ArgumentCaptor<ReviewEntity> captor = ArgumentCaptor.forClass(ReviewEntity.class);
        verify(reviewRepository).save(captor.capture());
        assertThat(captor.getValue().getServiceId()).isEqualTo(33L);
    }

    @Test
    void submitCustomerReviewAcceptsDisplayBookingIdAndReturnsClearErrorCodes() {
        SubmitReviewRequest request = new SubmitReviewRequest();
        request.setBookingId("B20481");
        request.setRating(5);

        Order order = new Order();
        order.setId(20481L);
        order.setUserId(11L);
        order.setStatus(OrderStatus.CONFIRMED);
        when(securityUtils.getCurrentUserId()).thenReturn(Optional.of(11L));
        when(orderRepository.findById(20481L)).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> reviewManagementService.submitCustomerReview(request))
                .isInstanceOf(ReviewValidationException.class)
                .extracting("code")
                .isEqualTo("BOOKING_NOT_COMPLETED");

        order.setStatus(OrderStatus.SERVICE_COMPLETED);
        when(reviewRepository.existsByBookingIdAndDeletedFalse(20481L)).thenReturn(true);
        assertThatThrownBy(() -> reviewManagementService.submitCustomerReview(request))
                .isInstanceOf(ReviewValidationException.class)
                .extracting("code")
                .isEqualTo("REVIEW_ALREADY_SUBMITTED");

        request.setBookingId("BAD");
        assertThatThrownBy(() -> reviewManagementService.submitCustomerReview(request))
                .isInstanceOf(ReviewNotFoundException.class)
                .extracting("code")
                .isEqualTo("BOOKING_NOT_FOUND");
    }

    @Test
    void reportReviewIncrementsReportCount() {
        Long reviewId = 55L;
        Long reporterId = 99L;
        ReviewEntity review = new ReviewEntity();
        review.setId(reviewId);
        review.setCustomerId(11L);
        review.setReportCount(0L);

        ReportReviewRequest request = new ReportReviewRequest();
        request.setReason("Abusive language");

        when(securityUtils.getCurrentUserId()).thenReturn(Optional.of(reporterId));
        when(reviewRepository.findByIdAndDeletedFalse(reviewId)).thenReturn(Optional.of(review));
        when(reviewReportRepository.existsByReviewIdAndReportedByUserId(reviewId, reporterId)).thenReturn(false);

        reviewManagementService.reportReview(reviewId, request);

        ArgumentCaptor<ReviewReport> reportCaptor = ArgumentCaptor.forClass(ReviewReport.class);
        verify(reviewReportRepository).save(reportCaptor.capture());
        assertEquals(reviewId, reportCaptor.getValue().getReviewId());
        assertEquals(reporterId, reportCaptor.getValue().getReportedByUserId());
        assertEquals(1L, review.getReportCount());
        verify(reviewRepository).save(review);
    }

    @Test
    void customerReviewLookupSupportsOrderAndLegacyBookingOwnership() {
        ReviewEntity review = review(5L, 101L, 11L, ReviewStatus.APPROVED);
        review.setReviewComment("Good");
        review.setCreatedAt(LocalDateTime.of(2026, 6, 1, 10, 0));
        Order order = new Order();
        order.setId(101L);
        order.setUserId(11L);
        when(securityUtils.getCurrentUserId()).thenReturn(Optional.of(11L));
        when(orderRepository.findById(101L)).thenReturn(Optional.of(order));
        when(reviewRepository.findByBookingIdAndCustomerIdAndDeletedFalse(101L, 11L))
                .thenReturn(Optional.of(review));

        var response = reviewManagementService.getCustomerReviewByBookingId(101L);

        assertThat(response.reviewId()).isEqualTo(5L);
        assertThat(response.reviewText()).isEqualTo("Good");

        Booking booking = mockBooking(102L, 11L, BookingStatus.COMPLETED);
        when(orderRepository.findById(102L)).thenReturn(Optional.empty());
        when(bookingRepository.findByIdAndDeletedFalse(102L)).thenReturn(Optional.of(booking));
        when(reviewRepository.findByBookingIdAndCustomerIdAndDeletedFalse(102L, 11L))
                .thenReturn(Optional.empty());
        assertThatThrownBy(() -> reviewManagementService.getCustomerReviewByBookingId(102L))
                .isInstanceOf(ReviewNotFoundException.class);
    }

    @Test
    void lookupAndMutationRejectUnauthenticatedOrNonOwnerUsers() {
        when(securityUtils.getCurrentUserId()).thenReturn(Optional.empty());
        assertThatThrownBy(() -> reviewManagementService.getMyReviews())
                .isInstanceOf(ReviewAccessDeniedException.class);

        ReviewEntity review = review(5L, 101L, 22L, ReviewStatus.PENDING);
        when(securityUtils.getCurrentUserId()).thenReturn(Optional.of(11L));
        when(reviewRepository.findByIdAndDeletedFalse(5L)).thenReturn(Optional.of(review));
        UpdateReviewRequest request = new UpdateReviewRequest();
        request.setRating(4);

        assertThatThrownBy(() -> reviewManagementService.updateMyReview(5L, request))
                .isInstanceOf(ReviewAccessDeniedException.class);
        assertThatThrownBy(() -> reviewManagementService.deleteMyReview(5L))
                .isInstanceOf(ReviewAccessDeniedException.class);
        verify(reviewRepository, never()).save(review);
    }

    @Test
    void updateAndDeleteOwnedReviewNormalizeContentAndPersist() {
        ReviewEntity review = review(5L, 101L, 11L, ReviewStatus.APPROVED);
        ReviewResponse mapped = new ReviewResponse();
        mapped.setId(5L);
        UpdateReviewRequest request = new UpdateReviewRequest();
        request.setRating(3);
        request.setReviewComment("   ");
        request.setReviewImages(List.of("one.jpg"));
        when(securityUtils.getCurrentUserId()).thenReturn(Optional.of(11L));
        when(reviewRepository.findByIdAndDeletedFalse(5L)).thenReturn(Optional.of(review));
        when(reviewMapper.serializeImages(List.of("one.jpg"))).thenReturn("one.jpg");
        when(reviewRepository.save(review)).thenReturn(review);
        when(reviewMapper.toResponse(review)).thenReturn(mapped);

        assertThat(reviewManagementService.updateMyReview(5L, request).getId()).isEqualTo(5L);
        assertThat(review.getRating()).isEqualTo(3);
        assertThat(review.getReviewComment()).isNull();
        assertThat(review.getStatus()).isEqualTo(ReviewStatus.PENDING);

        assertThat(reviewManagementService.deleteMyReview(5L).getMessage())
                .isEqualTo("Review deleted successfully");
        assertThat(review.isDeleted()).isTrue();
    }

    @Test
    void reportingRejectsOwnAndDuplicateReports() {
        ReportReviewRequest request = new ReportReviewRequest();
        request.setReason("spam");
        ReviewEntity own = review(5L, 101L, 11L, ReviewStatus.APPROVED);
        when(securityUtils.getCurrentUserId()).thenReturn(Optional.of(11L));
        when(reviewRepository.findByIdAndDeletedFalse(5L)).thenReturn(Optional.of(own));
        assertThatThrownBy(() -> reviewManagementService.reportReview(5L, request))
                .isInstanceOf(ReviewValidationException.class)
                .hasMessageContaining("own review");

        own.setCustomerId(22L);
        when(reviewReportRepository.existsByReviewIdAndReportedByUserId(5L, 11L)).thenReturn(true);
        assertThatThrownBy(() -> reviewManagementService.reportReview(5L, request))
                .isInstanceOf(ReviewValidationException.class)
                .hasMessageContaining("already reported");
    }

    @Test
    void moderationTransitionsRecalculateRatingsAndAudit() {
        ReviewEntity review = review(5L, 101L, 11L, ReviewStatus.PENDING);
        review.setProviderId(44L);
        ReviewResponse mapped = new ReviewResponse();
        Partner partner = mock(Partner.class);
        PartnerOperationalProfile profile = mock(PartnerOperationalProfile.class);
        when(reviewRepository.findByIdAndDeletedFalse(5L)).thenReturn(Optional.of(review));
        when(reviewRepository.save(review)).thenReturn(review);
        when(reviewMapper.toResponse(review)).thenReturn(mapped);
        when(reviewRepository.averageRatingByProviderIdAndStatus(44L, ReviewStatus.APPROVED))
                .thenReturn(4.456);
        when(reviewRepository.countByProviderIdAndStatusAndDeletedFalse(44L, ReviewStatus.APPROVED))
                .thenReturn(9L);
        when(partnerRepository.findById(44L)).thenReturn(Optional.of(partner));
        when(partnerProfileRepository.findById(44L)).thenReturn(Optional.of(profile));
        when(securityUtils.getCurrentUserId()).thenReturn(Optional.empty());

        reviewManagementService.approveReview(5L);

        assertThat(review.getStatus()).isEqualTo(ReviewStatus.APPROVED);
        verify(partner).setRating(4.46);
        verify(partner).setTotalReviews(9L);
        verify(partnerProfileRepository).save(profile);
        verify(auditLogService).log(0L, "ADMIN", "REVIEW_APPROVED", "REVIEW",
                "5", "PENDING", "APPROVED");

        reviewManagementService.rejectReview(5L);
        assertThat(review.getStatus()).isEqualTo(ReviewStatus.REJECTED);
        reviewManagementService.hideReview(5L);
        assertThat(review.getStatus()).isEqualTo(ReviewStatus.HIDDEN);
    }

    @Test
    void adminAndProviderPagesClampPaginationAndMapContent() {
        ReviewEntity review = review(5L, 101L, 11L, ReviewStatus.APPROVED);
        ReviewResponse mapped = new ReviewResponse();
        mapped.setId(5L);
        when(reviewMapper.toResponse(review)).thenReturn(mapped);
        when(reviewRepository.searchAdminReviews(any(), any(), any(), any(), any(), any()))
                .thenReturn(new PageImpl<>(List.of(review)));

        var adminPage = reviewManagementService.getAllReviewsForAdmin(
                -2, 500, "invalid", "asc", 5, ReviewStatus.APPROVED, 44L, 3L, "  good ");

        assertThat(adminPage.getContent()).extracting(ReviewResponse::getId).containsExactly(5L);
        ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);
        verify(reviewRepository).searchAdminReviews(eq("good"), eq(ReviewStatus.APPROVED), eq(5), eq(44L), eq(3L),
                pageable.capture());
        assertThat(pageable.getValue().getPageNumber()).isZero();
        assertThat(pageable.getValue().getPageSize()).isEqualTo(100);
        assertThat(pageable.getValue().getSort().getOrderFor("createdAt").isAscending()).isTrue();

        when(reviewRepository.findByReportCountGreaterThanAndDeletedFalse(anyLong(), any()))
                .thenReturn(new PageImpl<>(List.of(review)));
        assertThat(reviewManagementService.getReportedReviewsForAdmin(-1, 0).getContent()).hasSize(1);

        when(securityUtils.getCurrentUserId()).thenReturn(Optional.of(44L));
        when(reviewRepository.findByProviderIdAndDeletedFalse(any(), any()))
                .thenReturn(new PageImpl<>(List.of(review)));
        assertThat(reviewManagementService.getProviderReviews(-1, 1000).getContent()).hasSize(1);
    }

    @Test
    void reviewListsDetailAndStatisticsMapMissingAndPresentNames() {
        ReviewEntity review = review(5L, 101L, 11L, ReviewStatus.APPROVED);
        ReviewResponse mapped = new ReviewResponse();
        mapped.setId(5L);
        when(securityUtils.getCurrentUserId()).thenReturn(Optional.of(11L));
        when(reviewRepository.findByCustomerIdAndDeletedFalseOrderByCreatedAtDesc(11L))
                .thenReturn(List.of(review));
        when(reviewRepository.findByIdAndDeletedFalse(5L)).thenReturn(Optional.of(review));
        when(reviewMapper.toResponse(review)).thenReturn(mapped);

        assertThat(reviewManagementService.getMyReviews()).hasSize(1);
        assertThat(reviewManagementService.getReviewByIdForAdmin(5L).getId()).isEqualTo(5L);

        User provider = mock(User.class);
        when(provider.getFullName()).thenReturn("Provider");
        ServiceCategory category = mock(ServiceCategory.class);
        when(category.getName()).thenReturn("Cleaning");
        when(reviewRepository.countByDeletedFalse()).thenReturn(10L);
        when(reviewRepository.countByDeletedFalseAndReportCountGreaterThan(0L)).thenReturn(2L);
        when(reviewRepository.countByDeletedFalseAndStatus(ReviewStatus.HIDDEN)).thenReturn(1L);
        when(reviewRepository.averageRating()).thenReturn(null);
        when(reviewRepository.providerWiseRatingSummary())
                .thenReturn(List.of(new Object[]{44L, 4.555, 7L}, new Object[]{45L, 3.0, 1L}));
        when(reviewRepository.serviceWiseRatingSummary())
                .thenReturn(java.util.Collections.singletonList(new Object[]{3L, 4.4, 8L}));
        when(userRepository.findById(44L)).thenReturn(Optional.of(provider));
        when(userRepository.findById(45L)).thenReturn(Optional.empty());
        when(serviceCategoryLookupRepository.findById(3L)).thenReturn(Optional.of(category));

        var stats = reviewManagementService.getReviewStats();

        assertThat(stats.getTotalReviews()).isEqualTo(10L);
        assertThat(stats.getAverageRating()).isZero();
        assertThat(stats.getProviderWiseRatings()).hasSize(2);
        assertThat(stats.getProviderWiseRatings().get(0).getAverageRating()).isEqualTo(4.56);
        assertThat(stats.getProviderWiseRatings().get(1).getProviderName()).isNull();
        assertThat(stats.getServiceWiseRatings().get(0).getServiceName()).isEqualTo("Cleaning");
    }

    @Test
    void orderEligibilityRejectsWrongOwnerIncompleteAndDuplicate() {
        SubmitReviewRequest request = new SubmitReviewRequest();
        request.setBookingId(101L);
        request.setRating(5);
        Order order = new Order();
        order.setId(101L);
        order.setUserId(22L);
        order.setStatus(OrderStatus.SERVICE_COMPLETED);
        when(securityUtils.getCurrentUserId()).thenReturn(Optional.of(11L));
        when(orderRepository.findById(101L)).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> reviewManagementService.submitReview(request))
                .isInstanceOf(ReviewAccessDeniedException.class);

        order.setUserId(11L);
        order.setStatus(OrderStatus.CONFIRMED);
        assertThatThrownBy(() -> reviewManagementService.submitReview(request))
                .isInstanceOf(ReviewValidationException.class)
                .hasMessageContaining("completed");

        order.setStatus(OrderStatus.DELIVERED);
        when(reviewRepository.existsByBookingIdAndDeletedFalse(101L)).thenReturn(true);
        assertThatThrownBy(() -> reviewManagementService.submitReview(request))
                .isInstanceOf(ReviewValidationException.class)
                .hasMessageContaining("one review");
    }

    @Test
    void completedLegacyPartnerOrderStatusIsReviewEligibleThroughConverterMapping() {
        SubmitReviewRequest request = new SubmitReviewRequest();
        request.setBookingId(303L);
        request.setRating(5);
        Order order = new Order();
        order.setId(303L);
        order.setUserId(11L);
        order.setAssignedPartnerId(22L);
        order.setStatus(OrderStatus.SERVICE_COMPLETED);
        when(securityUtils.getCurrentUserId()).thenReturn(Optional.of(11L));
        when(orderRepository.findById(303L)).thenReturn(Optional.of(order));
        when(reviewRepository.existsByBookingIdAndDeletedFalse(303L)).thenReturn(false);
        when(reviewRepository.save(any(ReviewEntity.class))).thenAnswer(invocation -> {
            ReviewEntity saved = invocation.getArgument(0);
            saved.setId(909L);
            return saved;
        });

        reviewManagementService.submitReview(request);

        verify(reviewRepository).save(any(ReviewEntity.class));
    }

    @Test
    void submitReviewCreatesLegacyBookingReviewWhenOrderIsMissing() {
        SubmitReviewRequest request = new SubmitReviewRequest();
        request.setBookingId(101L);
        request.setRating(4);
        request.setReviewComment("  Good work  ");
        Booking booking = mockBooking(101L, 11L, BookingStatus.COMPLETED);
        ReviewResponse mapped = new ReviewResponse();
        mapped.setId(77L);
        when(securityUtils.getCurrentUserId()).thenReturn(Optional.of(11L));
        when(orderRepository.findById(101L)).thenReturn(Optional.empty());
        when(bookingRepository.findByIdAndDeletedFalse(101L)).thenReturn(Optional.of(booking));
        when(reviewRepository.existsByBookingIdAndDeletedFalse(101L)).thenReturn(false);
        when(reviewRepository.save(any(ReviewEntity.class))).thenAnswer(invocation -> {
            ReviewEntity saved = invocation.getArgument(0);
            saved.setId(77L);
            return saved;
        });
        when(reviewMapper.toResponse(any(ReviewEntity.class))).thenReturn(mapped);

        ReviewResponse response = reviewManagementService.submitReview(request);

        assertThat(response.getId()).isEqualTo(77L);
        ArgumentCaptor<ReviewEntity> reviewCaptor = ArgumentCaptor.forClass(ReviewEntity.class);
        verify(reviewRepository).save(reviewCaptor.capture());
        assertThat(reviewCaptor.getValue().getBookingId()).isEqualTo(101L);
        assertThat(reviewCaptor.getValue().getCustomerId()).isEqualTo(11L);
        assertThat(reviewCaptor.getValue().getReviewComment()).isEqualTo("Good work");
        assertThat(reviewCaptor.getValue().getStatus()).isEqualTo(ReviewStatus.PENDING);
    }

    @Test
    void advancedAdminSearchParsesDateFiltersAndRejectsInvalidDates() {
        ReviewEntity review = review(5L, 101L, 11L, ReviewStatus.APPROVED);
        ReviewResponse mapped = new ReviewResponse();
        mapped.setId(5L);
        when(reviewMapper.toResponse(review)).thenReturn(mapped);
        when(reviewRepository.searchAdminReviewsAdvanced(any(), any(), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(new PageImpl<>(List.of(review)));

        var response = reviewManagementService.getAllReviewsForAdmin(
                1, 20, "rating", "desc", 5, ReviewStatus.APPROVED, 44L, 3L, "  clean  ",
                "2026-08-01T10:15:00", "2026-08-02T10:15:00", true);

        assertThat(response.getContent()).extracting(ReviewResponse::getId).containsExactly(5L);
        verify(reviewRepository).searchAdminReviewsAdvanced(
                eq("clean"), eq(ReviewStatus.APPROVED), eq(5), eq(44L), eq(3L),
                eq(LocalDateTime.parse("2026-08-01T10:15:00")),
                eq(LocalDateTime.parse("2026-08-02T10:15:00")),
                eq(true), any(Pageable.class));

        assertThatThrownBy(() -> reviewManagementService.getAllReviewsForAdmin(
                0, 10, "createdAt", "desc", null, null, null, null, null, "bad-date", null, null))
                .isInstanceOf(ReviewValidationException.class)
                .extracting("code")
                .isEqualTo("INVALID_DATE_RANGE");
    }

    @Test
    void reviewDetailMapsBookingUsersServiceReportsAndHistory() {
        ReviewEntity review = review(5L, 101L, 11L, ReviewStatus.APPROVED);
        review.setProviderId(44L);
        review.setServiceId(3L);
        review.setReviewComment("Great");
        ReviewResponse mapped = new ReviewResponse();
        mapped.setId(5L);
        Order order = new Order();
        order.setId(101L);
        order.setUserId(11L);
        order.setAssignedPartnerId(44L);
        order.setStatus(OrderStatus.DELIVERED);
        User customer = mock(User.class);
        when(customer.getFullName()).thenReturn("Customer");
        when(customer.getEmail()).thenReturn("customer@example.test");
        User reporter = mock(User.class);
        when(reporter.getFullName()).thenReturn("Reporter");
        AdminServiceEntity service = new AdminServiceEntity();
        service.setName("Cleaning");
        service.setReviewEnabled(false);
        ReviewReport report = new ReviewReport();
        report.setId(9L);
        report.setReviewId(5L);
        report.setReportedByUserId(66L);
        report.setReason("spam");
        report.setStatus(ReviewReportStatus.OPEN);
        ReviewModerationHistory history = new ReviewModerationHistory();
        history.setId(8L);
        history.setReviewId(5L);
        history.setPreviousStatus(ReviewStatus.PENDING);
        history.setNewStatus(ReviewStatus.APPROVED);
        history.setAction("APPROVE");
        history.setAdminId(1L);
        when(reviewRepository.findByIdAndDeletedFalse(5L)).thenReturn(Optional.of(review));
        when(reviewMapper.toResponse(review)).thenReturn(mapped);
        when(orderRepository.findById(101L)).thenReturn(Optional.of(order));
        when(userRepository.findById(11L)).thenReturn(Optional.of(customer));
        when(userRepository.findById(44L)).thenReturn(Optional.empty());
        when(userRepository.findById(66L)).thenReturn(Optional.of(reporter));
        when(adminServiceEntityRepository.findByIdAndDeletedFalse(3L)).thenReturn(Optional.of(service));
        when(reviewReportRepository.findByReviewIdOrderByCreatedAtDesc(5L)).thenReturn(List.of(report));
        when(reviewRepository.findById(5L)).thenReturn(Optional.of(review));
        when(reviewModerationHistoryRepository.findByReviewIdOrderByCreatedAtAsc(5L)).thenReturn(List.of(history));

        var detail = reviewManagementService.getReviewDetailForAdmin(5L);

        assertThat(detail.review().getId()).isEqualTo(5L);
        assertThat(detail.booking()).containsEntry("status", OrderStatus.DELIVERED);
        assertThat(detail.customer()).containsEntry("name", "Customer");
        assertThat(detail.partner()).containsEntry("id", 44L);
        assertThat(detail.service()).containsEntry("name", "Cleaning");
        assertThat(detail.reports().get(0).reporterName()).isEqualTo("Reporter");
        assertThat(detail.moderationHistory().get(0).action()).isEqualTo("APPROVE");
    }

    @Test
    void reportPagesAndResolutionActionsMapReportsAndUpdateReviews() {
        ReviewEntity review = review(5L, 101L, 11L, ReviewStatus.APPROVED);
        ReviewReport report = new ReviewReport();
        report.setId(9L);
        report.setReviewId(5L);
        report.setReportedByUserId(66L);
        report.setReason("spam");
        report.setStatus(ReviewReportStatus.OPEN);
        when(reviewReportRepository.findAll(any(Pageable.class))).thenReturn(new PageImpl<>(List.of(report)));
        when(reviewReportRepository.findByStatus(eq(ReviewReportStatus.OPEN), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(report)));
        when(reviewRepository.findById(5L)).thenReturn(Optional.of(review));
        when(userRepository.findById(66L)).thenReturn(Optional.empty());

        assertThat(reviewManagementService.getReviewReportsForAdmin(-1, 0, null).getContent()).hasSize(1);
        assertThat(reviewManagementService.getReviewReportsForAdmin(0, 10, ReviewReportStatus.OPEN).getContent()).hasSize(1);

        when(securityUtils.getCurrentUserId()).thenReturn(Optional.of(1L));
        when(reviewReportRepository.findById(9L)).thenReturn(Optional.of(report));
        when(reviewReportRepository.save(report)).thenReturn(report);
        ReviewReportResponse dismissed = reviewManagementService.dismissReport(9L, "  duplicate  ");
        assertThat(dismissed.status()).isEqualTo(ReviewReportStatus.DISMISSED);
        assertThat(dismissed.resolutionNote()).isEqualTo("duplicate");

        ReviewResponse mapped = new ReviewResponse();
        mapped.setId(5L);
        when(reviewRepository.findByIdAndDeletedFalse(5L)).thenReturn(Optional.of(review));
        when(reviewRepository.save(review)).thenReturn(review);
        when(reviewMapper.toResponse(review)).thenReturn(mapped);
        assertThat(reviewManagementService.hideReportedReview(9L, "policy").getId()).isEqualTo(5L);
        assertThat(review.getStatus()).isEqualTo(ReviewStatus.HIDDEN);
        review.setStatus(ReviewStatus.APPROVED);
        assertThat(reviewManagementService.rejectReportedReview(9L, "policy").getId()).isEqualTo(5L);
        assertThat(review.getStatus()).isEqualTo(ReviewStatus.REJECTED);
    }

    @Test
    void filteredStatsBuildsDistributionAndSkipsGroupedSummariesWhenFiltered() {
        LocalDateTime start = LocalDateTime.parse("2026-08-01T00:00:00");
        LocalDateTime end = LocalDateTime.parse("2026-08-31T23:59:00");
        when(reviewRepository.countForStats(3L, 44L, start, end)).thenReturn(12L);
        when(reviewRepository.countByStatusForStats(ReviewStatus.PENDING, 3L, 44L, start, end)).thenReturn(1L);
        when(reviewRepository.countByStatusForStats(ReviewStatus.APPROVED, 3L, 44L, start, end)).thenReturn(8L);
        when(reviewRepository.countByStatusForStats(ReviewStatus.REJECTED, 3L, 44L, start, end)).thenReturn(2L);
        when(reviewRepository.countByStatusForStats(ReviewStatus.HIDDEN, 3L, 44L, start, end)).thenReturn(1L);
        when(reviewRepository.averageRatingForStats(3L, 44L, start, end)).thenReturn(4.234);
        when(reviewRepository.countByDeletedFalseAndReportCountGreaterThan(0L)).thenReturn(3L);
        when(reviewRepository.ratingDistribution(3L, 44L, start, end))
                .thenReturn(List.of(new Object[]{5, 6L}, new Object[]{1, 2L}));

        var stats = reviewManagementService.getReviewStats(
                "2026-08-01T00:00:00", "2026-08-31T23:59:00", 3L, 44L);

        assertThat(stats.getTotalReviews()).isEqualTo(12L);
        assertThat(stats.getAverageRating()).isEqualTo(4.23);
        assertThat(stats.getRatingDistribution()).extracting("rating").containsExactly(5, 4, 3, 2, 1);
        assertThat(stats.getRatingDistribution()).extracting("count").containsExactly(6L, 0L, 0L, 0L, 2L);
        assertThat(stats.getProviderWiseRatings()).isEmpty();
        assertThat(stats.getServiceWiseRatings()).isEmpty();
    }

    @Test
    void reviewSettingsReadAndUpdateGlobalAndServiceSettings() {
        AdminCategoryEntity category = mock(AdminCategoryEntity.class);
        when(category.getId()).thenReturn(2L);
        when(category.getName()).thenReturn("Home");
        AdminServiceEntity service = new AdminServiceEntity();
        service.setName("Cleaning");
        service.setCategory(category);
        service.setReviewEnabled(true);
        when(adminSettingsService.get("review.reviews.enabled")).thenReturn(Map.of("value", "false"));
        when(adminSettingsService.get("review.rating.enabled")).thenThrow(new RuntimeException("missing"));
        when(adminSettingsService.get("review.written.enabled")).thenReturn(Map.of("value", "true"));
        when(adminSettingsService.get("review.auto_approve.enabled")).thenReturn(Map.of("value", "true"));
        when(adminServiceEntityRepository.findByDeletedFalseAndActiveTrue()).thenReturn(List.of(service));

        var settings = reviewManagementService.getReviewSettings();

        assertThat(settings.reviewsEnabled()).isFalse();
        assertThat(settings.ratingEnabled()).isTrue();
        assertThat(settings.autoApproveReviews()).isTrue();
        assertThat(settings.services().get(0).categoryName()).isEqualTo("Home");

        ReviewSettingsUpdateRequest request = new ReviewSettingsUpdateRequest();
        request.reviewsEnabled = true;
        request.ratingEnabled = false;
        request.writtenReviewsEnabled = true;
        request.autoApproveReviews = false;
        ReviewSettingsUpdateRequest.ServiceReviewSettingUpdate ignored = new ReviewSettingsUpdateRequest.ServiceReviewSettingUpdate();
        ReviewSettingsUpdateRequest.ServiceReviewSettingUpdate item = new ReviewSettingsUpdateRequest.ServiceReviewSettingUpdate();
        item.serviceId = 3L;
        item.reviewEnabled = false;
        request.services = List.of(ignored, item);
        when(securityUtils.getCurrentUserId()).thenReturn(Optional.of(1L));
        when(adminServiceEntityRepository.findByIdAndDeletedFalse(3L)).thenReturn(Optional.of(service));
        when(adminServiceEntityRepository.save(service)).thenReturn(service);

        var updated = reviewManagementService.updateReviewSettings(request);

        assertThat(updated.services()).hasSize(1);
        assertThat(service.isReviewEnabled()).isFalse();
        verify(adminSettingsService).upsert("review.reviews.enabled", "true", "Global reviews enabled", "ADMIN:1");
        verify(adminSettingsService).upsert("review.auto_approve.enabled", "false", "Auto approve reviews", "ADMIN:1");
        verify(auditLogService).log(1L, "ADMIN", "REVIEW_SETTINGS_UPDATED", "REVIEW_SETTINGS", "global", null, "updated");

        item.serviceId = 99L;
        when(adminServiceEntityRepository.findByIdAndDeletedFalse(99L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> reviewManagementService.updateReviewSettings(request))
                .isInstanceOf(ReviewNotFoundException.class)
                .extracting("code")
                .isEqualTo("SERVICE_NOT_FOUND");
    }

    private ReviewEntity review(Long id, Long bookingId, Long customerId, ReviewStatus status) {
        ReviewEntity review = new ReviewEntity();
        review.setId(id);
        review.setBookingId(bookingId);
        review.setCustomerId(customerId);
        review.setRating(4);
        review.setStatus(status);
        review.setReportCount(0L);
        return review;
    }

    private Booking mockBooking(Long bookingId, Long customerId, BookingStatus status) {
        Booking booking = org.mockito.Mockito.mock(Booking.class);
        User customer = org.mockito.Mockito.mock(User.class);
        lenient().when(booking.getId()).thenReturn(bookingId);
        when(booking.getCustomer()).thenReturn(customer);
        when(customer.getId()).thenReturn(customerId);
        lenient().when(booking.getBookingStatus()).thenReturn(status);
        return booking;
    }
}

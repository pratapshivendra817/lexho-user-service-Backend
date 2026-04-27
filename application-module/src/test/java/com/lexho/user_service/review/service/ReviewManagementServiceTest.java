package com.lexho.user_service.review.service;

import com.lexho.booking.entity.Booking;
import com.lexho.booking.enums.BookingStatus;
import com.lexho.booking.repository.BookingRepository;
import com.lexho.user.entity.User;
import com.lexho.user.repository.UserRepository;
import com.lexho.user_service.review.dto.ReportReviewRequest;
import com.lexho.user_service.review.dto.SubmitReviewRequest;
import com.lexho.user_service.review.entity.ReviewEntity;
import com.lexho.user_service.review.entity.ReviewReport;
import com.lexho.user_service.review.exception.ReviewValidationException;
import com.lexho.user_service.review.mapper.ReviewMapper;
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

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.lenient;
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
    private UserRepository userRepository;
    @Mock
    private ServiceCategoryLookupRepository serviceCategoryLookupRepository;
    @Mock
    private ReviewMapper reviewMapper;
    @Mock
    private SecurityUtils securityUtils;

    @InjectMocks
    private ReviewManagementService reviewManagementService;

    @Test
    void submitReviewFailsWhenBookingNotCompleted() {
        SubmitReviewRequest request = new SubmitReviewRequest();
        request.setBookingId(101L);
        request.setRating(5);

        Booking booking = mockBooking(101L, 11L, BookingStatus.PENDING);
        when(securityUtils.getCurrentUserId()).thenReturn(Optional.of(11L));
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
        when(bookingRepository.findByIdAndDeletedFalse(101L)).thenReturn(Optional.of(booking));
        when(reviewRepository.existsByBookingIdAndDeletedFalse(101L)).thenReturn(true);

        assertThrows(ReviewValidationException.class, () -> reviewManagementService.submitReview(request));
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

    private Booking mockBooking(Long bookingId, Long customerId, BookingStatus status) {
        Booking booking = org.mockito.Mockito.mock(Booking.class);
        User customer = org.mockito.Mockito.mock(User.class);
        lenient().when(booking.getId()).thenReturn(bookingId);
        when(booking.getCustomer()).thenReturn(customer);
        when(customer.getId()).thenReturn(customerId);
        when(booking.getBookingStatus()).thenReturn(status);
        return booking;
    }
}

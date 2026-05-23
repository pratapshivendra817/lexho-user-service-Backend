package com.lexho.user_service.review.service;

import com.lexho.booking.entity.Booking;
import com.lexho.booking.enums.BookingStatus;
import com.lexho.booking.repository.BookingRepository;
import com.lexho.user.entity.User;
import com.lexho.user.repository.UserRepository;
import com.lexho.user_service.review.dto.ProviderRatingSummary;
import com.lexho.user_service.review.dto.ReportReviewRequest;
import com.lexho.user_service.review.dto.ReviewPageResponse;
import com.lexho.user_service.review.dto.ReviewResponse;
import com.lexho.user_service.review.dto.ReviewStatsResponse;
import com.lexho.user_service.review.dto.ServiceRatingSummary;
import com.lexho.user_service.review.dto.SimpleMessageResponse;
import com.lexho.user_service.review.dto.SubmitReviewRequest;
import com.lexho.user_service.review.dto.UpdateReviewRequest;
import com.lexho.user_service.review.entity.ReviewEntity;
import com.lexho.user_service.review.entity.ReviewReport;
import com.lexho.user_service.review.enums.ReviewStatus;
import com.lexho.user_service.review.exception.ReviewAccessDeniedException;
import com.lexho.user_service.review.exception.ReviewNotFoundException;
import com.lexho.user_service.review.exception.ReviewValidationException;
import com.lexho.user_service.review.mapper.ReviewMapper;
import com.lexho.user_service.review.repository.ReviewReportRepository;
import com.lexho.user_service.review.repository.ReviewRepository;
import com.lexho.user_service.review.repository.ServiceCategoryLookupRepository;
import com.lexho.user_service.security.SecurityUtils;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Service
public class ReviewManagementService {

    private static final Logger log = LoggerFactory.getLogger(ReviewManagementService.class);

    private final ReviewRepository reviewRepository;
    private final ReviewReportRepository reviewReportRepository;
    private final BookingRepository bookingRepository;
    private final UserRepository userRepository;
    private final ServiceCategoryLookupRepository serviceCategoryLookupRepository;
    private final ReviewMapper reviewMapper;
    private final SecurityUtils securityUtils;

    public ReviewManagementService(ReviewRepository reviewRepository,
                                   ReviewReportRepository reviewReportRepository,
                                   BookingRepository bookingRepository,
                                   UserRepository userRepository,
                                   ServiceCategoryLookupRepository serviceCategoryLookupRepository,
                                   ReviewMapper reviewMapper,
                                   SecurityUtils securityUtils) {
        this.reviewRepository = reviewRepository;
        this.reviewReportRepository = reviewReportRepository;
        this.bookingRepository = bookingRepository;
        this.userRepository = userRepository;
        this.serviceCategoryLookupRepository = serviceCategoryLookupRepository;
        this.reviewMapper = reviewMapper;
        this.securityUtils = securityUtils;
    }

    @Transactional
    public ReviewResponse submitReview(SubmitReviewRequest request) {
        Long customerId = currentUserId();
        Booking booking = bookingRepository.findByIdAndDeletedFalse(request.getBookingId())
                .orElseThrow(() -> new ReviewValidationException("Completed booking is required before review"));

        validateBookingOwnership(booking, customerId);
        if (booking.getBookingStatus() != BookingStatus.COMPLETED) {
            throw new ReviewValidationException("Review is allowed only for completed bookings");
        }
        if (reviewRepository.existsByBookingIdAndDeletedFalse(booking.getId())) {
            throw new ReviewValidationException("Only one review is allowed per booking");
        }

        ReviewEntity review = new ReviewEntity();
        review.setBookingId(booking.getId());
        review.setCustomerId(customerId);
        review.setProviderId(booking.getPartner() != null ? booking.getPartner().getId() : null);
        review.setServiceId(booking.getService() != null ? booking.getService().getId() : null);
        review.setRating(request.getRating());
        review.setReviewComment(normalizeComment(request.getReviewComment()));
        review.setReviewImages(reviewMapper.serializeImages(request.getReviewImages()));
        review.setStatus(ReviewStatus.PENDING);
        review.setDeleted(false);
        review.setReportCount(0L);

        ReviewEntity saved = reviewRepository.save(review);
        log.info("Review submitted. reviewId={}, bookingId={}, customerId={}", saved.getId(), saved.getBookingId(), customerId);
        return reviewMapper.toResponse(saved);
    }

    @Transactional
    public ReviewResponse updateMyReview(Long reviewId, UpdateReviewRequest request) {
        Long customerId = currentUserId();
        ReviewEntity review = findActiveReview(reviewId);
        validateReviewOwnership(review, customerId);

        review.setRating(request.getRating());
        review.setReviewComment(normalizeComment(request.getReviewComment()));
        review.setReviewImages(reviewMapper.serializeImages(request.getReviewImages()));
        review.setStatus(ReviewStatus.PENDING);
        ReviewEntity saved = reviewRepository.save(review);
        log.info("Review updated. reviewId={}, customerId={}", reviewId, customerId);
        return reviewMapper.toResponse(saved);
    }

    @Transactional
    public SimpleMessageResponse deleteMyReview(Long reviewId) {
        Long customerId = currentUserId();
        ReviewEntity review = findActiveReview(reviewId);
        validateReviewOwnership(review, customerId);
        review.setDeleted(true);
        reviewRepository.save(review);
        log.info("Review soft deleted by customer. reviewId={}, customerId={}", reviewId, customerId);
        return new SimpleMessageResponse("Review deleted successfully");
    }

    @Transactional
    public SimpleMessageResponse reportReview(Long reviewId, ReportReviewRequest request) {
        Long reporterId = currentUserId();
        ReviewEntity review = findActiveReview(reviewId);
        if (review.getCustomerId() != null && review.getCustomerId().equals(reporterId)) {
            throw new ReviewValidationException("You cannot report your own review");
        }
        if (reviewReportRepository.existsByReviewIdAndReportedByUserId(reviewId, reporterId)) {
            throw new ReviewValidationException("You have already reported this review");
        }

        ReviewReport report = new ReviewReport();
        report.setReviewId(reviewId);
        report.setReportedByUserId(reporterId);
        report.setReason(request.getReason().trim());
        reviewReportRepository.save(report);

        review.setReportCount((review.getReportCount() == null ? 0L : review.getReportCount()) + 1L);
        reviewRepository.save(review);
        log.info("Review reported. reviewId={}, reporterId={}, reportCount={}", reviewId, reporterId, review.getReportCount());
        return new SimpleMessageResponse("Review reported successfully");
    }

    @Transactional
    public ReviewResponse approveReview(Long reviewId) {
        return updateStatus(reviewId, ReviewStatus.APPROVED);
    }

    @Transactional
    public ReviewResponse rejectReview(Long reviewId) {
        return updateStatus(reviewId, ReviewStatus.REJECTED);
    }

    @Transactional
    public ReviewResponse hideReview(Long reviewId) {
        return updateStatus(reviewId, ReviewStatus.HIDDEN);
    }

    public ReviewPageResponse<ReviewResponse> getAllReviewsForAdmin(int page,
                                                                    int size,
                                                                    String sortBy,
                                                                    String sortDir,
                                                                    Integer rating,
                                                                    ReviewStatus status,
                                                                    Long providerId,
                                                                    Long serviceId,
                                                                    String search) {
        Pageable pageable = buildPageable(page, size, sortBy, sortDir);
        Page<ReviewEntity> result = reviewRepository.searchAdminReviews(
                normalizeSearch(search),
                status,
                rating,
                providerId,
                serviceId,
                pageable
        );
        List<ReviewResponse> content = result.getContent().stream().map(reviewMapper::toResponse).toList();
        return new ReviewPageResponse<>(content, result.getTotalElements(), result.getTotalPages(), result.getNumber());
    }

    public ReviewResponse getReviewByIdForAdmin(Long reviewId) {
        return reviewMapper.toResponse(findActiveReview(reviewId));
    }

    public ReviewPageResponse<ReviewResponse> getReportedReviewsForAdmin(int page, int size) {
        Pageable pageable = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 100), Sort.by(Sort.Direction.DESC, "updatedAt"));
        Page<ReviewEntity> result = reviewRepository.findByReportCountGreaterThanAndDeletedFalse(0L, pageable);
        List<ReviewResponse> content = result.getContent().stream().map(reviewMapper::toResponse).toList();
        return new ReviewPageResponse<>(content, result.getTotalElements(), result.getTotalPages(), result.getNumber());
    }

    public List<ReviewResponse> getMyReviews() {
        Long customerId = currentUserId();
        return reviewRepository.findByCustomerIdAndDeletedFalseOrderByCreatedAtDesc(customerId)
                .stream()
                .map(reviewMapper::toResponse)
                .toList();
    }

    public ReviewPageResponse<ReviewResponse> getProviderReviews(int page, int size) {
        Long providerId = currentUserId();
        Pageable pageable = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 100), Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<ReviewEntity> result = reviewRepository.findByProviderIdAndDeletedFalse(providerId, pageable);
        List<ReviewResponse> content = result.getContent().stream().map(reviewMapper::toResponse).toList();
        return new ReviewPageResponse<>(content, result.getTotalElements(), result.getTotalPages(), result.getNumber());
    }

    public ReviewStatsResponse getReviewStats() {
        long totalReviews = reviewRepository.countByDeletedFalse();
        long totalReported = reviewRepository.countByDeletedFalseAndReportCountGreaterThan(0L);
        long hiddenReviews = reviewRepository.countByDeletedFalseAndStatus(ReviewStatus.HIDDEN);
        double averageRating = round(reviewRepository.averageRating());

        List<ProviderRatingSummary> providerWise = reviewRepository.providerWiseRatingSummary().stream()
                .map(this::toProviderSummary)
                .toList();
        List<ServiceRatingSummary> serviceWise = reviewRepository.serviceWiseRatingSummary().stream()
                .map(this::toServiceSummary)
                .toList();

        return new ReviewStatsResponse(
                totalReviews,
                averageRating,
                totalReported,
                hiddenReviews,
                providerWise,
                serviceWise
        );
    }

    private ReviewResponse updateStatus(Long reviewId, ReviewStatus status) {
        ReviewEntity review = findActiveReview(reviewId);
        review.setStatus(status);
        ReviewEntity saved = reviewRepository.save(review);
        log.info("Review status updated. reviewId={}, status={}", reviewId, status);
        return reviewMapper.toResponse(saved);
    }

    private ReviewEntity findActiveReview(Long reviewId) {
        return reviewRepository.findByIdAndDeletedFalse(reviewId)
                .orElseThrow(() -> new ReviewNotFoundException(reviewId));
    }

    private void validateBookingOwnership(Booking booking, Long customerId) {
        if (booking.getCustomer() == null || booking.getCustomer().getId() == null) {
            throw new ReviewValidationException("Booking customer not found");
        }
        if (!booking.getCustomer().getId().equals(customerId)) {
            throw new ReviewAccessDeniedException("You can review only your own booking");
        }
    }

    private void validateReviewOwnership(ReviewEntity review, Long customerId) {
        if (review.getCustomerId() == null || !review.getCustomerId().equals(customerId)) {
            throw new ReviewAccessDeniedException("You can update only your own review");
        }
    }

    private Pageable buildPageable(int page, int size, String sortBy, String sortDir) {
        Sort.Direction direction = "asc".equalsIgnoreCase(sortDir) ? Sort.Direction.ASC : Sort.Direction.DESC;
        String resolvedSortBy = switch (sortBy == null ? "" : sortBy) {
            case "createdAt", "updatedAt", "rating", "reportCount", "status" -> sortBy;
            default -> "createdAt";
        };
        return PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 100), Sort.by(direction, resolvedSortBy));
    }

    private Long currentUserId() {
        return securityUtils.getCurrentUserId()
                .orElseThrow(() -> new ReviewAccessDeniedException("Authenticated user not found"));
    }

    private String normalizeSearch(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String normalizeComment(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private ProviderRatingSummary toProviderSummary(Object[] row) {
        Long providerId = (Long) row[0];
        Double average = ((Number) row[1]).doubleValue();
        Long total = ((Number) row[2]).longValue();
        String name = userRepository.findById(providerId).map(User::getFullName).orElse(null);
        return new ProviderRatingSummary(providerId, name, round(average), total);
    }

    private ServiceRatingSummary toServiceSummary(Object[] row) {
        Long serviceId = (Long) row[0];
        Double average = ((Number) row[1]).doubleValue();
        Long total = ((Number) row[2]).longValue();
        String name = serviceCategoryLookupRepository.findById(serviceId).map(s -> s.getName()).orElse(null);
        return new ServiceRatingSummary(serviceId, name, round(average), total);
    }

    private double round(Double value) {
        if (value == null) {
            return 0.0;
        }
        return BigDecimal.valueOf(value).setScale(2, RoundingMode.HALF_UP).doubleValue();
    }
}

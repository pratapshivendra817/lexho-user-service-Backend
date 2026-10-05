package com.lexho.user_service.review.service;

import com.lexho.booking.entity.Booking;
import com.lexho.booking.entity.Order;
import com.lexho.booking.enums.BookingStatus;
import com.lexho.booking.repository.BookingRepository;
import com.lexho.booking.repository.OrderRepository;
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
import com.lexho.user_service.review.dto.CustomerReviewResponse;
import com.lexho.user_service.review.dto.CustomerReviewSubmitResponse;
import com.lexho.user_service.review.dto.RatingDistributionResponse;
import com.lexho.user_service.review.dto.ProviderRatingSummary;
import com.lexho.user_service.review.dto.ReportReviewRequest;
import com.lexho.user_service.review.dto.ReviewDetailResponse;
import com.lexho.user_service.review.dto.ReviewModerationHistoryResponse;
import com.lexho.user_service.review.dto.ReviewPageResponse;
import com.lexho.user_service.review.dto.ReviewReportResponse;
import com.lexho.user_service.review.dto.ReviewResponse;
import com.lexho.user_service.review.dto.ReviewSettingsResponse;
import com.lexho.user_service.review.dto.ReviewSettingsUpdateRequest;
import com.lexho.user_service.review.dto.ReviewStatsResponse;
import com.lexho.user_service.review.dto.ServiceRatingSummary;
import com.lexho.user_service.review.dto.SimpleMessageResponse;
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
import com.lexho.user_service.lexhopartner.entity.PartnerOperationalProfile;
import com.lexho.user_service.lexhopartner.repository.PartnerOperationalProfileRepository;
import com.lexho.user_service.lexhopartner.service.CompletionSettingsService;
import com.lexho.user_service.lexhopartner.service.PartnerMarketplaceService;
import com.lexho.user_service.security.SecurityUtils;
import com.lexho.user_service.util.LexhoConstants;
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
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

@Service
public class ReviewManagementService {

    private static final Logger log = LoggerFactory.getLogger(ReviewManagementService.class);
    private static final String ACTION_APPROVE = "APPROVE";
    private static final String KEY_STATUS = "status";

    private final ReviewRepository reviewRepository;
    private final ReviewReportRepository reviewReportRepository;
    private final ReviewModerationHistoryRepository reviewModerationHistoryRepository;
    private final BookingRepository bookingRepository;
    private final OrderRepository orderRepository;
    private final UserRepository userRepository;
    private final PartnerRepository partnerRepository;
    private final PartnerMarketplaceService partnerMarketplaceService;
    private final PartnerOperationalProfileRepository partnerProfileRepository;
    private final AdminServiceEntityRepository adminServiceEntityRepository;
    private final AdminSettingsService adminSettingsService;
    private final ServiceCategoryLookupRepository serviceCategoryLookupRepository;
    private final ReviewMapper reviewMapper;
    private final SecurityUtils securityUtils;
    private final AuditLogService auditLogService;

    @org.springframework.beans.factory.annotation.Autowired(required = false)
    private CompletionSettingsService completionSettingsService;

    public ReviewManagementService(ReviewRepository reviewRepository,
                                   ReviewReportRepository reviewReportRepository,
                                   ReviewModerationHistoryRepository reviewModerationHistoryRepository,
                                   BookingRepository bookingRepository,
                                   OrderRepository orderRepository,
                                   UserRepository userRepository,
                                   PartnerRepository partnerRepository,
                                   PartnerOperationalProfileRepository partnerProfileRepository,
                                   AdminServiceEntityRepository adminServiceEntityRepository,
                                   AdminSettingsService adminSettingsService,
                                   ServiceCategoryLookupRepository serviceCategoryLookupRepository,
                                   ReviewMapper reviewMapper,
                                   SecurityUtils securityUtils,
                                   AuditLogService auditLogService) {
        this.reviewRepository = reviewRepository;
        this.reviewReportRepository = reviewReportRepository;
        this.reviewModerationHistoryRepository = reviewModerationHistoryRepository;
        this.bookingRepository = bookingRepository;
        this.orderRepository = orderRepository;
        this.userRepository = userRepository;
        this.partnerRepository = partnerRepository;
        this.partnerMarketplaceService = new PartnerMarketplaceService(partnerRepository);
        this.partnerProfileRepository = partnerProfileRepository;
        this.adminServiceEntityRepository = adminServiceEntityRepository;
        this.adminSettingsService = adminSettingsService;
        this.serviceCategoryLookupRepository = serviceCategoryLookupRepository;
        this.reviewMapper = reviewMapper;
        this.securityUtils = securityUtils;
        this.auditLogService = auditLogService;
    }

    @Transactional
    public ReviewResponse submitReview(SubmitReviewRequest request) {
        return reviewMapper.toResponse(createReview(request));
    }

    @Transactional
    public CustomerReviewSubmitResponse submitCustomerReview(SubmitReviewRequest request) {
        ReviewEntity saved = createReview(request);
        return new CustomerReviewSubmitResponse(
                saved.getId(),
                saved.getBookingId(),
                saved.getRating(),
                saved.getReviewComment() == null ? normalizeComment(reviewText(request)) : saved.getReviewComment(),
                saved.getCreatedAt(),
                true
        );
    }

    public CustomerReviewResponse getCustomerReviewByBookingId(Long bookingId) {
        Long customerId = currentUserId();
        validateBookingOwnerForReviewLookup(bookingId, customerId);
        ReviewEntity review = reviewRepository.findByBookingIdAndCustomerIdAndDeletedFalse(bookingId, customerId)
                .orElseThrow(() -> new ReviewNotFoundException(bookingId));
        return new CustomerReviewResponse(
                review.getId(),
                review.getRating(),
                review.getReviewComment(),
                review.getCreatedAt()
        );
    }

    private ReviewEntity createReview(SubmitReviewRequest request) {
        Long customerId = currentUserId();
        Long bookingId = parseBookingId(request.getBookingId());
        ReviewEntity review = orderRepository.findById(bookingId)
                .map(order -> buildOrderReview(request, customerId, order))
                .orElseGet(() -> buildLegacyBookingReview(request, customerId));

        ReviewEntity saved = reviewRepository.save(review);
        auditLogService.log(customerId, "CUSTOMER", "REVIEW_CREATED", "REVIEW", saved.getId().toString(),
                null, "bookingId=" + saved.getBookingId() + ",rating=" + saved.getRating());
        log.info("Review submitted. reviewId={}, bookingId={}, customerId={}", saved.getId(), saved.getBookingId(), customerId);
        return saved;
    }

    private ReviewEntity buildOrderReview(SubmitReviewRequest request, Long customerId, Order order) {
        validateOrderReviewEligibility(order, customerId);
        ReviewEntity review = baseReview(request, customerId);
        review.setBookingId(order.getId());
        review.setProviderId(order.getAssignedPartnerId());
        review.setServiceId(firstServiceId(order));
        return review;
    }

    private Long firstServiceId(Order order) {
        if (order.getItems() == null || order.getItems().isEmpty()) {
            return null;
        }
        return order.getItems().stream()
                .filter(java.util.Objects::nonNull)
                .map(com.lexho.booking.entity.OrderItem::getPartnerId)
                .filter(java.util.Objects::nonNull)
                .findFirst()
                .orElse(null);
    }

    private ReviewEntity buildLegacyBookingReview(SubmitReviewRequest request, Long customerId) {
        Long bookingId = parseBookingId(request.getBookingId());
        Booking booking = bookingRepository.findByIdAndDeletedFalse(bookingId)
                .orElseThrow(() -> new ReviewNotFoundException("BOOKING_NOT_FOUND", LexhoConstants.BOOKING_NOT_FOUND));

        validateBookingOwnership(booking, customerId);
        if (booking.getBookingStatus() != BookingStatus.COMPLETED) {
            throw new ReviewValidationException("BOOKING_NOT_COMPLETED", "Review is allowed only for completed bookings");
        }
        if (reviewRepository.existsByBookingIdAndDeletedFalse(booking.getId())) {
            throw new ReviewValidationException("REVIEW_ALREADY_SUBMITTED", "Only one review is allowed per booking");
        }

        ReviewEntity review = baseReview(request, customerId);
        review.setBookingId(booking.getId());
        review.setProviderId(booking.getPartner() != null ? booking.getPartner().getId() : null);
        review.setServiceId(booking.getService() != null ? booking.getService().getId() : null);
        return review;
    }

    private ReviewEntity baseReview(SubmitReviewRequest request, Long customerId) {
        ReviewEntity review = new ReviewEntity();
        review.setCustomerId(customerId);
        review.setRating(request.getRating());
        review.setReviewComment(normalizeComment(reviewText(request)));
        review.setReviewImages(reviewMapper.serializeImages(request.getReviewImages()));
        review.setStatus(ReviewStatus.PENDING);
        review.setDeleted(false);
        review.setReportCount(0L);
        return review;
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
        return updateStatus(reviewId, ReviewStatus.APPROVED, ACTION_APPROVE, null, false);
    }

    @Transactional
    public ReviewResponse approveReview(Long reviewId, String reason) {
        return updateStatus(reviewId, ReviewStatus.APPROVED, ACTION_APPROVE, reason, false);
    }

    @Transactional
    public ReviewResponse rejectReview(Long reviewId) {
        return updateReviewStatusAsRejected(reviewId, null, false);
    }

    @Transactional
    public ReviewResponse rejectReview(Long reviewId, String reason) {
        return updateReviewStatusAsRejected(reviewId, reason, true);
    }

    @Transactional
    public ReviewResponse hideReview(Long reviewId) {
        return updateReviewStatusAsHidden(reviewId, null, false);
    }

    @Transactional
    public ReviewResponse hideReview(Long reviewId, String reason) {
        return updateReviewStatusAsHidden(reviewId, reason, true);
    }

    @Transactional
    public ReviewResponse unhideReview(Long reviewId, String reason) {
        return updateStatus(reviewId, ReviewStatus.APPROVED, "UNHIDE", reason, false);
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

    public ReviewPageResponse<ReviewResponse> getAllReviewsForAdmin(int page,
                                                                    int size,
                                                                    String sortBy,
                                                                    String sortDir,
                                                                    Integer rating,
                                                                    ReviewStatus status,
                                                                    Long providerId,
                                                                    Long serviceId,
                                                                    String search,
                                                                    String startDate,
                                                                    String endDate,
                                                                    Boolean reported) {
        Pageable pageable = buildPageable(page, size, sortBy, sortDir);
        Page<ReviewEntity> result = reviewRepository.searchAdminReviewsAdvanced(
                normalizeSearch(search),
                status,
                rating,
                providerId,
                serviceId,
                parseDateTime(startDate),
                parseDateTime(endDate),
                reported,
                pageable
        );
        List<ReviewResponse> content = result.getContent().stream().map(reviewMapper::toResponse).toList();
        return new ReviewPageResponse<>(content, result.getTotalElements(), result.getTotalPages(), result.getNumber());
    }

    public ReviewResponse getReviewByIdForAdmin(Long reviewId) {
        return reviewMapper.toResponse(findActiveReview(reviewId));
    }

    public ReviewDetailResponse getReviewDetailForAdmin(Long reviewId) {
        ReviewEntity review = findActiveReview(reviewId);
        return new ReviewDetailResponse(
                reviewMapper.toResponse(review),
                bookingSummary(review.getBookingId()),
                userSummary(review.getCustomerId()),
                userSummary(review.getProviderId()),
                serviceSummary(review.getServiceId()),
                reviewReportRepository.findByReviewIdOrderByCreatedAtDesc(reviewId).stream()
                        .map(this::toReportResponse)
                        .toList(),
                moderationHistory(reviewId)
        );
    }

    public ReviewPageResponse<ReviewResponse> getReportedReviewsForAdmin(int page, int size) {
        Pageable pageable = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 100), Sort.by(Sort.Direction.DESC, LexhoConstants.FIELD_UPDATED_AT));
        Page<ReviewEntity> result = reviewRepository.findByReportCountGreaterThanAndDeletedFalse(0L, pageable);
        List<ReviewResponse> content = result.getContent().stream().map(reviewMapper::toResponse).toList();
        return new ReviewPageResponse<>(content, result.getTotalElements(), result.getTotalPages(), result.getNumber());
    }

    public ReviewPageResponse<ReviewReportResponse> getReviewReportsForAdmin(int page,
                                                                             int size,
                                                                             ReviewReportStatus status) {
        Pageable pageable = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 100),
                Sort.by(Sort.Direction.DESC, LexhoConstants.FIELD_CREATED_AT));
        Page<ReviewReport> result = status == null
                ? reviewReportRepository.findAll(pageable)
                : reviewReportRepository.findByStatus(status, pageable);
        List<ReviewReportResponse> content = result.getContent().stream()
                .map(this::toReportResponse)
                .toList();
        return new ReviewPageResponse<>(content, result.getTotalElements(), result.getTotalPages(), result.getNumber());
    }

    @Transactional
    public ReviewReportResponse dismissReport(Long reportId, String reason) {
        ReviewReport report = reviewReportRepository.findById(reportId)
                .orElseThrow(() -> new ReviewNotFoundException("REPORT_NOT_FOUND", "Review report not found"));
        report.setStatus(ReviewReportStatus.DISMISSED);
        report.setResolvedByAdminId(currentUserIdOrSystem());
        report.setResolutionNote(normalizeComment(reason));
        report.setResolvedAt(LocalDateTime.now());
        ReviewReport saved = reviewReportRepository.save(report);
        auditLogService.log(currentUserIdOrSystem(), LexhoConstants.ACTOR_ADMIN, "REVIEW_REPORT_DISMISSED", "REVIEW_REPORT",
                reportId.toString(), null, "reviewId=" + saved.getReviewId());
        return toReportResponse(saved);
    }

    @Transactional
    public ReviewResponse hideReportedReview(Long reportId, String reason) {
        ReviewReport report = resolveReportAction(reportId, reason);
        return updateReviewStatusAsHidden(report.getReviewId(), reason, true);
    }

    @Transactional
    public ReviewResponse rejectReportedReview(Long reportId, String reason) {
        ReviewReport report = resolveReportAction(reportId, reason);
        return updateReviewStatusAsRejected(report.getReviewId(), reason, true);
    }

    private ReviewResponse updateReviewStatusAsRejected(Long reviewId, String reason, boolean requireReason) {
        return updateStatus(reviewId, ReviewStatus.REJECTED, "REJECT", reason, requireReason);
    }

    private ReviewResponse updateReviewStatusAsHidden(Long reviewId, String reason, boolean requireReason) {
        return updateStatus(reviewId, ReviewStatus.HIDDEN, "HIDE", reason, requireReason);
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
        Pageable pageable = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 100), Sort.by(Sort.Direction.DESC, LexhoConstants.FIELD_CREATED_AT));
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
                reviewRepository.countByDeletedFalseAndStatus(ReviewStatus.PENDING),
                reviewRepository.countByDeletedFalseAndStatus(ReviewStatus.APPROVED),
                reviewRepository.countByDeletedFalseAndStatus(ReviewStatus.REJECTED),
                hiddenReviews,
                averageRating,
                totalReported,
                ratingDistribution(null, null, null, null),
                providerWise,
                serviceWise
        );
    }

    public ReviewStatsResponse getReviewStats(String startDate, String endDate, Long serviceId, Long providerId) {
        LocalDateTime startAt = parseDateTime(startDate);
        LocalDateTime endAt = parseDateTime(endDate);
        return new ReviewStatsResponse(
                reviewRepository.countForStats(serviceId, providerId, startAt, endAt),
                reviewRepository.countByStatusForStats(ReviewStatus.PENDING, serviceId, providerId, startAt, endAt),
                reviewRepository.countByStatusForStats(ReviewStatus.APPROVED, serviceId, providerId, startAt, endAt),
                reviewRepository.countByStatusForStats(ReviewStatus.REJECTED, serviceId, providerId, startAt, endAt),
                reviewRepository.countByStatusForStats(ReviewStatus.HIDDEN, serviceId, providerId, startAt, endAt),
                round(reviewRepository.averageRatingForStats(serviceId, providerId, startAt, endAt)),
                reviewRepository.countByDeletedFalseAndReportCountGreaterThan(0L),
                ratingDistribution(serviceId, providerId, startAt, endAt),
                providerId == null ? reviewRepository.providerWiseRatingSummary().stream().map(this::toProviderSummary).toList() : List.of(),
                serviceId == null ? reviewRepository.serviceWiseRatingSummary().stream().map(this::toServiceSummary).toList() : List.of()
        );
    }

    public ReviewSettingsResponse getReviewSettings() {
        List<ReviewSettingsResponse.ServiceReviewSettingResponse> serviceSettings =
                adminServiceEntityRepository == null ? List.of() : adminServiceEntityRepository.findByDeletedFalseAndActiveTrue()
                        .stream()
                        .map(this::toServiceSetting)
                        .toList();
        return new ReviewSettingsResponse(
                boolSetting("review.reviews.enabled", true),
                boolSetting("review.rating.enabled", true),
                boolSetting("review.written.enabled", true),
                boolSetting("review.auto_approve.enabled", false),
                serviceSettings
        );
    }

    @Transactional
    public ReviewSettingsResponse updateReviewSettings(ReviewSettingsUpdateRequest request) {
        String updatedBy = "ADMIN:" + currentUserIdOrSystem();
        if (request.reviewsEnabled != null) {
            adminSettingsService.upsert("review.reviews.enabled", String.valueOf(request.reviewsEnabled),
                    "Global reviews enabled", updatedBy);
        }
        if (request.ratingEnabled != null) {
            adminSettingsService.upsert("review.rating.enabled", String.valueOf(request.ratingEnabled),
                    "Rating enabled", updatedBy);
        }
        if (request.writtenReviewsEnabled != null) {
            adminSettingsService.upsert("review.written.enabled", String.valueOf(request.writtenReviewsEnabled),
                    "Written reviews enabled", updatedBy);
        }
        if (request.autoApproveReviews != null) {
            adminSettingsService.upsert("review.auto_approve.enabled", String.valueOf(request.autoApproveReviews),
                    "Auto approve reviews", updatedBy);
        }
        if (request.services != null) {
            for (ReviewSettingsUpdateRequest.ServiceReviewSettingUpdate item : request.services) {
                if (item == null || item.serviceId == null || item.reviewEnabled == null) {
                    continue;
                }
                AdminServiceEntity service = adminServiceEntityRepository.findByIdAndDeletedFalse(item.serviceId)
                        .orElseThrow(() -> new ReviewNotFoundException("SERVICE_NOT_FOUND", "Service not found"));
                service.setReviewEnabled(item.reviewEnabled);
                adminServiceEntityRepository.save(service);
            }
        }
        auditLogService.log(currentUserIdOrSystem(), LexhoConstants.ACTOR_ADMIN, "REVIEW_SETTINGS_UPDATED", "REVIEW_SETTINGS",
                "global", null, "updated");
        return getReviewSettings();
    }

    public List<ReviewModerationHistoryResponse> moderationHistory(Long reviewId) {
        return reviewModerationHistoryRepository == null ? List.of()
                : reviewModerationHistoryRepository.findByReviewIdOrderByCreatedAtAsc(reviewId).stream()
                .map(this::toHistoryResponse)
                .toList();
    }

    private ReviewResponse updateStatus(Long reviewId,
                                        ReviewStatus status,
                                        String action,
                                        String reason,
                                        boolean reasonRequired) {
        ReviewEntity review = findActiveReview(reviewId);
        String normalizedReason = normalizeComment(reason);
        if (reasonRequired && normalizedReason == null) {
            throw new ReviewValidationException("REASON_REQUIRED", "reason is required");
        }
        ReviewStatus oldStatus = review.getStatus();
        if (oldStatus == status) {
            throw new ReviewValidationException("INVALID_STATUS_TRANSITION", "Review is already " + status);
        }
        review.setStatus(status);
        ReviewEntity saved = reviewRepository.save(review);
        recordModerationHistory(saved, oldStatus, status, action, normalizedReason);
        if (status == ReviewStatus.APPROVED || oldStatus == ReviewStatus.APPROVED) {
            recalculateProviderRating(saved.getProviderId());
        }
        auditLogService.log(currentUserIdOrSystem(), LexhoConstants.ACTOR_ADMIN, auditAction(action), "REVIEW",
                reviewId.toString(), oldStatus.name(), status.name() + reasonSuffix(normalizedReason));
        log.info("Review status updated. reviewId={}, status={}", reviewId, status);
        return reviewMapper.toResponse(saved);
    }

    private void recordModerationHistory(ReviewEntity review,
                                         ReviewStatus previousStatus,
                                         ReviewStatus newStatus,
                                         String action,
                                         String reason) {
        if (reviewModerationHistoryRepository == null) {
            return;
        }
        Long adminId = currentUserIdOrSystem();
        ReviewModerationHistory history = new ReviewModerationHistory();
        history.setReviewId(review.getId());
        history.setPreviousStatus(previousStatus);
        history.setNewStatus(newStatus);
        history.setAction(action);
        history.setAdminId(adminId);
        userRepository.findById(adminId).ifPresent(admin -> {
            history.setAdminName(admin.getFullName());
            history.setAdminEmail(admin.getEmail());
        });
        history.setReason(reason);
        reviewModerationHistoryRepository.save(history);
    }

    private ReviewReport resolveReportAction(Long reportId, String reason) {
        ReviewReport report = reviewReportRepository.findById(reportId)
                .orElseThrow(() -> new ReviewNotFoundException("REPORT_NOT_FOUND", "Review report not found"));
        report.setStatus(ReviewReportStatus.ACTION_TAKEN);
        report.setResolvedByAdminId(currentUserIdOrSystem());
        report.setResolutionNote(normalizeComment(reason));
        report.setResolvedAt(LocalDateTime.now());
        return reviewReportRepository.save(report);
    }

    private ReviewReportResponse toReportResponse(ReviewReport report) {
        ReviewEntity review = reviewRepository.findById(report.getReviewId()).orElse(null);
        User reporter = userRepository.findById(report.getReportedByUserId()).orElse(null);
        return new ReviewReportResponse(
                report.getId(),
                report.getReviewId(),
                review == null ? null : review.getBookingId(),
                report.getReportedByUserId(),
                reporter == null ? null : reporter.getFullName(),
                report.getReason(),
                review == null ? null : review.getReviewComment(),
                report.getStatus(),
                report.getCreatedAt(),
                report.getResolvedAt(),
                report.getResolvedByAdminId(),
                report.getResolutionNote()
        );
    }

    private ReviewModerationHistoryResponse toHistoryResponse(ReviewModerationHistory history) {
        return new ReviewModerationHistoryResponse(
                history.getId(),
                history.getReviewId(),
                history.getPreviousStatus(),
                history.getNewStatus(),
                history.getAction(),
                history.getAdminId(),
                history.getAdminName(),
                history.getAdminEmail(),
                history.getReason(),
                history.getCreatedAt()
        );
    }

    private List<RatingDistributionResponse> ratingDistribution(Long serviceId,
                                                                Long providerId,
                                                                LocalDateTime startAt,
                                                                LocalDateTime endAt) {
        Map<Integer, Long> counts = new LinkedHashMap<>();
        IntStream.rangeClosed(1, 5).forEach(rating -> counts.put(rating, 0L));
        reviewRepository.ratingDistribution(serviceId, providerId, startAt, endAt).forEach(row -> {
            Integer rating = ((Number) row[0]).intValue();
            Long count = ((Number) row[1]).longValue();
            counts.put(rating, count);
        });
        return IntStream.iterate(5, value -> value >= 1, value -> value - 1)
                .mapToObj(rating -> new RatingDistributionResponse(rating, counts.getOrDefault(rating, 0L)))
                .toList();
    }

    private Map<String, Object> bookingSummary(Long bookingId) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("bookingId", bookingId);
        if (bookingId == null) {
            return row;
        }
        orderRepository.findById(bookingId).ifPresent(order -> {
            row.put(KEY_STATUS, order.getStatus());
            row.put("customerId", order.getUserId());
            row.put("partnerId", order.getAssignedPartnerId());
            row.put(LexhoConstants.FIELD_CREATED_AT, order.getCreatedAt());
            row.put(LexhoConstants.FIELD_UPDATED_AT, order.getUpdatedAt());
        });
        return row;
    }

    private Map<String, Object> userSummary(Long userId) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("id", userId);
        if (userId == null) {
            return row;
        }
        userRepository.findById(userId).ifPresent(user -> {
            row.put("name", user.getFullName());
            row.put("fullName", user.getFullName());
            row.put("email", user.getEmail());
            row.put("role", user.getRole());
            row.put(KEY_STATUS, user.getStatus());
        });
        return row;
    }

    private Map<String, Object> serviceSummary(Long serviceId) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("id", serviceId);
        if (serviceId == null || adminServiceEntityRepository == null) {
            return row;
        }
        adminServiceEntityRepository.findByIdAndDeletedFalse(serviceId).ifPresent(service -> {
            row.put("name", service.getName());
            row.put("reviewEnabled", service.isReviewEnabled());
            AdminCategoryEntity category = service.getCategory();
            if (category != null) {
                row.put("categoryId", category.getId());
                row.put("categoryName", category.getName());
                row.put("categoryReviewEnabled", category.isReviewEnabled());
            }
        });
        return row;
    }

    private ReviewSettingsResponse.ServiceReviewSettingResponse toServiceSetting(AdminServiceEntity service) {
        AdminCategoryEntity category = service.getCategory();
        return new ReviewSettingsResponse.ServiceReviewSettingResponse(
                service.getId(),
                service.getName(),
                category == null ? null : category.getId(),
                category == null ? service.getCategoryName() : category.getName(),
                service.isReviewEnabled()
        );
    }

    private boolean boolSetting(String key, boolean fallback) {
        if (adminSettingsService == null) {
            return fallback;
        }
        try {
            Object value = adminSettingsService.get(key).get("value");
            return value == null ? fallback : Boolean.parseBoolean(String.valueOf(value));
        } catch (RuntimeException ignored) {
            return fallback;
        }
    }

    private LocalDateTime parseDateTime(String value) {
        String normalized = normalizeSearch(value);
        if (normalized == null) {
            return null;
        }
        try {
            return LocalDateTime.parse(normalized);
        } catch (DateTimeParseException exception) {
            throw new ReviewValidationException("INVALID_DATE_RANGE", "date filters must use ISO local date-time format");
        }
    }

    private String reasonSuffix(String reason) {
        return reason == null ? "" : ",reason=" + reason;
    }

    private String auditAction(String action) {
        return switch (action) {
            case ACTION_APPROVE -> "REVIEW_APPROVED";
            case "REJECT" -> "REVIEW_REJECTED";
            case "HIDE" -> "REVIEW_HIDDEN";
            case "UNHIDE" -> "REVIEW_UNHIDDEN";
            default -> "REVIEW_" + action;
        };
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

    private void validateOrderReviewEligibility(Order order, Long customerId) {
        if (order.getUserId() == null || !order.getUserId().equals(customerId)) {
            throw new ReviewAccessDeniedException("You can review only your own booking");
        }
        if (!isCompletedStatus(order.getStatus())) {
            throw new ReviewValidationException("BOOKING_NOT_COMPLETED", "Review is allowed only for completed bookings");
        }
        if (completionSettingsService != null && !completionSettingsService.reviewEligibleFor(order)) {
            throw new ReviewValidationException("BOOKING_NOT_COMPLETED", "Review is not enabled for this service");
        }
        if (reviewRepository.existsByBookingIdAndDeletedFalse(order.getId())) {
            throw new ReviewValidationException("REVIEW_ALREADY_SUBMITTED", "Only one review is allowed per booking");
        }
    }

    private void validateBookingOwnerForReviewLookup(Long bookingId, Long customerId) {
        orderRepository.findById(bookingId).ifPresentOrElse(order -> {
            if (order.getUserId() == null || !order.getUserId().equals(customerId)) {
                throw new ReviewAccessDeniedException("You can view only your own review");
            }
        }, () -> {
            Booking booking = bookingRepository.findByIdAndDeletedFalse(bookingId)
                    .orElseThrow(() -> new ReviewNotFoundException("BOOKING_NOT_FOUND", LexhoConstants.BOOKING_NOT_FOUND));
            validateBookingOwnership(booking, customerId);
        });
    }

    private Long parseBookingId(String rawBookingId) {
        if (rawBookingId == null || rawBookingId.isBlank()) {
            throw new ReviewNotFoundException("BOOKING_NOT_FOUND", LexhoConstants.BOOKING_NOT_FOUND);
        }
        String normalized = rawBookingId.trim();
        if (normalized.length() > 1 && (normalized.charAt(0) == 'B' || normalized.charAt(0) == 'b')) {
            normalized = normalized.substring(1);
        }
        try {
            return Long.parseLong(normalized);
        } catch (NumberFormatException exception) {
            throw new ReviewNotFoundException("BOOKING_NOT_FOUND", LexhoConstants.BOOKING_NOT_FOUND);
        }
    }

    private boolean isCompletedStatus(OrderStatus status) {
        return status == OrderStatus.SERVICE_COMPLETED
                || status == OrderStatus.PAYMENT_COMPLETED
                || status == OrderStatus.DELIVERED;
    }

    private void validateReviewOwnership(ReviewEntity review, Long customerId) {
        if (review.getCustomerId() == null || !review.getCustomerId().equals(customerId)) {
            throw new ReviewAccessDeniedException("You can update only your own review");
        }
    }

    private Pageable buildPageable(int page, int size, String sortBy, String sortDir) {
        Sort.Direction direction = "asc".equalsIgnoreCase(sortDir) ? Sort.Direction.ASC : Sort.Direction.DESC;
        String resolvedSortBy = switch (sortBy == null ? "" : sortBy) {
            case LexhoConstants.FIELD_CREATED_AT, LexhoConstants.FIELD_UPDATED_AT, "rating", "reportCount", KEY_STATUS -> sortBy;
            default -> LexhoConstants.FIELD_CREATED_AT;
        };
        return PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 100), Sort.by(direction, resolvedSortBy));
    }

    private Long currentUserId() {
        return securityUtils.getCurrentUserId()
                .orElseThrow(() -> new ReviewAccessDeniedException("Authenticated user not found"));
    }

    private Long currentUserIdOrSystem() {
        return securityUtils.getCurrentUserId().orElse(0L);
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

    private String reviewText(SubmitReviewRequest request) {
        if (request.getReviewText() != null) {
            return request.getReviewText();
        }
        return request.getReviewComment();
    }

    private void recalculateProviderRating(Long providerId) {
        if (providerId == null) {
            return;
        }
        double average = round(reviewRepository.averageRatingByProviderIdAndStatus(providerId, ReviewStatus.APPROVED));
        long total = reviewRepository.countByProviderIdAndStatusAndDeletedFalse(providerId, ReviewStatus.APPROVED);
        partnerMarketplaceService.updateRating(providerId, average, total);
        partnerProfileRepository.findById(providerId).ifPresent(profile -> {
            profile.setRating(average);
            profile.setTotalReviews(total);
            partnerProfileRepository.save(profile);
        });
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

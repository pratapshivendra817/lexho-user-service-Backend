package com.lexho.user_service.review.controller;

import com.lexho.user_service.review.dto.ModerationActionRequest;
import com.lexho.user_service.review.dto.ReviewDetailResponse;
import com.lexho.user_service.review.dto.ReviewPageResponse;
import com.lexho.user_service.review.dto.ReviewReportResponse;
import com.lexho.user_service.review.dto.ReviewResponse;
import com.lexho.user_service.review.dto.ReviewSettingsResponse;
import com.lexho.user_service.review.dto.ReviewSettingsUpdateRequest;
import com.lexho.user_service.review.dto.ReviewStatsResponse;
import com.lexho.user_service.review.enums.ReviewReportStatus;
import com.lexho.user_service.review.enums.ReviewStatus;
import com.lexho.user_service.review.service.ReviewManagementService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestBody;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping({"/api/admin/reviews", "/api/v1/admin/reviews"})
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin Review Management", description = "Admin moderation and analytics APIs for reviews")
public class AdminReviewController {

    private final ReviewManagementService reviewManagementService;

    public AdminReviewController(ReviewManagementService reviewManagementService) {
        this.reviewManagementService = reviewManagementService;
    }

    @GetMapping
    @Operation(summary = "Get all reviews with filters")
    @PreAuthorize("hasRole('ADMIN') and hasAuthority('VIEW_REVIEWS')")
    public ResponseEntity<ReviewPageResponse<ReviewResponse>> all(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir,
            @RequestParam(required = false) Integer rating,
            @RequestParam(required = false) ReviewStatus status,
            @RequestParam(required = false) Long providerId,
            @RequestParam(required = false) Long serviceId,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(required = false) Boolean reported) {
        return ResponseEntity.ok(reviewManagementService.getAllReviewsForAdmin(
                page, size, sortBy, sortDir, rating, status, providerId, serviceId, search, startDate, endDate, reported
        ));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get review by id")
    @PreAuthorize("hasRole('ADMIN') and hasAuthority('VIEW_REVIEWS')")
    public ResponseEntity<ReviewDetailResponse> byId(@PathVariable Long id) {
        return ResponseEntity.ok(reviewManagementService.getReviewDetailForAdmin(id));
    }

    @PatchMapping("/{id}/approve")
    @Operation(summary = "Approve review")
    @PreAuthorize("hasRole('ADMIN') and hasAuthority('MODERATE_REVIEWS')")
    public ResponseEntity<ReviewResponse> approve(@PathVariable Long id,
                                                  @RequestBody(required = false) ModerationActionRequest request) {
        return ResponseEntity.ok(reviewManagementService.approveReview(id, request == null ? null : request.reason));
    }

    @PostMapping("/{id}/approve")
    @Operation(summary = "Approve review")
    @PreAuthorize("hasRole('ADMIN') and hasAuthority('MODERATE_REVIEWS')")
    public ResponseEntity<ReviewResponse> approvePost(@PathVariable Long id,
                                                      @RequestBody(required = false) ModerationActionRequest request) {
        return approve(id, request);
    }

    @PatchMapping("/{id}/reject")
    @Operation(summary = "Reject review")
    @PreAuthorize("hasRole('ADMIN') and hasAuthority('MODERATE_REVIEWS')")
    public ResponseEntity<ReviewResponse> reject(@PathVariable Long id,
                                                 @Valid @RequestBody ModerationActionRequest request) {
        return ResponseEntity.ok(reviewManagementService.rejectReview(id, request.reason));
    }

    @PostMapping("/{id}/reject")
    @Operation(summary = "Reject review")
    @PreAuthorize("hasRole('ADMIN') and hasAuthority('MODERATE_REVIEWS')")
    public ResponseEntity<ReviewResponse> rejectPost(@PathVariable Long id,
                                                     @Valid @RequestBody ModerationActionRequest request) {
        return reject(id, request);
    }

    @PatchMapping("/{id}/hide")
    @Operation(summary = "Hide review")
    @PreAuthorize("hasRole('ADMIN') and hasAuthority('MODERATE_REVIEWS')")
    public ResponseEntity<ReviewResponse> hide(@PathVariable Long id,
                                               @Valid @RequestBody ModerationActionRequest request) {
        return ResponseEntity.ok(reviewManagementService.hideReview(id, request.reason));
    }

    @PatchMapping("/{id}/unhide")
    @Operation(summary = "Unhide review (set to approved)")
    @PreAuthorize("hasRole('ADMIN') and hasAuthority('MODERATE_REVIEWS')")
    public ResponseEntity<ReviewResponse> unhide(@PathVariable Long id,
                                                 @RequestBody(required = false) ModerationActionRequest request) {
        return ResponseEntity.ok(reviewManagementService.unhideReview(id, request == null ? null : request.reason));
    }

    @GetMapping("/reported")
    @Operation(summary = "Get reported reviews")
    @PreAuthorize("hasRole('ADMIN') and hasAuthority('VIEW_REVIEW_REPORTS')")
    public ResponseEntity<ReviewPageResponse<ReviewReportResponse>> reported(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) ReviewReportStatus status) {
        return ResponseEntity.ok(reviewManagementService.getReviewReportsForAdmin(page, size, status));
    }

    @PatchMapping("/reported/{reportId}/dismiss")
    @Operation(summary = "Dismiss reported review")
    @PreAuthorize("hasRole('ADMIN') and hasAuthority('MODERATE_REVIEWS')")
    public ResponseEntity<ReviewReportResponse> dismissReport(@PathVariable Long reportId,
                                                              @RequestBody(required = false) ModerationActionRequest request) {
        return ResponseEntity.ok(reviewManagementService.dismissReport(reportId, request == null ? null : request.reason));
    }

    @PatchMapping("/reported/{reportId}/hide-review")
    @Operation(summary = "Hide review from report")
    @PreAuthorize("hasRole('ADMIN') and hasAuthority('MODERATE_REVIEWS')")
    public ResponseEntity<ReviewResponse> hideReportedReview(@PathVariable Long reportId,
                                                             @Valid @RequestBody ModerationActionRequest request) {
        return ResponseEntity.ok(reviewManagementService.hideReportedReview(reportId, request.reason));
    }

    @PatchMapping("/reported/{reportId}/reject-review")
    @Operation(summary = "Reject review from report")
    @PreAuthorize("hasRole('ADMIN') and hasAuthority('MODERATE_REVIEWS')")
    public ResponseEntity<ReviewResponse> rejectReportedReview(@PathVariable Long reportId,
                                                               @Valid @RequestBody ModerationActionRequest request) {
        return ResponseEntity.ok(reviewManagementService.rejectReportedReview(reportId, request.reason));
    }

    @GetMapping("/stats")
    @Operation(summary = "Review analytics")
    @PreAuthorize("hasRole('ADMIN') and hasAuthority('VIEW_REVIEW_STATS')")
    public ResponseEntity<ReviewStatsResponse> stats(@RequestParam(required = false) String startDate,
                                                     @RequestParam(required = false) String endDate,
                                                     @RequestParam(required = false) Long serviceId,
                                                     @RequestParam(required = false) Long providerId) {
        return ResponseEntity.ok(reviewManagementService.getReviewStats(startDate, endDate, serviceId, providerId));
    }

    @GetMapping("/settings")
    @Operation(summary = "Get review settings")
    @PreAuthorize("hasRole('ADMIN') and hasAuthority('MANAGE_REVIEW_SETTINGS')")
    public ResponseEntity<ReviewSettingsResponse> settings() {
        return ResponseEntity.ok(reviewManagementService.getReviewSettings());
    }

    @PutMapping("/settings")
    @Operation(summary = "Update review settings")
    @PreAuthorize("hasRole('ADMIN') and hasAuthority('MANAGE_REVIEW_SETTINGS')")
    public ResponseEntity<ReviewSettingsResponse> updateSettings(@RequestBody ReviewSettingsUpdateRequest request) {
        return ResponseEntity.ok(reviewManagementService.updateReviewSettings(request));
    }

    @PatchMapping("/bulk/status")
    @Operation(summary = "Bulk moderation on reviews")
    @PreAuthorize("hasRole('ADMIN') and hasAuthority('MODERATE_REVIEWS')")
    public ResponseEntity<Map<String, Object>> bulkStatus(@Valid @RequestBody BulkReviewStatusRequest request) {
        long processed = 0;
        for (Long reviewId : request.reviewIds) {
            if (reviewId == null) {
                continue;
            }
            switch ((request.action == null ? "" : request.action.trim().toUpperCase())) {
                case "APPROVE" -> reviewManagementService.approveReview(reviewId, request.reason);
                case "REJECT" -> reviewManagementService.rejectReview(reviewId, request.reason);
                case "HIDE" -> reviewManagementService.hideReview(reviewId, request.reason);
                case "UNHIDE" -> reviewManagementService.unhideReview(reviewId, request.reason);
                default -> throw new IllegalArgumentException("Invalid action. Use APPROVE, REJECT, HIDE, UNHIDE");
            }
            processed++;
        }
        return ResponseEntity.ok(Map.of("success", true, "processed", processed));
    }

    public static class BulkReviewStatusRequest {
        @NotEmpty(message = "reviewIds are required")
        public List<Long> reviewIds;
        public String action;
        public String reason;
    }
}

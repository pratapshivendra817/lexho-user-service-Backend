package com.lexho.user_service.review.controller;

import com.lexho.user_service.review.dto.ReviewPageResponse;
import com.lexho.user_service.review.dto.ReviewResponse;
import com.lexho.user_service.review.dto.ReviewStatsResponse;
import com.lexho.user_service.review.enums.ReviewStatus;
import com.lexho.user_service.review.service.ReviewManagementService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/reviews")
@PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
@Tag(name = "Admin Review Management", description = "Admin moderation and analytics APIs for reviews")
public class AdminReviewController {

    private final ReviewManagementService reviewManagementService;

    public AdminReviewController(ReviewManagementService reviewManagementService) {
        this.reviewManagementService = reviewManagementService;
    }

    @GetMapping
    @Operation(summary = "Get all reviews with filters")
    public ResponseEntity<ReviewPageResponse<ReviewResponse>> all(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir,
            @RequestParam(required = false) Integer rating,
            @RequestParam(required = false) ReviewStatus status,
            @RequestParam(required = false) Long providerId,
            @RequestParam(required = false) Long serviceId,
            @RequestParam(required = false) String search) {
        return ResponseEntity.ok(reviewManagementService.getAllReviewsForAdmin(
                page, size, sortBy, sortDir, rating, status, providerId, serviceId, search
        ));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get review by id")
    public ResponseEntity<ReviewResponse> byId(@PathVariable Long id) {
        return ResponseEntity.ok(reviewManagementService.getReviewByIdForAdmin(id));
    }

    @PatchMapping("/{id}/approve")
    @Operation(summary = "Approve review")
    public ResponseEntity<ReviewResponse> approve(@PathVariable Long id) {
        return ResponseEntity.ok(reviewManagementService.approveReview(id));
    }

    @PatchMapping("/{id}/reject")
    @Operation(summary = "Reject review")
    public ResponseEntity<ReviewResponse> reject(@PathVariable Long id) {
        return ResponseEntity.ok(reviewManagementService.rejectReview(id));
    }

    @PatchMapping("/{id}/hide")
    @Operation(summary = "Hide review")
    public ResponseEntity<ReviewResponse> hide(@PathVariable Long id) {
        return ResponseEntity.ok(reviewManagementService.hideReview(id));
    }

    @GetMapping("/reported")
    @Operation(summary = "Get reported reviews")
    public ResponseEntity<ReviewPageResponse<ReviewResponse>> reported(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(reviewManagementService.getReportedReviewsForAdmin(page, size));
    }

    @GetMapping("/stats")
    @Operation(summary = "Review analytics")
    public ResponseEntity<ReviewStatsResponse> stats() {
        return ResponseEntity.ok(reviewManagementService.getReviewStats());
    }
}

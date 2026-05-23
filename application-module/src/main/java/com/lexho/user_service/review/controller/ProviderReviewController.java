package com.lexho.user_service.review.controller;

import com.lexho.user_service.review.dto.ReviewPageResponse;
import com.lexho.user_service.review.dto.ReviewResponse;
import com.lexho.user_service.review.service.ReviewManagementService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/partner/reviews")
@PreAuthorize("hasAnyRole('PARTNER','PARTNER')")
@Tag(name = "Partner Reviews", description = "Partner-side review listing APIs")
public class ProviderReviewController {

    private final ReviewManagementService reviewManagementService;

    public ProviderReviewController(ReviewManagementService reviewManagementService) {
        this.reviewManagementService = reviewManagementService;
    }

    @GetMapping
    @Operation(summary = "Get partner reviews")
    public ResponseEntity<ReviewPageResponse<ReviewResponse>> providerReviews(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(reviewManagementService.getProviderReviews(page, size));
    }
}

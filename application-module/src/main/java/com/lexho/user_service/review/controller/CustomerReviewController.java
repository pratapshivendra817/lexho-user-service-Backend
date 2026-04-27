package com.lexho.user_service.review.controller;

import com.lexho.user_service.review.dto.ReportReviewRequest;
import com.lexho.user_service.review.dto.ReviewResponse;
import com.lexho.user_service.review.dto.SimpleMessageResponse;
import com.lexho.user_service.review.dto.SubmitReviewRequest;
import com.lexho.user_service.review.dto.UpdateReviewRequest;
import com.lexho.user_service.review.service.ReviewManagementService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/customer/reviews")
@PreAuthorize("hasRole('CUSTOMER')")
@Tag(name = "Customer Reviews", description = "Customer review APIs")
public class CustomerReviewController {

    private final ReviewManagementService reviewManagementService;

    public CustomerReviewController(ReviewManagementService reviewManagementService) {
        this.reviewManagementService = reviewManagementService;
    }

    @PostMapping
    @Operation(summary = "Submit review")
    public ResponseEntity<ReviewResponse> submit(@Valid @RequestBody SubmitReviewRequest request) {
        return ResponseEntity.ok(reviewManagementService.submitReview(request));
    }

    @GetMapping
    @Operation(summary = "Get my reviews")
    public ResponseEntity<List<ReviewResponse>> myReviews() {
        return ResponseEntity.ok(reviewManagementService.getMyReviews());
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update my review")
    public ResponseEntity<ReviewResponse> update(@PathVariable Long id,
                                                 @Valid @RequestBody UpdateReviewRequest request) {
        return ResponseEntity.ok(reviewManagementService.updateMyReview(id, request));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete my review (soft delete)")
    public ResponseEntity<SimpleMessageResponse> delete(@PathVariable Long id) {
        return ResponseEntity.ok(reviewManagementService.deleteMyReview(id));
    }

    @PostMapping("/{id}/report")
    @Operation(summary = "Report a review")
    public ResponseEntity<SimpleMessageResponse> report(@PathVariable Long id,
                                                        @Valid @RequestBody ReportReviewRequest request) {
        return ResponseEntity.ok(reviewManagementService.reportReview(id, request));
    }
}

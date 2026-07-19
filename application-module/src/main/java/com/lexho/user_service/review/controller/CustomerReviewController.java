package com.lexho.user_service.review.controller;

import com.lexho.user_service.dto.ApiResponse;
import com.lexho.user_service.review.dto.CustomerReviewResponse;
import com.lexho.user_service.review.dto.CustomerReviewSubmitResponse;
import com.lexho.user_service.review.dto.SubmitReviewRequest;
import com.lexho.user_service.review.service.ReviewManagementService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/customer/reviews")
@Tag(name = "Customer Reviews", description = "Customer review and rating APIs")
public class CustomerReviewController {

    private final ReviewManagementService reviewManagementService;

    public CustomerReviewController(ReviewManagementService reviewManagementService) {
        this.reviewManagementService = reviewManagementService;
    }

    @PostMapping
    @Operation(summary = "Submit customer review for a completed booking")
    public ApiResponse<CustomerReviewSubmitResponse> submit(@Valid @RequestBody SubmitReviewRequest request) {
        return ApiResponse.success("Review submitted", reviewManagementService.submitCustomerReview(request));
    }

    @GetMapping("/{bookingId}")
    @Operation(summary = "Get customer review for a booking")
    public ApiResponse<CustomerReviewResponse> byBooking(@PathVariable Long bookingId) {
        return ApiResponse.success("Review fetched", reviewManagementService.getCustomerReviewByBookingId(bookingId));
    }
}

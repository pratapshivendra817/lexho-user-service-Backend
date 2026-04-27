package com.lexho.user_service.review.dto;

import com.lexho.user_service.review.enums.ReviewStatus;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
public class ReviewResponse {

    private Long id;
    private Long bookingId;
    private Long customerId;
    private String customerName;
    private Long providerId;
    private String providerName;
    private Long serviceId;
    private String serviceName;
    private Integer rating;
    private String reviewComment;
    private List<String> reviewImages;
    private ReviewStatus status;
    private Long reportCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

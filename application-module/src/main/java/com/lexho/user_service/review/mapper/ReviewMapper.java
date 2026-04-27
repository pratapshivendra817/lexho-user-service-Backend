package com.lexho.user_service.review.mapper;

import com.lexho.catalog.entity.ServiceCategory;
import com.lexho.user.entity.User;
import com.lexho.user.repository.UserRepository;
import com.lexho.user_service.review.dto.ReviewResponse;
import com.lexho.user_service.review.entity.ReviewEntity;
import com.lexho.user_service.review.repository.ServiceCategoryLookupRepository;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@Component
public class ReviewMapper {

    private static final String IMAGE_SEPARATOR = "||";

    private final UserRepository userRepository;
    private final ServiceCategoryLookupRepository serviceCategoryLookupRepository;

    public ReviewMapper(UserRepository userRepository,
                        ServiceCategoryLookupRepository serviceCategoryLookupRepository) {
        this.userRepository = userRepository;
        this.serviceCategoryLookupRepository = serviceCategoryLookupRepository;
    }

    public ReviewResponse toResponse(ReviewEntity review) {
        ReviewResponse response = new ReviewResponse();
        response.setId(review.getId());
        response.setBookingId(review.getBookingId());
        response.setCustomerId(review.getCustomerId());
        response.setProviderId(review.getProviderId());
        response.setServiceId(review.getServiceId());
        response.setRating(review.getRating());
        response.setReviewComment(review.getReviewComment());
        response.setReviewImages(deserializeImages(review.getReviewImages()));
        response.setStatus(review.getStatus());
        response.setReportCount(review.getReportCount());
        response.setCreatedAt(review.getCreatedAt());
        response.setUpdatedAt(review.getUpdatedAt());

        response.setCustomerName(resolveUserName(review.getCustomerId()));
        response.setProviderName(resolveUserName(review.getProviderId()));
        response.setServiceName(resolveServiceName(review.getServiceId()));
        return response;
    }

    public String serializeImages(List<String> images) {
        if (images == null || images.isEmpty()) {
            return null;
        }
        return String.join(IMAGE_SEPARATOR, images);
    }

    public List<String> deserializeImages(String payload) {
        if (payload == null || payload.isBlank()) {
            return Collections.emptyList();
        }
        return Arrays.stream(payload.split("\\|\\|"))
                .filter(item -> !item.isBlank())
                .toList();
    }

    private String resolveUserName(Long userId) {
        if (userId == null) {
            return null;
        }
        return userRepository.findById(userId)
                .map(User::getFullName)
                .orElse(null);
    }

    private String resolveServiceName(Long serviceId) {
        if (serviceId == null) {
            return null;
        }
        return serviceCategoryLookupRepository.findById(serviceId)
                .map(ServiceCategory::getName)
                .orElse(null);
    }
}

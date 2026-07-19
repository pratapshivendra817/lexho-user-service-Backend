package com.lexho.user_service.review;

import com.lexho.catalog.entity.ServiceCategory;
import com.lexho.user.entity.User;
import com.lexho.user.repository.UserRepository;
import com.lexho.user_service.review.dto.ProviderRatingSummary;
import com.lexho.user_service.review.dto.ReviewPageResponse;
import com.lexho.user_service.review.dto.ReviewResponse;
import com.lexho.user_service.review.dto.ReviewStatsResponse;
import com.lexho.user_service.review.dto.ServiceRatingSummary;
import com.lexho.user_service.review.dto.SubmitReviewRequest;
import com.lexho.user_service.review.entity.ReviewEntity;
import com.lexho.user_service.review.entity.ReviewReport;
import com.lexho.user_service.review.enums.ReviewStatus;
import com.lexho.user_service.review.mapper.ReviewMapper;
import com.lexho.user_service.review.repository.ServiceCategoryLookupRepository;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ReviewSupportTest {

    private final UserRepository userRepository = mock(UserRepository.class);
    private final ServiceCategoryLookupRepository serviceCategoryLookupRepository = mock(ServiceCategoryLookupRepository.class);
    private final ReviewMapper reviewMapper = new ReviewMapper(userRepository, serviceCategoryLookupRepository);
    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void reviewMapperConvertsEntityAndResolvesNames() {
        User customer = new User();
        customer.setFullName("Customer One");
        User provider = new User();
        provider.setFullName("Provider Two");
        ServiceCategory category = new ServiceCategory();
        category.setName("Cleaning");

        when(userRepository.findById(11L)).thenReturn(Optional.of(customer));
        when(userRepository.findById(22L)).thenReturn(Optional.of(provider));
        when(serviceCategoryLookupRepository.findById(33L)).thenReturn(Optional.of(category));

        ReviewEntity entity = new ReviewEntity();
        entity.setId(5L);
        entity.setBookingId(99L);
        entity.setCustomerId(11L);
        entity.setProviderId(22L);
        entity.setServiceId(33L);
        entity.setRating(4);
        entity.setReviewComment("Good");
        entity.setReviewImages("img1|| ||img2");
        entity.setStatus(ReviewStatus.APPROVED);
        entity.setReportCount(3L);
        entity.setCreatedAt(LocalDateTime.now().minusDays(1));
        entity.setUpdatedAt(LocalDateTime.now());

        ReviewResponse response = reviewMapper.toResponse(entity);

        assertThat(response.getId()).isEqualTo(5L);
        assertThat(response.getReviewImages()).containsExactly("img1", "img2");
        assertThat(response.getCustomerName()).isEqualTo("Customer One");
        assertThat(response.getProviderName()).isEqualTo("Provider Two");
        assertThat(response.getServiceName()).isEqualTo("Cleaning");
    }

    @Test
    void reviewMapperHandlesEmptyCollectionsBlankPayloadsAndMissingLookups() {
        assertThat(reviewMapper.serializeImages(null)).isNull();
        assertThat(reviewMapper.serializeImages(List.of())).isNull();
        assertThat(reviewMapper.serializeImages(List.of("a", "b"))).isEqualTo("a||b");
        assertThat(reviewMapper.deserializeImages(null)).isEmpty();
        assertThat(reviewMapper.deserializeImages("  ")).isEmpty();
        assertThat(reviewMapper.deserializeImages("one|| ||two||")).containsExactly("one", "two");

        ReviewEntity entity = new ReviewEntity();
        entity.setCustomerId(null);
        entity.setProviderId(77L);
        entity.setServiceId(88L);

        when(userRepository.findById(77L)).thenReturn(Optional.empty());
        when(serviceCategoryLookupRepository.findById(88L)).thenReturn(Optional.empty());

        ReviewResponse response = reviewMapper.toResponse(entity);

        assertThat(response.getCustomerName()).isNull();
        assertThat(response.getProviderName()).isNull();
        assertThat(response.getServiceName()).isNull();
        assertThat(response.getReviewImages()).isEmpty();
    }

    @Test
    void reviewEntitiesLifecycleAndDtoAccessorsAreCovered() {
        ReviewEntity reviewEntity = new ReviewEntity();
        reviewEntity.setRating(5);
        reviewEntity.prePersist();
        LocalDateTime createdAt = reviewEntity.getCreatedAt();

        assertThat(reviewEntity.getStatus()).isEqualTo(ReviewStatus.PENDING);
        assertThat(reviewEntity.getReportCount()).isZero();
        assertThat(createdAt).isNotNull();
        assertThat(reviewEntity.getUpdatedAt()).isEqualTo(createdAt);

        reviewEntity.setReportCount(null);
        reviewEntity.preUpdate();
        assertThat(reviewEntity.getUpdatedAt()).isAfterOrEqualTo(createdAt);
        assertThat(reviewEntity.getReportCount()).isZero();

        ReviewReport report = new ReviewReport();
        report.setReviewId(10L);
        report.setReportedByUserId(20L);
        report.setReason("Spam");
        report.prePersist();
        assertThat(report.getCreatedAt()).isNotNull();

        ProviderRatingSummary providerRatingSummary = new ProviderRatingSummary(1L, "Partner", 4.2, 11L);
        ServiceRatingSummary serviceRatingSummary = new ServiceRatingSummary(2L, "Service", 4.8, 9L);
        ReviewStatsResponse statsResponse = new ReviewStatsResponse(15, 4.5, 2, 1,
                List.of(providerRatingSummary), List.of(serviceRatingSummary));
        ReviewPageResponse<String> pageResponse = new ReviewPageResponse<>(List.of("a", "b"), 2, 1, 0);

        assertThat(providerRatingSummary.getProviderId()).isEqualTo(1L);
        assertThat(serviceRatingSummary.getServiceId()).isEqualTo(2L);
        assertThat(statsResponse.getAverageRating()).isEqualTo(4.5);
        assertThat(statsResponse.getProviderWiseRatings()).containsExactly(providerRatingSummary);
        assertThat(pageResponse.getContent()).containsExactly("a", "b");
        assertThat(pageResponse.getTotalElements()).isEqualTo(2);
    }

    @Test
    void submitReviewRequestValidationCoversBoundaryConditions() {
        SubmitReviewRequest invalid = new SubmitReviewRequest();
        invalid.setRating(6);
        invalid.setReviewText("x".repeat(501));
        invalid.setReviewComment("y".repeat(501));

        assertThat(validator.validate(invalid))
                .extracting(violation -> violation.getPropertyPath().toString())
                .contains("bookingId", "rating", "reviewText", "reviewComment");

        SubmitReviewRequest valid = new SubmitReviewRequest();
        valid.setBookingId(99L);
        valid.setRating(1);
        valid.setReviewText("ok");

        assertThat(validator.validate(valid)).isEmpty();
    }
}

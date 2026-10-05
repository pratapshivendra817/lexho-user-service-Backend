package com.lexho.user_service.review.repository;

import com.lexho.user_service.review.entity.ReviewEntity;
import com.lexho.user_service.review.enums.ReviewStatus;
import com.lexho.testconfig.LexhoMySqlDataJpaTest;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ContextConfiguration;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@LexhoMySqlDataJpaTest
@ContextConfiguration(classes = ReviewRepositoryTest.JpaConfig.class)
class ReviewRepositoryTest {

    @Autowired
    private ReviewRepository repository;

    @Autowired
    private EntityManager entityManager;

    @Test
    void searchAndAggregatesRespectDeletedAndStatusFilters() {
        ReviewEntity approvedA = repository.save(review(1001L, 201L, 301L, 401L, 5, "Fast and clean", ReviewStatus.APPROVED, false, 1L));
        ReviewEntity approvedB = repository.save(review(1002L, 202L, 301L, 402L, 4, "Clean service", ReviewStatus.APPROVED, false, 0L));
        ReviewEntity hidden = repository.save(review(1003L, 203L, 302L, 401L, 2, "Late arrival", ReviewStatus.HIDDEN, false, 3L));
        repository.save(review(1004L, 204L, 301L, 401L, 1, "Deleted review", ReviewStatus.APPROVED, true, 9L));
        entityManager.flush();

        assertThat(repository.findByBookingIdAndCustomerIdAndDeletedFalse(1001L, 201L)).contains(approvedA);
        assertThat(repository.findByIdAndDeletedFalse(approvedB.getId())).contains(approvedB);
        assertThat(repository.existsByBookingIdAndDeletedFalse(1003L)).isTrue();
        assertThat(repository.existsByBookingIdAndDeletedFalse(1004L)).isFalse();

        assertThat(repository.averageRating()).isCloseTo(3.6667, org.assertj.core.data.Offset.offset(0.0001));
        assertThat(repository.averageRatingByProviderIdAndStatus(301L, ReviewStatus.APPROVED)).isEqualTo(4.5);
        assertThat(repository.countByDeletedFalseAndReportCountGreaterThan(0)).isEqualTo(2);
        assertThat(repository.countByProviderIdAndStatusAndDeletedFalse(301L, ReviewStatus.APPROVED)).isEqualTo(2);

        List<Object[]> providerSummary = repository.providerWiseRatingSummary();
        assertThat(providerSummary).hasSize(2);
        assertThat(providerSummary.get(0)[0]).isEqualTo(301L);
        assertThat(((Number) providerSummary.get(0)[1]).doubleValue()).isEqualTo(4.5);
        assertThat(((Number) providerSummary.get(0)[2]).longValue()).isEqualTo(2L);

        List<Object[]> serviceSummary = repository.serviceWiseRatingSummary();
        assertThat(serviceSummary).hasSize(2);
        assertThat(serviceSummary.get(0)[0]).isEqualTo(402L);
        assertThat(((Number) serviceSummary.get(0)[1]).doubleValue()).isEqualTo(4.0);

        Page<ReviewEntity> searchPage = repository.searchAdminReviews(
                "clean",
                ReviewStatus.APPROVED,
                4,
                301L,
                402L,
                PageRequest.of(0, 10)
        );

        assertThat(searchPage.getContent()).extracting(ReviewEntity::getBookingId)
                .containsExactly(1002L);

        Page<ReviewEntity> empty = repository.searchAdminReviews(
                "missing",
                ReviewStatus.APPROVED,
                5,
                301L,
                401L,
                PageRequest.of(0, 10)
        );
        assertThat(empty.getContent()).isEmpty();

        List<ReviewEntity> byCustomer = repository.findByCustomerIdAndDeletedFalseOrderByCreatedAtDesc(201L);
        assertThat(byCustomer).extracting(ReviewEntity::getBookingId).containsExactly(1001L);

        List<ReviewEntity> byService = repository.findByServiceIdAndDeletedFalseOrderByCreatedAtDesc(401L);
        assertThat(byService).extracting(ReviewEntity::getBookingId).containsExactly(1003L, 1001L);

        Page<ReviewEntity> providerPage = repository.findByProviderIdAndDeletedFalse(301L, PageRequest.of(0, 10));
        assertThat(providerPage.getContent()).hasSize(2);

        Page<ReviewEntity> reportedPage = repository.findByReportCountGreaterThanAndDeletedFalse(0, PageRequest.of(0, 10));
        assertThat(reportedPage.getContent()).extracting(ReviewEntity::getBookingId).containsExactlyInAnyOrder(1001L, 1003L);
    }

    @Test
    void saveFailsWhenRequiredRatingIsMissing() {
        ReviewEntity invalid = review(2001L, 901L, 902L, 903L, 5, "placeholder", ReviewStatus.PENDING, false, 0L);
        invalid.setRating(null);

        assertThatThrownBy(() -> repository.saveAndFlush(invalid))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    private ReviewEntity review(Long bookingId,
                                Long customerId,
                                Long providerId,
                                Long serviceId,
                                int rating,
                                String comment,
                                ReviewStatus status,
                                boolean deleted,
                                Long reportCount) {
        ReviewEntity entity = new ReviewEntity();
        entity.setBookingId(bookingId);
        entity.setCustomerId(customerId);
        entity.setProviderId(providerId);
        entity.setServiceId(serviceId);
        entity.setRating(rating);
        entity.setReviewComment(comment);
        entity.setStatus(status);
        entity.setDeleted(deleted);
        entity.setReportCount(reportCount);
        return entity;
    }

    @SpringBootConfiguration
    @EnableAutoConfiguration
    @EntityScan(basePackageClasses = ReviewEntity.class)
    @EnableJpaRepositories(
            basePackageClasses = ReviewRepository.class,
            includeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = ReviewRepository.class)
    )
    static class JpaConfig {
    }
}


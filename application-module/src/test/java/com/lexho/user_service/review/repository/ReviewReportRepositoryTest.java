package com.lexho.user_service.review.repository;

import com.lexho.user_service.review.entity.ReviewReport;
import com.lexho.testconfig.LexhoMySqlDataJpaTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.test.context.ContextConfiguration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@LexhoMySqlDataJpaTest
@ContextConfiguration(classes = ReviewReportRepositoryTest.JpaConfig.class)
class ReviewReportRepositoryTest {

    @Autowired
    private ReviewReportRepository repository;

    @Test
    void existsQueryAndNotNullConstraintAreEnforced() {
        ReviewReport report = new ReviewReport();
        report.setReviewId(2001L);
        report.setReportedByUserId(9001L);
        report.setReason("Spam");

        ReviewReport saved = repository.saveAndFlush(report);

        assertThat(repository.existsByReviewIdAndReportedByUserId(2001L, 9001L)).isTrue();
        assertThat(repository.existsByReviewIdAndReportedByUserId(2001L, 9002L)).isFalse();
        assertThat(saved.getCreatedAt()).isNotNull();

        ReviewReport invalid = new ReviewReport();
        invalid.setReviewId(2002L);
        invalid.setReportedByUserId(9003L);

        assertThatThrownBy(() -> repository.saveAndFlush(invalid))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @SpringBootConfiguration
    @EnableAutoConfiguration
    @EntityScan(basePackageClasses = ReviewReport.class)
    @EnableJpaRepositories(
            basePackageClasses = ReviewReportRepository.class,
            includeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = ReviewReportRepository.class)
    )
    static class JpaConfig {
    }
}


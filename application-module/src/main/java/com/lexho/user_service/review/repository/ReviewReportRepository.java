package com.lexho.user_service.review.repository;

import com.lexho.user_service.review.entity.ReviewReport;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReviewReportRepository extends JpaRepository<ReviewReport, Long> {

    boolean existsByReviewIdAndReportedByUserId(Long reviewId, Long reportedByUserId);
}

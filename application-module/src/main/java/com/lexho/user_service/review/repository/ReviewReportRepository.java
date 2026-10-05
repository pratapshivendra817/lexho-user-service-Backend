package com.lexho.user_service.review.repository;

import com.lexho.user_service.review.entity.ReviewReport;
import com.lexho.user_service.review.enums.ReviewReportStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ReviewReportRepository extends JpaRepository<ReviewReport, Long> {

    boolean existsByReviewIdAndReportedByUserId(Long reviewId, Long reportedByUserId);

    Page<ReviewReport> findByStatus(ReviewReportStatus status, Pageable pageable);

    long countByStatus(ReviewReportStatus status);

    List<ReviewReport> findByReviewIdOrderByCreatedAtDesc(Long reviewId);
}

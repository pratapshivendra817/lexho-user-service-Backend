package com.lexho.user_service.review.repository;

import com.lexho.user_service.review.entity.ReviewModerationHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ReviewModerationHistoryRepository extends JpaRepository<ReviewModerationHistory, Long> {

    List<ReviewModerationHistory> findByReviewIdOrderByCreatedAtAsc(Long reviewId);
}

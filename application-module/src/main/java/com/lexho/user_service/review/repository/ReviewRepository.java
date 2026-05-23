package com.lexho.user_service.review.repository;

import com.lexho.user_service.review.entity.ReviewEntity;
import com.lexho.user_service.review.enums.ReviewStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ReviewRepository extends JpaRepository<ReviewEntity, Long>, JpaSpecificationExecutor<ReviewEntity> {

    Optional<ReviewEntity> findByIdAndDeletedFalse(Long id);

    boolean existsByBookingIdAndDeletedFalse(Long bookingId);

    List<ReviewEntity> findByCustomerIdAndDeletedFalseOrderByCreatedAtDesc(Long customerId);

    List<ReviewEntity> findByServiceIdAndDeletedFalseOrderByCreatedAtDesc(Long serviceId);

    Page<ReviewEntity> findByProviderIdAndDeletedFalse(Long providerId, Pageable pageable);

    Page<ReviewEntity> findByReportCountGreaterThanAndDeletedFalse(long reportCount, Pageable pageable);

    long countByDeletedFalse();

    long countByDeletedFalseAndReportCountGreaterThan(long reportCount);

    long countByDeletedFalseAndStatus(ReviewStatus status);

    @Query("SELECT COALESCE(AVG(r.rating), 0) FROM MarketplaceReview r WHERE r.deleted = false")
    Double averageRating();

    @Query("""
            SELECT r.providerId, COALESCE(AVG(r.rating), 0), COUNT(r)
            FROM MarketplaceReview r
            WHERE r.deleted = false
            GROUP BY r.providerId
            ORDER BY COALESCE(AVG(r.rating), 0) DESC
            """)
    List<Object[]> providerWiseRatingSummary();

    @Query("""
            SELECT r.serviceId, COALESCE(AVG(r.rating), 0), COUNT(r)
            FROM MarketplaceReview r
            WHERE r.deleted = false
            GROUP BY r.serviceId
            ORDER BY COALESCE(AVG(r.rating), 0) DESC
            """)
    List<Object[]> serviceWiseRatingSummary();

    @Query("""
            SELECT r FROM MarketplaceReview r
            WHERE r.deleted = false
              AND (:search IS NULL OR LOWER(r.reviewComment) LIKE LOWER(CONCAT('%', :search, '%')))
              AND (:status IS NULL OR r.status = :status)
              AND (:rating IS NULL OR r.rating = :rating)
              AND (:providerId IS NULL OR r.providerId = :providerId)
              AND (:serviceId IS NULL OR r.serviceId = :serviceId)
            """)
    Page<ReviewEntity> searchAdminReviews(@Param("search") String search,
                                          @Param("status") ReviewStatus status,
                                          @Param("rating") Integer rating,
                                          @Param("providerId") Long providerId,
                                          @Param("serviceId") Long serviceId,
                                          Pageable pageable);
}

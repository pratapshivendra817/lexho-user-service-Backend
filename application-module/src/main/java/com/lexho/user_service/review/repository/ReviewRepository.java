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

    String RATING_PARAM = "rating";

    Optional<ReviewEntity> findByIdAndDeletedFalse(Long id);

    Optional<ReviewEntity> findByBookingIdAndCustomerIdAndDeletedFalse(Long bookingId, Long customerId);

    boolean existsByBookingIdAndDeletedFalse(Long bookingId);

    List<ReviewEntity> findByCustomerIdAndDeletedFalseOrderByCreatedAtDesc(Long customerId);

    List<ReviewEntity> findByServiceIdAndDeletedFalseOrderByCreatedAtDesc(Long serviceId);

    Page<ReviewEntity> findByProviderIdAndDeletedFalse(Long providerId, Pageable pageable);

    Page<ReviewEntity> findByReportCountGreaterThanAndDeletedFalse(long reportCount, Pageable pageable);

    long countByDeletedFalse();

    long countByDeletedFalseAndReportCountGreaterThan(long reportCount);

    long countByDeletedFalseAndStatus(ReviewStatus status);

    long countByProviderIdAndStatusAndDeletedFalse(Long providerId, ReviewStatus status);

    @Query("SELECT COALESCE(AVG(r.rating), 0) FROM MarketplaceReview r WHERE r.deleted = false")
    Double averageRating();

    @Query("""
            SELECT COALESCE(AVG(r.rating), 0)
            FROM MarketplaceReview r
            WHERE r.deleted = false
              AND r.status = :status
              AND r.providerId = :providerId
            """)
    Double averageRatingByProviderIdAndStatus(@Param("providerId") Long providerId,
                                              @Param("status") ReviewStatus status);

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
              AND (:search IS NULL OR LOWER(r.reviewComment) LIKE LOWER(CONCAT('%', :search, '%'))
                   OR str(r.id) = :search
                   OR str(r.bookingId) = :search)
              AND (:status IS NULL OR r.status = :status)
              AND (:rating IS NULL OR r.rating = :rating)
              AND (:providerId IS NULL OR r.providerId = :providerId)
              AND (:serviceId IS NULL OR r.serviceId = :serviceId)
            """)
    Page<ReviewEntity> searchAdminReviews(@Param("search") String search,
                                          @Param("status") ReviewStatus status,
                                          @Param(RATING_PARAM) Integer rating,
                                          @Param("providerId") Long providerId,
                                          @Param("serviceId") Long serviceId,
                                          Pageable pageable);

    @Query("""
            SELECT r
            FROM MarketplaceReview r
            WHERE r.deleted = false
              AND (:search IS NULL OR LOWER(r.reviewComment) LIKE LOWER(CONCAT('%', :search, '%'))
                   OR str(r.id) = :search
                   OR str(r.bookingId) = :search)
              AND (:status IS NULL OR r.status = :status)
              AND (:rating IS NULL OR r.rating = :rating)
              AND (:providerId IS NULL OR r.providerId = :providerId)
              AND (:serviceId IS NULL OR r.serviceId = :serviceId)
              AND (:startAt IS NULL OR r.createdAt >= :startAt)
              AND (:endAt IS NULL OR r.createdAt <= :endAt)
              AND (:reported IS NULL OR (:reported = true AND r.reportCount > 0) OR (:reported = false AND r.reportCount = 0))
            """)
    Page<ReviewEntity> searchAdminReviewsAdvanced(@Param("search") String search,
                                                  @Param("status") ReviewStatus status,
                                                  @Param(RATING_PARAM) Integer rating,
                                                  @Param("providerId") Long providerId,
                                                  @Param("serviceId") Long serviceId,
                                                  @Param("startAt") java.time.LocalDateTime startAt,
                                                  @Param("endAt") java.time.LocalDateTime endAt,
                                                  @Param("reported") Boolean reported,
                                                  Pageable pageable);

    @Query("""
            SELECT r.rating, COUNT(r)
            FROM MarketplaceReview r
            WHERE r.deleted = false
              AND (:serviceId IS NULL OR r.serviceId = :serviceId)
              AND (:providerId IS NULL OR r.providerId = :providerId)
              AND (:startAt IS NULL OR r.createdAt >= :startAt)
              AND (:endAt IS NULL OR r.createdAt <= :endAt)
            GROUP BY r.rating
            """)
    List<Object[]> ratingDistribution(@Param("serviceId") Long serviceId,
                                      @Param("providerId") Long providerId,
                                      @Param("startAt") java.time.LocalDateTime startAt,
                                      @Param("endAt") java.time.LocalDateTime endAt);

    @Query("""
            SELECT COUNT(r)
            FROM MarketplaceReview r
            WHERE r.deleted = false
              AND r.status = :status
              AND (:serviceId IS NULL OR r.serviceId = :serviceId)
              AND (:providerId IS NULL OR r.providerId = :providerId)
              AND (:startAt IS NULL OR r.createdAt >= :startAt)
              AND (:endAt IS NULL OR r.createdAt <= :endAt)
            """)
    long countByStatusForStats(@Param("status") ReviewStatus status,
                               @Param("serviceId") Long serviceId,
                               @Param("providerId") Long providerId,
                               @Param("startAt") java.time.LocalDateTime startAt,
                               @Param("endAt") java.time.LocalDateTime endAt);

    @Query("""
            SELECT COUNT(r)
            FROM MarketplaceReview r
            WHERE r.deleted = false
              AND (:serviceId IS NULL OR r.serviceId = :serviceId)
              AND (:providerId IS NULL OR r.providerId = :providerId)
              AND (:startAt IS NULL OR r.createdAt >= :startAt)
              AND (:endAt IS NULL OR r.createdAt <= :endAt)
            """)
    long countForStats(@Param("serviceId") Long serviceId,
                       @Param("providerId") Long providerId,
                       @Param("startAt") java.time.LocalDateTime startAt,
                       @Param("endAt") java.time.LocalDateTime endAt);

    @Query("""
            SELECT COALESCE(AVG(r.rating), 0)
            FROM MarketplaceReview r
            WHERE r.deleted = false
              AND (:serviceId IS NULL OR r.serviceId = :serviceId)
              AND (:providerId IS NULL OR r.providerId = :providerId)
              AND (:startAt IS NULL OR r.createdAt >= :startAt)
              AND (:endAt IS NULL OR r.createdAt <= :endAt)
            """)
    Double averageRatingForStats(@Param("serviceId") Long serviceId,
                                 @Param("providerId") Long providerId,
                                 @Param("startAt") java.time.LocalDateTime startAt,
                                 @Param("endAt") java.time.LocalDateTime endAt);
}

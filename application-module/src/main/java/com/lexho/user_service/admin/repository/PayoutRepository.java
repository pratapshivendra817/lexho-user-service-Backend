package com.lexho.user_service.admin.repository;

import com.lexho.user_service.admin.entity.Payout;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.time.LocalDateTime;
import java.util.List;

public interface PayoutRepository extends JpaRepository<Payout, Long> {

    Optional<Payout> findTopByReferenceBookingIdOrderByCreatedAtDesc(Long referenceBookingId);

    Optional<Payout> findByIdAndProviderId(Long id, Long providerId);

    Page<Payout> findByProviderId(Long providerId, Pageable pageable);

    Page<Payout> findByProviderIdAndCreatedAtBetween(Long providerId,
                                                     LocalDateTime start,
                                                     LocalDateTime end,
                                                     Pageable pageable);

    List<Payout> findByProviderId(Long providerId);

    long countByProviderIdAndStatusIn(Long providerId, List<String> statuses);

    @Query("""
            SELECT p FROM Payout p
            WHERE (:search IS NULL OR str(p.providerId) LIKE CONCAT('%', :search, '%'))
              AND (:status IS NULL OR LOWER(p.status) = LOWER(:status))
            """)
    Page<Payout> search(@Param("search") String search,
                        @Param("status") String status,
                        Pageable pageable);
}

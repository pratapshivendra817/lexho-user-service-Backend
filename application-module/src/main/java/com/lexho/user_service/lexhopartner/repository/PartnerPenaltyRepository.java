package com.lexho.user_service.lexhopartner.repository;

import com.lexho.user_service.lexhopartner.entity.PartnerPenalty;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface PartnerPenaltyRepository extends JpaRepository<PartnerPenalty, Long> {
    List<PartnerPenalty> findByPartnerIdOrderByCreatedAtDesc(Long partnerId);
    java.util.Optional<PartnerPenalty> findByIdAndPartnerId(Long id, Long partnerId);

    @Query("""
            SELECT p FROM PartnerPenalty p
            WHERE UPPER(p.status) = 'WARNING'
              AND p.disputeStatus IS NOT NULL
              AND UPPER(p.disputeStatus) <> 'NONE'
              AND (:disputeStatus IS NULL OR UPPER(p.disputeStatus) = UPPER(:disputeStatus))
            ORDER BY p.disputedAt DESC, p.createdAt DESC
            """)
    Page<PartnerPenalty> findWarningDisputes(String disputeStatus, Pageable pageable);
}

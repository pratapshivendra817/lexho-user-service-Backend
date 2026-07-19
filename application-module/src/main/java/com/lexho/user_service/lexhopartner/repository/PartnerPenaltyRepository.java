package com.lexho.user_service.lexhopartner.repository;

import com.lexho.user_service.lexhopartner.entity.PartnerPenalty;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PartnerPenaltyRepository extends JpaRepository<PartnerPenalty, Long> {
    List<PartnerPenalty> findByPartnerIdOrderByCreatedAtDesc(Long partnerId);
}

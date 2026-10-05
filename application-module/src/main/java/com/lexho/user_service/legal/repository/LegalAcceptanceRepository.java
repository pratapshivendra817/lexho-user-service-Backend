package com.lexho.user_service.legal.repository;

import com.lexho.user_service.legal.entity.LegalAcceptance;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface LegalAcceptanceRepository extends JpaRepository<LegalAcceptance, Long> {
    Optional<LegalAcceptance> findByUserIdAndUserTypeAndDocumentTypeAndDocumentVersion(
            Long userId, String userType, String documentType, int documentVersion);

    List<LegalAcceptance> findByUserIdAndUserTypeOrderByAcceptedAtDesc(Long userId, String userType);
}

package com.lexho.user_service.lexhopartner.repository;

import com.lexho.user_service.lexhopartner.entity.PartnerLeave;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface PartnerLeaveRepository extends JpaRepository<PartnerLeave, Long> {
    List<PartnerLeave> findByPartnerIdOrderByFromDateDesc(Long partnerId);
    boolean existsByPartnerIdAndFromDateLessThanEqualAndToDateGreaterThanEqual(Long partnerId, LocalDate toDate, LocalDate fromDate);
    java.util.Optional<PartnerLeave> findByIdAndPartnerId(Long id, Long partnerId);
}

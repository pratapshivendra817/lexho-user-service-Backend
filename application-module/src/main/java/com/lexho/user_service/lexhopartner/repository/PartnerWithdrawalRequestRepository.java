package com.lexho.user_service.lexhopartner.repository;

import com.lexho.user_service.lexhopartner.entity.PartnerWithdrawalRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PartnerWithdrawalRequestRepository extends JpaRepository<PartnerWithdrawalRequest, Long> {

    Page<PartnerWithdrawalRequest> findByPartnerId(Long partnerId, Pageable pageable);

    List<PartnerWithdrawalRequest> findByPartnerIdAndStatusIn(Long partnerId, List<String> statuses);
}

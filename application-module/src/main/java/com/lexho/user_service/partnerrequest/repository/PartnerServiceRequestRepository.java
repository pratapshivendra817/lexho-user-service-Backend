package com.lexho.user_service.partnerrequest.repository;

import com.lexho.user_service.partnerrequest.entity.PartnerServiceRequestEntity;
import com.lexho.user_service.partnerrequest.model.PartnerServiceRequestStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;

import java.util.List;
import java.util.Optional;

public interface PartnerServiceRequestRepository extends JpaRepository<PartnerServiceRequestEntity, Long> {
    Page<PartnerServiceRequestEntity> findByPartnerIdOrderByCreatedAtDesc(Long partnerId, Pageable pageable);
    Page<PartnerServiceRequestEntity> findByPartnerIdAndStatusOrderByCreatedAtDesc(Long partnerId, PartnerServiceRequestStatus status, Pageable pageable);
    Optional<PartnerServiceRequestEntity> findByIdAndPartnerId(Long id, Long partnerId);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from PartnerServiceRequestEntity r where r.id = :id and r.partnerId = :partnerId")
    Optional<PartnerServiceRequestEntity> findByIdAndPartnerIdForUpdate(@Param("id") Long id, @Param("partnerId") Long partnerId);
    List<PartnerServiceRequestEntity> findByCartIdAndStatus(Long cartId, PartnerServiceRequestStatus status);
    List<PartnerServiceRequestEntity> findByCartId(Long cartId);
    boolean existsByCartIdAndPartnerIdAndStatus(Long cartId, Long partnerId, PartnerServiceRequestStatus status);
    long countByCartIdAndStatus(Long cartId, PartnerServiceRequestStatus status);
}

package com.lexho.user_service.dispatch.repository;

import com.lexho.user_service.dispatch.entity.PartnerShiftEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface PartnerShiftRepository extends JpaRepository<PartnerShiftEntity, Long> {
    Optional<PartnerShiftEntity> findByPartnerIdAndDeletedFalse(Long partnerId);
    List<PartnerShiftEntity> findByPartnerIdAndDeletedFalseOrderByUpdatedAtDesc(Long partnerId);

    List<PartnerShiftEntity> findByCurrentPincodeInAndOnlineTrueAndDeletedFalse(Collection<String> pincodes);
    List<PartnerShiftEntity> findByPartnerIdInAndDeletedFalse(Collection<Long> partnerIds);
    List<PartnerShiftEntity> findByOnlineTrueAndDeletedFalseAndUpdatedAtBefore(LocalDateTime cutoff);
    List<PartnerShiftEntity> findByOnlineTrueAndDeletedFalseAndShiftEndBefore(LocalDateTime cutoff);
    boolean existsByOnlineTrueAndDeletedFalse();

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from PartnerShiftEntity s where s.partnerId = :partnerId and s.deleted = false")
    Optional<PartnerShiftEntity> findByPartnerIdForUpdate(@Param("partnerId") Long partnerId);

    default Optional<PartnerShiftEntity> findByPartnerId(Long partnerId) {
        return findByPartnerIdAndDeletedFalse(partnerId);
    }

    default List<PartnerShiftEntity> findShiftHistoryByPartnerId(Long partnerId) {
        return findByPartnerIdAndDeletedFalseOrderByUpdatedAtDesc(partnerId);
    }

    default List<PartnerShiftEntity> findByCurrentPincodeInAndOnlineTrue(Collection<String> pincodes) {
        return findByCurrentPincodeInAndOnlineTrueAndDeletedFalse(pincodes);
    }

    default boolean existsByOnlineTrue() {
        return existsByOnlineTrueAndDeletedFalse();
    }
}

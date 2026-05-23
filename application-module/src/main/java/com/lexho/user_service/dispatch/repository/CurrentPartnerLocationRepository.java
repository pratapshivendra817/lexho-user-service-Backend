package com.lexho.user_service.dispatch.repository;

import com.lexho.user_service.dispatch.entity.CurrentPartnerLocationEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface CurrentPartnerLocationRepository extends JpaRepository<CurrentPartnerLocationEntity, Long> {
    Optional<CurrentPartnerLocationEntity> findFirstByPartnerId(Long partnerId);
    Optional<CurrentPartnerLocationEntity> findByPartnerIdAndDeletedFalse(Long partnerId);
    List<CurrentPartnerLocationEntity> findByPartnerIdInAndDeletedFalse(Collection<Long> partnerIds);
    List<CurrentPartnerLocationEntity> findByDeletedFalseAndUpdatedAtBefore(LocalDateTime cutoff);

    default Optional<CurrentPartnerLocationEntity> findByPartnerId(Long partnerId) {
        return findByPartnerIdAndDeletedFalse(partnerId);
    }

    default Optional<CurrentPartnerLocationEntity> findAnyByPartnerId(Long partnerId) {
        return findFirstByPartnerId(partnerId);
    }
}

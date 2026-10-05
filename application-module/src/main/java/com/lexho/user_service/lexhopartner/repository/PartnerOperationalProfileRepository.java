package com.lexho.user_service.lexhopartner.repository;

import com.lexho.user_service.lexhopartner.entity.PartnerOperationalProfile;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface PartnerOperationalProfileRepository extends JpaRepository<PartnerOperationalProfile, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from PartnerOperationalProfile p where p.id = :id")
    Optional<PartnerOperationalProfile> findByIdForUpdate(@Param("id") Long id);

    boolean existsByPhoneNumberAndIdNot(String phoneNumber, Long id);

    Page<PartnerOperationalProfile> findByDeletedFalse(Pageable pageable);
}

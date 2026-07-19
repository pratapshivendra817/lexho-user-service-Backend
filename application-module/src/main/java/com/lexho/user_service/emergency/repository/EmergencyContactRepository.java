package com.lexho.user_service.emergency.repository;

import com.lexho.user_service.emergency.entity.EmergencyContact;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EmergencyContactRepository extends JpaRepository<EmergencyContact, Long> {

    List<EmergencyContact> findByPartnerIdAndActiveTrueOrderByPrimaryContactDescCreatedAtAsc(Long partnerId);

    long countByPartnerIdAndActiveTrue(Long partnerId);

    boolean existsByPartnerIdAndPhoneNumberAndActiveTrue(Long partnerId, String phoneNumber);
}

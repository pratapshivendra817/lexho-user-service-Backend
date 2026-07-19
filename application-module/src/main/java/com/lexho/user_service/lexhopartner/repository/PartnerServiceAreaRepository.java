package com.lexho.user_service.lexhopartner.repository;

import com.lexho.user_service.lexhopartner.entity.PartnerServiceArea;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PartnerServiceAreaRepository extends JpaRepository<PartnerServiceArea, Long> {
    List<PartnerServiceArea> findByPartnerIdOrderByPincodeAsc(Long partnerId);
    void deleteByPartnerId(Long partnerId);
}

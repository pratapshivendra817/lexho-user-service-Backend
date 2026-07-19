package com.lexho.user_service.admin.serviceablearea.repository;

import com.lexho.user_service.admin.serviceablearea.entity.ServiceabilityRuleEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ServiceabilityRuleRepository extends JpaRepository<ServiceabilityRuleEntity, Long> {
    List<ServiceabilityRuleEntity> findByPincodeAndActiveTrueAndDeletedFalse(String pincode);
    List<ServiceabilityRuleEntity> findByCityIgnoreCaseAndActiveTrueAndDeletedFalse(String city);
    List<ServiceabilityRuleEntity> findByActiveTrueAndDeletedFalse();
}

package com.lexho.user_service.admin.serviceablearea.repository;

import com.lexho.user_service.admin.serviceablearea.entity.ServiceableAreaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;
import java.util.Optional;

public interface ServiceableAreaRepository extends JpaRepository<ServiceableAreaEntity, Long>, JpaSpecificationExecutor<ServiceableAreaEntity> {
    boolean existsByPincodeAndActiveTrueAndDeletedFalse(String pincode);
    Optional<ServiceableAreaEntity> findFirstByPincodeAndActiveTrueAndDeletedFalse(String pincode);
    List<ServiceableAreaEntity> findByPincodeAndActiveTrueAndDeletedFalse(String pincode);
    List<ServiceableAreaEntity> findByCityIgnoreCaseAndActiveTrueAndDeletedFalse(String city);
    List<ServiceableAreaEntity> findByActiveTrueAndDeletedFalse();
}

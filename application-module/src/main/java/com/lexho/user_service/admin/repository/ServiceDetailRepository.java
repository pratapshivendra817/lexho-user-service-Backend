package com.lexho.user_service.admin.repository;

import com.lexho.user_service.admin.entity.DetailType;
import com.lexho.user_service.admin.entity.ServiceDetailEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ServiceDetailRepository extends JpaRepository<ServiceDetailEntity, Long> {
    List<ServiceDetailEntity> findByService_Id(Long serviceId);
    List<ServiceDetailEntity> findByService_IdAndType(Long serviceId, DetailType type);
    Optional<ServiceDetailEntity> findByIdAndService_Id(Long id, Long serviceId);
}

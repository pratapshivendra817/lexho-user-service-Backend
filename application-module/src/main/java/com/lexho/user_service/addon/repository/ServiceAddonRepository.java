package com.lexho.user_service.addon.repository;

import com.lexho.user_service.addon.entity.ServiceAddonEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface ServiceAddonRepository extends JpaRepository<ServiceAddonEntity, Long> {
    Optional<ServiceAddonEntity> findByServiceIdAndAddonId(Long serviceId, Long addonId);
    List<ServiceAddonEntity> findByServiceId(Long serviceId);
    List<ServiceAddonEntity> findByServiceIdIn(Collection<Long> serviceIds);
    List<ServiceAddonEntity> findByAddonId(Long addonId);
    void deleteByAddonId(Long addonId);
}

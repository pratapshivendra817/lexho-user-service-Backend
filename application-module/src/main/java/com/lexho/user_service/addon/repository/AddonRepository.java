package com.lexho.user_service.addon.repository;

import com.lexho.user_service.addon.entity.AddonEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface AddonRepository extends JpaRepository<AddonEntity, Long> {
    Page<AddonEntity> findByAddonNameContainingIgnoreCase(String addonName, Pageable pageable);
    List<AddonEntity> findByIdInAndActiveTrue(Collection<Long> ids);
    List<AddonEntity> findByIdIn(Collection<Long> ids);
}

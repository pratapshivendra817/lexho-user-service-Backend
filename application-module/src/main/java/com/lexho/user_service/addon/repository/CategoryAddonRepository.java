package com.lexho.user_service.addon.repository;

import com.lexho.user_service.addon.entity.CategoryAddonEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface CategoryAddonRepository extends JpaRepository<CategoryAddonEntity, Long> {
    Optional<CategoryAddonEntity> findByCategoryIdAndAddonId(Long categoryId, Long addonId);
    List<CategoryAddonEntity> findByCategoryId(Long categoryId);
    List<CategoryAddonEntity> findByCategoryIdIn(Collection<Long> categoryIds);
    List<CategoryAddonEntity> findByAddonId(Long addonId);
    void deleteByAddonId(Long addonId);
}

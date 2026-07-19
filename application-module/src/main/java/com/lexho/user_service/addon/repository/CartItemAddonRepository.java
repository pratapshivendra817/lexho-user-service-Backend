package com.lexho.user_service.addon.repository;

import com.lexho.user_service.addon.entity.CartItemAddonEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface CartItemAddonRepository extends JpaRepository<CartItemAddonEntity, Long> {
    List<CartItemAddonEntity> findByCartItemId(Long cartItemId);
    List<CartItemAddonEntity> findByCartItemIdIn(Collection<Long> cartItemIds);
    List<CartItemAddonEntity> findByUserId(Long userId);
    void deleteByCartItemId(Long cartItemId);
    void deleteByCartItemIdIn(Collection<Long> cartItemIds);
    void deleteByUserId(Long userId);
    void deleteByAddonId(Long addonId);
}

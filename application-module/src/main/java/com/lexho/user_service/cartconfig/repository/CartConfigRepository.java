package com.lexho.user_service.cartconfig.repository;

import com.lexho.user_service.cartconfig.entity.CartConfigEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CartConfigRepository extends JpaRepository<CartConfigEntity, Long> {
}

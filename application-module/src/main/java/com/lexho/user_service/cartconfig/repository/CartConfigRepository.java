package com.lexho.user_service.cartconfig.repository;

import com.lexho.user_service.cartconfig.entity.CartConfigEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface CartConfigRepository extends JpaRepository<CartConfigEntity, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from CartConfigEntity c where c.id = :id")
    Optional<CartConfigEntity> findByIdForUpdate(@Param("id") Long id);
}

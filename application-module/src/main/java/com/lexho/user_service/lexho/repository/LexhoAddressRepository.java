package com.lexho.user_service.lexho.repository;

import com.lexho.user_service.lexho.entity.LexhoAddress;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import jakarta.persistence.LockModeType;

import java.util.List;
import java.util.Optional;

public interface LexhoAddressRepository extends JpaRepository<LexhoAddress, Long> {
    List<LexhoAddress> findByUserIdAndIsDeletedFalse(Long userId);
    Optional<LexhoAddress> findByIdAndUserIdAndIsDeletedFalse(Long id, Long userId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select a from LexhoAddress a where a.userId = :userId and a.isDeleted = false")
    List<LexhoAddress> findByUserIdAndIsDeletedFalseForUpdate(Long userId);

    default List<LexhoAddress> findByUserId(Long userId) {
        return findByUserIdAndIsDeletedFalse(userId);
    }

    default Optional<LexhoAddress> findByIdAndUserId(Long id, Long userId) {
        return findByIdAndUserIdAndIsDeletedFalse(id, userId);
    }
}

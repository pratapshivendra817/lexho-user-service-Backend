package com.lexho.user_service.lexho.repository;

import com.lexho.user_service.lexho.entity.LexhoFavorite;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface LexhoFavoriteRepository extends JpaRepository<LexhoFavorite, Long> {
    List<LexhoFavorite> findByUserIdOrderByIdDesc(Long userId);
    Optional<LexhoFavorite> findByIdAndUserId(Long id, Long userId);
    boolean existsByUserIdAndServiceId(Long userId, Long serviceId);
}

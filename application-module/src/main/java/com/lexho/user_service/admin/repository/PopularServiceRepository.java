package com.lexho.user_service.admin.repository;

import com.lexho.user_service.admin.entity.PopularServiceEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface PopularServiceRepository extends JpaRepository<PopularServiceEntity, Long> {

    List<PopularServiceEntity> findByCategory_IdAndActiveTrueOrderByPriorityAsc(Long categoryId);

    boolean existsByService_IdAndCategory_IdAndActiveTrue(Long serviceId, Long categoryId);

    boolean existsByCategory_IdAndPriorityAndActiveTrue(Long categoryId, Integer priority);

    @Query("""
            SELECT ps.id AS id,
                   s.name AS name,
                   s.imageUrl AS imageUrl,
                   s.basePrice AS price,
                   s.avgRating AS rating,
                   ps.priority AS priority
            FROM PopularServiceEntity ps
            JOIN ps.service s
            WHERE ps.active = true
              AND ps.category.id = :categoryId
              AND s.deleted = false
              AND s.active = true
            ORDER BY ps.priority ASC
            """)
    List<PopularServiceView> findActiveServiceViewsByCategory(@Param("categoryId") Long categoryId);
}

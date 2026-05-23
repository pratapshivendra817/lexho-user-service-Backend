package com.lexho.user_service.customer.repository;

import com.lexho.user_service.admin.entity.PopularServiceEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface CustomerPopularServiceRepository extends JpaRepository<PopularServiceEntity, Long> {

    @Query("""
            SELECT s.id AS id,
                   s.name AS name,
                   s.imageUrl AS imageUrl,
                   s.basePrice AS price,
                   s.avgRating AS rating,
                   c.id AS categoryId
            FROM PopularServiceEntity ps
            JOIN ps.service s
            JOIN ps.category c
            WHERE ps.active = true
              AND c.id = :categoryId
              AND s.active = true
              AND s.deleted = false
            ORDER BY ps.priority ASC
            """)
    List<CustomerPopularServiceView> findActivePopularServicesByCategory(@Param("categoryId") Long categoryId);
}

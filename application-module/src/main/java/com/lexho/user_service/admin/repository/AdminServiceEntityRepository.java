package com.lexho.user_service.admin.repository;

import com.lexho.user_service.admin.entity.AdminServiceEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface AdminServiceEntityRepository extends JpaRepository<AdminServiceEntity, Long> {

    @Query("""
            SELECT s FROM AdminServiceEntity s
            WHERE s.deleted = false
              AND (:search IS NULL OR LOWER(s.name) LIKE LOWER(CONCAT('%', :search, '%')))
              AND (:status IS NULL OR LOWER(s.status) = LOWER(:status))
            """)
    Page<AdminServiceEntity> search(@Param("search") String search,
                                    @Param("status") String status,
                                    Pageable pageable);

    @Query("""
            SELECT s FROM AdminServiceEntity s
            LEFT JOIN s.category c
            WHERE s.deleted = false
              AND (:search IS NULL OR LOWER(s.name) LIKE LOWER(CONCAT('%', :search, '%')))
              AND (:status IS NULL OR s.active = :status)
              AND (:category IS NULL OR LOWER(c.name) = LOWER(:category))
            """)
    Page<AdminServiceEntity> searchCatalog(@Param("search") String search,
                                           @Param("status") Boolean status,
                                           @Param("category") String category,
                                           Pageable pageable);

    Optional<AdminServiceEntity> findByIdAndDeletedFalse(Long id);

    List<AdminServiceEntity> findByCategory_IdAndDeletedFalse(Long categoryId);

    @Query("""
            SELECT COALESCE(s.categoryName, 'UNKNOWN'), COUNT(s)
            FROM AdminServiceEntity s
            WHERE s.deleted = false
            GROUP BY s.categoryName
            ORDER BY COUNT(s) DESC
            """)
    List<Object[]> distributionByCategory();

    @Query("""
            SELECT COALESCE(c.name, 'UNKNOWN'),
                   COUNT(s),
                   COALESCE(SUM(s.totalBookings), 0)
            FROM AdminServiceEntity s
            LEFT JOIN s.category c
            WHERE s.deleted = false
            GROUP BY c.name
            ORDER BY COUNT(s) DESC
            """)
    List<Object[]> serviceSummaryByCategory();

    @Query("""
            SELECT s FROM AdminServiceEntity s
            WHERE s.deleted = false AND s.active = true
            ORDER BY COALESCE(s.totalBookings, 0) DESC
            """)
    List<AdminServiceEntity> findTopBookedServices(Pageable pageable);

    @Query("""
            SELECT COALESCE(c.name, 'UNKNOWN'),
                   COALESCE(AVG(s.avgRating), 0),
                   COALESCE(MAX(s.avgRating), 0),
                   COALESCE(MIN(s.avgRating), 0),
                   COUNT(s)
            FROM AdminServiceEntity s
            LEFT JOIN s.category c
            WHERE s.deleted = false
            GROUP BY c.name
            ORDER BY COALESCE(AVG(s.avgRating), 0) DESC
            """)
    List<Object[]> ratingAnalyticsByCategory();
}

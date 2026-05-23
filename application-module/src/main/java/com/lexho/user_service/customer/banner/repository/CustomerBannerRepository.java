package com.lexho.user_service.customer.banner.repository;

import com.lexho.user_service.customer.banner.entity.CustomerBannerEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface CustomerBannerRepository extends JpaRepository<CustomerBannerEntity, Long> {

    @Query("""
            SELECT b FROM CustomerBannerEntity b
            WHERE b.active = true
              AND (b.categoryId = :categoryId OR b.categoryId IS NULL)
            ORDER BY b.priority ASC, b.id DESC
            """)
    List<CustomerBannerEntity> findActiveByCategoryOrGlobal(@Param("categoryId") Long categoryId, Pageable pageable);

    @Query("""
            SELECT b FROM CustomerBannerEntity b
            WHERE b.active = true
              AND b.categoryId IS NULL
            ORDER BY b.priority ASC, b.id DESC
            """)
    List<CustomerBannerEntity> findActiveGlobal(Pageable pageable);

    @Query("""
            SELECT b FROM CustomerBannerEntity b
            WHERE (:categoryId IS NULL OR b.categoryId = :categoryId)
              AND (:active IS NULL OR b.active = :active)
            """)
    Page<CustomerBannerEntity> searchForAdmin(@Param("categoryId") Long categoryId,
                                              @Param("active") Boolean active,
                                              Pageable pageable);
}

package com.lexho.user_service.admin.repository;

import com.lexho.user_service.admin.entity.Banner;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface BannerRepository extends JpaRepository<Banner, Long> {

    @Query("""
            SELECT b FROM Banner b
            WHERE (:search IS NULL OR LOWER(b.title) LIKE LOWER(CONCAT('%', :search, '%')))
              AND (:active IS NULL OR b.active = :active)
            """)
    Page<Banner> search(@Param("search") String search,
                        @Param("active") Boolean active,
                        Pageable pageable);
}

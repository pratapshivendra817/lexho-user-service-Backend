package com.lexho.user_service.homebanner.repository;

import com.lexho.user_service.homebanner.entity.HomeBanner;
import com.lexho.user_service.homebanner.entity.HomeBannerAudience;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Collection;

public interface HomeBannerRepository extends JpaRepository<HomeBanner, Long> {

    @Query("""
            SELECT b FROM HomeBanner b
            WHERE b.active = true
              AND b.audience IN :audiences
            ORDER BY b.priority ASC, b.id ASC
            """)
    List<HomeBanner> findTopActiveByAudienceIn(Collection<HomeBannerAudience> audiences, Pageable pageable);

    List<HomeBanner> findAllByOrderByPriorityAscIdAsc();

    List<HomeBanner> findByAudienceOrderByPriorityAscIdAsc(HomeBannerAudience audience);
}

package com.lexho.user_service.dispatch.repository;

import com.lexho.user_service.dispatch.entity.PartnerServiceSkillEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface PartnerServiceSkillRepository extends JpaRepository<PartnerServiceSkillEntity, Long> {
    boolean existsByPartnerIdAndSubServiceId(Long partnerId, Long subServiceId);
    List<PartnerServiceSkillEntity> findByPartnerId(Long partnerId);
    void deleteByPartnerId(Long partnerId);

    @Query("""
            SELECT CASE WHEN COUNT(ps) > 0 THEN true ELSE false END
            FROM PartnerServiceSkillEntity ps
            JOIN com.lexho.user_service.admin.entity.AdminServiceEntity s ON s.id = ps.subServiceId
            WHERE ps.partnerId = :partnerId
              AND s.category.id = :categoryId
            """)
    boolean existsByPartnerIdAndCategoryId(@Param("partnerId") Long partnerId, @Param("categoryId") Long categoryId);
}

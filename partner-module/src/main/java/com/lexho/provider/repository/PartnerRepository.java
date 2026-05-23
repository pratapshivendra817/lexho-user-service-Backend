package com.lexho.partner.repository;

import com.lexho.partner.entity.Partner;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface PartnerRepository extends JpaRepository<Partner, Long> {

    List<Partner> findBySubCategoryIdAndAvailableTrue(Long subCategoryId);

    List<Partner> findBySubCategoryIdAndAvailableTrueOrderByRatingDesc(Long subCategoryId);

    List<Partner> findBySubCategoryIdAndAvailableTrueOrderByPriceAsc(Long subCategoryId);

    List<Partner> findByNameAndSubCategoryId(String name, Long subCategoryId);

    List<Partner> findTop6ByAvailableTrueOrderByRatingDesc();

    List<Partner> findTop6ByCategoryIdAndAvailableTrueOrderByRatingDesc(Long categoryId);

    @Query("""
            SELECT p FROM Partner p
            WHERE (:search IS NULL OR LOWER(COALESCE(p.name, '')) LIKE LOWER(CONCAT('%', :search, '%')))
              AND (:verificationStatus IS NULL OR LOWER(COALESCE(p.verificationStatus, 'PENDING')) = LOWER(:verificationStatus))
            """)
    Page<Partner> searchPartners(@Param("search") String search,
                                 @Param("verificationStatus") String verificationStatus,
                                 Pageable pageable);
}

package com.lexho.user_service.subscription.repository;

import com.lexho.user_service.subscription.entity.CookCoverage;
import com.lexho.user_service.subscription.enums.FamilySize;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CookCoverageRepository extends JpaRepository<CookCoverage, Long> {
    @EntityGraph(attributePaths = "items")
    List<CookCoverage> findByActiveTrueOrderBySortOrderAscFamilySizeAsc();

    @EntityGraph(attributePaths = "items")
    List<CookCoverage> findByFamilySizeAndActiveTrueOrderBySortOrderAsc(FamilySize familySize);

    @EntityGraph(attributePaths = "items")
    List<CookCoverage> findByServiceIdAndActiveTrueOrderBySortOrderAscFamilySizeAsc(Long serviceId);

    @EntityGraph(attributePaths = "items")
    List<CookCoverage> findByServiceIdAndFamilySizeAndActiveTrueOrderBySortOrderAsc(Long serviceId, FamilySize familySize);

    @EntityGraph(attributePaths = "items")
    List<CookCoverage> findByServiceIdIsNullAndActiveTrueOrderBySortOrderAscFamilySizeAsc();

    @EntityGraph(attributePaths = "items")
    List<CookCoverage> findByServiceIdIsNullAndFamilySizeAndActiveTrueOrderBySortOrderAsc(FamilySize familySize);

    @EntityGraph(attributePaths = "items")
    List<CookCoverage> findAllByOrderBySortOrderAscFamilySizeAsc();

    @EntityGraph(attributePaths = "items")
    List<CookCoverage> findAllByServiceIdOrderBySortOrderAscFamilySizeAsc(Long serviceId);

    @EntityGraph(attributePaths = "items")
    List<CookCoverage> findAllByServiceIdIsNullOrderBySortOrderAscFamilySizeAsc();

    Optional<CookCoverage> findByServiceIdAndFamilySizeAndActiveTrue(Long serviceId, FamilySize familySize);

    Optional<CookCoverage> findByServiceIdIsNullAndFamilySizeAndActiveTrue(FamilySize familySize);

    @Override
    @EntityGraph(attributePaths = "items")
    Optional<CookCoverage> findById(Long id);
}

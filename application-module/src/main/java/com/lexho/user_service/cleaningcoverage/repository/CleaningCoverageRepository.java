package com.lexho.user_service.cleaningcoverage.repository;

import com.lexho.user_service.cleaningcoverage.entity.CleaningCoverageEntity;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface CleaningCoverageRepository extends JpaRepository<CleaningCoverageEntity, Long> {

    @EntityGraph(attributePaths = "tasks")
    List<CleaningCoverageEntity> findByActiveTrueOrderBySortOrderAscDurationHoursAsc();

    @EntityGraph(attributePaths = "tasks")
    List<CleaningCoverageEntity> findByDurationHoursAndActiveTrueOrderBySortOrderAsc(BigDecimal durationHours);

    @EntityGraph(attributePaths = "tasks")
    List<CleaningCoverageEntity> findAllByOrderBySortOrderAscDurationHoursAsc();

    @Override
    @EntityGraph(attributePaths = "tasks")
    Optional<CleaningCoverageEntity> findById(Long id);
}

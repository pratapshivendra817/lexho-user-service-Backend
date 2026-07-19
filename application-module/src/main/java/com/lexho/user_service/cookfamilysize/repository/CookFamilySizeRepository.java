package com.lexho.user_service.cookfamilysize.repository;

import com.lexho.user_service.cookfamilysize.entity.CookFamilySizeEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CookFamilySizeRepository extends JpaRepository<CookFamilySizeEntity, Long> {
    Optional<CookFamilySizeEntity> findByFamilySize(String familySize);
    Optional<CookFamilySizeEntity> findByIdAndActiveTrue(Long id);
    Optional<CookFamilySizeEntity> findByFamilySizeAndActiveTrue(String familySize);
    List<CookFamilySizeEntity> findByActiveTrueOrderByDisplayOrderAscIdAsc();
    List<CookFamilySizeEntity> findAllByOrderByDisplayOrderAscIdAsc();
    boolean existsByFamilySizeAndIdNot(String familySize, Long id);
}

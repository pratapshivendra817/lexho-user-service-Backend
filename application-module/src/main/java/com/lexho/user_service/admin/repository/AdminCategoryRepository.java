package com.lexho.user_service.admin.repository;

import com.lexho.user_service.admin.entity.AdminCategoryEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface AdminCategoryRepository extends JpaRepository<AdminCategoryEntity, Long> {

    @Query("""
            SELECT c FROM AdminCategoryEntity c
            WHERE c.deleted = false
              AND (:search IS NULL OR LOWER(c.name) LIKE LOWER(CONCAT('%', :search, '%')))
            """)
    Page<AdminCategoryEntity> search(@Param("search") String search, Pageable pageable);

    Optional<AdminCategoryEntity> findByIdAndDeletedFalse(Long id);

    boolean existsByNameIgnoreCaseAndDeletedFalse(String name);

    boolean existsByNameIgnoreCaseAndDeletedFalseAndIdNot(String name, Long id);

    Optional<AdminCategoryEntity> findByNameIgnoreCase(String name);
    Optional<AdminCategoryEntity> findFirstByNameContainingIgnoreCaseAndDeletedFalse(String namePart);

    java.util.List<AdminCategoryEntity> findAllByDeletedFalseOrderByCreatedAtDesc(Pageable pageable);

    java.util.List<AdminCategoryEntity> findByActiveTrueAndDeletedFalseOrderByCreatedAtDesc(Pageable pageable);
}

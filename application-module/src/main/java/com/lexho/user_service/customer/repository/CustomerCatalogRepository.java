package com.lexho.user_service.customer.repository;

import com.lexho.user_service.admin.entity.AdminServiceEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CustomerCatalogRepository extends JpaRepository<AdminServiceEntity, Long> {
    Page<AdminServiceEntity> findByCategory_IdAndActiveTrueAndDeletedFalseOrderByNameAscIdAsc(Long categoryId, Pageable pageable);
}

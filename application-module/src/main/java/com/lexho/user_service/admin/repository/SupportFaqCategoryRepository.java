package com.lexho.user_service.admin.repository;

import com.lexho.user_service.admin.entity.SupportFaqCategory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SupportFaqCategoryRepository extends JpaRepository<SupportFaqCategory, Long> {
    Optional<SupportFaqCategory> findByCategoryKey(String categoryKey);

    boolean existsByCategoryKey(String categoryKey);

    List<SupportFaqCategory> findByActiveTrueOrderByDisplayOrderAscIdAsc();
}


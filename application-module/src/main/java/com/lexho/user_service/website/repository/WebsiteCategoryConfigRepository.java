package com.lexho.user_service.website.repository;

import com.lexho.user_service.website.entity.WebsiteCategoryConfig;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface WebsiteCategoryConfigRepository extends JpaRepository<WebsiteCategoryConfig, Long> {
    Optional<WebsiteCategoryConfig> findByCategory_Id(Long categoryId);
    List<WebsiteCategoryConfig> findByShowOnWebsiteTrueAndCategory_ActiveTrueAndCategory_DeletedFalseOrderByDisplayOrderAscCategory_NameAsc();
}

package com.lexho.user_service.website.repository;

import com.lexho.user_service.website.entity.WebsiteCityEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface WebsiteCityRepository extends JpaRepository<WebsiteCityEntity, Long> {
    List<WebsiteCityEntity> findByDeletedFalseOrderByDisplayOrderAscNameAsc();
    List<WebsiteCityEntity> findByActiveTrueAndDeletedFalseOrderByDisplayOrderAscNameAsc();
    Optional<WebsiteCityEntity> findByIdAndDeletedFalse(Long id);
    Optional<WebsiteCityEntity> findBySlugIgnoreCaseAndActiveTrueAndDeletedFalse(String slug);
    boolean existsBySlugIgnoreCaseAndDeletedFalse(String slug);
    boolean existsBySlugIgnoreCaseAndDeletedFalseAndIdNot(String slug, Long id);
}

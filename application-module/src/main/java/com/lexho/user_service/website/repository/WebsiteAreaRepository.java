package com.lexho.user_service.website.repository;

import com.lexho.user_service.website.entity.WebsiteAreaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface WebsiteAreaRepository extends JpaRepository<WebsiteAreaEntity, Long> {
    List<WebsiteAreaEntity> findByCity_IdAndDeletedFalseOrderByDisplayOrderAscAreaNameAsc(Long cityId);
    List<WebsiteAreaEntity> findByCity_IdAndActiveTrueAndDeletedFalseOrderByDisplayOrderAscAreaNameAsc(Long cityId);
    Optional<WebsiteAreaEntity> findByIdAndDeletedFalse(Long id);
    boolean existsByCity_IdAndSlugIgnoreCaseAndDeletedFalse(Long cityId, String slug);
    boolean existsByCity_IdAndSlugIgnoreCaseAndDeletedFalseAndIdNot(Long cityId, String slug, Long id);
}

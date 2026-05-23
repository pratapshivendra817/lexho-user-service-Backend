package com.lexho.user_service.section.repository;

import com.lexho.user_service.section.entity.SectionItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SectionItemRepository extends JpaRepository<SectionItem, Long> {
    List<SectionItem> findBySectionIdOrderByPriorityAscIdAsc(Long sectionId);
    List<SectionItem> findBySectionIdAndActiveTrueOrderByPriorityAscIdAsc(Long sectionId);
}

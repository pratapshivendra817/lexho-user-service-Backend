package com.lexho.user_service.review.repository;

import com.lexho.catalog.entity.ServiceCategory;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ServiceCategoryLookupRepository extends JpaRepository<ServiceCategory, Long> {
}

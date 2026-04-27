package com.lexho.catalog.repository;

import java.util.Optional;
import com.lexho.catalog.entity.ServiceCategory;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CategoryRepository extends JpaRepository<ServiceCategory, Long> {
    Optional<ServiceCategory> findByName(String name);
}

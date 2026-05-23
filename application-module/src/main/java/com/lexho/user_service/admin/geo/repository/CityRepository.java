package com.lexho.user_service.admin.geo.repository;

import com.lexho.user_service.admin.geo.entity.CityEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CityRepository extends JpaRepository<CityEntity, Long> {
    List<CityEntity> findByDeletedFalseOrderByNameAsc();
}

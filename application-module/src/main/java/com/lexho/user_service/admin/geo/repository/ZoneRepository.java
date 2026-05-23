package com.lexho.user_service.admin.geo.repository;

import com.lexho.user_service.admin.geo.entity.ZoneEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ZoneRepository extends JpaRepository<ZoneEntity, Long> {
    List<ZoneEntity> findByDeletedFalseOrderByNameAsc();
    List<ZoneEntity> findByCityIdAndDeletedFalseOrderByNameAsc(Long cityId);
}

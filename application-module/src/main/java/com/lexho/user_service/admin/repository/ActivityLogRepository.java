package com.lexho.user_service.admin.repository;

import com.lexho.user_service.admin.entity.ActivityLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ActivityLogRepository extends JpaRepository<ActivityLog, Long> {

    @Query("""
            SELECT a FROM ActivityLog a
            WHERE (:search IS NULL OR LOWER(a.action) LIKE LOWER(CONCAT('%', :search, '%'))
                   OR LOWER(a.entity) LIKE LOWER(CONCAT('%', :search, '%')))
            """)
    Page<ActivityLog> search(@Param("search") String search, Pageable pageable);
}

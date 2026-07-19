package com.lexho.user_service.audit.repository;

import com.lexho.user_service.audit.entity.AuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;

public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

    @Query("""
            select a from AuditLog a
            where (:startAt is null or a.timestamp >= :startAt)
              and (:endAt is null or a.timestamp <= :endAt)
              and (:action is null or upper(a.action) = upper(:action))
              and (:user is null or upper(coalesce(a.actorIdentifier, '')) like upper(concat('%', :user, '%')))
              and (:role is null or upper(coalesce(a.actorRole, '')) = upper(:role))
            order by a.timestamp desc
            """)
    Page<AuditLog> search(@Param("startAt") LocalDateTime startAt,
                          @Param("endAt") LocalDateTime endAt,
                          @Param("action") String action,
                          @Param("user") String user,
                          @Param("role") String role,
                          Pageable pageable);
}

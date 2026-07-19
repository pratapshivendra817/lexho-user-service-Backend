package com.lexho.user_service.customer.location.repository;

import com.lexho.user_service.customer.location.entity.LocationAuditLog;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LocationAuditLogRepository extends JpaRepository<LocationAuditLog, Long> {
}

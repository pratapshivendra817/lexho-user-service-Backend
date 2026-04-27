package com.lexho.user_service.admin.service;

import com.lexho.user_service.admin.entity.ActivityLog;
import com.lexho.user_service.admin.repository.ActivityLogRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdminAuditService {

    private final ActivityLogRepository activityLogRepository;

    public AdminAuditService(ActivityLogRepository activityLogRepository) {
        this.activityLogRepository = activityLogRepository;
    }

    @Transactional
    public void log(Long adminId, String action, String entity, Long entityId, String metaData) {
        ActivityLog log = new ActivityLog();
        log.setAdminId(adminId == null ? 0L : adminId);
        log.setAction(action);
        log.setEntity(entity);
        log.setEntityId(entityId);
        log.setMetaData(metaData);
        activityLogRepository.save(log);
    }
}

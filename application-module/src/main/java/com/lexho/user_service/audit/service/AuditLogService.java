package com.lexho.user_service.audit.service;

import com.lexho.user_service.audit.entity.AuditLog;
import com.lexho.user_service.audit.repository.AuditLogRepository;
import com.lexho.user_service.security.SecurityUtils;
import com.lexho.user_service.util.StringSanitizers;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import static com.lexho.user_service.config.CorrelationIdFilter.MDC_KEY;
import static com.lexho.user_service.config.CorrelationIdFilter.REQUEST_MDC_KEY;

@Service
public class AuditLogService {

    private final AuditLogRepository auditLogRepository;
    private final SecurityUtils securityUtils;

    public AuditLogService(AuditLogRepository auditLogRepository,
                           SecurityUtils securityUtils) {
        this.auditLogRepository = auditLogRepository;
        this.securityUtils = securityUtils;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void log(Long actorId, String actorRole, String action, String entityType, String entityId,
                    String oldValue, String newValue) {
        saveLog(actorId, actorRole, action, entityType, entityId, oldValue, newValue);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void log(Long userId, Long partnerId, Long bookingId, String action, String oldValue, String newValue) {
        Long actorId = userId != null ? userId : partnerId;
        String actorRole = userId != null ? "CUSTOMER" : partnerId != null ? "PARTNER" : "SYSTEM";
        saveLog(actorId, actorRole, action, "BOOKING", bookingId == null ? null : bookingId.toString(), oldValue,
                newValue);
    }

    private void saveLog(Long actorId, String actorRole, String action, String entityType, String entityId,
                         String oldValue, String newValue) {
        AuditLog log = new AuditLog();
        log.setActorId(actorId);
        log.setActorRole(actorRole);
        log.setActorIdentifier(resolveActorIdentifier(actorId));
        log.setAction(action);
        log.setEntityType(entityType);
        log.setEntityId(entityId);
        log.setOldValue(oldValue);
        log.setNewValue(newValue);
        log.setRequestId(trimToNull(MDC.get(REQUEST_MDC_KEY)));
        log.setCorrelationId(trimToNull(MDC.get(MDC_KEY)));
        auditLogRepository.save(log);
    }

    private String resolveActorIdentifier(Long actorId) {
        String principal = securityUtils.getCurrentPrincipalName().orElse(null);
        if (principal != null && !principal.isBlank()) {
            return principal.trim();
        }
        return actorId == null ? null : String.valueOf(actorId);
    }

    private String trimToNull(String value) {
        return StringSanitizers.trimToNull(value);
    }
}

package com.lexho.user_service.emergency.service;

import com.lexho.user_service.admin.dto.DataResponse;
import com.lexho.user_service.emergency.dto.EmergencyIncidentPageResponse;
import com.lexho.user_service.audit.service.AuditLogService;
import com.lexho.user_service.emergency.dto.EmergencyEventResponse;
import com.lexho.user_service.emergency.entity.EmergencyEvent;
import com.lexho.user_service.emergency.entity.EmergencyEventStatus;
import com.lexho.user_service.emergency.repository.EmergencyEventRepository;
import com.lexho.user_service.exception.BadRequestException;
import com.lexho.user_service.exception.ResourceNotFoundException;
import com.lexho.user_service.security.SecurityUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Locale;

@Service
public class EmergencyAdminService {

    private final EmergencyEventRepository emergencyEventRepository;
    private final EmergencyService emergencyService;
    private final AuditLogService auditLogService;
    private final SecurityUtils securityUtils;

    public EmergencyAdminService(EmergencyEventRepository emergencyEventRepository,
                                 EmergencyService emergencyService,
                                 AuditLogService auditLogService,
                                 SecurityUtils securityUtils) {
        this.emergencyEventRepository = emergencyEventRepository;
        this.emergencyService = emergencyService;
        this.auditLogService = auditLogService;
        this.securityUtils = securityUtils;
    }

    @Transactional(readOnly = true)
    public DataResponse<EmergencyIncidentPageResponse> incidents(int page, int limit, String status, Long partnerId) {
        int normalizedPage = Math.max(page, 1);
        int normalizedLimit = Math.min(Math.max(limit, 1), 100);
        Pageable pageable = PageRequest.of(normalizedPage - 1, normalizedLimit,
                Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<EmergencyEvent> result = emergencyEventRepository.search(parseStatus(status), partnerId, pageable);
        return new DataResponse<>(new EmergencyIncidentPageResponse(
                result.getContent().stream().map(emergencyService::toResponse).toList(),
                normalizedPage,
                normalizedLimit,
                result.getTotalElements(),
                result.getTotalPages()
        ));
    }

    @Transactional(readOnly = true)
    public DataResponse<EmergencyEventResponse> incident(Long eventId) {
        EmergencyEvent event = emergencyEventRepository.findWithLocationsById(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Emergency incident not found"));
        return new DataResponse<>(emergencyService.toResponse(event));
    }

    @Transactional
    public DataResponse<EmergencyEventResponse> acknowledge(Long eventId, String note) {
        EmergencyEvent event = emergencyEventRepository.findWithLocationsById(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Emergency incident not found"));
        if (event.getStatus() == EmergencyEventStatus.RESOLVED) {
            throw new BadRequestException("Resolved emergency incident cannot be acknowledged");
        }
        String actor = currentActor();
        event.setStatus(EmergencyEventStatus.ACKNOWLEDGED);
        event.setAcknowledgementNote(trimToNull(note));
        if (event.getAcknowledgedAt() == null) {
            event.setAcknowledgedAt(LocalDateTime.now());
        }
        event.setAcknowledgedBy(actor);
        EmergencyEvent saved = emergencyEventRepository.save(event);
        audit("SOS_ACKNOWLEDGED", saved, note);
        return new DataResponse<>(emergencyService.toResponse(saved));
    }

    @Transactional
    public DataResponse<EmergencyEventResponse> resolve(Long eventId, String note) {
        EmergencyEvent event = emergencyEventRepository.findWithLocationsById(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Emergency incident not found"));
        String actor = currentActor();
        event.setStatus(EmergencyEventStatus.RESOLVED);
        event.setResolutionNote(trimToNull(note));
        if (event.getResolvedAt() == null) {
            event.setResolvedAt(LocalDateTime.now());
        }
        event.setResolvedBy(actor);
        EmergencyEvent saved = emergencyEventRepository.save(event);
        audit("SOS_RESOLVED", saved, note);
        return new DataResponse<>(emergencyService.toResponse(saved));
    }

    private void audit(String action, EmergencyEvent event, String note) {
        Long adminId = securityUtils.getCurrentUserId().orElse(0L);
        auditLogService.log(adminId, "ADMIN", action, "EMERGENCY_EVENT", event.getId().toString(),
                null, note == null || note.isBlank() ? event.getIncidentCode() : note.trim());
    }

    private String currentActor() {
        return securityUtils.getCurrentPrincipalName()
                .orElseGet(() -> String.valueOf(securityUtils.getCurrentUserId().orElse(0L)));
    }

    private EmergencyEventStatus parseStatus(String status) {
        if (status == null || status.isBlank()) {
            return null;
        }
        try {
            return EmergencyEventStatus.valueOf(status.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            throw new BadRequestException("Invalid emergency status filter");
        }
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        return normalized.isEmpty() ? null : normalized;
    }
}

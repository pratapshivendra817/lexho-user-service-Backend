package com.lexho.user_service.audit.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lexho.user_service.admin.dto.DataResponse;
import com.lexho.user_service.audit.dto.AdminAuditChangeResponse;
import com.lexho.user_service.audit.dto.AdminAuditLogListResponse;
import com.lexho.user_service.audit.dto.AdminAuditLogResponse;
import com.lexho.user_service.audit.entity.AuditLog;
import com.lexho.user_service.audit.repository.AuditLogRepository;
import com.lexho.user_service.exception.BadRequestException;
import com.lexho.user_service.exception.ResourceNotFoundException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class AdminAuditLogQueryService {

    private static final TypeReference<LinkedHashMap<String, Object>> MAP_TYPE = new TypeReference<>() {
    };

    private final AuditLogRepository auditLogRepository;
    private final ObjectMapper objectMapper;

    public AdminAuditLogQueryService(AuditLogRepository auditLogRepository,
                                     ObjectMapper objectMapper) {
        this.auditLogRepository = auditLogRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional(readOnly = true)
    public DataResponse<AdminAuditLogListResponse> list(String startDate,
                                                        String endDate,
                                                        String action,
                                                        String user,
                                                        String role) {
        List<AdminAuditLogResponse> content = auditLogRepository.search(
                        parseStart(startDate),
                        parseEnd(endDate),
                        trimToNull(action),
                        trimToNull(user),
                        trimToNull(role),
                        PageRequest.of(0, 200, Sort.by(Sort.Direction.DESC, "timestamp")))
                .getContent()
                .stream()
                .map(this::toListResponse)
                .toList();
        return new DataResponse<>(new AdminAuditLogListResponse(content));
    }

    @Transactional(readOnly = true)
    public DataResponse<AdminAuditLogResponse> detail(Long id) {
        AuditLog auditLog = auditLogRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Audit log not found"));
        return new DataResponse<>(toDetailResponse(auditLog));
    }

    private AdminAuditLogResponse toListResponse(AuditLog auditLog) {
        return new AdminAuditLogResponse(
                String.valueOf(auditLog.getId()),
                String.valueOf(auditLog.getId()),
                auditLog.getTimestamp(),
                auditLog.getTimestamp(),
                auditLog.getTimestamp(),
                firstNonBlank(auditLog.getActorIdentifier(), stringValue(auditLog.getActorId())),
                auditLog.getActorIdentifier(),
                auditLog.getActorIdentifier(),
                auditLog.getActorRole(),
                auditLog.getAction(),
                auditLog.getEntityType(),
                auditLog.getEntityType(),
                auditLog.getEntityId(),
                auditLog.getEntityId(),
                auditLog.getRequestId(),
                auditLog.getCorrelationId(),
                null,
                null,
                null,
                null
        );
    }

    private AdminAuditLogResponse toDetailResponse(AuditLog auditLog) {
        Map<String, Object> oldValue = parseValue(auditLog.getOldValue());
        Map<String, Object> newValue = parseValue(auditLog.getNewValue());
        List<AdminAuditChangeResponse> changeHistory = buildChangeHistory(auditLog, oldValue, newValue);
        return new AdminAuditLogResponse(
                String.valueOf(auditLog.getId()),
                String.valueOf(auditLog.getId()),
                auditLog.getTimestamp(),
                auditLog.getTimestamp(),
                auditLog.getTimestamp(),
                firstNonBlank(auditLog.getActorIdentifier(), stringValue(auditLog.getActorId())),
                auditLog.getActorIdentifier(),
                auditLog.getActorIdentifier(),
                auditLog.getActorRole(),
                auditLog.getAction(),
                auditLog.getEntityType(),
                auditLog.getEntityType(),
                auditLog.getEntityId(),
                auditLog.getEntityId(),
                auditLog.getRequestId(),
                auditLog.getCorrelationId(),
                oldValue,
                newValue,
                changeHistory,
                changeHistory
        );
    }

    private List<AdminAuditChangeResponse> buildChangeHistory(AuditLog auditLog,
                                                              Map<String, Object> oldValue,
                                                              Map<String, Object> newValue) {
        List<AdminAuditChangeResponse> changes = new ArrayList<>();
        Map<String, Object> left = oldValue == null ? Map.of() : oldValue;
        Map<String, Object> right = newValue == null ? Map.of() : newValue;

        if (!left.isEmpty() || !right.isEmpty()) {
            java.util.LinkedHashSet<String> fields = new java.util.LinkedHashSet<>();
            fields.addAll(left.keySet());
            fields.addAll(right.keySet());
            int index = 0;
            for (String field : fields) {
                changes.add(new AdminAuditChangeResponse(
                        auditLog.getId() + "-" + (++index),
                        field,
                        stringValue(left.get(field)),
                        stringValue(right.get(field)),
                        auditLog.getTimestamp()
                ));
            }
        }

        if (changes.isEmpty()) {
            changes.add(new AdminAuditChangeResponse(
                    auditLog.getId() + "-1",
                    "value",
                    trimToNull(auditLog.getOldValue()),
                    trimToNull(auditLog.getNewValue()),
                    auditLog.getTimestamp()
            ));
        }
        return changes;
    }

    private Map<String, Object> parseValue(String value) {
        String normalized = trimToNull(value);
        if (normalized == null) {
            return null;
        }
        try {
            return objectMapper.readValue(normalized, MAP_TYPE);
        } catch (Exception ignored) {
            LinkedHashMap<String, Object> fallback = new LinkedHashMap<>();
            fallback.put("value", normalized);
            return fallback;
        }
    }

    private LocalDateTime parseStart(String value) {
        String normalized = trimToNull(value);
        if (normalized == null) {
            return null;
        }
        try {
            return LocalDate.parse(normalized).atStartOfDay();
        } catch (DateTimeParseException exception) {
            throw new BadRequestException("Invalid startDate");
        }
    }

    private LocalDateTime parseEnd(String value) {
        String normalized = trimToNull(value);
        if (normalized == null) {
            return null;
        }
        try {
            return LocalDate.parse(normalized).atTime(LocalTime.MAX);
        } catch (DateTimeParseException exception) {
            throw new BadRequestException("Invalid endDate");
        }
    }

    private String firstNonBlank(String first, String second) {
        if (first != null && !first.isBlank()) {
            return first;
        }
        return second == null || second.isBlank() ? null : second;
    }

    private String stringValue(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        return normalized.isEmpty() ? null : normalized;
    }
}

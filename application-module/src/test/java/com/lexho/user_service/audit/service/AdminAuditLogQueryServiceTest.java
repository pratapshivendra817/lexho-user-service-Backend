package com.lexho.user_service.audit.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lexho.user_service.audit.entity.AuditLog;
import com.lexho.user_service.audit.repository.AuditLogRepository;
import com.lexho.user_service.exception.BadRequestException;
import com.lexho.user_service.exception.ResourceNotFoundException;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AdminAuditLogQueryServiceTest {

    private final AuditLogRepository repository = mock(AuditLogRepository.class);
    private final AdminAuditLogQueryService service = new AdminAuditLogQueryService(repository, new ObjectMapper());

    @Test
    void listReturnsFilteredContentShape() {
        AuditLog log = auditLog(5L);
        when(repository.search(eq(LocalDateTime.of(2026, 7, 1, 0, 0)),
                eq(LocalDateTime.of(2026, 7, 16, 23, 59, 59, 999999999)),
                eq("LOGIN"),
                eq("admin@lexho.com"),
                eq("ADMIN"),
                any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(log)));

        var response = service.list("2026-07-01", "2026-07-16", "LOGIN", "admin@lexho.com", "ADMIN");

        assertThat(response.getData().content()).hasSize(1);
        assertThat(response.getData().content().get(0).user()).isEqualTo("admin@lexho.com");
        assertThat(response.getData().content().get(0).requestId()).isEqualTo("req-1");
    }

    @Test
    void detailReturnsChangeHistoryAnd404ForMissingRecord() {
        AuditLog log = auditLog(9L);
        when(repository.findById(9L)).thenReturn(Optional.of(log));

        var response = service.detail(9L);

        assertThat(response.getData().oldValue()).containsEntry("status", "CREATED");
        assertThat(response.getData().newValue()).containsEntry("status", "UPDATED");
        assertThat(response.getData().changeHistory()).isNotEmpty();
        assertThat(response.getData().changes()).isNotEmpty();

        when(repository.findById(10L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.detail(10L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Audit log not found");
    }

    @Test
    void invalidDateIsRejected() {
        assertThatThrownBy(() -> service.list("2026/07/01", null, null, null, null))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("startDate");
    }

    private AuditLog auditLog(Long id) {
        AuditLog log = new AuditLog();
        ReflectionTestUtils.setField(log, "id", id);
        ReflectionTestUtils.setField(log, "actorId", 2L);
        ReflectionTestUtils.setField(log, "actorRole", "ADMIN");
        ReflectionTestUtils.setField(log, "actorIdentifier", "admin@lexho.com");
        log.setAction("LOGIN");
        log.setEntityType("AUTH");
        log.setEntityId("session-1");
        log.setOldValue("{\"status\":\"CREATED\"}");
        log.setNewValue("{\"status\":\"UPDATED\"}");
        log.setRequestId("req-1");
        log.setCorrelationId("corr-1");
        ReflectionTestUtils.setField(log, "timestamp", LocalDateTime.of(2026, 7, 16, 12, 30));
        return log;
    }
}

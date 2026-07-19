package com.lexho.user_service.audit.service;

import com.lexho.user_service.audit.entity.AuditLog;
import com.lexho.user_service.audit.repository.AuditLogRepository;
import com.lexho.user_service.security.SecurityUtils;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class AuditLogServiceTest {

    private final AuditLogRepository repository = mock(AuditLogRepository.class);
    private final SecurityUtils securityUtils = mock(SecurityUtils.class);
    private final AuditLogService service = new AuditLogService(repository, securityUtils);

    @Test
    void logsExplicitAndDerivedBookingActors() {
        service.log(4L, "ADMIN", "UPDATE", "BOOKING", "9", "old", "new");
        service.log(null, 7L, 9L, "ASSIGNED", null, "partner");
        service.log(null, null, null, "SYSTEM_EVENT", null, null);

        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(repository, org.mockito.Mockito.times(3)).save(captor.capture());
        assertThat(ReflectionTestUtils.getField(captor.getAllValues().get(0), "actorRole")).isEqualTo("ADMIN");
        assertThat(ReflectionTestUtils.getField(captor.getAllValues().get(1), "actorRole")).isEqualTo("PARTNER");
        assertThat(ReflectionTestUtils.getField(captor.getAllValues().get(1), "entityId")).isEqualTo("9");
        assertThat(ReflectionTestUtils.getField(captor.getAllValues().get(2), "actorRole")).isEqualTo("SYSTEM");
        assertThat(ReflectionTestUtils.getField(captor.getAllValues().get(0), "actorIdentifier")).isEqualTo("4");
    }
}

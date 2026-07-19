package com.lexho.user_service.emergency.service;

import com.lexho.user_service.audit.service.AuditLogService;
import com.lexho.user_service.emergency.entity.EmergencyEvent;
import com.lexho.user_service.emergency.entity.EmergencyEventStatus;
import com.lexho.user_service.emergency.repository.EmergencyEventRepository;
import com.lexho.user_service.exception.BadRequestException;
import com.lexho.user_service.security.SecurityUtils;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class EmergencyAdminServiceTest {

    private final EmergencyEventRepository eventRepository = mock(EmergencyEventRepository.class);
    private final EmergencyService emergencyService = mock(EmergencyService.class);
    private final AuditLogService auditLogService = mock(AuditLogService.class);
    private final SecurityUtils securityUtils = new SecurityUtils();
    private final EmergencyAdminService service = new EmergencyAdminService(
            eventRepository,
            emergencyService,
            auditLogService,
            securityUtils
    );

    @Test
    void incidentsDelegatesToRepositoryAndMapsResponses() {
        EmergencyEvent event = event(1L, EmergencyEventStatus.TRIGGERED);
        when(eventRepository.search(eq(EmergencyEventStatus.TRIGGERED), eq(7L), any(PageRequest.class)))
                .thenReturn(new PageImpl<>(List.of(event)));
        when(emergencyService.toResponse(event)).thenReturn(mock(com.lexho.user_service.emergency.dto.EmergencyEventResponse.class));

        var response = service.incidents(1, 20, "TRIGGERED", 7L);

        assertThat(response.getData().totalElements()).isEqualTo(1);
        assertThat(response.getData().content()).hasSize(1);
        assertThat(response.getData().page()).isEqualTo(1);
        assertThat(response.getData().limit()).isEqualTo(20);
    }

    @Test
    void acknowledgeAndResolveUpdateStatusAndAudit() {
        setAdminAuthentication(99L);
        EmergencyEvent event = event(2L, EmergencyEventStatus.TRIGGERED);
        when(eventRepository.findWithLocationsById(2L)).thenReturn(Optional.of(event));
        when(eventRepository.save(any(EmergencyEvent.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(emergencyService.toResponse(any(EmergencyEvent.class)))
                .thenReturn(mock(com.lexho.user_service.emergency.dto.EmergencyEventResponse.class));

        service.acknowledge(2L, "seen");
        assertThat(event.getStatus()).isEqualTo(EmergencyEventStatus.ACKNOWLEDGED);
        assertThat(event.getAcknowledgementNote()).isEqualTo("seen");
        assertThat(event.getAcknowledgedAt()).isNotNull();
        assertThat(event.getAcknowledgedBy()).isEqualTo("admin");
        verify(auditLogService).log(99L, "ADMIN", "SOS_ACKNOWLEDGED", "EMERGENCY_EVENT", "2", null, "seen");

        service.resolve(2L, "closed");
        assertThat(event.getStatus()).isEqualTo(EmergencyEventStatus.RESOLVED);
        assertThat(event.getResolutionNote()).isEqualTo("closed");
        assertThat(event.getResolvedAt()).isNotNull();
        assertThat(event.getResolvedBy()).isEqualTo("admin");
        verify(auditLogService).log(99L, "ADMIN", "SOS_RESOLVED", "EMERGENCY_EVENT", "2", null, "closed");

        SecurityContextHolder.clearContext();
    }

    @Test
    void acknowledgeRejectsResolvedIncidentAndBadStatusFilter() {
        EmergencyEvent resolved = event(3L, EmergencyEventStatus.RESOLVED);
        when(eventRepository.findWithLocationsById(3L)).thenReturn(Optional.of(resolved));

        assertThatThrownBy(() -> service.acknowledge(3L, null))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Resolved");

        assertThatThrownBy(() -> service.incidents(1, 20, "UNKNOWN", null))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("status filter");
    }

    private void setAdminAuthentication(Long adminId) {
        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                "admin",
                null,
                List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))
        );
        authentication.setDetails(adminId);
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    private EmergencyEvent event(Long id, EmergencyEventStatus status) {
        EmergencyEvent event = new EmergencyEvent();
        ReflectionTestUtils.setField(event, "id", id);
        event.setIncidentCode("SOS-" + id);
        event.setPartnerId(7L);
        event.setPartnerName("Ravi");
        event.setPartnerMobileNumber("9999999999");
        event.setStatus(status);
        return event;
    }
}

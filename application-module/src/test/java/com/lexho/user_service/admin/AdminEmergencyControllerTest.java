package com.lexho.user_service.admin;

import com.lexho.user_service.admin.controller.AdminEmergencyController;
import com.lexho.user_service.emergency.dto.EmergencyAdminActionRequest;
import com.lexho.user_service.emergency.service.EmergencyAdminService;
import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class AdminEmergencyControllerTest {

    private final EmergencyAdminService emergencyAdminService = mock(EmergencyAdminService.class);
    private final AdminEmergencyController controller = new AdminEmergencyController(emergencyAdminService);

    @Test
    void delegatesIncidentEndpoints() {
        controller.incidents(2, 25, "TRIGGERED", 8L);
        controller.incident(11L);
        controller.acknowledge(11L, new EmergencyAdminActionRequest("seen"));
        controller.resolve(11L, new EmergencyAdminActionRequest("closed"));

        verify(emergencyAdminService).incidents(2, 25, "TRIGGERED", 8L);
        verify(emergencyAdminService).incident(11L);
        verify(emergencyAdminService).acknowledge(11L, "seen");
        verify(emergencyAdminService).resolve(11L, "closed");
    }
}

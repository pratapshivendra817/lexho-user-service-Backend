package com.lexho.user_service.emergency.controller;

import com.lexho.user_service.emergency.dto.EmergencyContactRequest;
import com.lexho.user_service.emergency.dto.EmergencySosRequest;
import com.lexho.user_service.emergency.service.EmergencyService;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class EmergencyControllerTest {

    private final EmergencyService emergencyService = mock(EmergencyService.class);
    private final EmergencyController controller = new EmergencyController(emergencyService);

    @Test
    void delegatesSosHistoryAndContactEndpoints() {
        Long partnerId = 7L;
        EmergencySosRequest sosRequest = new EmergencySosRequest(
                BigDecimal.valueOf(28.6139),
                BigDecimal.valueOf(77.2090),
                BigDecimal.valueOf(5.2),
                "Connaught Place",
                "Need help"
        );
        EmergencyContactRequest contactRequest = new EmergencyContactRequest(
                "Amit",
                "9999999999",
                "Brother",
                "amit@example.com",
                true
        );

        assertThat(controller.triggerSos(partnerId, sosRequest).isSuccess()).isTrue();
        assertThat(controller.history(partnerId).isSuccess()).isTrue();
        assertThat(controller.addContact(partnerId, contactRequest).isSuccess()).isTrue();

        verify(emergencyService).triggerSos(partnerId, sosRequest);
        verify(emergencyService).history(partnerId);
        verify(emergencyService).addContact(partnerId, contactRequest);
    }
}

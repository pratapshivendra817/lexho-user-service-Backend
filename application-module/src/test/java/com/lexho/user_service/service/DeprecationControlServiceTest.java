package com.lexho.user_service.service;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import static org.junit.jupiter.api.Assertions.*;

class DeprecationControlServiceTest {
    @Test
    void enabledFlagsAllowLegacyApis() {
        DeprecationControlService service = new DeprecationControlService(true, true, true);
        assertDoesNotThrow(service::ensureLegacyCustomerLocationEnabled);
        assertDoesNotThrow(service::ensureLegacyPartnerShiftEnabled);
        assertDoesNotThrow(service::ensureLegacyAddressModuleEnabled);
    }

    @Test
    void disabledFlagsReturnGone() {
        DeprecationControlService service = new DeprecationControlService(false, false, false);
        assertGone(service::ensureLegacyCustomerLocationEnabled);
        assertGone(service::ensureLegacyPartnerShiftEnabled);
        assertGone(service::ensureLegacyAddressModuleEnabled);
    }

    private void assertGone(Runnable action) {
        ResponseStatusException ex = assertThrows(ResponseStatusException.class, action::run);
        assertEquals(HttpStatus.GONE, ex.getStatusCode());
    }
}

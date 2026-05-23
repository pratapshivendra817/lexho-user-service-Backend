package com.lexho.user_service.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class DeprecationControlService {

    private final boolean legacyCustomerLocationEnabled;
    private final boolean legacyPartnerShiftEnabled;
    private final boolean legacyAddressModuleEnabled;

    public DeprecationControlService(
            @Value("${app.api.legacy.customer-location.enabled:true}") boolean legacyCustomerLocationEnabled,
            @Value("${app.api.legacy.partner-shift.enabled:true}") boolean legacyPartnerShiftEnabled,
            @Value("${app.api.legacy.address-module.enabled:true}") boolean legacyAddressModuleEnabled) {
        this.legacyCustomerLocationEnabled = legacyCustomerLocationEnabled;
        this.legacyPartnerShiftEnabled = legacyPartnerShiftEnabled;
        this.legacyAddressModuleEnabled = legacyAddressModuleEnabled;
    }

    public void ensureLegacyCustomerLocationEnabled() {
        if (!legacyCustomerLocationEnabled) {
            throw new ResponseStatusException(HttpStatus.GONE, "Deprecated API removed. Use /api/customer/location/current or /api/customer/location/all");
        }
    }

    public void ensureLegacyPartnerShiftEnabled() {
        if (!legacyPartnerShiftEnabled) {
            throw new ResponseStatusException(HttpStatus.GONE, "Deprecated API removed. Use /api/partner/dispatch/shift/start, /shift/end, /availability");
        }
    }

    public void ensureLegacyAddressModuleEnabled() {
        if (!legacyAddressModuleEnabled) {
            throw new ResponseStatusException(HttpStatus.GONE, "Deprecated API removed. Use /api/customer/location/**");
        }
    }
}


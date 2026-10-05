package com.lexho.user_service.lexhopartner.service;

import com.lexho.user_service.lexhopartner.model.PartnerOperationalStatus;
import com.lexho.user_service.exception.BadRequestException;
import com.lexho.user_service.lexhopartner.entity.PartnerOperationalProfile;
import org.springframework.stereotype.Service;

@Service
public class PartnerOperationalStatusTransitionService {

    public void setAvailability(PartnerOperationalProfile profile, boolean online) {
        if (online && profile.getPartnerOperationalStatus() != null
                && (profile.getPartnerOperationalStatus() == PartnerOperationalStatus.BLOCKED
                || profile.getPartnerOperationalStatus() == PartnerOperationalStatus.REJECTED)) {
            throw new BadRequestException("Blocked or rejected partners cannot be activated");
        }
        profile.setOnline(online);
    }
}

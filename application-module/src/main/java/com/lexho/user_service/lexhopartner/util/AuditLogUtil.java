package com.lexho.user_service.lexhopartner.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class AuditLogUtil {

    private static final Logger log = LoggerFactory.getLogger("AUDIT_LOGGER");

    public void audit(Long partnerId, String action, String details) {
        log.info("partnerId={} action={} details={}", partnerId, action, details);
    }
}

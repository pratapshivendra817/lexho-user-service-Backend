package com.lexho.user_service.partnerrequest.scheduler;

import com.lexho.user_service.partnerrequest.service.PartnerRequestService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class PartnerRequestExpiryScheduler {

    private static final Logger log = LoggerFactory.getLogger(PartnerRequestExpiryScheduler.class);

    private final PartnerRequestService partnerRequestService;

    public PartnerRequestExpiryScheduler(PartnerRequestService partnerRequestService) {
        this.partnerRequestService = partnerRequestService;
    }

    @Scheduled(fixedDelayString = "${app.partner.requests.expiry-scan-ms:30000}")
    public void expirePendingRequests() {
        int expired = partnerRequestService.expirePendingRequests();
        if (expired > 0) {
            log.info("Expired {} partner requests", expired);
        }
    }
}

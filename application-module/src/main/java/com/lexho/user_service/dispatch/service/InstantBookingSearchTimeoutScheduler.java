package com.lexho.user_service.dispatch.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class InstantBookingSearchTimeoutScheduler {

    private static final Logger LOG = LoggerFactory.getLogger(InstantBookingSearchTimeoutScheduler.class);

    private final DispatchOrchestrationService dispatchOrchestrationService;
    private final long searchTimeoutSeconds;

    public InstantBookingSearchTimeoutScheduler(DispatchOrchestrationService dispatchOrchestrationService,
                                                @Value("${instant.booking.search-timeout-seconds:180}") long searchTimeoutSeconds) {
        this.dispatchOrchestrationService = dispatchOrchestrationService;
        this.searchTimeoutSeconds = Math.max(30L, searchTimeoutSeconds);
    }

    @Scheduled(fixedDelayString = "${instant.booking.search-timeout-scan-ms:60000}")
    public void expireStuckSearches() {
        LocalDateTime cutoff = LocalDateTime.now().minusSeconds(searchTimeoutSeconds);
        int expired = dispatchOrchestrationService.expireStuckInstantSearches(cutoff);
        if (expired > 0) {
            LOG.info("Expired {} instant searches olderThan={}", expired, cutoff);
        }
    }
}

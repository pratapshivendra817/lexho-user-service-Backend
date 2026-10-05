package com.lexho.user_service.booking.idempotency;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class BookingIdempotencyCleanupScheduler {
    private static final Logger LOG = LoggerFactory.getLogger(BookingIdempotencyCleanupScheduler.class);

    private final BookingIdempotencyService service;

    public BookingIdempotencyCleanupScheduler(BookingIdempotencyService service) {
        this.service = service;
    }

    @Scheduled(cron = "${app.booking.idempotency.cleanup-cron:0 20 3 * * *}")
    public void cleanup() {
        int deleted = service.cleanupExpiredRecords();
        if (deleted > 0) {
            LOG.info("Deleted expired booking idempotency records count={}", deleted);
        }
    }
}

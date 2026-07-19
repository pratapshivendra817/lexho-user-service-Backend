package com.lexho.user_service.dispatch.scheduler;

import com.lexho.user_service.dispatch.entity.CurrentPartnerLocationEntity;
import com.lexho.user_service.dispatch.entity.PartnerShiftEntity;
import com.lexho.user_service.dispatch.repository.CurrentPartnerLocationRepository;
import com.lexho.user_service.dispatch.repository.PartnerShiftRepository;
import com.lexho.user_service.dispatch.service.PartnerGeoIndexService;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Component
public class DispatchCleanupScheduler {
    private static final Logger log = LoggerFactory.getLogger(DispatchCleanupScheduler.class);

    private final PartnerShiftRepository partnerShiftRepository;
    private final CurrentPartnerLocationRepository currentPartnerLocationRepository;
    private final PartnerGeoIndexService partnerGeoIndexService;
    private final long partnerOfflineTimeoutSeconds;
    private final long staleLocationTimeoutMinutes;
    private final Counter staleCleanupCounter;

    public DispatchCleanupScheduler(PartnerShiftRepository partnerShiftRepository,
                                    CurrentPartnerLocationRepository currentPartnerLocationRepository,
                                    PartnerGeoIndexService partnerGeoIndexService,
                                    MeterRegistry meterRegistry,
                                    @Value("${app.dispatch.partner-offline-timeout-seconds:120}") long partnerOfflineTimeoutSeconds,
                                    @Value("${app.dispatch.location-stale-timeout-minutes:30}") long staleLocationTimeoutMinutes) {
        this.partnerShiftRepository = partnerShiftRepository;
        this.currentPartnerLocationRepository = currentPartnerLocationRepository;
        this.partnerGeoIndexService = partnerGeoIndexService;
        this.partnerOfflineTimeoutSeconds = partnerOfflineTimeoutSeconds;
        this.staleLocationTimeoutMinutes = staleLocationTimeoutMinutes;
        this.staleCleanupCounter = Counter.builder("dispatch.geo.stale_cleanup.count").register(meterRegistry);
    }

    @Scheduled(fixedDelayString = "${app.dispatch.cleanup-interval-ms:60000}")
    @Transactional
    public void cleanupStaleDispatchState() {
        markInactivePartnersOffline();
        cleanupStaleGpsRecords();
    }

    private void markInactivePartnersOffline() {
        LocalDateTime cutoff = LocalDateTime.now().minusSeconds(partnerOfflineTimeoutSeconds);
        List<PartnerShiftEntity> staleOnlineShifts = partnerShiftRepository
                .findByOnlineTrueAndDeletedFalseAndUpdatedAtBefore(cutoff);
        for (PartnerShiftEntity shift : staleOnlineShifts) {
            shift.setOnline(false);
            shift.setUpdatedBy("SCHEDULER");
        }
        List<PartnerShiftEntity> endedShifts = partnerShiftRepository
                .findByOnlineTrueAndDeletedFalseAndShiftEndBefore(LocalDateTime.now());
        for (PartnerShiftEntity shift : endedShifts) {
            shift.setOnline(false);
            shift.setUpdatedBy("SCHEDULER_SHIFT_ENDED");
        }
        if (!staleOnlineShifts.isEmpty() || !endedShifts.isEmpty()) {
            java.util.ArrayList<PartnerShiftEntity> changed = new java.util.ArrayList<>();
            changed.addAll(staleOnlineShifts);
            changed.addAll(endedShifts);
            partnerShiftRepository.saveAll(changed);
        }
    }

    private void cleanupStaleGpsRecords() {
        LocalDateTime cutoff = LocalDateTime.now().minusMinutes(staleLocationTimeoutMinutes);
        List<CurrentPartnerLocationEntity> staleLocations = currentPartnerLocationRepository
                .findByDeletedFalseAndUpdatedAtBefore(cutoff);
        for (CurrentPartnerLocationEntity location : staleLocations) {
            location.setDeleted(true);
            location.setUpdatedBy("SCHEDULER");
        }
        if (!staleLocations.isEmpty()) {
            staleLocations.forEach(location -> partnerGeoIndexService.removePartner(location.getPartnerId()));
            currentPartnerLocationRepository.saveAll(staleLocations);
            staleCleanupCounter.increment(staleLocations.size());
            log.info("dispatch stale cleanup: removed={} cutoff={}", staleLocations.size(), cutoff);
        }
    }
}

package com.lexho.user_service.dispatch.scheduler;

import com.lexho.user_service.dispatch.entity.CurrentPartnerLocationEntity;
import com.lexho.user_service.dispatch.entity.PartnerShiftEntity;
import com.lexho.user_service.dispatch.repository.CurrentPartnerLocationRepository;
import com.lexho.user_service.dispatch.repository.PartnerShiftRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Component
public class DispatchCleanupScheduler {

    private final PartnerShiftRepository partnerShiftRepository;
    private final CurrentPartnerLocationRepository currentPartnerLocationRepository;
    private final long partnerOfflineTimeoutSeconds;
    private final long staleLocationTimeoutMinutes;

    public DispatchCleanupScheduler(PartnerShiftRepository partnerShiftRepository,
                                    CurrentPartnerLocationRepository currentPartnerLocationRepository,
                                    @Value("${app.dispatch.partner-offline-timeout-seconds:120}") long partnerOfflineTimeoutSeconds,
                                    @Value("${app.dispatch.location-stale-timeout-minutes:30}") long staleLocationTimeoutMinutes) {
        this.partnerShiftRepository = partnerShiftRepository;
        this.currentPartnerLocationRepository = currentPartnerLocationRepository;
        this.partnerOfflineTimeoutSeconds = partnerOfflineTimeoutSeconds;
        this.staleLocationTimeoutMinutes = staleLocationTimeoutMinutes;
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
        if (!staleOnlineShifts.isEmpty()) {
            partnerShiftRepository.saveAll(staleOnlineShifts);
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
            currentPartnerLocationRepository.saveAll(staleLocations);
        }
    }
}


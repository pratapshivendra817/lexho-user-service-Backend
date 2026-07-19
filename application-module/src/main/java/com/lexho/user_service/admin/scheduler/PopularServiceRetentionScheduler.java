package com.lexho.user_service.admin.scheduler;

import com.lexho.user_service.admin.repository.PopularServiceRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Component
public class PopularServiceRetentionScheduler {

    private static final Logger log = LoggerFactory.getLogger(PopularServiceRetentionScheduler.class);

    private final PopularServiceRepository popularServiceRepository;
    private final int retentionDays;

    public PopularServiceRetentionScheduler(
            PopularServiceRepository popularServiceRepository,
            @Value("${app.popular-services.purge.retention-days:180}") int retentionDays) {
        this.popularServiceRepository = popularServiceRepository;
        this.retentionDays = retentionDays;
    }

    @Scheduled(cron = "${app.popular-services.purge.cron:0 30 2 * * *}")
    @Transactional
    public void purgeSoftDeletedRecords() {
        LocalDateTime cutoff = LocalDateTime.now().minusDays(retentionDays);
        long purgeCandidates = popularServiceRepository.countByActiveFalseAndDeletedAtBefore(cutoff);
        if (purgeCandidates == 0) {
            return;
        }

        int deleted = popularServiceRepository.purgeInactiveOlderThan(cutoff);
        log.info("Popular services purge completed: retentionDays={}, cutoff={}, candidates={}, deleted={}",
                retentionDays, cutoff, purgeCandidates, deleted);
    }
}

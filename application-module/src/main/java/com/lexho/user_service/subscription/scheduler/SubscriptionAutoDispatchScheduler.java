package com.lexho.user_service.subscription.scheduler;

import com.lexho.user_service.subscription.service.SubscriptionAutoDispatchService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class SubscriptionAutoDispatchScheduler {
    private final SubscriptionAutoDispatchService subscriptionAutoDispatchService;

    public SubscriptionAutoDispatchScheduler(SubscriptionAutoDispatchService subscriptionAutoDispatchService) {
        this.subscriptionAutoDispatchService = subscriptionAutoDispatchService;
    }

    @Scheduled(fixedDelayString = "${app.subscription.auto-dispatch.scan-ms:60000}")
    public void autoAssignDueVisits() {
        subscriptionAutoDispatchService.autoAssignDueVisits();
    }
}

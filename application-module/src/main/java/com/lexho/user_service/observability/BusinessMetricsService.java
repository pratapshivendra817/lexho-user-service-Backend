package com.lexho.user_service.observability;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

@Component
public class BusinessMetricsService {

    private final Counter ordersCreated;
    private final Counter ordersCompleted;
    private final Counter partnerAcceptance;

    public BusinessMetricsService(MeterRegistry meterRegistry) {
        this.ordersCreated = Counter.builder("orders_created")
                .description("Orders created")
                .register(meterRegistry);
        this.ordersCompleted = Counter.builder("orders_completed")
                .description("Orders completed")
                .register(meterRegistry);
        this.partnerAcceptance = Counter.builder("partner_acceptance")
                .description("Partner order acceptances")
                .register(meterRegistry);
    }

    public void incrementOrdersCreated() {
        ordersCreated.increment();
    }

    public void incrementOrdersCompleted() {
        ordersCompleted.increment();
    }

    public void incrementPartnerAcceptance() {
        partnerAcceptance.increment();
    }
}

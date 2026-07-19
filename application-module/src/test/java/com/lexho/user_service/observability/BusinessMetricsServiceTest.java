package com.lexho.user_service.observability;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class BusinessMetricsServiceTest {

    @Test
    void incrementMethodsShouldUpdateRegisteredCounters() {
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        BusinessMetricsService service = new BusinessMetricsService(registry);

        service.incrementOrdersCreated();
        service.incrementOrdersCompleted();
        service.incrementPartnerAcceptance();
        service.incrementPartnerAcceptance();

        assertEquals(1.0, registry.counter("orders_created").count());
        assertEquals(1.0, registry.counter("orders_completed").count());
        assertEquals(2.0, registry.counter("partner_acceptance").count());
    }
}

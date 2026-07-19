package com.lexho.user_service.admin.service;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Service;

import java.lang.management.ManagementFactory;
import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class AdminMonitoringService {

    private final AdminDashboardService adminDashboardService;
    private final MeterRegistry meterRegistry;

    public AdminMonitoringService(AdminDashboardService adminDashboardService,
                                  MeterRegistry meterRegistry) {
        this.adminDashboardService = adminDashboardService;
        this.meterRegistry = meterRegistry;
    }

    public Map<String, Object> dashboard() {
        Map<String, Object> dashboard = new LinkedHashMap<>(adminDashboardService.dashboardStats());
        dashboard.put("status", "UP");
        dashboard.put("timestamp", OffsetDateTime.now().toString());
        dashboard.put("uptimeSeconds", ManagementFactory.getRuntimeMXBean().getUptime() / 1000L);
        dashboard.put("apiResponseTimeMs", timerMeanMillis("http.server.requests"));
        dashboard.put("apiRequestCount", timerCount("http.server.requests"));
        dashboard.put("apiErrorCount", counterValue("http.server.requests.errors"));
        dashboard.put("paymentFailureCount", counterValue("payment.failures"));
        dashboard.put("bookingCreationCount", counterValue("booking.created"));
        dashboard.put("partnerAssignmentCount", counterValue("booking.partner.assigned"));
        dashboard.put("webhookFailureCount", counterValue("webhook.failures"));
        dashboard.put("webhookDeadLetterCount", counterValue("webhook.dead_letter"));
        dashboard.put("rateLimitRejectionCount", counterValue("rate_limit.rejections"));
        return dashboard;
    }

    private double timerMeanMillis(String name) {
        Timer timer = meterRegistry.find(name).timer();
        return timer == null ? 0d : timer.mean(java.util.concurrent.TimeUnit.MILLISECONDS);
    }

    private long timerCount(String name) {
        Timer timer = meterRegistry.find(name).timer();
        return timer == null ? 0L : timer.count();
    }

    private double counterValue(String name) {
        Counter counter = meterRegistry.find(name).counter();
        return counter == null ? 0d : counter.count();
    }
}

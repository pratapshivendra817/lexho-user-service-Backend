package com.lexho.user_service.localization.dynamic;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

@Component
public class DynamicTranslationMetrics {

    private final Counter providerRequests;
    private final Counter providerFailures;
    private final Counter cacheHits;
    private final Counter cacheMisses;
    private final Counter dbHits;
    private final Counter generationCount;

    public DynamicTranslationMetrics(MeterRegistry meterRegistry) {
        providerRequests = Counter.builder("translation_provider_requests").register(meterRegistry);
        providerFailures = Counter.builder("translation_provider_failures").register(meterRegistry);
        cacheHits = Counter.builder("translation_cache_hits").register(meterRegistry);
        cacheMisses = Counter.builder("translation_cache_misses").register(meterRegistry);
        dbHits = Counter.builder("translation_db_hits").register(meterRegistry);
        generationCount = Counter.builder("translation_generation_count").register(meterRegistry);
    }

    public void providerRequest() { providerRequests.increment(); }
    public void providerFailure() { providerFailures.increment(); }
    public void cacheHit() { cacheHits.increment(); }
    public void cacheMiss() { cacheMisses.increment(); }
    public void dbHit() { dbHits.increment(); }
    public void generated() { generationCount.increment(); }
}

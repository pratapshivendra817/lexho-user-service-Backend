package com.lexho.user_service.dispatch.service;

import com.lexho.common.enums.OrderStatus;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TrackingLifecycleServiceTest {

    private final StringRedisTemplate redis = mock(StringRedisTemplate.class);
    private final ValueOperations<String, String> values = mock(ValueOperations.class);
    private final TrackingLifecycleService service =
            new TrackingLifecycleService(redis, 60, 7, 30_000, 20_000, 5_000, 3_000, 2_000, 2_000, 30_000, 15_000);

    @Test
    void resolvesArrivalTerminalStatesAndIntervals() {
        assertThat(service.resolveState(TrackingLifecycleState.EN_ROUTE, 28.6139, 77.2090, 28.6139, 77.2090))
                .isEqualTo(TrackingLifecycleState.ARRIVED);
        assertThat(service.resolveState(TrackingLifecycleState.EN_ROUTE, null, 77.2, 28.6, 77.2))
                .isEqualTo(TrackingLifecycleState.EN_ROUTE);
        assertThat(service.isTerminal(TrackingLifecycleState.JOB_COMPLETED)).isTrue();
        assertThat(service.isTerminal(TrackingLifecycleState.ASSIGNED)).isFalse();
        assertThat(service.ingestMinIntervalMs(TrackingLifecycleState.EN_ROUTE)).isEqualTo(5_000);
        assertThat(service.ingestMinIntervalMs(TrackingLifecycleState.ARRIVED)).isEqualTo(2_000);
        assertThat(service.ingestMinIntervalMs(TrackingLifecycleState.JOB_STARTED)).isEqualTo(30_000);
        assertThat(service.ingestMinIntervalMs(TrackingLifecycleState.ASSIGNED)).isEqualTo(30_000);
        assertThat(service.ingestMinIntervalMs(TrackingLifecycleState.OFFLINE)).isEqualTo(60_000);
        assertThat(service.sseIntervalMs(TrackingLifecycleState.EN_ROUTE)).isEqualTo(3_000);
        assertThat(service.sseIntervalMs(TrackingLifecycleState.ARRIVED)).isEqualTo(2_000);
        assertThat(service.sseIntervalMs(TrackingLifecycleState.JOB_IN_PROGRESS)).isEqualTo(15_000);
        assertThat(service.sseIntervalMs(TrackingLifecycleState.ASSIGNED)).isEqualTo(20_000);
        assertThat(service.sseIntervalMs(TrackingLifecycleState.CANCELLED)).isEqualTo(30_000);
    }

    @Test
    void persistsFetchesAndDerivesStates() {
        when(redis.opsForValue()).thenReturn(values);
        when(values.get("tracking:state:7")).thenReturn("EN_ROUTE:99", " ", "bad");

        service.persistState(7L, TrackingLifecycleState.EN_ROUTE, 99L);

        verify(values).set("tracking:state:7", "EN_ROUTE:99", Duration.ofHours(6));
        assertThat(service.fetchState(7L)).isEqualTo(TrackingLifecycleState.EN_ROUTE);
        assertThat(service.fetchState(7L)).isEqualTo(TrackingLifecycleState.OFFLINE);
        assertThat(service.fetchState(7L)).isEqualTo(TrackingLifecycleState.OFFLINE);
        assertThat(service.fetchState(null)).isEqualTo(TrackingLifecycleState.OFFLINE);
        assertThat(service.deriveFromOrder("scheduled", OrderStatus.SCHEDULED, TrackingLifecycleState.OFFLINE))
                .isEqualTo(TrackingLifecycleState.ASSIGNED);
        assertThat(service.deriveFromOrder("invalid", OrderStatus.SCHEDULED, TrackingLifecycleState.OFFLINE))
                .isEqualTo(TrackingLifecycleState.OFFLINE);
        assertThat(service.deriveFromOrder(null, OrderStatus.SERVICE_COMPLETED, TrackingLifecycleState.OFFLINE))
                .isEqualTo(TrackingLifecycleState.JOB_COMPLETED);
        assertThat(service.deriveFromOrder(null, null, TrackingLifecycleState.ASSIGNED))
                .isEqualTo(TrackingLifecycleState.ASSIGNED);
    }
}

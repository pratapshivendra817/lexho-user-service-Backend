package com.lexho.user.service;

import com.lexho.user.exception.TooManyRequestsException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.scheduling.TaskScheduler;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RateLimiterAndTimeoutServiceTest {

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @Mock
    private TaskScheduler taskScheduler;

    @Mock
    private ScheduledFuture<Object> scheduledFuture;

    @Test
    void checkAllowsFirstRedisRequestAndSetsExpiry() {
        RateLimiterService service = new RateLimiterService(Optional.of(redisTemplate));

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.increment("rate-limit:calls:user-1")).thenReturn(1L);

        assertThatCode(() -> service.check("calls", "user-1", 2, Duration.ofSeconds(30), "Too many calls"))
                .doesNotThrowAnyException();

        verify(redisTemplate).expire("rate-limit:calls:user-1", Duration.ofSeconds(30));
    }

    @Test
    void checkRedisOverflowFallsBackToLocalCounterAndEventuallyThrows() {
        RateLimiterService service = new RateLimiterService(Optional.of(redisTemplate));

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.increment("rate-limit:calls:user-2")).thenReturn(3L);
        when(redisTemplate.getExpire("rate-limit:calls:user-2", TimeUnit.SECONDS)).thenReturn(17L);

        assertThatCode(() -> service.check("calls", "user-2", 2, Duration.ofSeconds(30), "Slow down"))
                .doesNotThrowAnyException();
        assertThatCode(() -> service.check("calls", "user-2", 2, Duration.ofSeconds(30), "Slow down"))
                .doesNotThrowAnyException();
        assertThatThrownBy(() -> service.check("calls", "user-2", 2, Duration.ofSeconds(30), "Slow down"))
                .isInstanceOf(TooManyRequestsException.class)
                .hasMessage("Slow down");
    }

    @Test
    void checkFallsBackToLocalCounterWhenRedisFails() {
        RateLimiterService service = new RateLimiterService(Optional.of(redisTemplate));

        when(redisTemplate.opsForValue()).thenThrow(new RuntimeException("redis unavailable"));

        assertThatCode(() -> service.check("calls", "user-3", 2, Duration.ofSeconds(30), "Slow down"))
                .doesNotThrowAnyException();
        assertThatCode(() -> service.check("calls", "user-3", 2, Duration.ofSeconds(30), "Slow down"))
                .doesNotThrowAnyException();

        assertThatThrownBy(() -> service.check("calls", "user-3", 2, Duration.ofSeconds(30), "Slow down"))
                .isInstanceOf(TooManyRequestsException.class)
                .hasMessage("Slow down");
    }

    @Test
    void checkUsesLocalCounterWhenRedisIsAbsent() {
        RateLimiterService service = new RateLimiterService(Optional.empty());

        assertThatCode(() -> service.check("calls", "user-4", 1, Duration.ofMillis(5), "Too many"))
                .doesNotThrowAnyException();

        assertThatThrownBy(() -> service.check("calls", "user-4", 1, Duration.ofMillis(5), "Too many"))
                .isInstanceOf(TooManyRequestsException.class)
                .hasMessage("Too many");
    }

    @Test
    void scheduleReplacesExistingTimeoutAndCancelStopsFuture() {
        CallTimeoutScheduler scheduler = new CallTimeoutScheduler(taskScheduler);

        doReturn(scheduledFuture).when(taskScheduler).schedule(any(Runnable.class), any(Instant.class));

        scheduler.schedule(55L, Duration.ofSeconds(5), () -> {
        });
        scheduler.schedule(55L, Duration.ofSeconds(10), () -> {
        });

        verify(scheduledFuture).cancel(false);

        scheduler.cancel(55L);

        verify(scheduledFuture, org.mockito.Mockito.times(2)).cancel(false);
    }

    @Test
    void scheduleIgnoresNullFutureFromScheduler() {
        CallTimeoutScheduler scheduler = new CallTimeoutScheduler(taskScheduler);

        when(taskScheduler.schedule(any(Runnable.class), any(Instant.class))).thenReturn(null);

        scheduler.schedule(56L, Duration.ofSeconds(5), () -> {
        });
        scheduler.cancel(56L);

        verify(scheduledFuture, never()).cancel(false);
    }
}

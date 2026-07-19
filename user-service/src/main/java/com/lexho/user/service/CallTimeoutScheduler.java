package com.lexho.user.service;

import org.springframework.scheduling.TaskScheduler;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;

@Component
public class CallTimeoutScheduler {

    private final TaskScheduler taskScheduler;
    private final Map<Long, ScheduledFuture<?>> scheduledTimeouts = new ConcurrentHashMap<>();

    public CallTimeoutScheduler(TaskScheduler taskScheduler) {
        this.taskScheduler = taskScheduler;
    }

    public void schedule(Long sessionId, Duration delay, Runnable task) {
        cancel(sessionId);
        ScheduledFuture<?> future = taskScheduler.schedule(task, Instant.now().plus(delay));
        if (future != null) {
            scheduledTimeouts.put(sessionId, future);
        }
    }

    public void cancel(Long sessionId) {
        ScheduledFuture<?> future = scheduledTimeouts.remove(sessionId);
        if (future != null) {
            future.cancel(false);
        }
    }
}

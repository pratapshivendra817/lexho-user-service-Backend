package com.lexho.user_service.admin;

import com.lexho.user_service.admin.service.AdminDashboardService;
import com.lexho.user_service.admin.service.AdminMonitoringService;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.Statement;
import java.time.Duration;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AdminMonitoringServiceTest {

    @Test
    void combinesBusinessStatsWithRuntimeMetrics() throws Exception {
        AdminDashboardService dashboardService = mock(AdminDashboardService.class);
        SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();
        when(dashboardService.dashboardStats()).thenReturn(Map.of(
                "totalUsers", 12L,
                "totalBookings", 30L));
        Timer timer = Timer.builder("http.server.requests").register(meterRegistry);
        timer.record(Duration.ofMillis(120));
        Counter.builder("payment.failures").register(meterRegistry).increment(2d);
        Counter.builder("booking.created").register(meterRegistry).increment(5d);

        Map<String, Object> result =
                new AdminMonitoringService(
                        dashboardService,
                        meterRegistry,
                        provider(healthyDataSource()),
                        provider(healthyRedisTemplate())).dashboard();

        assertThat(result)
                .containsEntry("status", "UP")
                .containsEntry("apiHealth", "UP")
                .containsEntry("redisHealth", "UP")
                .containsEntry("databaseHealth", "UP")
                .containsEntry("totalUsers", 12L)
                .containsEntry("totalBookings", 30L)
                .containsEntry("apiRequestCount", 1L)
                .containsEntry("apiResponseTimeMs", 120d)
                .containsEntry("paymentFailureCount", 2d)
                .containsEntry("bookingCreationCount", 5d)
                .containsEntry("partnerAssignmentCount", 0d);
        assertThat(result.get("timestamp")).isNotNull();
        assertThat((Long) result.get("uptimeSeconds")).isNotNegative();
    }

    @Test
    void returnsZeroWhenMetricsAreNotRegistered() {
        AdminDashboardService dashboardService = mock(AdminDashboardService.class);
        when(dashboardService.dashboardStats()).thenReturn(Map.of());

        Map<String, Object> result =
                new AdminMonitoringService(
                        dashboardService,
                        new SimpleMeterRegistry(),
                        provider(null),
                        provider(null)).dashboard();

        assertThat(result)
                .containsEntry("apiHealth", "UP")
                .containsEntry("redisHealth", "DOWN")
                .containsEntry("databaseHealth", "DOWN")
                .containsEntry("apiResponseTimeMs", 0d)
                .containsEntry("apiRequestCount", 0L)
                .containsEntry("apiErrorCount", 0d)
                .containsEntry("webhookFailureCount", 0d)
                .containsEntry("webhookDeadLetterCount", 0d)
                .containsEntry("rateLimitRejectionCount", 0d);
    }

    private static DataSource healthyDataSource() throws Exception {
        DataSource dataSource = mock(DataSource.class);
        Connection connection = mock(Connection.class);
        Statement statement = mock(Statement.class);
        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.createStatement()).thenReturn(statement);
        return dataSource;
    }

    private static StringRedisTemplate healthyRedisTemplate() {
        StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
        RedisConnectionFactory connectionFactory = mock(RedisConnectionFactory.class);
        RedisConnection redisConnection = mock(RedisConnection.class);
        when(redisTemplate.getConnectionFactory()).thenReturn(connectionFactory);
        when(connectionFactory.getConnection()).thenReturn(redisConnection);
        when(redisConnection.ping()).thenReturn("PONG");
        return redisTemplate;
    }

    private static <T> ObjectProvider<T> provider(T value) {
        ObjectProvider<T> provider = mock(ObjectProvider.class);
        when(provider.getIfAvailable()).thenReturn(value);
        return provider;
    }
}

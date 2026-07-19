package com.lexho.user_service.health;

import com.lexho.payment.config.RazorpayProperties;
import com.lexho.user_service.notification.service.NotificationManagementService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.Statement;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class HealthControllerTest {

    @Test
    void livenessAlwaysReturnsUp() {
        HealthController controller = new HealthController(
                mock(DataSource.class),
                provider(null),
                provider(null),
                provider(null));

        ResponseEntity<Map<String, Object>> response = controller.liveness();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).containsEntry("status", "UP");
        assertThat(response.getBody()).containsKey("checkedAt");
    }

    @Test
    void readinessReturnsUpWhenAllDependenciesAreHealthy() throws Exception {
        DataSource dataSource = mock(DataSource.class);
        Connection connection = mock(Connection.class);
        Statement statement = mock(Statement.class);
        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.createStatement()).thenReturn(statement);

        StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
        RedisConnectionFactory connectionFactory = mock(RedisConnectionFactory.class);
        RedisConnection redisConnection = mock(RedisConnection.class);
        when(redisTemplate.getConnectionFactory()).thenReturn(connectionFactory);
        when(connectionFactory.getConnection()).thenReturn(redisConnection);
        when(redisConnection.ping()).thenReturn("PONG");

        RazorpayProperties properties = new RazorpayProperties();
        properties.setKeyId("key-id");
        properties.setKeySecret("key-secret");

        HealthController controller = new HealthController(
                dataSource,
                provider(redisTemplate),
                provider(properties),
                provider(mock(NotificationManagementService.class)));

        ResponseEntity<Map<String, Object>> response = controller.readiness();
        @SuppressWarnings("unchecked")
        Map<String, Object> checks = (Map<String, Object>) response.getBody().get("checks");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).containsEntry("status", "UP");
        assertThat(checks)
                .containsEntry("database", "UP")
                .containsEntry("redis", "UP")
                .containsEntry("paymentGateway", "UP")
                .containsEntry("notificationService", "UP");
    }

    @Test
    void readinessReturnsDownWhenAnyDependencyFails() throws Exception {
        DataSource dataSource = mock(DataSource.class);
        when(dataSource.getConnection()).thenThrow(new IllegalStateException("db down"));

        StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
        when(redisTemplate.getConnectionFactory()).thenReturn(null);

        RazorpayProperties properties = new RazorpayProperties();
        properties.setKeyId(" ");
        properties.setKeySecret("key-secret");

        HealthController controller = new HealthController(
                dataSource,
                provider(redisTemplate),
                provider(properties),
                provider(null));

        ResponseEntity<Map<String, Object>> response = controller.health();
        @SuppressWarnings("unchecked")
        Map<String, Object> checks = (Map<String, Object>) response.getBody().get("checks");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
        assertThat(response.getBody()).containsEntry("status", "DOWN");
        assertThat(checks)
                .containsEntry("database", "DOWN")
                .containsEntry("redis", "DOWN")
                .containsEntry("paymentGateway", "DOWN")
                .containsEntry("notificationService", "DOWN");
    }

    private static <T> ObjectProvider<T> provider(T value) {
        ObjectProvider<T> provider = mock(ObjectProvider.class);
        when(provider.getIfAvailable()).thenReturn(value);
        return provider;
    }
}

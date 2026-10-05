package com.lexho.user_service.health;

import com.lexho.payment.config.RazorpayProperties;
import com.lexho.user_service.notification.service.NotificationManagementService;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.sql.DataSource;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

@RestController("productionHealthController")
public class HealthController {

    private final DataSource dataSource;
    private final ObjectProvider<StringRedisTemplate> redisTemplateProvider;
    private final ObjectProvider<RazorpayProperties> razorpayPropertiesProvider;
    private final ObjectProvider<NotificationManagementService> notificationServiceProvider;

    public HealthController(DataSource dataSource,
                            ObjectProvider<StringRedisTemplate> redisTemplateProvider,
                            ObjectProvider<RazorpayProperties> razorpayPropertiesProvider,
                            ObjectProvider<NotificationManagementService> notificationServiceProvider) {
        this.dataSource = dataSource;
        this.redisTemplateProvider = redisTemplateProvider;
        this.razorpayPropertiesProvider = razorpayPropertiesProvider;
        this.notificationServiceProvider = notificationServiceProvider;
    }

    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {
        return readiness();
    }

    @GetMapping({"/health/liveness", "/api/health/liveness"})
    public ResponseEntity<Map<String, Object>> liveness() {
        return ResponseEntity.ok(Map.of(
                "status", "UP",
                "checkedAt", Instant.now().toString()
        ));
    }

    @GetMapping({"/health/readiness", "/api/health/readiness"})
    public ResponseEntity<Map<String, Object>> readiness() {
        Map<String, Object> checks = new LinkedHashMap<>();
        checks.put("database", checkDatabase());
        checks.put("redis", checkRedis());
        checks.put("paymentGateway", checkPaymentGateway());
        checks.put("notificationService", checkNotificationService());
        boolean up = checks.values().stream().allMatch(value -> "UP".equals(value));
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("status", up ? "UP" : "DOWN");
        response.put("checkedAt", Instant.now().toString());
        response.put("checks", checks);
        return ResponseEntity.status(up ? 200 : 503).body(response);
    }

    private String checkDatabase() {
        return DatabaseHealthChecker.check(dataSource);
    }

    private String checkRedis() {
        try {
            StringRedisTemplate redisTemplate = redisTemplateProvider.getIfAvailable();
            if (redisTemplate == null) {
                return "DOWN";
            }
            RedisConnectionFactory connectionFactory = redisTemplate.getConnectionFactory();
            if (connectionFactory == null) {
                return "DOWN";
            }
            String ping = connectionFactory.getConnection().ping();
            return "PONG".equalsIgnoreCase(ping) ? "UP" : "DOWN";
        } catch (Exception exception) {
            return "DOWN";
        }
    }

    private String checkPaymentGateway() {
        RazorpayProperties properties = razorpayPropertiesProvider.getIfAvailable();
        if (properties == null || isBlank(properties.getKeyId()) || isBlank(properties.getKeySecret())) {
            return "DOWN";
        }
        return "UP";
    }

    private String checkNotificationService() {
        return notificationServiceProvider.getIfAvailable() == null ? "DOWN" : "UP";
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}

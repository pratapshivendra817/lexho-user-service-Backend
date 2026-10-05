package com.lexho.user_service.notification;

import com.lexho.notification.enums.NotificationType;
import com.lexho.user_service.notification.config.NotificationAsyncConfig;
import com.lexho.user_service.notification.config.NotificationChannelConfig;
import com.lexho.user_service.notification.config.NotificationChannelProperties;
import com.lexho.user_service.notification.entity.NotificationTemplateEntity;
import com.lexho.user_service.notification.entity.PushDeviceTokenEntity;
import com.lexho.user_service.notification.exception.NotificationAccessDeniedException;
import com.lexho.user_service.notification.exception.NotificationExceptionHandler;
import com.lexho.user_service.notification.exception.NotificationNotFoundException;
import com.lexho.user_service.notification.exception.NotificationValidationException;
import org.junit.jupiter.api.Test;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.core.MethodParameter;

import java.lang.reflect.Method;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.Executor;

import static org.assertj.core.api.Assertions.assertThat;

class NotificationSupportCoverageTest {

    @Test
    void pushDeviceTokenLifecycleDefaultsLastSeenAndUpdatesTimestamp() {
        PushDeviceTokenEntity token = new PushDeviceTokenEntity();
        token.setUserId(7L);
        token.setRole("CUSTOMER");
        token.setDeviceId("device-1");
        token.setToken("push-token");
        token.setPlatform("android");
        token.setAppType("customer");
        assertThat(token.isActive()).isTrue();

        token.onCreate();
        LocalDateTime firstSeen = token.getLastSeenAt();
        token.setActive(false);
        token.onUpdate();

        assertThat(token.getId()).isNull();
        assertThat(token.getUserId()).isEqualTo(7L);
        assertThat(token.getRole()).isEqualTo("CUSTOMER");
        assertThat(token.getDeviceId()).isEqualTo("device-1");
        assertThat(token.getToken()).isEqualTo("push-token");
        assertThat(token.getPlatform()).isEqualTo("android");
        assertThat(token.getAppType()).isEqualTo("customer");
        assertThat(token.isActive()).isFalse();
        assertThat(token.getLastSeenAt()).isAfterOrEqualTo(firstSeen);
    }

    @Test
    void notificationTemplateLifecycleDefaultsLanguageAndRetainsConfiguredFields() {
        NotificationTemplateEntity template = new NotificationTemplateEntity();
        template.setTemplateName("booking_assigned");
        template.setNotificationType(NotificationType.BOOKING);
        template.setTitleTemplate("Title");
        template.setMessageTemplate("Message");
        template.setLanguage(" ");
        template.setActive(false);

        template.prePersist();

        assertThat(template.getId()).isNull();
        assertThat(template.getTemplateName()).isEqualTo("booking_assigned");
        assertThat(template.getNotificationType()).isEqualTo(NotificationType.BOOKING);
        assertThat(template.getTitleTemplate()).isEqualTo("Title");
        assertThat(template.getMessageTemplate()).isEqualTo("Message");
        assertThat(template.getLanguage()).isEqualTo("en");
        assertThat(template.isActive()).isFalse();
        assertThat(template.getCreatedAt()).isNotNull();
    }

    @Test
    void notificationChannelPropertiesExposeEmailAndSmsSettings() {
        NotificationChannelProperties properties = new NotificationChannelProperties();
        properties.getEmail().setEnabled(true);
        properties.getEmail().setFrom("support@lexho.test");
        properties.getSms().setEnabled(true);
        properties.getSms().setGatewayUrl("https://sms.example.test");
        properties.getSms().setAuthToken("secret");
        properties.getSms().setSenderId("LEXHO");

        assertThat(properties.getEmail().isEnabled()).isTrue();
        assertThat(properties.getEmail().getFrom()).isEqualTo("support@lexho.test");
        assertThat(properties.getSms().isEnabled()).isTrue();
        assertThat(properties.getSms().getGatewayUrl()).isEqualTo("https://sms.example.test");
        assertThat(properties.getSms().getAuthToken()).isEqualTo("secret");
        assertThat(properties.getSms().getSenderId()).isEqualTo("LEXHO");
        assertThat(new NotificationChannelConfig()).isNotNull();
    }

    @Test
    void asyncConfigCreatesInitializedTaskExecutorWithExpectedThreadPrefix() {
        Executor executor = new NotificationAsyncConfig().taskExecutor();

        assertThat(executor).isInstanceOf(ThreadPoolTaskExecutor.class);
        ThreadPoolTaskExecutor taskExecutor = (ThreadPoolTaskExecutor) executor;
        assertThat(taskExecutor.getCorePoolSize()).isEqualTo(8);
        assertThat(taskExecutor.getMaxPoolSize()).isEqualTo(24);
        assertThat(taskExecutor.getThreadNamePrefix()).isEqualTo("app-async-");
        taskExecutor.shutdown();
    }

    @Test
    void exceptionHandlerMapsDomainExceptionsAndValidationErrors() throws Exception {
        NotificationExceptionHandler handler = new NotificationExceptionHandler();

        assertThat(handler.handleNotFound(new NotificationNotFoundException(9L)).getStatusCode().value())
                .isEqualTo(404);
        assertThat(handler.handleAccessDenied(new NotificationAccessDeniedException("denied")).getStatusCode().value())
                .isEqualTo(403);
        assertThat(handler.handleValidation(new NotificationValidationException("bad")).getStatusCode().value())
                .isEqualTo(400);

        Map<String, Object> validationBody = handler.handleMethodValidation(methodValidationException("title required"))
                .getBody();
        assertThat(validationBody).containsEntry("message", "title required");
    }

    private MethodArgumentNotValidException methodValidationException(String message) throws Exception {
        BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(new Object(), "request");
        bindingResult.addError(new FieldError("request", "title", message));
        Method method = NotificationSupportCoverageTest.class.getDeclaredMethod("methodValidationException", String.class);
        return new MethodArgumentNotValidException(new MethodParameter(method, 0), bindingResult);
    }
}

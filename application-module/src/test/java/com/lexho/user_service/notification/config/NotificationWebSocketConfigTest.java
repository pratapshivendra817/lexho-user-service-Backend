package com.lexho.user_service.notification.config;

import com.lexho.user_service.chat.config.ChatStompAuthChannelInterceptor;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class NotificationWebSocketConfigTest {

    @Test
    void websocketOriginsUseExplicitPropertyWhenConfigured() {
        NotificationWebSocketConfig config = new NotificationWebSocketConfig(
                mock(ChatStompAuthChannelInterceptor.class),
                new MockEnvironment()
                        .withProperty("app.security.websocket.allowed-origins",
                                "https://admin.lexho.com, https://ops.lexho.com"));

        assertThat(config.resolveAllowedOrigins())
                .containsExactly("https://admin.lexho.com", "https://ops.lexho.com");
    }

    @Test
    void websocketOriginsFallBackToHttpCorsOrigins() {
        NotificationWebSocketConfig config = new NotificationWebSocketConfig(
                mock(ChatStompAuthChannelInterceptor.class),
                new MockEnvironment()
                        .withProperty("app.security.cors.allowed-origins",
                                "https://customer.lexho.com, https://partner.lexho.com"));

        assertThat(config.resolveAllowedOrigins())
                .containsExactly("https://customer.lexho.com", "https://partner.lexho.com");
    }

    @Test
    void websocketOriginsFallBackToLocalDevelopmentDefaults() {
        NotificationWebSocketConfig config = new NotificationWebSocketConfig(
                mock(ChatStompAuthChannelInterceptor.class),
                new MockEnvironment());

        assertThat(config.resolveAllowedOrigins())
                .containsExactly("http://localhost:3000", "http://localhost:5173", "http://localhost:4200");
    }

    @Test
    void parseAllowedOriginsRemovesBlanksAndDuplicates() {
        assertThat(NotificationWebSocketConfig.parseAllowedOrigins(
                " https://admin.lexho.com,,https://admin.lexho.com, https://customer.lexho.com "))
                .containsExactly("https://admin.lexho.com", "https://customer.lexho.com");
    }
}

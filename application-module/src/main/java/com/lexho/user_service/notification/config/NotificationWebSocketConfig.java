package com.lexho.user_service.notification.config;

import com.lexho.user_service.chat.config.ChatStompAuthChannelInterceptor;
import com.lexho.user_service.config.AllowedOriginsParser;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

import java.util.List;

@Configuration
@EnableWebSocketMessageBroker
public class NotificationWebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private static final String DEFAULT_ALLOWED_ORIGINS =
            "http://localhost:3000,http://localhost:5173,http://localhost:4200";

    private final ChatStompAuthChannelInterceptor chatStompAuthChannelInterceptor;
    private final Environment environment;

    public NotificationWebSocketConfig(ChatStompAuthChannelInterceptor chatStompAuthChannelInterceptor,
                                       Environment environment) {
        this.chatStompAuthChannelInterceptor = chatStompAuthChannelInterceptor;
        this.environment = environment;
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        String[] allowedOrigins = resolveAllowedOrigins();
        registry.addEndpoint("/ws-notifications").setAllowedOrigins(allowedOrigins);
        registry.addEndpoint("/ws/admin/notifications").setAllowedOrigins(allowedOrigins);
        registry.addEndpoint("/ws/customer/bookings").setAllowedOrigins(allowedOrigins);
        registry.addEndpoint("/ws/customer/bookings/{bookingId}").setAllowedOrigins(allowedOrigins);
        registry.addEndpoint("/ws/partner/dispatch").setAllowedOrigins(allowedOrigins);
        registry.addEndpoint("/ws/admin/bookings/{bookingId}").setAllowedOrigins(allowedOrigins);
        registry.addEndpoint("/ws-chat").setAllowedOrigins(allowedOrigins);
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        registry.enableSimpleBroker("/topic", "/queue");
        registry.setApplicationDestinationPrefixes("/app");
        registry.setUserDestinationPrefix("/user");
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(chatStompAuthChannelInterceptor);
    }

    String[] resolveAllowedOrigins() {
        String rawOrigins = environment.getProperty(
                "app.security.websocket.allowed-origins",
                environment.getProperty("app.security.cors.allowed-origins", DEFAULT_ALLOWED_ORIGINS)
        );
        return parseAllowedOrigins(rawOrigins).toArray(String[]::new);
    }

    static List<String> parseAllowedOrigins(String rawOrigins) {
        return AllowedOriginsParser.parse(rawOrigins);
    }
}

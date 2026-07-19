package com.lexho.user_service.notification.config;

import com.lexho.user_service.chat.config.ChatStompAuthChannelInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
public class NotificationWebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final ChatStompAuthChannelInterceptor chatStompAuthChannelInterceptor;

    public NotificationWebSocketConfig(ChatStompAuthChannelInterceptor chatStompAuthChannelInterceptor) {
        this.chatStompAuthChannelInterceptor = chatStompAuthChannelInterceptor;
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws-notifications").setAllowedOriginPatterns("*");
        registry.addEndpoint("/ws/admin/notifications").setAllowedOriginPatterns("*");
        registry.addEndpoint("/ws/customer/bookings").setAllowedOriginPatterns("*");
        registry.addEndpoint("/ws/customer/bookings/{bookingId}").setAllowedOriginPatterns("*");
        registry.addEndpoint("/ws/admin/bookings/{bookingId}").setAllowedOriginPatterns("*");
        registry.addEndpoint("/ws-chat").setAllowedOriginPatterns("*");
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
}

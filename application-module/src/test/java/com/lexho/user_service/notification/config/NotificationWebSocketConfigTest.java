package com.lexho.user_service.notification.config;

import com.lexho.user_service.chat.config.ChatStompAuthChannelInterceptor;
import org.junit.jupiter.api.Test;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.StompWebSocketEndpointRegistration;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class NotificationWebSocketConfigTest {

    @Test
    void shouldRegisterEndpointsBrokerAndInboundInterceptor() {
        ChatStompAuthChannelInterceptor interceptor = mock(ChatStompAuthChannelInterceptor.class);
        NotificationWebSocketConfig config = new NotificationWebSocketConfig(interceptor);
        StompEndpointRegistry registry = mock(StompEndpointRegistry.class);
        StompWebSocketEndpointRegistration registration = mock(StompWebSocketEndpointRegistration.class);
        MessageBrokerRegistry brokerRegistry = mock(MessageBrokerRegistry.class);
        ChannelRegistration channelRegistration = mock(ChannelRegistration.class);

        when(registry.addEndpoint("/ws-notifications")).thenReturn(registration);
        when(registry.addEndpoint("/ws/admin/notifications")).thenReturn(registration);
        when(registry.addEndpoint("/ws/customer/bookings")).thenReturn(registration);
        when(registry.addEndpoint("/ws/customer/bookings/{bookingId}")).thenReturn(registration);
        when(registry.addEndpoint("/ws/admin/bookings/{bookingId}")).thenReturn(registration);
        when(registry.addEndpoint("/ws-chat")).thenReturn(registration);
        when(registration.setAllowedOriginPatterns("*")).thenReturn(registration);

        config.registerStompEndpoints(registry);
        config.configureMessageBroker(brokerRegistry);
        config.configureClientInboundChannel(channelRegistration);

        verify(registry).addEndpoint("/ws-notifications");
        verify(registry).addEndpoint("/ws/admin/notifications");
        verify(registry).addEndpoint("/ws/customer/bookings");
        verify(registry).addEndpoint("/ws/customer/bookings/{bookingId}");
        verify(registry).addEndpoint("/ws/admin/bookings/{bookingId}");
        verify(registry).addEndpoint("/ws-chat");
        verify(registration, org.mockito.Mockito.times(6)).setAllowedOriginPatterns("*");
        verify(brokerRegistry).enableSimpleBroker("/topic", "/queue");
        verify(brokerRegistry).setApplicationDestinationPrefixes("/app");
        verify(brokerRegistry).setUserDestinationPrefix("/user");
        verify(channelRegistration).interceptors(interceptor);
    }
}

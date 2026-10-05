package com.lexho.user_service.partnerrequest.service;

import com.lexho.common.enums.Role;
import com.lexho.testconfig.WebSliceBootTestConfig;
import com.lexho.user_service.chat.config.ChatStompAuthChannelInterceptor;
import com.lexho.user_service.chat.service.ChatService;
import com.lexho.user_service.notification.config.NotificationWebSocketConfig;
import com.lexho.user_service.partnerrequest.entity.PartnerServiceRequestEntity;
import com.lexho.user_service.partnerrequest.model.PartnerServiceRequestStatus;
import com.lexho.user_service.util.JwtUtil;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.impl.DefaultClaims;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.messaging.converter.MappingJackson2MessageConverter;
import org.springframework.messaging.simp.stomp.StompFrameHandler;
import org.springframework.messaging.simp.stomp.StompHeaders;
import org.springframework.messaging.simp.stomp.StompSession;
import org.springframework.messaging.simp.stomp.StompSessionHandlerAdapter;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.WebSocketHttpHeaders;
import org.springframework.web.socket.messaging.WebSocketStompClient;

import java.lang.reflect.Type;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@SpringBootTest(
        classes = WebSliceBootTestConfig.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "spring.cloud.compatibility-verifier.enabled=false",
                "spring.autoconfigure.exclude="
                        + "org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration,"
                        + "org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration"
        }
)
@Import({
        NotificationWebSocketConfig.class,
        ChatStompAuthChannelInterceptor.class,
        PartnerDispatchRealtimeService.class
})
@ActiveProfiles("test")
@TestPropertySource(properties = "app.security.websocket.allowed-origins=*")
class PartnerDispatchStompE2ETest {

    private static final Long PARTNER_ID = 77L;

    @LocalServerPort
    private int port;

    @Autowired
    private PartnerDispatchRealtimeService realtimeService;

    @MockBean
    private JwtUtil jwtUtil;

    @MockBean
    private ChatService chatService;

    @Test
    void partnerReceivesDispatchEventOverAuthenticatedStompTopic() throws Exception {
        Claims claims = new DefaultClaims();
        claims.put("userId", PARTNER_ID);
        claims.put("role", Role.PARTNER.name());
        when(jwtUtil.validateToken("partner-token")).thenReturn(claims);

        WebSocketStompClient stompClient = new WebSocketStompClient(new StandardWebSocketClient());
        stompClient.setMessageConverter(new MappingJackson2MessageConverter());

        StompHeaders connectHeaders = new StompHeaders();
        connectHeaders.add("Authorization", "Bearer partner-token");
        StompSession session = stompClient
                .connectAsync("ws://localhost:" + port + "/ws/partner/dispatch",
                        new WebSocketHttpHeaders(),
                        connectHeaders,
                        new StompSessionHandlerAdapter() {
                        })
                .get(5, TimeUnit.SECONDS);

        CompletableFuture<Map<String, Object>> received = new CompletableFuture<>();
        session.subscribe("/topic/partner/dispatch/" + PARTNER_ID, new StompFrameHandler() {
            @Override
            public Type getPayloadType(StompHeaders headers) {
                return Map.class;
            }

            @Override
            @SuppressWarnings("unchecked")
            public void handleFrame(StompHeaders headers, Object payload) {
                received.complete((Map<String, Object>) payload);
            }
        });
        Thread.sleep(250L);

        PartnerServiceRequestEntity request = new PartnerServiceRequestEntity();
        ReflectionTestUtils.setField(request, "id", 41L);
        request.setPartnerId(PARTNER_ID);
        request.setCartId(1001L);
        request.setCustomerId(9L);
        request.setServiceId(3L);
        request.setServiceType("Cleaning");
        request.setScheduledTime(LocalDateTime.of(2026, 10, 5, 10, 0));
        request.setStatus(PartnerServiceRequestStatus.PENDING);
        request.setEtaMinutes(12);
        request.setExpiresAt(LocalDateTime.of(2026, 10, 5, 9, 58));

        realtimeService.publishRequestEvent(request, "REQUEST_CREATED");

        Map<String, Object> payload = received.get(10, TimeUnit.SECONDS);
        assertThat(payload)
                .containsEntry("event", "REQUEST_CREATED")
                .containsEntry("requestId", 41)
                .containsEntry("partnerId", PARTNER_ID.intValue())
                .containsEntry("orderId", 1001)
                .containsEntry("status", "PENDING")
                .containsEntry("etaMinutes", 12);

        session.disconnect();
    }
}

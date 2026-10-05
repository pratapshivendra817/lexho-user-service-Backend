package com.lexho.user_service.partnerrequest.service;

import com.lexho.user_service.partnerrequest.entity.PartnerServiceRequestEntity;
import com.lexho.user_service.partnerrequest.model.PartnerServiceRequestStatus;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

class PartnerDispatchRealtimeServiceTest {

    private final SimpMessagingTemplate messagingTemplate = mock(SimpMessagingTemplate.class);
    private final PartnerDispatchRealtimeService service = new PartnerDispatchRealtimeService(messagingTemplate);

    @Test
    void publishRequestEventSendsPartnerSpecificAndBroadcastTopics() {
        PartnerServiceRequestEntity request = new PartnerServiceRequestEntity();
        ReflectionTestUtils.setField(request, "id", 41L);
        request.setPartnerId(7L);
        request.setCartId(1001L);
        request.setSubscriptionId(500L);
        request.setSubscriptionVisitId(600L);
        request.setCustomerId(9L);
        request.setServiceId(3L);
        request.setServiceType("Cleaning");
        request.setScheduledTime(LocalDateTime.of(2026, 10, 5, 10, 0));
        request.setStatus(PartnerServiceRequestStatus.PENDING);
        request.setEtaMinutes(12);
        request.setExpiresAt(LocalDateTime.of(2026, 10, 5, 9, 58));

        service.publishRequestEvent(request, "REQUEST_CREATED");

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Map<String, Object>> payloadCaptor = ArgumentCaptor.forClass(Map.class);
        verify(messagingTemplate).convertAndSend(
                org.mockito.ArgumentMatchers.eq("/topic/partner/dispatch/7"),
                payloadCaptor.capture());
        verify(messagingTemplate).convertAndSend(
                org.mockito.ArgumentMatchers.eq("/topic/partner/dispatch"),
                org.mockito.ArgumentMatchers.any(Map.class));

        Map<String, Object> payload = payloadCaptor.getValue();
        assertThat(payload)
                .containsEntry("event", "REQUEST_CREATED")
                .containsEntry("requestId", 41L)
                .containsEntry("partnerId", 7L)
                .containsEntry("orderId", 1001L)
                .containsEntry("subscriptionId", 500L)
                .containsEntry("subscriptionVisitId", 600L)
                .containsEntry("status", "PENDING")
                .containsEntry("etaMinutes", 12);
    }

    @Test
    void publishRequestEventIgnoresIncompleteInput() {
        service.publishRequestEvent(null, "REQUEST_CREATED");

        PartnerServiceRequestEntity request = new PartnerServiceRequestEntity();
        service.publishRequestEvent(request, "REQUEST_CREATED");

        request.setPartnerId(7L);
        service.publishRequestEvent(request, " ");

        verify(messagingTemplate, never()).convertAndSend(
                org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.any(Object.class));
    }
}

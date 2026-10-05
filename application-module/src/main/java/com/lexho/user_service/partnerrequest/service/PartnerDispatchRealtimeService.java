package com.lexho.user_service.partnerrequest.service;

import com.lexho.user_service.partnerrequest.entity.PartnerServiceRequestEntity;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class PartnerDispatchRealtimeService {

    public static final String PARTNER_DISPATCH_TOPIC_PREFIX = "/topic/partner/dispatch/";
    public static final String PARTNER_DISPATCH_BROADCAST_TOPIC = "/topic/partner/dispatch";

    private final SimpMessagingTemplate messagingTemplate;

    public PartnerDispatchRealtimeService(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    public void publishRequestEvent(PartnerServiceRequestEntity request, String event) {
        if (request == null || request.getPartnerId() == null || event == null || event.isBlank()) {
            return;
        }
        Map<String, Object> payload = payload(request, event);
        messagingTemplate.convertAndSend(PARTNER_DISPATCH_TOPIC_PREFIX + request.getPartnerId(), payload);
        messagingTemplate.convertAndSend(PARTNER_DISPATCH_BROADCAST_TOPIC, payload);
    }

    private Map<String, Object> payload(PartnerServiceRequestEntity request, String event) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("event", event);
        payload.put("requestId", request.getId());
        payload.put("partnerId", request.getPartnerId());
        payload.put("orderId", request.getCartId());
        payload.put("cartId", request.getCartId());
        payload.put("subscriptionId", request.getSubscriptionId());
        payload.put("subscriptionVisitId", request.getSubscriptionVisitId());
        payload.put("customerId", request.getCustomerId());
        payload.put("serviceId", request.getServiceId());
        payload.put("serviceType", request.getServiceType());
        payload.put("scheduledTime", request.getScheduledTime());
        payload.put("status", request.getStatus() == null ? null : request.getStatus().name());
        payload.put("etaMinutes", request.getEtaMinutes());
        payload.put("expiresAt", request.getExpiresAt());
        payload.put("timestamp", LocalDateTime.now().toString());
        return payload;
    }
}

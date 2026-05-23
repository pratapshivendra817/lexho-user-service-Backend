package com.lexho.user_service.notification.service;

import com.lexho.notification.entity.Notification;
import com.lexho.user_service.notification.config.NotificationChannelProperties;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@Service
public class SmsNotificationService {

    private final NotificationChannelProperties properties;
    private final NotificationRecipientResolver recipientResolver;
    private final RestTemplate restTemplate = new RestTemplate();

    public SmsNotificationService(NotificationChannelProperties properties,
                                  NotificationRecipientResolver recipientResolver) {
        this.properties = properties;
        this.recipientResolver = recipientResolver;
    }

    public void send(Notification notification) {
        if (!properties.getSms().isEnabled()) {
            throw new IllegalStateException("SMS channel is disabled");
        }
        String gatewayUrl = properties.getSms().getGatewayUrl();
        if (gatewayUrl == null || gatewayUrl.isBlank()) {
            throw new IllegalStateException("SMS gateway URL is not configured");
        }

        NotificationRecipientResolver.RecipientContact contact = recipientResolver.resolve(notification);
        if (contact.mobile() == null || contact.mobile().isBlank()) {
            throw new IllegalStateException("Recipient mobile not found");
        }

        Map<String, Object> payload = new HashMap<>();
        payload.put("to", contact.mobile().trim());
        payload.put("message", notification.getMessage() == null ? "" : notification.getMessage());
        payload.put("title", notification.getTitle() == null ? "Lexho" : notification.getTitle());
        payload.put("senderId", properties.getSms().getSenderId());

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        if (properties.getSms().getAuthToken() != null && !properties.getSms().getAuthToken().isBlank()) {
            headers.setBearerAuth(properties.getSms().getAuthToken().trim());
        }

        restTemplate.postForEntity(gatewayUrl.trim(), new HttpEntity<>(payload, headers), String.class);
    }
}


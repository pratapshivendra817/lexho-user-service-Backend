package com.lexho.user_service.notification.service;

import com.lexho.user_service.notification.config.NotificationChannelProperties;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import java.util.HashMap;
import java.util.Map;

public final class SmsGatewayRequestFactory {

    private SmsGatewayRequestFactory() {
    }

    public static HttpEntity<Map<String, Object>> create(String recipient,
                                                         String message,
                                                         String title,
                                                         NotificationChannelProperties.Sms smsProperties) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("to", recipient.trim());
        payload.put("message", message);
        payload.put("title", title);
        payload.put("senderId", smsProperties.getSenderId());

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        if (smsProperties.getAuthToken() != null && !smsProperties.getAuthToken().isBlank()) {
            headers.setBearerAuth(smsProperties.getAuthToken().trim());
        }
        return new HttpEntity<>(payload, headers);
    }
}

package com.lexho.user_service.notification.service;

import com.lexho.notification.entity.Notification;
import com.lexho.user_service.notification.config.NotificationChannelProperties;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

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

        String message = notification.getMessage() == null ? "" : notification.getMessage();
        String title = notification.getTitle() == null ? "Lexho" : notification.getTitle();
        restTemplate.postForEntity(
                gatewayUrl.trim(),
                SmsGatewayRequestFactory.create(contact.mobile(), message, title, properties.getSms()),
                String.class);
    }
}

package com.lexho.user_service.notification.service;

import com.lexho.notification.entity.Notification;
import com.lexho.user_service.notification.config.NotificationChannelProperties;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.lang.Nullable;

@Service
public class EmailNotificationService {

    private final NotificationChannelProperties properties;
    private final NotificationRecipientResolver recipientResolver;
    private final JavaMailSender mailSender;

    public EmailNotificationService(NotificationChannelProperties properties,
                                    NotificationRecipientResolver recipientResolver,
                                    @Nullable JavaMailSender mailSender) {
        this.properties = properties;
        this.recipientResolver = recipientResolver;
        this.mailSender = mailSender;
    }

    public void send(Notification notification) {
        if (!properties.getEmail().isEnabled()) {
            throw new IllegalStateException("Email channel is disabled");
        }
        if (mailSender == null) {
            throw new IllegalStateException("Mail sender is not configured");
        }
        NotificationRecipientResolver.RecipientContact contact = recipientResolver.resolve(notification);
        if (contact.email() == null || contact.email().isBlank()) {
            throw new IllegalStateException("Recipient email not found");
        }

        SimpleMailMessage message = new SimpleMailMessage();
        if (properties.getEmail().getFrom() != null && !properties.getEmail().getFrom().isBlank()) {
            message.setFrom(properties.getEmail().getFrom().trim());
        }
        message.setTo(contact.email().trim());
        message.setSubject(notification.getTitle() == null ? "Lexho Notification" : notification.getTitle());
        message.setText(notification.getMessage() == null ? "" : notification.getMessage());
        mailSender.send(message);
    }
}


package com.lexho.user_service.notification.service;

import com.lexho.notification.entity.Notification;
import com.lexho.user_service.notification.config.NotificationChannelProperties;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class EmailNotificationServiceTest {

    private final NotificationChannelProperties properties = new NotificationChannelProperties();
    private final NotificationRecipientResolver resolver = mock(NotificationRecipientResolver.class);
    private final JavaMailSender sender = mock(JavaMailSender.class);

    @Test
    void validatesConfigurationAndRecipient() {
        Notification notification = new Notification();
        assertThatThrownBy(() -> new EmailNotificationService(properties, resolver, sender).send(notification))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("disabled");

        properties.getEmail().setEnabled(true);
        assertThatThrownBy(() -> new EmailNotificationService(properties, resolver, null).send(notification))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("not configured");

        when(resolver.resolve(notification)).thenReturn(new NotificationRecipientResolver.RecipientContact(null, null, null));
        assertThatThrownBy(() -> new EmailNotificationService(properties, resolver, sender).send(notification))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("email");
    }

    @Test
    void sendsTrimmedEmailWithDefaults() {
        Notification notification = new Notification();
        properties.getEmail().setEnabled(true);
        properties.getEmail().setFrom(" no-reply@lexho.com ");
        when(resolver.resolve(notification))
                .thenReturn(new NotificationRecipientResolver.RecipientContact("999", " user@example.com ", "User"));

        new EmailNotificationService(properties, resolver, sender).send(notification);

        ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(sender).send(captor.capture());
        assertThat(captor.getValue().getFrom()).isEqualTo("no-reply@lexho.com");
        assertThat(captor.getValue().getTo()).containsExactly("user@example.com");
        assertThat(captor.getValue().getSubject()).isEqualTo("Lexho Notification");
        assertThat(captor.getValue().getText()).isEmpty();
    }
}

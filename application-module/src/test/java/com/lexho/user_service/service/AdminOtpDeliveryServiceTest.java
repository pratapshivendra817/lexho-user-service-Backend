package com.lexho.user_service.service;

import com.lexho.user_service.config.AdminAuthProperties;
import com.lexho.user_service.exception.InfrastructureException;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mail.MailSendException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class AdminOtpDeliveryServiceTest {

    private final JavaMailSender sender = mock(JavaMailSender.class);
    private final AdminAuthProperties properties = new AdminAuthProperties();

    @Test
    void sendsConfiguredOtpEmail() {
        properties.getOtp().setDeliveryFrom("admin@lexho.com");

        new AdminOtpDeliveryService(sender, properties).sendOtpByEmail("user@example.com", "123456");

        ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(sender).send(captor.capture());
        assertThat(captor.getValue().getFrom()).isEqualTo("admin@lexho.com");
        assertThat(captor.getValue().getText()).contains("123456");
    }

    @Test
    void failsClosedOrFallsBackWhenMailDeliveryFails() {
        doThrow(new MailSendException("down")).when(sender).send(org.mockito.ArgumentMatchers.any(SimpleMailMessage.class));
        properties.getInfrastructure().setAllowConsoleOtpDeliveryFallback(false);
        AdminOtpDeliveryService service = new AdminOtpDeliveryService(sender, properties);

        assertThatThrownBy(() -> service.sendOtpByEmail("user@example.com", "123456"))
                .isInstanceOf(InfrastructureException.class);

        properties.getInfrastructure().setAllowConsoleOtpDeliveryFallback(true);
        service.sendOtpByEmail("user@example.com", "123456");
    }
}

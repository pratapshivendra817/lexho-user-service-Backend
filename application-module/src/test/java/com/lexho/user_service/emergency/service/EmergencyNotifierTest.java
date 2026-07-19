package com.lexho.user_service.emergency.service;

import com.lexho.admin.entity.Admin;
import com.lexho.admin.repository.AdminRepository;
import com.lexho.user_service.emergency.entity.EmergencyContact;
import com.lexho.user_service.emergency.entity.EmergencyEvent;
import com.lexho.user_service.emergency.entity.EmergencyLocation;
import com.lexho.user_service.notification.config.NotificationChannelProperties;
import com.lexho.user_service.notification.service.NotificationTriggerService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.HttpEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class EmergencyNotifierTest {

    @Test
    void externalNotifierSendsSmsAndEmailWhenChannelsAreEnabled() {
        NotificationChannelProperties properties = notificationProperties(true, true);
        JavaMailSender mailSender = mock(JavaMailSender.class);
        RestTemplate restTemplate = mock(RestTemplate.class);
        EmergencyExternalContactNotifier notifier = new EmergencyExternalContactNotifier(properties, mailSender);
        ReflectionTestUtils.setField(notifier, "restTemplate", restTemplate);

        when(restTemplate.postForEntity(eq("https://sms.example.com"), any(HttpEntity.class), eq(String.class)))
                .thenReturn(ResponseEntity.ok("ok"));

        int notified = notifier.notifyContacts(event(), location("Main Street"), List.of(contact("9999999999", "help@example.com")));

        assertThat(notified).isEqualTo(1);

        ArgumentCaptor<HttpEntity> smsCaptor = ArgumentCaptor.forClass(HttpEntity.class);
        verify(restTemplate).postForEntity(eq("https://sms.example.com"), smsCaptor.capture(), eq(String.class));
        assertThat(smsCaptor.getValue().getHeaders().getFirst("Authorization")).isEqualTo("Bearer token-123");
        assertThat(smsCaptor.getValue().getBody().toString()).contains("Lexho SOS", "Main Street");

        ArgumentCaptor<SimpleMailMessage> mailCaptor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(mailCaptor.capture());
        assertThat(mailCaptor.getValue().getFrom()).isEqualTo("alerts@lexho.com");
        assertThat(mailCaptor.getValue().getTo()).containsExactly("help@example.com");
        assertThat(mailCaptor.getValue().getText()).contains("SOS from Partner One", "Main Street");
    }

    @Test
    void externalNotifierHandlesDisabledChannelsMissingTargetsAndSmsFailureWithEmailFallback() {
        NotificationChannelProperties properties = notificationProperties(true, true);
        JavaMailSender mailSender = mock(JavaMailSender.class);
        RestTemplate restTemplate = mock(RestTemplate.class);
        EmergencyExternalContactNotifier notifier = new EmergencyExternalContactNotifier(properties, mailSender);
        ReflectionTestUtils.setField(notifier, "restTemplate", restTemplate);

        when(restTemplate.postForEntity(eq("https://sms.example.com"), any(HttpEntity.class), eq(String.class)))
                .thenThrow(new RuntimeException("sms down"));
        doNothing().when(mailSender).send(any(SimpleMailMessage.class));

        EmergencyContact emailOnly = contact("", "fallback@example.com");
        EmergencyContact blankContact = contact("", " ");

        int notified = notifier.notifyContacts(event(), location(""), List.of(emailOnly, blankContact));

        assertThat(notified).isEqualTo(1);
        verify(mailSender).send(any(SimpleMailMessage.class));

        NotificationChannelProperties disabledProperties = notificationProperties(false, false);
        EmergencyExternalContactNotifier disabledNotifier = new EmergencyExternalContactNotifier(disabledProperties, null);
        ReflectionTestUtils.setField(disabledNotifier, "restTemplate", restTemplate);

        int disabledCount = disabledNotifier.notifyContacts(event(), location(""), List.of(blankContact));

        assertThat(disabledCount).isZero();
        verify(restTemplate, never()).postForEntity(eq(""), any(HttpEntity.class), eq(String.class));
    }

    @Test
    void externalNotifierSwallowsEmailFailuresAndAdminNotifierNotifiesOnlyActiveAdmins() {
        NotificationChannelProperties properties = notificationProperties(false, true);
        JavaMailSender mailSender = mock(JavaMailSender.class);
        EmergencyExternalContactNotifier notifier = new EmergencyExternalContactNotifier(properties, mailSender);
        doThrow(new RuntimeException("mail down")).when(mailSender).send(any(SimpleMailMessage.class));

        int notified = notifier.notifyContacts(event(), location("HQ"), List.of(contact("", "admin@example.com")));

        assertThat(notified).isZero();

        AdminRepository adminRepository = mock(AdminRepository.class);
        NotificationTriggerService notificationTriggerService = mock(NotificationTriggerService.class);
        EmergencyAdminNotifier emergencyAdminNotifier = new EmergencyAdminNotifier(adminRepository, notificationTriggerService);

        Admin active = new Admin();
        active.setId(1L);
        active.setActive(true);
        Admin inactive = new Admin();
        inactive.setId(2L);
        inactive.setActive(false);
        when(adminRepository.findAll()).thenReturn(List.of(active, inactive));

        int adminNotifications = emergencyAdminNotifier.notifyAdmins(event(), location("HQ"));

        assertThat(adminNotifications).isEqualTo(1);
        verify(notificationTriggerService).emergencyAlert(eq(1L), eq("Partner SOS Alert"), any(String.class), eq(77L));
        verify(notificationTriggerService, never()).emergencyAlert(eq(2L), any(String.class), any(String.class), any(Long.class));
    }

    private NotificationChannelProperties notificationProperties(boolean smsEnabled, boolean emailEnabled) {
        NotificationChannelProperties properties = new NotificationChannelProperties();
        properties.getSms().setEnabled(smsEnabled);
        properties.getSms().setGatewayUrl("https://sms.example.com");
        properties.getSms().setAuthToken("token-123");
        properties.getSms().setSenderId("LEXHO");
        properties.getEmail().setEnabled(emailEnabled);
        properties.getEmail().setFrom("alerts@lexho.com");
        return properties;
    }

    private EmergencyEvent event() {
        EmergencyEvent event = new EmergencyEvent();
        event.setId(77L);
        event.setIncidentCode("INC-1");
        event.setPartnerId(8L);
        event.setPartnerName("Partner One");
        event.setPartnerMobileNumber("8888888888");
        return event;
    }

    private EmergencyLocation location(String address) {
        EmergencyLocation location = new EmergencyLocation();
        location.setLatitude(BigDecimal.valueOf(12.34));
        location.setLongitude(BigDecimal.valueOf(56.78));
        location.setAccuracyMeters(BigDecimal.valueOf(8.5));
        location.setAddress(address);
        location.setSource("GPS");
        return location;
    }

    private EmergencyContact contact(String phone, String email) {
        EmergencyContact contact = new EmergencyContact();
        contact.setId(5L);
        contact.setName("Primary");
        contact.setPhoneNumber(phone);
        contact.setEmail(email);
        return contact;
    }
}

package com.lexho.user_service.emergency.service;

import com.lexho.user_service.emergency.entity.EmergencyContact;
import com.lexho.user_service.emergency.entity.EmergencyEvent;
import com.lexho.user_service.emergency.entity.EmergencyLocation;
import com.lexho.user_service.notification.config.NotificationChannelProperties;
import com.lexho.user_service.notification.service.SmsGatewayRequestFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.lang.Nullable;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

@Service
public class EmergencyExternalContactNotifier {

    private static final Logger LOG = LoggerFactory.getLogger(EmergencyExternalContactNotifier.class);

    private final NotificationChannelProperties properties;
    private final JavaMailSender mailSender;
    private final RestTemplate restTemplate = new RestTemplate();

    public EmergencyExternalContactNotifier(NotificationChannelProperties properties,
                                            @Nullable JavaMailSender mailSender) {
        this.properties = properties;
        this.mailSender = mailSender;
    }

    public int notifyContacts(EmergencyEvent event,
                              EmergencyLocation location,
                              List<EmergencyContact> contacts) {
        int notified = 0;
        for (EmergencyContact contact : contacts) {
            boolean delivered = false;
            try {
                delivered = sendSms(contact, event, location);
            } catch (RuntimeException exception) {
                LOG.warn("Emergency SMS failed for contactId={}: {}", contact.getId(), exception.getMessage());
            }
            try {
                delivered = sendEmail(contact, event, location) || delivered;
            } catch (RuntimeException exception) {
                LOG.warn("Emergency email failed for contactId={}: {}", contact.getId(), exception.getMessage());
            }
            if (delivered) {
                notified++;
            }
        }
        return notified;
    }

    private boolean sendSms(EmergencyContact contact,
                            EmergencyEvent event,
                            EmergencyLocation location) {
        if (!properties.getSms().isEnabled()) {
            return false;
        }
        String gatewayUrl = properties.getSms().getGatewayUrl();
        if (gatewayUrl == null || gatewayUrl.isBlank() || contact.getPhoneNumber() == null || contact.getPhoneNumber().isBlank()) {
            return false;
        }

        restTemplate.postForEntity(
                gatewayUrl.trim(),
                SmsGatewayRequestFactory.create(contact.getPhoneNumber(), smsMessage(event, location), "Lexho SOS",
                        properties.getSms()),
                String.class);
        return true;
    }

    private boolean sendEmail(EmergencyContact contact,
                              EmergencyEvent event,
                              EmergencyLocation location) {
        if (!properties.getEmail().isEnabled() || mailSender == null) {
            return false;
        }
        if (contact.getEmail() == null || contact.getEmail().isBlank()) {
            return false;
        }
        SimpleMailMessage mail = new SimpleMailMessage();
        if (properties.getEmail().getFrom() != null && !properties.getEmail().getFrom().isBlank()) {
            mail.setFrom(properties.getEmail().getFrom().trim());
        }
        mail.setTo(contact.getEmail().trim());
        mail.setSubject("Lexho SOS Alert");
        mail.setText(smsMessage(event, location));
        mailSender.send(mail);
        return true;
    }

    private String smsMessage(EmergencyEvent event, EmergencyLocation location) {
        StringBuilder builder = new StringBuilder();
        builder.append("SOS from ").append(event.getPartnerName())
                .append(" (").append(event.getPartnerMobileNumber()).append(")")
                .append(". Incident ").append(event.getIncidentCode())
                .append(". Location: ").append(location.getLatitude()).append(",").append(location.getLongitude());
        if (location.getAddress() != null && !location.getAddress().isBlank()) {
            builder.append(". Address: ").append(location.getAddress().trim());
        }
        return builder.toString();
    }
}

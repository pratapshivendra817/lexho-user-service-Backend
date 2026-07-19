package com.lexho.user_service.emergency.service;

import com.lexho.user_service.audit.service.AuditLogService;
import com.lexho.user_service.emergency.dto.EmergencyContactRequest;
import com.lexho.user_service.emergency.dto.EmergencySosRequest;
import com.lexho.user_service.emergency.entity.EmergencyContact;
import com.lexho.user_service.emergency.entity.EmergencyEvent;
import com.lexho.user_service.emergency.entity.EmergencyLocation;
import com.lexho.user_service.emergency.repository.EmergencyContactRepository;
import com.lexho.user_service.emergency.repository.EmergencyEventRepository;
import com.lexho.user_service.emergency.repository.EmergencyLocationRepository;
import com.lexho.user_service.exception.BadRequestException;
import com.lexho.user_service.lexhopartner.entity.PartnerProfile;
import com.lexho.user_service.lexhopartner.repository.PartnerProfileRepository;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class EmergencyServiceTest {

    private final EmergencyEventRepository eventRepository = mock(EmergencyEventRepository.class);
    private final EmergencyContactRepository contactRepository = mock(EmergencyContactRepository.class);
    private final EmergencyLocationRepository locationRepository = mock(EmergencyLocationRepository.class);
    private final PartnerProfileRepository partnerProfileRepository = mock(PartnerProfileRepository.class);
    private final EmergencyAdminNotifier adminNotifier = mock(EmergencyAdminNotifier.class);
    private final EmergencyExternalContactNotifier externalContactNotifier = mock(EmergencyExternalContactNotifier.class);
    private final AuditLogService auditLogService = mock(AuditLogService.class);

    private final EmergencyService service = new EmergencyService(
            eventRepository,
            contactRepository,
            locationRepository,
            partnerProfileRepository,
            adminNotifier,
            externalContactNotifier,
            auditLogService,
            5
    );

    @Test
    void triggerSosPersistsIncidentLocationNotificationsAndAudit() {
        MDC.put("requestId", "req-101");
        MDC.put("correlationId", "corr-101");
        PartnerProfile partner = new PartnerProfile();
        partner.setName("Ravi");
        partner.setMobileNumber("9999999999");
        when(partnerProfileRepository.findById(5L)).thenReturn(Optional.of(partner));
        when(contactRepository.findByPartnerIdAndActiveTrueOrderByPrimaryContactDescCreatedAtAsc(5L))
                .thenReturn(List.of(activeContact(5L, "Asha")));
        when(adminNotifier.notifyAdmins(any(EmergencyEvent.class), any(EmergencyLocation.class))).thenReturn(2);
        when(externalContactNotifier.notifyContacts(any(EmergencyEvent.class), any(EmergencyLocation.class), any()))
                .thenReturn(1);
        when(eventRepository.save(any(EmergencyEvent.class))).thenAnswer(invocation -> {
            EmergencyEvent event = invocation.getArgument(0);
            if (event.getId() == null) {
                ReflectionTestUtils.setField(event, "id", 101L);
            }
            ReflectionTestUtils.setField(event, "createdAt", java.time.LocalDateTime.now());
            ReflectionTestUtils.setField(event, "updatedAt", java.time.LocalDateTime.now());
            event.getLocations().forEach(location -> {
                if (location.getId() == null) {
                    ReflectionTestUtils.setField(location, "id", 501L);
                }
                ReflectionTestUtils.setField(location, "createdAt", java.time.LocalDateTime.now());
                ReflectionTestUtils.setField(location, "updatedAt", java.time.LocalDateTime.now());
            });
            return event;
        });
        when(locationRepository.save(any(EmergencyLocation.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var response = service.triggerSos(5L, new EmergencySosRequest(
                BigDecimal.valueOf(28.6139),
                BigDecimal.valueOf(77.2090),
                BigDecimal.valueOf(4.5),
                "Delhi",
                "Urgent help"
        ));

        assertThat(response.id()).isEqualTo(101L);
        assertThat(response.eventId()).isEqualTo(101L);
        assertThat(response.partnerPhone()).isEqualTo("9999999999");
        assertThat(response.requestId()).isEqualTo("req-101");
        assertThat(response.correlationId()).isEqualTo("corr-101");
        assertThat(response.adminAlertSent()).isTrue();
        assertThat(response.notifiedContactsCount()).isEqualTo(1);
        assertThat(response.location().address()).isEqualTo("Delhi");
        assertThat(response.latestLocation().latitude()).isEqualByComparingTo("28.6139");
        verify(auditLogService).log(5L, "PARTNER", "SOS_TRIGGERED", "EMERGENCY_EVENT", "101", null, response.incidentCode());
        MDC.clear();
    }

    @Test
    void addContactValidatesLimitsAndDuplicates() {
        when(partnerProfileRepository.existsById(5L)).thenReturn(true);
        when(contactRepository.countByPartnerIdAndActiveTrue(5L)).thenReturn(0L);
        when(contactRepository.existsByPartnerIdAndPhoneNumberAndActiveTrue(5L, "9999999999")).thenReturn(false);
        when(contactRepository.save(any(EmergencyContact.class))).thenAnswer(invocation -> {
            EmergencyContact contact = invocation.getArgument(0);
            ReflectionTestUtils.setField(contact, "id", 201L);
            return contact;
        });

        var response = service.addContact(5L, new EmergencyContactRequest(
                " Amit ",
                "9999999999",
                "Brother",
                "amit@example.com",
                true
        ));

        assertThat(response.id()).isEqualTo(201L);
        assertThat(response.name()).isEqualTo("Amit");
        verify(auditLogService).log(5L, "PARTNER", "EMERGENCY_CONTACT_ADDED", "EMERGENCY_CONTACT", "201", null, "9999999999");

        when(contactRepository.countByPartnerIdAndActiveTrue(5L)).thenReturn(5L);
        assertThatThrownBy(() -> service.addContact(5L, new EmergencyContactRequest(
                "Asha",
                "8888888888",
                null,
                null,
                false
        ))).isInstanceOf(BadRequestException.class).hasMessageContaining("Maximum emergency contacts");
    }

    @Test
    void historyReturnsMappedEvents() {
        when(partnerProfileRepository.existsById(5L)).thenReturn(true);
        EmergencyEvent event = new EmergencyEvent();
        ReflectionTestUtils.setField(event, "id", 301L);
        event.setIncidentCode("SOS-ABCD1234");
        event.setPartnerId(5L);
        event.setPartnerName("Ravi");
        event.setPartnerMobileNumber("9999999999");
        ReflectionTestUtils.setField(event, "createdAt", java.time.LocalDateTime.now());
        ReflectionTestUtils.setField(event, "updatedAt", java.time.LocalDateTime.now());
        EmergencyLocation location = new EmergencyLocation();
        ReflectionTestUtils.setField(location, "id", 302L);
        location.setLatitude(BigDecimal.ONE);
        location.setLongitude(BigDecimal.TEN);
        location.setSource("SOS_TRIGGER");
        ReflectionTestUtils.setField(location, "createdAt", java.time.LocalDateTime.now());
        ReflectionTestUtils.setField(location, "updatedAt", java.time.LocalDateTime.now());
        event.addLocation(location);
        when(eventRepository.findByPartnerIdOrderByCreatedAtDesc(5L)).thenReturn(List.of(event));

        var history = service.history(5L);

        assertThat(history).hasSize(1);
        assertThat(history.get(0).latestLocation()).isNotNull();
    }

    private EmergencyContact activeContact(Long partnerId, String name) {
        EmergencyContact contact = new EmergencyContact();
        ReflectionTestUtils.setField(contact, "id", 1L);
        contact.setPartnerId(partnerId);
        contact.setName(name);
        contact.setPhoneNumber("9999999999");
        contact.setActive(true);
        return contact;
    }
}

package com.lexho.user_service.emergency.repository;

import com.lexho.user_service.emergency.entity.EmergencyContact;
import com.lexho.user_service.emergency.entity.EmergencyEvent;
import com.lexho.user_service.emergency.entity.EmergencyEventStatus;
import com.lexho.user_service.emergency.entity.EmergencyLocation;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ContextConfiguration(classes = EmergencyRepositoryIntegrationTest.JpaTestConfig.class)
@TestPropertySource(properties = {
        "spring.flyway.enabled=false",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
class EmergencyRepositoryIntegrationTest {

    @Autowired
    private EmergencyEventRepository emergencyEventRepository;

    @Autowired
    private EmergencyContactRepository emergencyContactRepository;

    @Test
    void eventQueriesShouldLoadLocationsAndSupportAdminSearch() {
        EmergencyEvent triggeredEvent = buildEvent("SOS-1001", 41L, EmergencyEventStatus.TRIGGERED);
        triggeredEvent.addLocation(buildLocation("28.6139000", "77.2090000", "Connaught Place"));
        emergencyEventRepository.saveAndFlush(triggeredEvent);

        EmergencyEvent resolvedEvent = buildEvent("SOS-1002", 41L, EmergencyEventStatus.RESOLVED);
        resolvedEvent.addLocation(buildLocation("28.6328000", "77.2197000", "Karol Bagh"));
        emergencyEventRepository.saveAndFlush(resolvedEvent);

        List<EmergencyEvent> partnerHistory = emergencyEventRepository.findByPartnerIdOrderByCreatedAtDesc(41L);
        assertThat(partnerHistory).hasSize(2);
        assertThat(partnerHistory).allSatisfy(event -> assertThat(event.getLocations()).hasSize(1));

        EmergencyEvent partnerScopedEvent = emergencyEventRepository.findByIdAndPartnerId(
                triggeredEvent.getId(), 41L).orElseThrow();
        assertThat(partnerScopedEvent.getIncidentCode()).isEqualTo("SOS-1001");
        assertThat(partnerScopedEvent.getLocations()).hasSize(1);

        Page<EmergencyEvent> searchResult = emergencyEventRepository.search(
                EmergencyEventStatus.TRIGGERED, 41L, PageRequest.of(0, 10));
        assertThat(searchResult.getContent()).hasSize(1);
        assertThat(searchResult.getContent().get(0).getIncidentCode()).isEqualTo("SOS-1001");

        EmergencyEvent eventWithLocations = emergencyEventRepository.findWithLocationsById(
                resolvedEvent.getId()).orElseThrow();
        assertThat(eventWithLocations.getLocations()).hasSize(1);
        assertThat(eventWithLocations.getLastLatitude()).isEqualByComparingTo("28.6328000");
    }

    @Test
    void contactQueriesShouldRespectActivePrimaryAndDuplicateChecks() {
        EmergencyContact primaryContact = buildContact(41L, "Amit", "8888888888", true, true);
        EmergencyContact secondaryContact = buildContact(41L, "Neha", "7777777777", false, true);
        EmergencyContact inactiveContact = buildContact(41L, "Old Contact", "6666666666", false, false);

        emergencyContactRepository.save(primaryContact);
        emergencyContactRepository.save(secondaryContact);
        emergencyContactRepository.save(inactiveContact);

        List<EmergencyContact> activeContacts =
                emergencyContactRepository.findByPartnerIdAndActiveTrueOrderByPrimaryContactDescCreatedAtAsc(41L);

        assertThat(activeContacts).extracting(EmergencyContact::getPhoneNumber)
                .containsExactly("8888888888", "7777777777");
        assertThat(emergencyContactRepository.countByPartnerIdAndActiveTrue(41L)).isEqualTo(2);
        assertThat(emergencyContactRepository.existsByPartnerIdAndPhoneNumberAndActiveTrue(41L, "8888888888"))
                .isTrue();
        assertThat(emergencyContactRepository.existsByPartnerIdAndPhoneNumberAndActiveTrue(41L, "6666666666"))
                .isFalse();
    }

    private EmergencyEvent buildEvent(String incidentCode, Long partnerId, EmergencyEventStatus status) {
        EmergencyEvent event = new EmergencyEvent();
        event.setIncidentCode(incidentCode);
        event.setPartnerId(partnerId);
        event.setPartnerName("Ravi Partner");
        event.setPartnerMobileNumber("9999999999");
        event.setStatus(status);
        event.setTriggerMessage("SOS test");
        event.setAdminAlertSent(true);
        event.setNotifiedContactsCount(2);
        return event;
    }

    private EmergencyLocation buildLocation(String latitude, String longitude, String address) {
        EmergencyLocation location = new EmergencyLocation();
        location.setLatitude(new BigDecimal(latitude));
        location.setLongitude(new BigDecimal(longitude));
        location.setAccuracyMeters(new BigDecimal("5.50"));
        location.setAddress(address);
        location.setSource("PARTNER_APP");
        return location;
    }

    private EmergencyContact buildContact(Long partnerId,
                                          String name,
                                          String phoneNumber,
                                          boolean primaryContact,
                                          boolean active) {
        EmergencyContact contact = new EmergencyContact();
        contact.setPartnerId(partnerId);
        contact.setName(name);
        contact.setPhoneNumber(phoneNumber);
        contact.setRelationship("Family");
        contact.setEmail(name.toLowerCase() + "@example.com");
        contact.setPrimaryContact(primaryContact);
        contact.setActive(active);
        return contact;
    }

    @SpringBootConfiguration
    @EnableAutoConfiguration
    @EntityScan(basePackageClasses = {
            EmergencyEvent.class,
            EmergencyLocation.class,
            EmergencyContact.class
    })
    @EnableJpaRepositories(basePackageClasses = {
            EmergencyEventRepository.class,
            EmergencyContactRepository.class
    })
    static class JpaTestConfig {
    }
}

package com.lexho.user_service.emergency.service;

import com.lexho.user_service.audit.service.AuditLogService;
import com.lexho.user_service.emergency.dto.EmergencyContactRequest;
import com.lexho.user_service.emergency.dto.EmergencyContactResponse;
import com.lexho.user_service.emergency.dto.EmergencyEventResponse;
import com.lexho.user_service.emergency.dto.EmergencyIncidentLocationView;
import com.lexho.user_service.emergency.dto.EmergencyIncidentPartnerView;
import com.lexho.user_service.emergency.dto.EmergencyLocationResponse;
import com.lexho.user_service.emergency.dto.EmergencySosRequest;
import com.lexho.user_service.emergency.entity.EmergencyContact;
import com.lexho.user_service.emergency.entity.EmergencyEvent;
import com.lexho.user_service.emergency.entity.EmergencyEventStatus;
import com.lexho.user_service.emergency.entity.EmergencyLocation;
import com.lexho.user_service.emergency.repository.EmergencyContactRepository;
import com.lexho.user_service.emergency.repository.EmergencyEventRepository;
import com.lexho.user_service.emergency.repository.EmergencyLocationRepository;
import com.lexho.user_service.exception.BadRequestException;
import com.lexho.user_service.exception.ResourceNotFoundException;
import com.lexho.user_service.lexhopartner.entity.PartnerProfile;
import com.lexho.user_service.lexhopartner.repository.PartnerProfileRepository;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

import static com.lexho.user_service.config.CorrelationIdFilter.MDC_KEY;
import static com.lexho.user_service.config.CorrelationIdFilter.REQUEST_MDC_KEY;

@Service
public class EmergencyService {

    private static final String SOS_SOURCE = "SOS_TRIGGER";

    private final EmergencyEventRepository emergencyEventRepository;
    private final EmergencyContactRepository emergencyContactRepository;
    private final EmergencyLocationRepository emergencyLocationRepository;
    private final PartnerProfileRepository partnerProfileRepository;
    private final EmergencyAdminNotifier emergencyAdminNotifier;
    private final EmergencyExternalContactNotifier emergencyExternalContactNotifier;
    private final AuditLogService auditLogService;
    private final int maxContacts;

    public EmergencyService(EmergencyEventRepository emergencyEventRepository,
                            EmergencyContactRepository emergencyContactRepository,
                            EmergencyLocationRepository emergencyLocationRepository,
                            PartnerProfileRepository partnerProfileRepository,
                            EmergencyAdminNotifier emergencyAdminNotifier,
                            EmergencyExternalContactNotifier emergencyExternalContactNotifier,
                            AuditLogService auditLogService,
                            @org.springframework.beans.factory.annotation.Value("${app.emergency.max-contacts:5}") int maxContacts) {
        this.emergencyEventRepository = emergencyEventRepository;
        this.emergencyContactRepository = emergencyContactRepository;
        this.emergencyLocationRepository = emergencyLocationRepository;
        this.partnerProfileRepository = partnerProfileRepository;
        this.emergencyAdminNotifier = emergencyAdminNotifier;
        this.emergencyExternalContactNotifier = emergencyExternalContactNotifier;
        this.auditLogService = auditLogService;
        this.maxContacts = maxContacts;
    }

    @Transactional
    public EmergencyEventResponse triggerSos(Long partnerId, EmergencySosRequest request) {
        PartnerProfile partner = partnerProfileRepository.findById(partnerId)
                .orElseThrow(() -> new ResourceNotFoundException("Partner not found"));

        EmergencyEvent event = new EmergencyEvent();
        event.setIncidentCode("SOS-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        event.setPartnerId(partnerId);
        event.setPartnerName(partner.getName());
        event.setPartnerMobileNumber(partner.getMobileNumber());
        event.setTriggerMessage(trimToNull(request.message()));
        event.setRequestId(trimToNull(MDC.get(REQUEST_MDC_KEY)));
        event.setCorrelationId(trimToNull(MDC.get(MDC_KEY)));

        EmergencyLocation location = buildLocation(request);
        event.addLocation(location);
        EmergencyEvent savedEvent = emergencyEventRepository.save(event);
        emergencyLocationRepository.save(location);

        int adminAlerts = emergencyAdminNotifier.notifyAdmins(savedEvent, location);
        savedEvent.setAdminAlertSent(adminAlerts > 0);

        List<EmergencyContact> contacts = emergencyContactRepository.findByPartnerIdAndActiveTrueOrderByPrimaryContactDescCreatedAtAsc(partnerId);
        int notifiedContactsCount = emergencyExternalContactNotifier.notifyContacts(savedEvent, location, contacts);
        savedEvent.setNotifiedContactsCount(notifiedContactsCount);

        EmergencyEvent updatedEvent = emergencyEventRepository.save(savedEvent);
        auditLogService.log(partnerId, "PARTNER", "SOS_TRIGGERED", "EMERGENCY_EVENT",
                updatedEvent.getId().toString(), null, updatedEvent.getIncidentCode());
        return toResponse(updatedEvent);
    }

    @Transactional(readOnly = true)
    public List<EmergencyEventResponse> history(Long partnerId) {
        ensurePartnerExists(partnerId);
        return emergencyEventRepository.findByPartnerIdOrderByCreatedAtDesc(partnerId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public EmergencyContactResponse addContact(Long partnerId, EmergencyContactRequest request) {
        ensurePartnerExists(partnerId);
        if (emergencyContactRepository.countByPartnerIdAndActiveTrue(partnerId) >= maxContacts) {
            throw new BadRequestException("Maximum emergency contacts limit reached");
        }
        String normalizedPhone = normalizePhone(request.phoneNumber());
        if (emergencyContactRepository.existsByPartnerIdAndPhoneNumberAndActiveTrue(partnerId, normalizedPhone)) {
            throw new BadRequestException("Emergency contact already exists");
        }

        EmergencyContact contact = new EmergencyContact();
        contact.setPartnerId(partnerId);
        contact.setName(request.name().trim());
        contact.setPhoneNumber(normalizedPhone);
        contact.setRelationship(trimToNull(request.relationship()));
        contact.setEmail(trimToNull(request.email()));
        contact.setPrimaryContact(request.primaryContact());
        contact.setActive(true);
        EmergencyContact saved = emergencyContactRepository.save(contact);
        auditLogService.log(partnerId, "PARTNER", "EMERGENCY_CONTACT_ADDED", "EMERGENCY_CONTACT",
                saved.getId().toString(), null, saved.getPhoneNumber());
        return toResponse(saved);
    }

    private void ensurePartnerExists(Long partnerId) {
        if (!partnerProfileRepository.existsById(partnerId)) {
            throw new ResourceNotFoundException("Partner not found");
        }
    }

    private EmergencyLocation buildLocation(EmergencySosRequest request) {
        EmergencyLocation location = new EmergencyLocation();
        location.setLatitude(request.latitude());
        location.setLongitude(request.longitude());
        location.setAccuracyMeters(request.accuracyMeters());
        location.setAddress(trimToNull(request.address()));
        location.setSource(SOS_SOURCE);
        return location;
    }

    public EmergencyEventResponse toResponse(EmergencyEvent event) {
        List<EmergencyLocationResponse> locations = event.getLocations().stream()
                .sorted((left, right) -> right.getCreatedAt().compareTo(left.getCreatedAt()))
                .map(this::toResponse)
                .toList();
        EmergencyLocationResponse latestLocation = locations.isEmpty() ? null : locations.get(0);
        EmergencyIncidentPartnerView partner = new EmergencyIncidentPartnerView(
                event.getPartnerId(),
                event.getPartnerName(),
                event.getPartnerMobileNumber()
        );
        EmergencyIncidentLocationView location = new EmergencyIncidentLocationView(
                latestLocation == null ? event.getLastLatitude() : latestLocation.latitude(),
                latestLocation == null ? event.getLastLongitude() : latestLocation.longitude(),
                latestLocation == null ? event.getLastAccuracyMeters() : latestLocation.accuracyMeters(),
                latestLocation == null ? event.getLastAddress() : latestLocation.address()
        );
        return new EmergencyEventResponse(
                event.getId(),
                event.getId(),
                event.getIncidentCode(),
                partner,
                event.getPartnerId(),
                event.getPartnerName(),
                event.getPartnerMobileNumber(),
                event.getPartnerMobileNumber(),
                event.getStatus(),
                location.latitude(),
                location.longitude(),
                location.accuracyMeters(),
                location.address(),
                location,
                event.getTriggerMessage(),
                event.getCreatedAt(),
                event.getCreatedAt(),
                event.getAcknowledgementNote(),
                event.getAcknowledgementNote(),
                event.getResolutionNote(),
                event.getResolutionNote(),
                event.getAcknowledgedAt(),
                event.getAcknowledgedBy(),
                event.getResolvedAt(),
                event.getResolvedBy(),
                event.getRequestId(),
                event.getCorrelationId(),
                event.isAdminAlertSent(),
                event.getNotifiedContactsCount(),
                latestLocation,
                locations
        );
    }

    private EmergencyLocationResponse toResponse(EmergencyLocation location) {
        return new EmergencyLocationResponse(
                location.getId(),
                location.getLatitude(),
                location.getLongitude(),
                location.getAccuracyMeters(),
                location.getAddress(),
                location.getSource(),
                location.getCreatedAt()
        );
    }

    private EmergencyContactResponse toResponse(EmergencyContact contact) {
        return new EmergencyContactResponse(
                contact.getId(),
                contact.getPartnerId(),
                contact.getName(),
                contact.getPhoneNumber(),
                contact.getRelationship(),
                contact.getEmail(),
                contact.isPrimaryContact(),
                contact.isActive()
        );
    }

    private String normalizePhone(String phoneNumber) {
        return phoneNumber == null ? null : phoneNumber.trim();
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        return normalized.isEmpty() ? null : normalized;
    }
}

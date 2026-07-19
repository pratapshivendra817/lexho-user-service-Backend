package com.lexho.user_service.emergency.entity;

import com.lexho.user_service.lexhopartner.entity.BaseEntity;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "emergency_event", indexes = {
        @Index(name = "idx_emergency_event_partner_created", columnList = "partner_id,created_at"),
        @Index(name = "idx_emergency_event_incident_code", columnList = "incident_code", unique = true)
})
@Getter
@Setter
public class EmergencyEvent extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "incident_code", nullable = false, length = 50, unique = true)
    private String incidentCode;

    @Column(name = "partner_id", nullable = false)
    private Long partnerId;

    @Column(name = "partner_name", nullable = false, length = 120)
    private String partnerName;

    @Column(name = "partner_mobile_number", nullable = false, length = 20)
    private String partnerMobileNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EmergencyEventStatus status = EmergencyEventStatus.TRIGGERED;

    @Column(name = "trigger_message", length = 500)
    private String triggerMessage;

    @Column(name = "admin_alert_sent", nullable = false)
    private boolean adminAlertSent;

    @Column(name = "notified_contacts_count", nullable = false)
    private int notifiedContactsCount;

    @Column(name = "last_latitude", precision = 10, scale = 7)
    private BigDecimal lastLatitude;

    @Column(name = "last_longitude", precision = 10, scale = 7)
    private BigDecimal lastLongitude;

    @Column(name = "last_accuracy_meters", precision = 8, scale = 2)
    private BigDecimal lastAccuracyMeters;

    @Column(name = "last_address", length = 500)
    private String lastAddress;

    @Column(name = "acknowledgement_note", length = 500)
    private String acknowledgementNote;

    @Column(name = "resolved_note", length = 500)
    private String resolutionNote;

    @Column(name = "acknowledged_at")
    private java.time.LocalDateTime acknowledgedAt;

    @Column(name = "acknowledged_by", length = 120)
    private String acknowledgedBy;

    @Column(name = "resolved_at")
    private java.time.LocalDateTime resolvedAt;

    @Column(name = "resolved_by", length = 120)
    private String resolvedBy;

    @Column(name = "request_id", length = 80)
    private String requestId;

    @Column(name = "correlation_id", length = 80)
    private String correlationId;

    @OneToMany(mappedBy = "event", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<EmergencyLocation> locations = new ArrayList<>();

    public void addLocation(EmergencyLocation location) {
        locations.add(location);
        location.setEvent(this);
        location.setPartnerId(partnerId);
        this.lastLatitude = location.getLatitude();
        this.lastLongitude = location.getLongitude();
        this.lastAccuracyMeters = location.getAccuracyMeters();
        this.lastAddress = location.getAddress();
    }
}

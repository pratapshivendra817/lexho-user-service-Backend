package com.lexho.user_service.lexhopartner.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "partner_documents", indexes = {
        @Index(name = "idx_partner_documents_partner_id", columnList = "partner_id"),
        @Index(name = "idx_partner_documents_type", columnList = "document_type")
})
@Getter
@Setter
public class PartnerDocument extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "partner_id", nullable = false)
    private PartnerProfile partner;

    @Enumerated(EnumType.STRING)
    @Column(name = "document_type", nullable = false, length = 20)
    private DocumentType documentType;

    @Column(name = "document_url", nullable = false, length = 1000)
    private String documentUrl;

    @Enumerated(EnumType.STRING)
    @Column(name = "verification_status", nullable = false, length = 20)
    private VerificationStatus verificationStatus = VerificationStatus.PENDING;

    @Column(name = "review_reason", length = 500)
    private String reviewReason;

    @Column(name = "reviewed_by_admin_id")
    private Long reviewedByAdminId;

    @Column(name = "reviewed_at")
    private LocalDateTime reviewedAt;
}

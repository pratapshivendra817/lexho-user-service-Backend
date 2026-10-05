package com.lexho.user_service.legal.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.LocalDateTime;

@Entity
@Table(name = "legal_acceptances",
        uniqueConstraints = @UniqueConstraint(name = "uk_legal_acceptance_user_doc_version",
                columnNames = {"user_id", "user_type", "document_type", "document_version"}))
public class LegalAcceptance {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "user_id", nullable = false)
    private Long userId;
    @Column(name = "user_type", nullable = false, length = 30)
    private String userType;
    @Column(name = "document_type", nullable = false, length = 80)
    private String documentType;
    @Column(name = "document_version", nullable = false)
    private int documentVersion;
    @Column(name = "accepted_at", nullable = false, updatable = false)
    private LocalDateTime acceptedAt;

    @PrePersist
    public void prePersist() {
        acceptedAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public String getUserType() { return userType; }
    public void setUserType(String userType) { this.userType = userType; }
    public String getDocumentType() { return documentType; }
    public void setDocumentType(String documentType) { this.documentType = documentType; }
    public int getDocumentVersion() { return documentVersion; }
    public void setDocumentVersion(int documentVersion) { this.documentVersion = documentVersion; }
    public LocalDateTime getAcceptedAt() { return acceptedAt; }
}

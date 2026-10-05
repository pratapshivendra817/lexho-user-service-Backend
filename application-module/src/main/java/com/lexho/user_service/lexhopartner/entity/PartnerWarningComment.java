package com.lexho.user_service.lexhopartner.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;

@Entity
@Table(name = "partner_warning_comments")
public class PartnerWarningComment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "warning_id", nullable = false)
    private PartnerPenalty warning;

    @Column(name = "sender_type", nullable = false, length = 20)
    private String senderType;

    @Column(name = "sender_id")
    private Long senderId;

    @Column(nullable = false, length = 2000)
    private String message;

    @JdbcTypeCode(SqlTypes.LONGVARCHAR)
    @Column(name = "attachment_urls", columnDefinition = "LONGTEXT")
    private String attachmentUrls;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    public void onCreate() {
        createdAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public PartnerPenalty getWarning() {
        return warning;
    }

    public void setWarning(PartnerPenalty warning) {
        this.warning = warning;
    }

    public String getSenderType() {
        return senderType;
    }

    public void setSenderType(String senderType) {
        this.senderType = senderType;
    }

    public Long getSenderId() {
        return senderId;
    }

    public void setSenderId(Long senderId) {
        this.senderId = senderId;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getAttachmentUrls() {
        return attachmentUrls;
    }

    public void setAttachmentUrls(String attachmentUrls) {
        this.attachmentUrls = attachmentUrls;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}

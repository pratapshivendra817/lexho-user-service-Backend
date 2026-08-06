package com.lexho.user_service.admin.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;

@Entity
@Table(name = "support_ticket_replies")
public class SupportTicketReply {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ticket_id", nullable = false)
    private SupportTicket ticket;

    @Column(name = "admin_id")
    private Long adminId;

    @Column(name = "sender_type", nullable = false, length = 20)
    private String senderType;

    @Column(nullable = false, length = 2000)
    private String message;

    @JdbcTypeCode(SqlTypes.LONGVARCHAR)
    @Column(name = "attachment_urls", columnDefinition = "TEXT")
    private String attachmentUrls;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    public void onCreate() {
        if (senderType == null || senderType.isBlank()) {
            senderType = "SUPPORT";
        }
        this.createdAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public SupportTicket getTicket() {
        return ticket;
    }

    public void setTicket(SupportTicket ticket) {
        this.ticket = ticket;
    }

    public Long getAdminId() {
        return adminId;
    }

    public void setAdminId(Long adminId) {
        this.adminId = adminId;
    }

    public String getSenderType() {
        return senderType;
    }

    public void setSenderType(String senderType) {
        this.senderType = senderType;
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

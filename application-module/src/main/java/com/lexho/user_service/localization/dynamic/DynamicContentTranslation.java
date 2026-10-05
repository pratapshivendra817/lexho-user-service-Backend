package com.lexho.user_service.localization.dynamic;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.LocalDateTime;

@Entity
@Table(name = "dynamic_content_translation",
        uniqueConstraints = @UniqueConstraint(name = "uk_dynamic_translation_source",
                columnNames = {"entity_type", "entity_id", "field_name", "source_locale", "target_locale", "source_text_hash"}))
public class DynamicContentTranslation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "entity_type", nullable = false, length = 80)
    private String entityType;

    @Column(name = "entity_id", nullable = false, length = 80)
    private String entityId;

    @Column(name = "field_name", nullable = false, length = 120)
    private String fieldName;

    @Column(name = "source_locale", nullable = false, length = 10)
    private String sourceLocale;

    @Column(name = "target_locale", nullable = false, length = 10)
    private String targetLocale;

    @Column(name = "source_text_hash", nullable = false, length = 64, columnDefinition = "CHAR(64)")
    private String sourceTextHash;

    @Column(name = "translated_text", nullable = false, columnDefinition = "TEXT")
    private String translatedText;

    @Column(name = "translation_provider", nullable = false, length = 80)
    private String translationProvider;

    @Enumerated(EnumType.STRING)
    @Column(name = "translation_status", nullable = false, length = 20)
    private TranslationStatus translationStatus = TranslationStatus.ACTIVE;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    public void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    public void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public String getEntityType() { return entityType; }
    public void setEntityType(String entityType) { this.entityType = entityType; }
    public String getEntityId() { return entityId; }
    public void setEntityId(String entityId) { this.entityId = entityId; }
    public String getFieldName() { return fieldName; }
    public void setFieldName(String fieldName) { this.fieldName = fieldName; }
    public String getSourceLocale() { return sourceLocale; }
    public void setSourceLocale(String sourceLocale) { this.sourceLocale = sourceLocale; }
    public String getTargetLocale() { return targetLocale; }
    public void setTargetLocale(String targetLocale) { this.targetLocale = targetLocale; }
    public String getSourceTextHash() { return sourceTextHash; }
    public void setSourceTextHash(String sourceTextHash) { this.sourceTextHash = sourceTextHash; }
    public String getTranslatedText() { return translatedText; }
    public void setTranslatedText(String translatedText) { this.translatedText = translatedText; }
    public String getTranslationProvider() { return translationProvider; }
    public void setTranslationProvider(String translationProvider) { this.translationProvider = translationProvider; }
    public TranslationStatus getTranslationStatus() { return translationStatus; }
    public void setTranslationStatus(TranslationStatus translationStatus) { this.translationStatus = translationStatus; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}

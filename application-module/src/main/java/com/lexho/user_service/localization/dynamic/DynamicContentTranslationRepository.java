package com.lexho.user_service.localization.dynamic;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface DynamicContentTranslationRepository extends JpaRepository<DynamicContentTranslation, Long> {

    Optional<DynamicContentTranslation> findByEntityTypeAndEntityIdAndFieldNameAndSourceLocaleAndTargetLocaleAndSourceTextHashAndTranslationStatus(
            String entityType,
            String entityId,
            String fieldName,
            String sourceLocale,
            String targetLocale,
            String sourceTextHash,
            TranslationStatus translationStatus);
}

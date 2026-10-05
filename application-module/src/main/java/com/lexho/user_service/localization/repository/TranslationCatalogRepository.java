package com.lexho.user_service.localization.repository;

import com.lexho.user_service.localization.entity.TranslationCatalog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface TranslationCatalogRepository extends JpaRepository<TranslationCatalog, Long> {
    List<TranslationCatalog> findByLocaleCodeOrderByTranslationKeyAsc(String localeCode);

    List<TranslationCatalog> findByLocaleCodeIn(Collection<String> localeCodes);

    Optional<TranslationCatalog> findByLocaleCodeAndTranslationKey(String localeCode, String translationKey);
}

package com.lexho.user_service.localization.repository;

import com.lexho.user_service.localization.entity.SupportedLanguage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SupportedLanguageRepository extends JpaRepository<SupportedLanguage, String> {
    List<SupportedLanguage> findByEnabledTrueOrderByCodeAsc();

    Optional<SupportedLanguage> findFirstByDefaultLanguageTrue();
}

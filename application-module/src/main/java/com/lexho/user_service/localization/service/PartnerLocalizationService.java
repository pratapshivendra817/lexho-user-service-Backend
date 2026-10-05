package com.lexho.user_service.localization.service;

import com.lexho.user_service.exception.BadRequestException;
import com.lexho.user_service.localization.LocaleContext;
import com.lexho.user_service.localization.dto.LanguageDto;
import com.lexho.user_service.localization.dto.LanguagesResponse;
import com.lexho.user_service.localization.dto.MissingTranslationReportResponse;
import com.lexho.user_service.localization.dto.PartnerPreferenceResponse;
import com.lexho.user_service.localization.dto.TranslationsResponse;
import com.lexho.user_service.localization.entity.PartnerPreference;
import com.lexho.user_service.localization.entity.SupportedLanguage;
import com.lexho.user_service.localization.entity.TranslationCatalog;
import com.lexho.user_service.localization.repository.PartnerPreferenceRepository;
import com.lexho.user_service.localization.repository.SupportedLanguageRepository;
import com.lexho.user_service.localization.repository.TranslationCatalogRepository;
import com.lexho.user_service.util.StringSanitizers;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class PartnerLocalizationService {

    public static final String FALLBACK_LOCALE = "en-IN";
    private static final String UNSUPPORTED_LANGUAGE = "Unsupported language";

    private final SupportedLanguageRepository languageRepository;
    private final TranslationCatalogRepository translationRepository;
    private final PartnerPreferenceRepository preferenceRepository;

    public PartnerLocalizationService(SupportedLanguageRepository languageRepository,
                                      TranslationCatalogRepository translationRepository,
                                      PartnerPreferenceRepository preferenceRepository) {
        this.languageRepository = languageRepository;
        this.translationRepository = translationRepository;
        this.preferenceRepository = preferenceRepository;
    }

    @Transactional(readOnly = true)
    public LanguagesResponse partnerLanguages() {
        String defaultLocale = defaultLocale();
        List<LanguageDto> languages = languageRepository.findByEnabledTrueOrderByCodeAsc().stream()
                .sorted(languageComparator())
                .map(language -> toDto(language, defaultLocale))
                .toList();
        return new LanguagesResponse(defaultLocale, languages);
    }

    @Transactional(readOnly = true)
    public LanguagesResponse adminLanguages() {
        String defaultLocale = defaultLocale();
        List<LanguageDto> languages = languageRepository.findAll().stream()
                .sorted(languageComparator())
                .map(language -> toDto(language, defaultLocale))
                .toList();
        return new LanguagesResponse(defaultLocale, languages);
    }

    @Transactional(readOnly = true)
    public TranslationsResponse translations(String requestedLocale) {
        return translationsResponse(requestedLocale);
    }

    private TranslationsResponse translationsResponse(String requestedLocale) {
        String locale = resolveEnabledLocale(requestedLocale);
        List<TranslationCatalog> fallbackRows = translationRepository.findByLocaleCodeOrderByTranslationKeyAsc(FALLBACK_LOCALE);
        List<TranslationCatalog> localeRows = FALLBACK_LOCALE.equals(locale)
                ? fallbackRows
                : translationRepository.findByLocaleCodeOrderByTranslationKeyAsc(locale);
        Map<String, String> translations = fallbackRows.stream()
                .collect(Collectors.toMap(TranslationCatalog::getTranslationKey,
                        TranslationCatalog::getTranslationValue,
                        (left, right) -> right,
                        LinkedHashMap::new));
        localeRows.forEach(row -> translations.put(row.getTranslationKey(), row.getTranslationValue()));
        int version = localeRows.stream().mapToInt(TranslationCatalog::getVersion).max()
                .orElseGet(() -> fallbackRows.stream().mapToInt(TranslationCatalog::getVersion).max().orElse(1));
        return new TranslationsResponse(locale, version, translations);
    }

    @Transactional(readOnly = true)
    public TranslationsResponse translationsForPartner(Long partnerId, String requestedLocale) {
        String locale = normalizeLocaleOrNull(requestedLocale);
        if (locale == null) {
            locale = localeForPartnerValue(partnerId);
        }
        return translationsResponse(locale);
    }

    @Transactional
    public PartnerPreferenceResponse updatePartnerPreference(Long partnerId, String requestedLanguage) {
        if (partnerId == null) {
            throw new BadRequestException("Partner id is required");
        }
        String language = normalizeLocale(requestedLanguage);
        SupportedLanguage supported = languageRepository.findById(language)
                .orElseThrow(() -> new BadRequestException(UNSUPPORTED_LANGUAGE));
        if (!supported.isEnabled()) {
            throw new BadRequestException(UNSUPPORTED_LANGUAGE);
        }
        PartnerPreference preference = preferenceRepository.findById(partnerId).orElseGet(PartnerPreference::new);
        preference.setPartnerId(partnerId);
        preference.setLanguageCode(language);
        preferenceRepository.save(preference);
        LocaleContext.setLocale(language);
        return new PartnerPreferenceResponse(language);
    }

    @Transactional
    public LanguageDto updateLanguage(String code, boolean enabled) {
        SupportedLanguage language = languageRepository.findById(normalizeLocale(code))
                .orElseThrow(() -> new BadRequestException(UNSUPPORTED_LANGUAGE));
        if (language.isDefaultLanguage() && !enabled) {
            throw new BadRequestException("Default language cannot be disabled");
        }
        language.setEnabled(enabled);
        SupportedLanguage saved = languageRepository.save(language);
        return toDto(saved, defaultLocale());
    }

    @Transactional
    public LanguageDto setDefaultLanguage(String code) {
        String locale = normalizeLocale(code);
        SupportedLanguage selected = languageRepository.findById(locale)
                .orElseThrow(() -> new BadRequestException(UNSUPPORTED_LANGUAGE));
        if (!selected.isEnabled()) {
            throw new BadRequestException("Default language must be enabled");
        }
        List<SupportedLanguage> languages = languageRepository.findAll();
        for (SupportedLanguage language : languages) {
            language.setDefaultLanguage(language.getCode().equals(locale));
        }
        languageRepository.saveAll(languages);
        return toDto(selected, locale);
    }

    @Transactional
    public TranslationsResponse upsertTranslation(String localeCode, String key, String value) {
        String locale = normalizeLocale(localeCode);
        ensureSupported(locale);
        String normalizedKey = normalizeRequired(key, "key is required");
        String normalizedValue = normalizeRequired(value, "value is required");
        TranslationCatalog translation = translationRepository.findByLocaleCodeAndTranslationKey(locale, normalizedKey)
                .orElseGet(TranslationCatalog::new);
        translation.setLocaleCode(locale);
        translation.setTranslationKey(normalizedKey);
        translation.setTranslationValue(normalizedValue);
        if (translation.getId() != null) {
        translation.setVersion(translation.getVersion() + 1);
        }
        translationRepository.save(translation);
        return translationsResponse(locale);
    }

    @Transactional
    public TranslationsResponse publishTranslations(String localeCode) {
        String locale = normalizeLocale(localeCode);
        ensureSupported(locale);
        List<TranslationCatalog> translations = translationRepository.findByLocaleCodeOrderByTranslationKeyAsc(locale);
        int nextVersion = translations.stream().mapToInt(TranslationCatalog::getVersion).max().orElse(0) + 1;
        for (TranslationCatalog translation : translations) {
            translation.setVersion(nextVersion);
        }
        translationRepository.saveAll(translations);
        return translationsResponse(locale);
    }

    @Transactional(readOnly = true)
    public MissingTranslationReportResponse missingTranslations() {
        Set<String> baseKeys = translationRepository.findByLocaleCodeOrderByTranslationKeyAsc(FALLBACK_LOCALE).stream()
                .map(TranslationCatalog::getTranslationKey)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        Map<String, Set<String>> keysByLocale = translationRepository.findByLocaleCodeIn(
                        languageRepository.findAll().stream().map(SupportedLanguage::getCode).toList()).stream()
                .collect(Collectors.groupingBy(TranslationCatalog::getLocaleCode,
                        Collectors.mapping(TranslationCatalog::getTranslationKey, Collectors.toSet())));

        Map<String, List<String>> missingByLocale = new LinkedHashMap<>();
        languageRepository.findAll().stream()
                .sorted(languageComparator())
                .filter(language -> !FALLBACK_LOCALE.equals(language.getCode()))
                .forEach(language -> {
                    Set<String> localeKeys = keysByLocale.getOrDefault(language.getCode(), Set.of());
                    List<String> missing = new ArrayList<>(baseKeys);
                    missing.removeAll(localeKeys);
                    missingByLocale.put(language.getCode(), missing);
                });
        return new MissingTranslationReportResponse(FALLBACK_LOCALE, missingByLocale);
    }

    @Transactional(readOnly = true)
    public String translate(String key, String fallback) {
        return translateValue(key, fallback);
    }

    private String translateValue(String key, String fallback) {
        String locale = resolveEnabledLocale(LocaleContext.getLocale());
        return translationRepository.findByLocaleCodeAndTranslationKey(locale, key)
                .or(() -> translationRepository.findByLocaleCodeAndTranslationKey(FALLBACK_LOCALE, key))
                .map(TranslationCatalog::getTranslationValue)
                .orElse(fallback == null ? key : fallback);
    }

    @Transactional(readOnly = true)
    public String translateForPartner(Long partnerId, String key, String fallback) {
        String previous = LocaleContext.getLocale();
        try {
            LocaleContext.setLocale(localeForPartnerValue(partnerId));
            return translateValue(key, fallback);
        } finally {
            if (previous == null) {
                LocaleContext.clear();
            } else {
                LocaleContext.setLocale(previous);
            }
        }
    }

    @Transactional(readOnly = true)
    public String translateMessage(String message) {
        if (message == null || message.isBlank()) {
            return message;
        }
        return translateValue("api.message." + slug(message), message);
    }

    public String localeFromHeader(String acceptLanguage) {
        if (acceptLanguage == null || acceptLanguage.isBlank()) {
            return defaultLocale();
        }
        String first = acceptLanguage.split(",")[0].trim();
        int weightIndex = first.indexOf(';');
        if (weightIndex >= 0) {
            first = first.substring(0, weightIndex).trim();
        }
        return resolveEnabledLocale(first);
    }

    @Transactional(readOnly = true)
    public String localeForPartner(Long partnerId) {
        return localeForPartnerValue(partnerId);
    }

    private String localeForPartnerValue(Long partnerId) {
        if (partnerId == null) {
            return FALLBACK_LOCALE;
        }
        return preferenceRepository.findById(partnerId)
                .map(PartnerPreference::getLanguageCode)
                .map(this::resolveEnabledLocale)
                .orElse(FALLBACK_LOCALE);
    }

    @Transactional(readOnly = true)
    public String resolveLocaleForPartner(Long partnerId, String requestedLocale) {
        String locale = normalizeLocaleOrNull(requestedLocale);
        if (locale != null) {
            return resolveEnabledLocale(locale);
        }
        return localeForPartnerValue(partnerId);
    }

    private String resolveEnabledLocale(String requestedLocale) {
        String locale = normalizeLocaleOrNull(requestedLocale);
        if (locale != null) {
            SupportedLanguage language = languageRepository.findById(locale).orElse(null);
            if (language != null && language.isEnabled()) {
                return locale;
            }
        }
        return FALLBACK_LOCALE;
    }

    private String defaultLocale() {
        return languageRepository.findFirstByDefaultLanguageTrue()
                .map(SupportedLanguage::getCode)
                .orElse(FALLBACK_LOCALE);
    }

    private void ensureSupported(String locale) {
        languageRepository.findById(locale).orElseThrow(() -> new BadRequestException(UNSUPPORTED_LANGUAGE));
    }

    private LanguageDto toDto(SupportedLanguage language, String defaultLocale) {
        return new LanguageDto(language.getCode(), language.getName(), language.getNativeName(),
                language.isEnabled(), language.getCode().equals(defaultLocale));
    }

    private Comparator<SupportedLanguage> languageComparator() {
        Map<String, Integer> order = Map.of(
                FALLBACK_LOCALE, 0, "mr-IN", 1, "hi-IN", 2, "gu-IN", 3, "ta-IN", 4,
                "te-IN", 5, "bn-IN", 6, "kn-IN", 7, "ml-IN", 8);
        return Comparator.comparing(language -> order.getOrDefault(language.getCode(), 100));
    }

    private String normalizeLocale(String value) {
        String normalized = normalizeLocaleOrNull(value);
        if (normalized == null) {
            throw new BadRequestException("language is required");
        }
        return normalized;
    }

    private String normalizeLocaleOrNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        Locale locale = Locale.forLanguageTag(value.trim().replace('_', '-'));
        String language = locale.getLanguage();
        String country = locale.getCountry();
        if (language.isBlank() || country.isBlank()) {
            return value.trim();
        }
        return language.toLowerCase(Locale.ROOT) + "-" + country.toUpperCase(Locale.ROOT);
    }

    private String normalizeRequired(String value, String message) {
        return StringSanitizers.requireTrimmed(value, message);
    }

    private String slug(String value) {
        String normalized = value.toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", ".");
        return trimBoundaryCharacter(normalized, '.');
    }

    private String trimBoundaryCharacter(String value, char boundaryCharacter) {
        int start = 0;
        int end = value.length();
        while (start < end && value.charAt(start) == boundaryCharacter) {
            start++;
        }
        while (end > start && value.charAt(end - 1) == boundaryCharacter) {
            end--;
        }
        return value.substring(start, end);
    }
}

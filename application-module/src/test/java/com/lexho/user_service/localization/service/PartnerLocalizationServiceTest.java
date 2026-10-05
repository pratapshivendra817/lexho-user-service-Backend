package com.lexho.user_service.localization.service;

import com.lexho.user_service.exception.BadRequestException;
import com.lexho.user_service.localization.LocaleContext;
import com.lexho.user_service.localization.entity.PartnerPreference;
import com.lexho.user_service.localization.entity.SupportedLanguage;
import com.lexho.user_service.localization.entity.TranslationCatalog;
import com.lexho.user_service.localization.repository.PartnerPreferenceRepository;
import com.lexho.user_service.localization.repository.SupportedLanguageRepository;
import com.lexho.user_service.localization.repository.TranslationCatalogRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PartnerLocalizationServiceTest {

    private final SupportedLanguageRepository languageRepository = mock(SupportedLanguageRepository.class);
    private final TranslationCatalogRepository translationRepository = mock(TranslationCatalogRepository.class);
    private final PartnerPreferenceRepository preferenceRepository = mock(PartnerPreferenceRepository.class);
    private final PartnerLocalizationService service =
            new PartnerLocalizationService(languageRepository, translationRepository, preferenceRepository);

    @AfterEach
    void clearLocale() {
        LocaleContext.clear();
    }

    @Test
    void partnerLanguagesReturnsEnabledLanguagesAndDefaultLocale() {
        when(languageRepository.findFirstByDefaultLanguageTrue()).thenReturn(Optional.of(language("en-IN", true, true)));
        when(languageRepository.findByEnabledTrueOrderByCodeAsc()).thenReturn(List.of(
                language("mr-IN", true, false),
                language("en-IN", true, true)
        ));

        var response = service.partnerLanguages();

        assertThat(response.defaultLocale()).isEqualTo("en-IN");
        assertThat(response.languages()).extracting("code").containsExactly("en-IN", "mr-IN");
        assertThat(response.languages().get(0).isDefault()).isTrue();
    }

    @Test
    void translationsFallsBackToEnglishForMissingOrDisabledLocale() {
        when(languageRepository.findById("mr-IN")).thenReturn(Optional.empty());
        TranslationCatalog translation = translation("en-IN", "profile.title", "Profile", 2);
        when(translationRepository.findByLocaleCodeOrderByTranslationKeyAsc("en-IN")).thenReturn(List.of(translation));

        var response = service.translations("mr-IN");

        assertThat(response.locale()).isEqualTo("en-IN");
        assertThat(response.version()).isEqualTo(2);
        assertThat(response.translations()).containsEntry("profile.title", "Profile");
    }

    @Test
    void translationsForPartnerUsesSavedPreferenceWhenLocaleIsMissing() {
        PartnerPreference preference = new PartnerPreference();
        preference.setPartnerId(7L);
        preference.setLanguageCode("hi-IN");
        when(preferenceRepository.findById(7L)).thenReturn(Optional.of(preference));
        when(languageRepository.findById("hi-IN")).thenReturn(Optional.of(language("hi-IN", true, false)));
        when(translationRepository.findByLocaleCodeOrderByTranslationKeyAsc("en-IN"))
                .thenReturn(List.of(translation("en-IN", "profile.title", "Profile", 1)));
        when(translationRepository.findByLocaleCodeOrderByTranslationKeyAsc("hi-IN"))
                .thenReturn(List.of(translation("hi-IN", "profile.title", "प्रोफाइल", 3)));

        var response = service.translationsForPartner(7L, null);

        assertThat(response.locale()).isEqualTo("hi-IN");
        assertThat(response.version()).isEqualTo(3);
        assertThat(response.translations()).containsEntry("profile.title", "प्रोफाइल");
    }

    @Test
    void updatePartnerPreferenceRejectsDisabledLanguage() {
        when(languageRepository.findById("mr-IN")).thenReturn(Optional.of(language("mr-IN", false, false)));

        assertThrows(BadRequestException.class, () -> service.updatePartnerPreference(7L, "mr-IN"));
    }

    @Test
    void translateForPartnerUsesPreferenceAndRestoresPreviousLocale() {
        PartnerPreference preference = new PartnerPreference();
        preference.setPartnerId(7L);
        preference.setLanguageCode("mr-IN");
        when(preferenceRepository.findById(7L)).thenReturn(Optional.of(preference));
        when(languageRepository.findById("mr-IN")).thenReturn(Optional.of(language("mr-IN", true, false)));
        when(translationRepository.findByLocaleCodeAndTranslationKey("mr-IN", "notification.warning.title"))
                .thenReturn(Optional.of(translation("mr-IN", "notification.warning.title", "नवीन चेतावणी जारी झाली", 1)));

        LocaleContext.setLocale("hi-IN");

        String translated = service.translateForPartner(7L, "notification.warning.title", "New Warning Issued");

        assertThat(translated).isEqualTo("नवीन चेतावणी जारी झाली");
        assertThat(LocaleContext.getLocale()).isEqualTo("hi-IN");
    }

    @Test
    void setDefaultRejectsDisabledLanguage() {
        when(languageRepository.findById("mr-IN")).thenReturn(Optional.of(language("mr-IN", false, false)));

        assertThrows(BadRequestException.class, () -> service.setDefaultLanguage("mr-IN"));
    }

    @Test
    void adminLanguagesIncludesDisabledLanguagesInConfiguredOrder() {
        when(languageRepository.findFirstByDefaultLanguageTrue()).thenReturn(Optional.empty());
        when(languageRepository.findAll()).thenReturn(List.of(
                language("zz-ZZ", false, false),
                language("hi-IN", true, false),
                language("en-IN", true, true)
        ));

        var response = service.adminLanguages();

        assertThat(response.defaultLocale()).isEqualTo("en-IN");
        assertThat(response.languages()).extracting("code").containsExactly("en-IN", "hi-IN", "zz-ZZ");
        assertThat(response.languages()).extracting("enabled").containsExactly(true, true, false);
    }

    @Test
    void updatePartnerPreferenceRequiresPartnerIdAndNormalizesLocale() {
        when(languageRepository.findById("mr-IN")).thenReturn(Optional.of(language("mr-IN", true, false)));
        when(preferenceRepository.save(any(PartnerPreference.class))).thenAnswer(invocation -> invocation.getArgument(0));

        assertThrows(BadRequestException.class, () -> service.updatePartnerPreference(null, "mr-IN"));

        var response = service.updatePartnerPreference(7L, " mr_in ");

        assertThat(response.language()).isEqualTo("mr-IN");
        assertThat(LocaleContext.getLocale()).isEqualTo("mr-IN");
        verify(preferenceRepository).save(any(PartnerPreference.class));
    }

    @Test
    void updateLanguageCannotDisableDefaultButCanEnableSupportedLanguage() {
        SupportedLanguage english = language("en-IN", true, true);
        when(languageRepository.findById("en-IN")).thenReturn(Optional.of(english));

        assertThrows(BadRequestException.class, () -> service.updateLanguage("en-IN", false));

        SupportedLanguage hindi = language("hi-IN", false, false);
        when(languageRepository.findById("hi-IN")).thenReturn(Optional.of(hindi));
        when(languageRepository.save(hindi)).thenReturn(hindi);
        when(languageRepository.findFirstByDefaultLanguageTrue()).thenReturn(Optional.of(english));

        var response = service.updateLanguage("hi-IN", true);

        assertThat(response.code()).isEqualTo("hi-IN");
        assertThat(response.enabled()).isTrue();
        assertThat(response.isDefault()).isFalse();
    }

    @Test
    void setDefaultLanguageClearsOtherDefaultsAndReturnsSelectedLanguage() {
        SupportedLanguage english = language("en-IN", true, true);
        SupportedLanguage hindi = language("hi-IN", true, false);
        when(languageRepository.findById("hi-IN")).thenReturn(Optional.of(hindi));
        when(languageRepository.findAll()).thenReturn(List.of(english, hindi));
        when(languageRepository.saveAll(any())).thenAnswer(invocation -> invocation.getArgument(0));

        var response = service.setDefaultLanguage("hi-IN");

        assertThat(response.code()).isEqualTo("hi-IN");
        assertThat(response.isDefault()).isTrue();
        assertThat(english.isDefaultLanguage()).isFalse();
        assertThat(hindi.isDefaultLanguage()).isTrue();
    }

    @Test
    void upsertTranslationValidatesInputsAndIncrementsExistingVersion() {
        when(languageRepository.findById("hi-IN")).thenReturn(Optional.of(language("hi-IN", true, false)));
        TranslationCatalog existing = translation("hi-IN", "profile.title", "Old", 4);
        existing.setId(22L);
        when(translationRepository.findByLocaleCodeAndTranslationKey("hi-IN", "profile.title"))
                .thenReturn(Optional.of(existing));
        when(translationRepository.findByLocaleCodeOrderByTranslationKeyAsc("en-IN"))
                .thenReturn(List.of(translation("en-IN", "profile.title", "Profile", 1)));
        when(translationRepository.findByLocaleCodeOrderByTranslationKeyAsc("hi-IN"))
                .thenReturn(List.of(existing));

        assertThrows(BadRequestException.class, () -> service.upsertTranslation("hi-IN", " ", "value"));

        var response = service.upsertTranslation("hi-IN", " profile.title ", " नया ");

        assertThat(existing.getVersion()).isEqualTo(5);
        assertThat(existing.getTranslationValue()).isEqualTo("नया");
        assertThat(response.version()).isEqualTo(5);
    }

    @Test
    void publishTranslationsUsesNextVersionAndReturnsMergedTranslations() {
        when(languageRepository.findById("mr-IN")).thenReturn(Optional.of(language("mr-IN", true, false)));
        TranslationCatalog first = translation("mr-IN", "b", "Bee", 2);
        TranslationCatalog second = translation("mr-IN", "a", "Aye", 5);
        when(translationRepository.findByLocaleCodeOrderByTranslationKeyAsc("mr-IN"))
                .thenReturn(List.of(first, second));
        when(translationRepository.findByLocaleCodeOrderByTranslationKeyAsc("en-IN"))
                .thenReturn(List.of(translation("en-IN", "a", "A", 1), translation("en-IN", "c", "C", 1)));

        var response = service.publishTranslations("mr-IN");

        assertThat(first.getVersion()).isEqualTo(6);
        assertThat(second.getVersion()).isEqualTo(6);
        assertThat(response.translations()).containsEntry("a", "Aye").containsEntry("c", "C");
        assertThat(response.version()).isEqualTo(6);
        verify(translationRepository).saveAll(List.of(first, second));
    }

    @Test
    void missingTranslationsReportsBaseKeysAbsentFromEachNonFallbackLanguage() {
        when(languageRepository.findAll()).thenReturn(List.of(
                language("en-IN", true, true),
                language("mr-IN", true, false),
                language("hi-IN", true, false)
        ));
        when(translationRepository.findByLocaleCodeOrderByTranslationKeyAsc("en-IN"))
                .thenReturn(List.of(
                        translation("en-IN", "profile.title", "Profile", 1),
                        translation("en-IN", "button.save", "Save", 1)
                ));
        when(translationRepository.findByLocaleCodeIn(List.of("en-IN", "mr-IN", "hi-IN")))
                .thenReturn(List.of(
                        translation("mr-IN", "profile.title", "Profile MR", 1),
                        translation("hi-IN", "button.save", "Save HI", 1)
                ));

        var response = service.missingTranslations();

        assertThat(response.baseLocale()).isEqualTo("en-IN");
        assertThat(response.missingByLocale()).containsEntry("mr-IN", List.of("button.save"));
        assertThat(response.missingByLocale()).containsEntry("hi-IN", List.of("profile.title"));
    }

    @Test
    void translateUsesContextLocaleThenFallbackThenProvidedFallbackOrKey() {
        when(languageRepository.findById("mr-IN")).thenReturn(Optional.of(language("mr-IN", true, false)));
        when(translationRepository.findByLocaleCodeAndTranslationKey("mr-IN", "button.save"))
                .thenReturn(Optional.empty());
        when(translationRepository.findByLocaleCodeAndTranslationKey("en-IN", "button.save"))
                .thenReturn(Optional.of(translation("en-IN", "button.save", "Save", 1)));
        LocaleContext.setLocale("mr-IN");

        assertThat(service.translate("button.save", "Fallback")).isEqualTo("Save");

        when(translationRepository.findByLocaleCodeAndTranslationKey("mr-IN", "missing.key"))
                .thenReturn(Optional.empty());
        when(translationRepository.findByLocaleCodeAndTranslationKey("en-IN", "missing.key"))
                .thenReturn(Optional.empty());
        assertThat(service.translate("missing.key", null)).isEqualTo("missing.key");
    }

    @Test
    void localeFromHeaderHandlesWeightsAndFallsBackForBlankOrUnsupportedLanguage() {
        when(languageRepository.findFirstByDefaultLanguageTrue()).thenReturn(Optional.of(language("hi-IN", true, true)));
        when(languageRepository.findById("mr-IN")).thenReturn(Optional.of(language("mr-IN", true, false)));
        when(languageRepository.findById("fr-FR")).thenReturn(Optional.empty());

        assertThat(service.localeFromHeader(null)).isEqualTo("hi-IN");
        assertThat(service.localeFromHeader("mr-IN;q=0.8,en-IN;q=0.7")).isEqualTo("mr-IN");
        assertThat(service.localeFromHeader("fr-FR")).isEqualTo("en-IN");
    }

    @Test
    void translateMessageLeavesBlankValuesAndSlugsMessagesForLookup() {
        when(languageRepository.findById("en-IN")).thenReturn(Optional.of(language("en-IN", true, true)));
        when(translationRepository.findByLocaleCodeAndTranslationKey("en-IN", "api.message.saved.ok"))
                .thenReturn(Optional.of(translation("en-IN", "api.message.saved.ok", "Saved", 1)));

        assertThat(service.translateMessage(null)).isNull();
        assertThat(service.translateMessage("   ")).isEqualTo("   ");
        assertThat(service.translateMessage("Saved OK!")).isEqualTo("Saved");
    }

    @Test
    void translateMessageSlugsSeparatorHeavyMessagesWithoutRegexBoundaryBacktracking() {
        when(languageRepository.findById("en-IN")).thenReturn(Optional.of(language("en-IN", true, true)));
        when(translationRepository.findByLocaleCodeAndTranslationKey("en-IN", "api.message.saved.ok"))
                .thenReturn(Optional.of(translation("en-IN", "api.message.saved.ok", "Saved", 1)));

        assertThat(service.translateMessage("...Saved OK!!!")).isEqualTo("Saved");
    }

    private SupportedLanguage language(String code, boolean enabled, boolean isDefault) {
        SupportedLanguage language = new SupportedLanguage();
        language.setCode(code);
        language.setName(code);
        language.setNativeName(code);
        language.setEnabled(enabled);
        language.setDefaultLanguage(isDefault);
        return language;
    }

    private TranslationCatalog translation(String locale, String key, String value, int version) {
        TranslationCatalog translation = new TranslationCatalog();
        translation.setLocaleCode(locale);
        translation.setTranslationKey(key);
        translation.setTranslationValue(value);
        translation.setVersion(version);
        return translation;
    }
}

package com.lexho.user_service.localization.controller;

import com.lexho.user_service.localization.dto.LanguageDto;
import com.lexho.user_service.localization.dto.LanguageUpdateRequest;
import com.lexho.user_service.localization.dto.LanguagesResponse;
import com.lexho.user_service.localization.dto.MissingTranslationReportResponse;
import com.lexho.user_service.localization.dto.TranslationUpsertRequest;
import com.lexho.user_service.localization.dto.TranslationsResponse;
import com.lexho.user_service.localization.service.PartnerLocalizationService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping({"/api/admin/partner/localization", "/api/v1/admin/partner/localization"})
@PreAuthorize("hasRole('ADMIN')")
public class AdminPartnerLocalizationController {

    private final PartnerLocalizationService localizationService;

    public AdminPartnerLocalizationController(PartnerLocalizationService localizationService) {
        this.localizationService = localizationService;
    }

    @GetMapping("/languages")
    public LanguagesResponse languages() {
        return localizationService.adminLanguages();
    }

    @PatchMapping("/languages/{code}")
    public LanguageDto updateLanguage(@PathVariable String code,
                                      @Valid @RequestBody LanguageUpdateRequest request) {
        return localizationService.updateLanguage(code, request.enabled());
    }

    @PatchMapping("/languages/{code}/default")
    public LanguageDto setDefault(@PathVariable String code) {
        return localizationService.setDefaultLanguage(code);
    }

    @GetMapping("/translations")
    public TranslationsResponse translations(@RequestParam String locale) {
        return localizationService.translations(locale);
    }

    @PutMapping("/translations")
    public TranslationsResponse upsertTranslation(@RequestParam String locale,
                                                  @Valid @RequestBody TranslationUpsertRequest request) {
        return localizationService.upsertTranslation(locale, request.key(), request.value());
    }

    @PostMapping("/translations/publish")
    public TranslationsResponse publish(@RequestParam String locale) {
        return localizationService.publishTranslations(locale);
    }

    @GetMapping("/translations/missing")
    public MissingTranslationReportResponse missing() {
        return localizationService.missingTranslations();
    }
}

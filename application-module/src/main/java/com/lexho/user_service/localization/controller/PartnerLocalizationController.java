package com.lexho.user_service.localization.controller;

import com.lexho.user_service.localization.dto.LanguagesResponse;
import com.lexho.user_service.localization.dto.PartnerPreferenceRequest;
import com.lexho.user_service.localization.dto.PartnerPreferenceResponse;
import com.lexho.user_service.localization.dto.TranslationsResponse;
import com.lexho.user_service.localization.service.PartnerLocalizationService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping({"/api/partner", "/api/v1/partner"})
@PreAuthorize("hasRole('PARTNER')")
public class PartnerLocalizationController {

    private final PartnerLocalizationService localizationService;

    public PartnerLocalizationController(PartnerLocalizationService localizationService) {
        this.localizationService = localizationService;
    }

    @GetMapping("/app-settings/languages")
    public LanguagesResponse languages() {
        return localizationService.partnerLanguages();
    }

    @GetMapping("/translations")
    public TranslationsResponse translations(@RequestAttribute("userId") Long partnerId,
                                             @RequestParam(required = false) String locale) {
        return localizationService.translationsForPartner(partnerId, locale);
    }

    @PatchMapping("/preferences")
    public PartnerPreferenceResponse preferences(@RequestAttribute("userId") Long partnerId,
                                                 @Valid @RequestBody PartnerPreferenceRequest request) {
        return localizationService.updatePartnerPreference(partnerId, request.language());
    }
}

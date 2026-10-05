package com.lexho.user_service.localization.dto;

import java.util.List;

public record LanguagesResponse(String defaultLocale, List<LanguageDto> languages) {
}

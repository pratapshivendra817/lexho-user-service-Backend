package com.lexho.user_service.localization.dto;

import java.util.Map;

public record TranslationsResponse(String locale, int version, Map<String, String> translations) {
}

package com.lexho.user_service.localization.dto;

import java.util.List;
import java.util.Map;

public record MissingTranslationReportResponse(String baseLocale, Map<String, List<String>> missingByLocale) {
}

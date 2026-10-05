package com.lexho.user_service.localization.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record LanguageDto(String code,
                          String name,
                          String nativeName,
                          boolean enabled,
                          @JsonProperty("isDefault") boolean isDefault) {
}

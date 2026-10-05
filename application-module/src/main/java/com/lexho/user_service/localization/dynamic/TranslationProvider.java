package com.lexho.user_service.localization.dynamic;

public interface TranslationProvider {

    TranslationResult translate(TranslationRequest request);
}

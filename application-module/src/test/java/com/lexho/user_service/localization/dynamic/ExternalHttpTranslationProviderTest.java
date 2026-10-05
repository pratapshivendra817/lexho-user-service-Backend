package com.lexho.user_service.localization.dynamic;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ExternalHttpTranslationProviderTest {

    @Test
    void disabledProviderRejectsTranslationBeforeNetworkCall() {
        DynamicTranslationProperties properties = new DynamicTranslationProperties();
        ExternalHttpTranslationProvider provider = new ExternalHttpTranslationProvider(properties, new ObjectMapper());

        assertThatThrownBy(() -> provider.translate(request()))
                .isInstanceOf(TranslationProviderException.class)
                .hasMessageContaining("disabled");
    }

    @Test
    void providerConfigurationValidatesUrlAndAllowlistedHost() {
        DynamicTranslationProperties properties = new DynamicTranslationProperties();
        properties.getProvider().setEnabled(true);
        ExternalHttpTranslationProvider provider = new ExternalHttpTranslationProvider(properties, new ObjectMapper());

        assertThatThrownBy(() -> provider.translate(request()))
                .isInstanceOf(TranslationProviderException.class)
                .hasMessageContaining("URL is not configured");

        properties.getProvider().setUrl("http://translation.example.test/api");
        properties.getProvider().setAllowedHosts(List.of("translation.example.test"));
        assertThatThrownBy(() -> provider.translate(request()))
                .isInstanceOf(TranslationProviderException.class)
                .hasMessageContaining("must use HTTPS");

        properties.getProvider().setUrl("https://translation.example.test/api");
        properties.getProvider().setAllowedHosts(List.of("other.example.test"));
        assertThatThrownBy(() -> provider.translate(request()))
                .isInstanceOf(TranslationProviderException.class)
                .hasMessageContaining("host is not allowlisted");
    }

    @Test
    void dynamicTranslationPropertiesApplyDefaultsAndBounds() {
        DynamicTranslationProperties properties = new DynamicTranslationProperties();

        properties.setCacheTtl(null);
        properties.setProvider(null);
        properties.getProvider().setTimeout(null);
        properties.getProvider().setBackoff(null);
        properties.getProvider().setAllowedHosts(null);
        properties.getProvider().setMaxAttempts(99);

        assertThat(properties.getCacheTtl()).isEqualTo(Duration.ofDays(7));
        assertThat(properties.getProvider().getTimeout()).isEqualTo(Duration.ofSeconds(3));
        assertThat(properties.getProvider().getBackoff()).isEqualTo(Duration.ofMillis(200));
        assertThat(properties.getProvider().getAllowedHosts()).isEmpty();
        assertThat(properties.getProvider().getMaxAttempts()).isEqualTo(5);

        properties.getProvider().setMaxAttempts(-1);

        assertThat(properties.getProvider().getMaxAttempts()).isEqualTo(1);
    }

    private TranslationRequest request() {
        return new TranslationRequest("en-IN", "hi-IN", "Cleaning", "service", "name");
    }
}

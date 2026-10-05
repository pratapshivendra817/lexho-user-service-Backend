package com.lexho.user_service.localization.dynamic;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

@Component
public class ExternalHttpTranslationProvider implements TranslationProvider {

    private static final Logger log = LoggerFactory.getLogger(ExternalHttpTranslationProvider.class);

    private final DynamicTranslationProperties properties;
    private final ObjectMapper objectMapper;

    public ExternalHttpTranslationProvider(DynamicTranslationProperties properties, ObjectMapper objectMapper) {
        this.properties = properties;
        this.objectMapper = objectMapper;
    }

    @Override
    public TranslationResult translate(TranslationRequest request) {
        DynamicTranslationProperties.Provider provider = properties.getProvider();
        if (!provider.isEnabled()) {
            throw new TranslationProviderException("Dynamic translation provider is disabled");
        }
        URI uri = validateProviderUri(provider);
        HttpClient client = HttpClient.newBuilder()
                .connectTimeout(provider.getTimeout())
                .build();

        RuntimeException lastFailure = null;
        for (int attempt = 1; attempt <= provider.getMaxAttempts(); attempt++) {
            try {
                HttpRequest httpRequest = buildRequest(provider, uri, request);
                HttpResponse<String> response = client.send(httpRequest, HttpResponse.BodyHandlers.ofString());
                return successfulResult(response, provider);
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                throw new TranslationProviderException("Translation provider call interrupted", exception);
            } catch (RuntimeException | java.io.IOException exception) {
                lastFailure = translationFailure(exception);
                log.warn("Dynamic translation provider attempt failed provider={} attempt={} targetLocale={} entityType={} fieldName={}",
                        providerName(provider), attempt, request.targetLocale(), request.entityType(), request.fieldName());
                sleepBeforeRetry(provider.getBackoff(), attempt, provider.getMaxAttempts());
            }
        }
        throw new TranslationProviderException("Translation provider failed after bounded retries", lastFailure);
    }

    private TranslationResult successfulResult(HttpResponse<String> response,
                                               DynamicTranslationProperties.Provider provider) {
        validateStatus(response.statusCode());
        String translated = parseTranslatedText(response.body());
        if (translated == null || translated.isBlank()) {
            throw new TranslationProviderException("Translation provider returned empty translation");
        }
        return new TranslationResult(translated.trim(), providerName(provider));
    }

    private void validateStatus(int statusCode) {
        if (statusCode == 429 || statusCode >= 500) {
            throw new TranslationProviderException("Translation provider returned retryable status " + statusCode);
        }
        if (statusCode < 200 || statusCode >= 300) {
            throw new TranslationProviderException("Translation provider returned status " + statusCode);
        }
    }

    private RuntimeException translationFailure(Exception exception) {
        return exception instanceof RuntimeException runtime
                ? runtime
                : new TranslationProviderException("Translation provider I/O failure", exception);
    }

    private HttpRequest buildRequest(DynamicTranslationProperties.Provider provider,
                                     URI uri,
                                     TranslationRequest request) {
        try {
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("sourceLocale", request.sourceLocale());
            payload.put("targetLocale", request.targetLocale());
            payload.put("text", request.text());
            if (provider.getModel() != null && !provider.getModel().isBlank()) {
                payload.put("model", provider.getModel());
            }
            HttpRequest.Builder builder = HttpRequest.newBuilder(uri)
                    .timeout(provider.getTimeout())
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(payload)));
            if (provider.getApiKey() != null && !provider.getApiKey().isBlank()) {
                builder.header("Authorization", "Bearer " + provider.getApiKey());
            }
            return builder.build();
        } catch (Exception exception) {
            throw new TranslationProviderException("Unable to build translation provider request", exception);
        }
    }

    private String parseTranslatedText(String body) {
        try {
            JsonNode root = objectMapper.readTree(body);
            JsonNode translatedText = root.path("translatedText");
            if (translatedText.isMissingNode()) {
                translatedText = root.path("translation");
            }
            return translatedText.isTextual() ? translatedText.asText() : null;
        } catch (Exception exception) {
            throw new TranslationProviderException("Unable to parse translation provider response", exception);
        }
    }

    private URI validateProviderUri(DynamicTranslationProperties.Provider provider) {
        if (provider.getUrl() == null || provider.getUrl().isBlank()) {
            throw new TranslationProviderException("Translation provider URL is not configured");
        }
        URI uri = URI.create(provider.getUrl().trim());
        if (!"https".equalsIgnoreCase(uri.getScheme())) {
            throw new TranslationProviderException("Translation provider URL must use HTTPS");
        }
        String host = uri.getHost() == null ? "" : uri.getHost().toLowerCase(Locale.ROOT);
        boolean allowed = provider.getAllowedHosts().stream()
                .map(value -> value == null ? "" : value.toLowerCase(Locale.ROOT).trim())
                .anyMatch(host::equals);
        if (!allowed) {
            throw new TranslationProviderException("Translation provider host is not allowlisted");
        }
        return uri;
    }

    private String providerName(DynamicTranslationProperties.Provider provider) {
        return provider.getName() == null || provider.getName().isBlank() ? "external" : provider.getName().trim();
    }

    private void sleepBeforeRetry(Duration backoff, int attempt, int maxAttempts) {
        if (attempt >= maxAttempts || backoff.isZero() || backoff.isNegative()) {
            return;
        }
        try {
            Thread.sleep(backoff.toMillis());
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new TranslationProviderException("Translation provider retry interrupted", exception);
        }
    }
}

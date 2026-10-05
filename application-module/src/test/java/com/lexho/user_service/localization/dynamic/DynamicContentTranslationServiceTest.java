package com.lexho.user_service.localization.dynamic;

import com.lexho.user_service.localization.service.PartnerLocalizationService;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DynamicContentTranslationServiceTest {

    private DynamicContentTranslationRepository repository;
    private StringRedisTemplate redisTemplate;
    private ValueOperations<String, String> valueOperations;
    private PartnerLocalizationService localizationService;
    private DynamicTranslationProperties properties;
    private AtomicInteger providerCalls;
    private AtomicReference<DynamicContentTranslation> savedTranslation;
    private DynamicContentTranslationService service;

    @BeforeEach
    void setUp() {
        repository = mock(DynamicContentTranslationRepository.class);
        redisTemplate = mock(StringRedisTemplate.class);
        valueOperations = mock(ValueOperations.class);
        localizationService = mock(PartnerLocalizationService.class);
        properties = new DynamicTranslationProperties();
        properties.setCacheTtl(Duration.ofMinutes(30));
        providerCalls = new AtomicInteger();
        savedTranslation = new AtomicReference<>();
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(localizationService.resolveLocaleForPartner(7L, null)).thenReturn("hi-IN");
        when(localizationService.resolveLocaleForPartner(7L, "bad-locale")).thenReturn("en-IN");
        when(repository.save(any(DynamicContentTranslation.class))).thenAnswer(invocation -> {
            DynamicContentTranslation row = invocation.getArgument(0);
            savedTranslation.set(row);
            return row;
        });
        when(repository.findByEntityTypeAndEntityIdAndFieldNameAndSourceLocaleAndTargetLocaleAndSourceTextHashAndTranslationStatus(
                anyString(), anyString(), anyString(), anyString(), anyString(), anyString(), any()))
                .thenAnswer(invocation -> {
                    DynamicContentTranslation row = savedTranslation.get();
                    if (row == null) {
                        return Optional.empty();
                    }
                    return matches(row, invocation) ? Optional.of(row) : Optional.empty();
                });
        TranslationProvider provider = request -> {
            providerCalls.incrementAndGet();
            return new TranslationResult("हिंदी सेवा", "unit-test");
        };
        service = new DynamicContentTranslationService(repository, redisTemplate, provider, localizationService,
                properties, new DynamicTranslationMetrics(new SimpleMeterRegistry()));
    }

    @Test
    void englishLocaleReturnsOriginalContent() {
        String translated = service.translate("en-IN", "service", 10L, "name", "Cleaning");

        assertThat(translated).isEqualTo("Cleaning");
        verify(repository, never()).save(any());
        assertThat(providerCalls).hasValue(0);
    }

    @Test
    void hindiLocaleReturnsDbTranslationAndPopulatesCache() {
        DynamicContentTranslation row = translation("service", "10", "name", "hi-IN", "Cleaning", "सफाई");
        savedTranslation.set(row);

        String translated = service.translate("hi-IN", "service", 10L, "name", "Cleaning");

        assertThat(translated).isEqualTo("सफाई");
        verify(valueOperations).set(anyString(), eq("सफाई"), eq(Duration.ofMinutes(30)));
        assertThat(providerCalls).hasValue(0);
    }

    @Test
    void redisCacheHitAvoidsDbAndProviderCall() {
        when(valueOperations.get(anyString())).thenReturn("कैश सेवा");

        String translated = service.translate("hi-IN", "service", 10L, "name", "Cleaning");

        assertThat(translated).isEqualTo("कैश सेवा");
        verify(repository, never()).findByEntityTypeAndEntityIdAndFieldNameAndSourceLocaleAndTargetLocaleAndSourceTextHashAndTranslationStatus(
                anyString(), anyString(), anyString(), anyString(), anyString(), anyString(), any());
        assertThat(providerCalls).hasValue(0);
    }

    @Test
    void missingTranslationCallsProviderPersistsAndCachesResult() {
        String translated = service.translate("hi-IN", "service", 10L, "name", "Cleaning");

        assertThat(translated).isEqualTo("हिंदी सेवा");
        assertThat(savedTranslation.get().getTranslatedText()).isEqualTo("हिंदी सेवा");
        assertThat(savedTranslation.get().getTranslationProvider()).isEqualTo("unit-test");
        verify(valueOperations).set(anyString(), eq("हिंदी सेवा"), eq(Duration.ofMinutes(30)));
        assertThat(providerCalls).hasValue(1);
    }

    @Test
    void providerFailureFallsBackToEnglish() {
        service = new DynamicContentTranslationService(repository, redisTemplate, request -> {
            throw new TranslationProviderException("down");
        }, localizationService, properties, new DynamicTranslationMetrics(new SimpleMeterRegistry()));

        String translated = service.translate("hi-IN", "service", 10L, "name", "Cleaning");

        assertThat(translated).isEqualTo("Cleaning");
        verify(repository, never()).save(any());
    }

    @Test
    void partnerSavedLanguageIsUsedWhenRequestLocaleIsMissing() {
        String translated = service.translateForPartner(7L, null, "service", 10L, "name", "Cleaning");

        assertThat(translated).isEqualTo("हिंदी सेवा");
        verify(localizationService).resolveLocaleForPartner(7L, null);
    }

    @Test
    void invalidLocaleFallsBackSafelyToEnglishThroughResolver() {
        String translated = service.translateForPartner(7L, "bad-locale", "service", 10L, "name", "Cleaning");

        assertThat(translated).isEqualTo("Cleaning");
        assertThat(providerCalls).hasValue(0);
    }

    @Test
    void sourceTextModificationDoesNotUseOldTranslation() {
        savedTranslation.set(translation("service", "10", "name", "hi-IN", "Cleaning", "पुरानी सफाई"));

        String translated = service.translate("hi-IN", "service", 10L, "name", "Deep Cleaning");

        assertThat(translated).isEqualTo("हिंदी सेवा");
        assertThat(providerCalls).hasValue(1);
    }

    @Test
    void concurrentRequestsGenerateOneProviderCall() throws Exception {
        CountDownLatch ready = new CountDownLatch(5);
        CountDownLatch start = new CountDownLatch(1);
        var executor = Executors.newFixedThreadPool(5);
        for (int i = 0; i < 5; i++) {
            executor.submit(() -> {
                ready.countDown();
                start.await(2, TimeUnit.SECONDS);
                return service.translate("hi-IN", "service", 10L, "name", "Cleaning");
            });
        }

        assertThat(ready.await(2, TimeUnit.SECONDS)).isTrue();
        start.countDown();
        executor.shutdown();
        assertThat(executor.awaitTermination(5, TimeUnit.SECONDS)).isTrue();

        assertThat(providerCalls).hasValue(1);
    }

    @Test
    void redisTtlIsConfigurable() {
        properties.setCacheTtl(Duration.ofHours(6));

        service.translate("hi-IN", "service", 10L, "name", "Cleaning");

        verify(valueOperations).set(anyString(), eq("हिंदी सेवा"), eq(Duration.ofHours(6)));
    }

    @Test
    void blankEnglishTextIsReturnedAsIsWithoutCacheRepositoryOrProvider() {
        assertThat(service.translate("hi-IN", "service", 10L, "name", null)).isNull();
        assertThat(service.translate("hi-IN", "service", 10L, "name", "   ")).isEqualTo("   ");

        verify(redisTemplate, never()).opsForValue();
        verify(repository, never()).save(any());
        assertThat(providerCalls).hasValue(0);
    }

    @Test
    void invalidEntityTokensAreRejectedBeforeProviderCall() {
        assertThatThrownBy(() -> service.translate("hi-IN", " ", 10L, "name", "Cleaning"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("entityType");
        assertThatThrownBy(() -> service.translate("hi-IN", "service", null, "name", "Cleaning"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("entityId");
        assertThatThrownBy(() -> service.translate("hi-IN", "service", 10L, " ", "Cleaning"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("fieldName");

        verify(repository, never()).save(any());
        assertThat(providerCalls).hasValue(0);
    }

    @Test
    void tokenNormalizationLowercasesAndReplacesUnsafeCharactersInCacheKey() {
        service.translate("hi-IN", "Service Type", "ID 10", "Display Name", "Cleaning");

        DynamicContentTranslation saved = savedTranslation.get();
        assertThat(saved.getEntityType()).isEqualTo("service_type");
        assertThat(saved.getEntityId()).isEqualTo("id_10");
        assertThat(saved.getFieldName()).isEqualTo("display_name");
    }

    @Test
    void redisReadAndWriteFailuresFallBackToRepositoryAndProvider() {
        when(valueOperations.get(anyString())).thenThrow(new RedisConnectionFailureException("down"));
        doThrow(new RedisConnectionFailureException("down"))
                .when(valueOperations).set(anyString(), anyString(), any(Duration.class));

        String translated = service.translate("hi-IN", "service", 10L, "name", "Cleaning");

        assertThat(translated).isEqualTo(savedTranslation.get().getTranslatedText());
        assertThat(providerCalls).hasValue(1);
    }

    @Test
    void duplicateInsertReturnsConcurrentPersistedTranslationWhenAvailable() {
        DynamicContentTranslation concurrent = translation("service", "10", "name", "hi-IN",
                "Cleaning", "concurrent value");
        when(repository.save(any(DynamicContentTranslation.class)))
                .thenThrow(new DataIntegrityViolationException("duplicate"));
        when(repository.findByEntityTypeAndEntityIdAndFieldNameAndSourceLocaleAndTargetLocaleAndSourceTextHashAndTranslationStatus(
                anyString(), anyString(), anyString(), anyString(), anyString(), anyString(), any()))
                .thenReturn(Optional.empty(), Optional.empty(), Optional.of(concurrent));

        String translated = service.translate("hi-IN", "service", 10L, "name", "Cleaning");

        assertThat(translated).isEqualTo("concurrent value");
        assertThat(providerCalls).hasValue(1);
    }

    @Test
    void duplicateInsertFallsBackToGeneratedValueWhenConcurrentRowIsMissing() {
        when(repository.save(any(DynamicContentTranslation.class)))
                .thenThrow(new DataIntegrityViolationException("duplicate"));
        when(repository.findByEntityTypeAndEntityIdAndFieldNameAndSourceLocaleAndTargetLocaleAndSourceTextHashAndTranslationStatus(
                anyString(), anyString(), anyString(), anyString(), anyString(), anyString(), any()))
                .thenReturn(Optional.empty(), Optional.empty(), Optional.empty());

        String translated = service.translate("hi-IN", "service", 10L, "name", "Cleaning");

        assertThat(translated).isNotEqualTo("Cleaning");
        assertThat(providerCalls).hasValue(1);
    }

    private DynamicContentTranslation translation(String entityType,
                                                  String entityId,
                                                  String fieldName,
                                                  String targetLocale,
                                                  String sourceText,
                                                  String translatedText) {
        DynamicContentTranslation row = new DynamicContentTranslation();
        row.setEntityType(entityType);
        row.setEntityId(entityId);
        row.setFieldName(fieldName);
        row.setSourceLocale("en-IN");
        row.setTargetLocale(targetLocale);
        row.setSourceTextHash(service.sourceHash(sourceText));
        row.setTranslatedText(translatedText);
        row.setTranslationProvider("unit-test");
        row.setTranslationStatus(TranslationStatus.ACTIVE);
        return row;
    }

    private boolean matches(DynamicContentTranslation row, org.mockito.invocation.InvocationOnMock invocation) {
        return row.getEntityType().equals(invocation.getArgument(0))
                && row.getEntityId().equals(invocation.getArgument(1))
                && row.getFieldName().equals(invocation.getArgument(2))
                && row.getSourceLocale().equals(invocation.getArgument(3))
                && row.getTargetLocale().equals(invocation.getArgument(4))
                && row.getSourceTextHash().equals(invocation.getArgument(5))
                && row.getTranslationStatus() == invocation.getArgument(6);
    }
}

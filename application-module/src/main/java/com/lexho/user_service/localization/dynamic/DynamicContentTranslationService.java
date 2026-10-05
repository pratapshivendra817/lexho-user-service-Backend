package com.lexho.user_service.localization.dynamic;

import com.lexho.user_service.localization.service.PartnerLocalizationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class DynamicContentTranslationService {

    public static final String SOURCE_LOCALE = PartnerLocalizationService.FALLBACK_LOCALE;
    private static final Logger log = LoggerFactory.getLogger(DynamicContentTranslationService.class);

    private final DynamicContentTranslationRepository translationRepository;
    private final StringRedisTemplate redisTemplate;
    private final TranslationProvider translationProvider;
    private final PartnerLocalizationService localizationService;
    private final DynamicTranslationProperties properties;
    private final DynamicTranslationMetrics metrics;
    private final Map<String, Object> locks = new ConcurrentHashMap<>();

    public DynamicContentTranslationService(DynamicContentTranslationRepository translationRepository,
                                            StringRedisTemplate redisTemplate,
                                            TranslationProvider translationProvider,
                                            PartnerLocalizationService localizationService,
                                            DynamicTranslationProperties properties,
                                            DynamicTranslationMetrics metrics) {
        this.translationRepository = translationRepository;
        this.redisTemplate = redisTemplate;
        this.translationProvider = translationProvider;
        this.localizationService = localizationService;
        this.properties = properties;
        this.metrics = metrics;
    }

    @Transactional
    public String translateForPartner(Long partnerId,
                                      String requestedLocale,
                                      String entityType,
                                      Object entityId,
                                      String fieldName,
                                      String englishText) {
        String targetLocale = localizationService.resolveLocaleForPartner(partnerId, requestedLocale);
        return translateText(targetLocale, entityType, entityId, fieldName, englishText);
    }

    @Transactional
    public String translate(String targetLocale,
                            String entityType,
                            Object entityId,
                            String fieldName,
                            String englishText) {
        return translateText(targetLocale, entityType, entityId, fieldName, englishText);
    }

    private String translateText(String targetLocale,
                                 String entityType,
                                 Object entityId,
                                 String fieldName,
                                 String englishText) {
        if (englishText == null || englishText.isBlank()) {
            return englishText;
        }
        String locale = normalize(targetLocale);
        if (SOURCE_LOCALE.equals(locale)) {
            return englishText;
        }
        String normalizedEntityType = normalizeToken(entityType, "entityType");
        String normalizedEntityId = normalizeToken(entityId == null ? null : String.valueOf(entityId), "entityId");
        String normalizedField = normalizeToken(fieldName, "fieldName");
        String sourceHash = sourceHash(englishText);
        String cacheKey = cacheKey(normalizedEntityType, normalizedEntityId, normalizedField, SOURCE_LOCALE, locale, sourceHash);

        String cached = readCache(cacheKey);
        if (cached != null) {
            metrics.cacheHit();
            return cached;
        }
        metrics.cacheMiss();

        return translationRepository
                .findByEntityTypeAndEntityIdAndFieldNameAndSourceLocaleAndTargetLocaleAndSourceTextHashAndTranslationStatus(
                        normalizedEntityType, normalizedEntityId, normalizedField, SOURCE_LOCALE, locale, sourceHash, TranslationStatus.ACTIVE)
                .map(existing -> {
                    metrics.dbHit();
                    writeCache(cacheKey, existing.getTranslatedText());
                    return existing.getTranslatedText();
                })
                .orElseGet(() -> translateWithLock(cacheKey, normalizedEntityType, normalizedEntityId,
                        normalizedField, locale, sourceHash, englishText));
    }

    private String translateWithLock(String cacheKey,
                                     String entityType,
                                     String entityId,
                                     String fieldName,
                                     String targetLocale,
                                     String sourceHash,
                                     String englishText) {
        Object lock = locks.computeIfAbsent(cacheKey, key -> new Object());
        synchronized (lock) {
            try {
                String cached = readCache(cacheKey);
                if (cached != null) {
                    metrics.cacheHit();
                    return cached;
                }
                var existing = translationRepository
                        .findByEntityTypeAndEntityIdAndFieldNameAndSourceLocaleAndTargetLocaleAndSourceTextHashAndTranslationStatus(
                                entityType, entityId, fieldName, SOURCE_LOCALE, targetLocale, sourceHash, TranslationStatus.ACTIVE);
                if (existing.isPresent()) {
                    metrics.dbHit();
                    writeCache(cacheKey, existing.get().getTranslatedText());
                    return existing.get().getTranslatedText();
                }
                return generateAndPersist(entityType, entityId, fieldName, targetLocale, sourceHash, englishText, cacheKey);
            } finally {
                locks.remove(cacheKey, lock);
            }
        }
    }

    private String generateAndPersist(String entityType,
                                      String entityId,
                                      String fieldName,
                                      String targetLocale,
                                      String sourceHash,
                                      String englishText,
                                      String cacheKey) {
        try {
            metrics.providerRequest();
            TranslationResult result = translationProvider.translate(
                    new TranslationRequest(SOURCE_LOCALE, targetLocale, englishText, entityType, fieldName));
            String translated = result.translatedText();
            DynamicContentTranslation row = new DynamicContentTranslation();
            row.setEntityType(entityType);
            row.setEntityId(entityId);
            row.setFieldName(fieldName);
            row.setSourceLocale(SOURCE_LOCALE);
            row.setTargetLocale(targetLocale);
            row.setSourceTextHash(sourceHash);
            row.setTranslatedText(translated);
            row.setTranslationProvider(result.providerName());
            row.setTranslationStatus(TranslationStatus.ACTIVE);
            try {
                translationRepository.save(row);
            } catch (DataIntegrityViolationException duplicate) {
                return translationRepository
                        .findByEntityTypeAndEntityIdAndFieldNameAndSourceLocaleAndTargetLocaleAndSourceTextHashAndTranslationStatus(
                                entityType, entityId, fieldName, SOURCE_LOCALE, targetLocale, sourceHash, TranslationStatus.ACTIVE)
                        .map(DynamicContentTranslation::getTranslatedText)
                        .orElse(translated);
            }
            metrics.generated();
            writeCache(cacheKey, translated);
            return translated;
        } catch (Exception exception) {
            metrics.providerFailure();
            log.warn("Dynamic translation failed entityType={} entityId={} fieldName={} targetLocale={}",
                    entityType, entityId, fieldName, targetLocale);
            return englishText;
        }
    }

    private String readCache(String cacheKey) {
        try {
            return redisTemplate.opsForValue().get(cacheKey);
        } catch (RedisConnectionFailureException exception) {
            log.warn("Redis unavailable while reading dynamic translation cache key={}", cacheKey);
            return null;
        }
    }

    private void writeCache(String cacheKey, String translatedText) {
        try {
            Duration ttl = properties.getCacheTtl();
            redisTemplate.opsForValue().set(cacheKey, translatedText, ttl);
        } catch (RedisConnectionFailureException exception) {
            log.warn("Redis unavailable while writing dynamic translation cache key={}", cacheKey);
        }
    }

    public String cacheKey(String entityType,
                           String entityId,
                           String fieldName,
                           String sourceLocale,
                           String targetLocale,
                           String sourceHash) {
        return "lexho:translation:%s:%s:%s:%s:%s:%s".formatted(
                entityType, entityId, fieldName, sourceLocale, targetLocale, sourceHash);
    }

    public String sourceHash(String text) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(text.getBytes(StandardCharsets.UTF_8));
            StringBuilder builder = new StringBuilder(hash.length * 2);
            for (byte value : hash) {
                builder.append(String.format("%02x", value));
            }
            return builder.toString();
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }

    private String normalize(String locale) {
        return locale == null || locale.isBlank() ? SOURCE_LOCALE : locale.trim();
    }

    private String normalizeToken(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " is required");
        }
        return value.trim().replaceAll("[^A-Za-z0-9_.-]", "_").toLowerCase(Locale.ROOT);
    }
}
